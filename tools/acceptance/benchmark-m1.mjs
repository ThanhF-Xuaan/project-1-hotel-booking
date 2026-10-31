import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname, join } from 'node:path';
import { execFileSync, spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import os from 'node:os';
import { performance } from 'node:perf_hooks';
import { setTimeout as sleep } from 'node:timers/promises';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '../..');
const output = process.argv[process.argv.indexOf('--output') + 1];
const smoke = process.argv.includes('--smoke');
if (!output || !existsSync(output)) throw new Error('Existing evidence directory required');
const runtime = JSON.parse(readFileSync(join(here, '.runtime/env.json'), 'utf8').replace(/^\uFEFF/, ''));
if (runtime.BACKEND_PORT !== 18080 || runtime.KEYCLOAK_SERVER_URL !== 'http://localhost:18081' || !/^[a-z][a-z0-9-]{0,62}$/.test(runtime.ACCEPTANCE_VOLUME_PREFIX || '')) throw new Error('Unexpected sandbox');
const base = 'http://localhost:18080';
const image = readFileSync(join(root, 'backend/src/test/resources/ai/ocr/cccd-synthetic.png'));
const sampleHash = createHash('sha256').update(image).digest('hex');
const container = execFileSync('docker', ['ps', '--filter', 'label=com.docker.compose.project=hotel-m1-acceptance', '--filter', 'label=com.docker.compose.service=acceptance_postgres', '--format', '{{.ID}}'], {encoding:'utf8'}).trim();
if (!/^[a-f0-9]+$/.test(container)) throw new Error('Ambiguous acceptance container');
const mounts = JSON.parse(execFileSync('docker', ['inspect', container, '--format', '{{json .Mounts}}'], {encoding:'utf8'}));
if (!mounts.some(mount => mount.Destination === '/var/lib/postgresql/data' && mount.Name === `${runtime.ACCEPTANCE_VOLUME_PREFIX}-pg`)) throw new Error('Sandbox volume differs from declared fixture environment');
const digest = () => createHash('sha256').update(execFileSync('docker', ['exec',container,'pg_dump','--data-only','--no-owner','--no-privileges','-U','postgres','-d','hotel_acceptance_db'], {maxBuffer:64*1024*1024}).toString('utf8').replace(/^\\(?:un)?restrict\s+\S+\r?$/gm,'')).digest('hex');
const before = digest();
const dates = JSON.parse(execFileSync('docker',['exec',container,'psql','-U','postgres','-d','hotel_acceptance_db','-Atc',"SELECT json_build_object('checkIn',((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+7)::text,'checkOut',((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+9)::text)"],{encoding:'utf8'}));
const hardware = {os:os.type(),release:os.release(),cpu:os.cpus()[0]?.model,logicalCpus:os.cpus().length,memoryGiB:Math.round(os.totalmem()/2**30),node:process.version,jdk:spawnSync('java',['-version'],{encoding:'utf8'}).stderr.trim()};
writeFileSync(join(output,'method-freeze.json'), JSON.stringify({recordedBeforeRequests:true,
  scriptSha256:createHash('sha256').update(readFileSync(fileURLToPath(import.meta.url))).digest('hex'),
  sampleHash,smoke,rounds:smoke?1:2,warmup:smoke?0:20,measuredPerCell:smoke?1:100,
  concurrency:smoke?[1]:[1,5],p95MustBeBelowMs:2000,successRateMinimum:.99,
  allMeasuredFailuresIncluded:true,retries:0,hardware},null,2));
const rows = [];
const summaries = [];
async function token() {
  // These are public synthetic fixture credentials from the existing auth helper.
  const response = await fetch('http://localhost:18081/realms/hotel-acceptance/protocol/openid-connect/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'password',client_id:'hotel-frontend',username:'reception_h1',password:'Password123!'}),signal:AbortSignal.timeout(10000)});
  if (!response.ok) throw new Error(`Synthetic token setup failed: HTTP ${response.status}`);
  return (await response.json()).access_token;
}
let receptionToken;
let lastAdvisorBatch = 0;
async function request(kind) {
  const start = performance.now();
  let http = 0, ok = false, error = null;
  try {
    let path, body, headers = {};
    if (kind === 'advisor') {
      path='/api/v1/public/ai/room-advisor/search';
      body=JSON.stringify({contractVersion:'2.0',criteria:{hotelId:1,locationKeyword:'Hà Nội',checkInDate:dates.checkIn,checkOutDate:dates.checkOut,adults:2,children:0,roomsCount:1}});
      headers['Content-Type']='application/json';
    } else if (kind === 'faq') {
      path='/api/v1/public/ai/room-advisor/knowledge/ask';
      body=JSON.stringify({hotelId:1,question:'Giờ nhận phòng là mấy giờ?',language:'vi',topK:5});
      headers['Content-Type']='application/json';
    } else {
      path='/api/v1/ai/ocr/extract-file';
      body=new FormData();body.append('file',new Blob([image],{type:'image/png'}),'cccd-synthetic.png');
      headers.Authorization=`Bearer ${receptionToken}`;
    }
    const response = await fetch(base+path,{method:'POST',headers,body,signal:AbortSignal.timeout(10000)});
    http=response.status;
    const result=(await response.json()).result;
    ok=response.ok && (kind==='advisor' ? result?.status==='RESULTS' && result.recommendations?.length>0 && result.recommendations.length<=5 : kind==='faq' ? result?.status==='GROUNDED_ANSWER' && result.answer.includes('14:00') && result.citations?.length>0 : result?.requiresHumanVerification===true && result.fullName?.value==='NGUYEN VAN TEST' && result.documentNumber?.value==='079190012345' && result.dateOfBirth?.value==='1990-06-15');
    if (!ok) error=`HTTP_${http}_OR_ORACLE_MISMATCH`;
  } catch (e) {error=e.name==='TimeoutError'?'TIMEOUT':'REQUEST_OR_PARSE_ERROR';}
  return {milliseconds:performance.now()-start,http,ok,error};
}
const percentile=(values,p)=>values[Math.max(0,Math.ceil(p*values.length)-1)];
if (!smoke) {
  console.log('Allowing the existing 60-second limiter window to expire after adversarial browser tests.');
  await sleep(30000);
  await sleep(30000);
}
for (let round=1; round<=(smoke?1:2); round++) {
  for (const kind of ['advisor','faq','ocr']) {
    for (const concurrency of (smoke?[1]:[1,5])) {
      if (kind==='ocr') receptionToken=await token();
      const measured=[];
      const warmup=smoke?0:20;
      const count=smoke?1:100;
      for (const [phase,total] of [['warmup',warmup],['measured',count]]) {
        for (let offset=0; offset<total; offset+=concurrency) {
          const batch=Math.min(concurrency,total-offset);
          // Cold anonymous sessions; no reused cookie. REAL source IP stays below 60/min.
          // Pacing is outside latency: batches of five every 5.5s at concurrency five.
          if (kind==='advisor') {await sleep(Math.max(0,lastAdvisorBatch+concurrency*1100-performance.now()));lastAdvisorBatch=performance.now();}
          const samples=await Promise.all(Array.from({length:batch},()=>request(kind)));
          samples.forEach((sample,i)=>{const row={round,kind,concurrency,phase,index:offset+i,...sample};rows.push(row);if(phase==='measured')measured.push(row);});
        }
      }
      const sorted=measured.map(x=>x.milliseconds).sort((a,b)=>a-b);
      const successes=measured.filter(x=>x.ok).length;
      const summary={round,kind,concurrency,measured:count,successes,errors:count-successes,timeouts:measured.filter(x=>x.error==='TIMEOUT').length,successRate:successes/count,p50:percentile(sorted,.5),p95:percentile(sorted,.95),p99:percentile(sorted,.99)};
      summary.pass=summary.p95<2000 && summary.successRate>=.99;
      summaries.push(summary);
      writeFileSync(join(output,'samples.json'),JSON.stringify(rows,null,2));
      console.log(JSON.stringify(summary));
    }
  }
}
const after=digest();
const report={status:summaries.every(x=>x.pass)&&before===after?'PASS':'FAIL',smoke,base:execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),hardware,profile:{provider:'offline deterministic; no Gemini',rag:'in-memory hash256 synthetic demo plus API/browser test policies',ocr:'Tesseract vie+eng max2 processes, original quality/deadline',advisor:'cold anonymous sessions; real IP paced <60/min; no limiter bypass/retry; initial 60s washout',warmup:smoke?0:20,measuredPerCell:smoke?1:100,rounds:smoke?1:2,timeoutMs:10000,percentile:'nearest rank over all measured responses including failures',sampleHash},businessDatabaseUnchanged:before===after,before,after,summaries};
writeFileSync(join(output,'result.json'),JSON.stringify(report,null,2));
console.log(`Benchmark ${report.status}; business DB unchanged=${report.businessDatabaseUnchanged}`);
if(report.status!=='PASS')process.exitCode=1;
