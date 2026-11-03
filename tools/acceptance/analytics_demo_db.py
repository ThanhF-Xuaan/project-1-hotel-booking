"""Explicit local-only provisioning; no business writes, no input/credential echo."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'backend/src/main/resources/ai/demo'
DB_RESOURCE = ROOT / 'backend/src/main/resources/db'
SOURCES = {'occupancy-v1': ('occupancy-history-v1.json', '8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20'),
           'group-v1': ('group-preview-v1.json', '8d569695a769c55b9bc94ec465d47063555a87917eb47b415a1341405d1994cf')}
TABLES = ('datasets','occupancy_samples','group_scenarios','group_candidates','group_quotas','group_night_eligibility')

def sql(value):
    if value is None: return 'NULL'
    if isinstance(value, bool): return 'true' if value else 'false'
    if isinstance(value, int): return str(value)
    if not isinstance(value, str): value = json.dumps(value, ensure_ascii=True, separators=(',', ':'))
    return "'" + value.replace("'", "''") + "'"

def source_payload(key, node):
    if key == 'occupancy-v1':
        return {'hotels': [{k: h[k] for k in ('hotelId','regionId','samples')} for h in node['hotels']]}
    return {'cases': sorted(node['cases'], key=lambda x: x['id'])}

def load_sources():
    result = {}
    for key, (name, sha) in SOURCES.items():
        raw = (ASSETS/name).read_bytes()
        if len(raw)>512000 or hashlib.sha256(raw).hexdigest()!=sha: raise ValueError('SOURCE_DRIFT')
        node=json.loads(raw)
        if node['dataMode']!='SYNTHETIC': raise ValueError('SOURCE_MODE')
        result[key]=node
    return result

def seed_sql(sources):
    statements=[]
    for key,node in sources.items():
        statements.append('INSERT INTO m1_demo.datasets(dataset_key,version,data_mode,reference_instant,source_sha256,content_digest,state) VALUES ('+
            ','.join(map(sql,[key,1,'SYNTHETIC',node['referenceInstant'],SOURCES[key][1],'','STAGING']))+');')
        if key=='occupancy-v1':
            values=[]
            for scenario,h in zip(('HOTEL_A','HOTEL_B'),node['hotels']):
                for r in h['samples']:
                    values.append('('+','.join(map(sql,[key,scenario,r['hotelId'],h['regionId'],r['date'],r['occupiedRooms'],r['capacityRooms'],r['coverage']]))+')')
            statements.append('INSERT INTO m1_demo.occupancy_samples(dataset_key,scenario,hotel_id,region_id,sample_date,occupied_rooms,capacity_rooms,coverage) VALUES '+','.join(values)+';')
        else:
            for case in node['cases']:
                scenario=case['id']; snap=case['snapshot']
                meta={k:v for k,v in snap.items() if k not in ('candidates','quotaRows','nightEligibility')}
                statements.append('INSERT INTO m1_demo.group_scenarios(dataset_key,scenario,request,snapshot_metadata) VALUES ('+','.join(map(sql,[key,scenario,case['request'],meta]))+');')
                specs=[('group_candidates','room_id,hotel_id,block_id,room_type_id,capacity_persons,bed_units,floor_number,eligible','candidates',('roomInstanceId','hotelId','roomBlockId','hotelRoomTypeId','capacityPersons','bedUnits','floorNumber','eligibleForEntireStay')),
                       ('group_quotas','hotel_id,contract_id,block_id,room_type_id,stay_date,room_count','quotaRows',('hotelId','contractId','roomBlockId','hotelRoomTypeId','stayDate','previewableRoomCount')),
                       ('group_night_eligibility','room_id,hotel_id,contract_id,block_id,room_type_id,stay_date,state','nightEligibility',('roomInstanceId','hotelId','contractId','roomBlockId','hotelRoomTypeId','stayDate','state'))]
                for table,columns,collection,fields in specs:
                    values=['('+','.join(map(sql,[key,scenario,i]+[r[f] for f in fields]))+')' for i,r in enumerate(snap[collection])]
                    if values: statements.append(f'INSERT INTO m1_demo.{table}(dataset_key,scenario,ordinal,{columns}) VALUES '+','.join(values)+';')
        statements.append("UPDATE m1_demo.datasets SET content_digest=encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex'),state='READY' WHERE dataset_key="+sql(key)+';')
    return '\n'.join(statements)

class LocalDatabase:
    def __init__(self, container, database):
        if not re.fullmatch(r'[a-zA-Z0-9_-]{1,80}',container): raise ValueError('TARGET_INVALID')
        self.container=container; self.database=database
        context=self.command(['docker','context','inspect'],json_output=True)[0]
        endpoint=os.environ.get('DOCKER_HOST') or context['Endpoints']['docker']['Host']
        if not endpoint.startswith(('npipe://','unix://')): raise ValueError('REMOTE_DOCKER_NOT_ALLOWED')
        data=self.command(['docker','inspect',container],json_output=True)[0]
        label=data['Config'].get('Labels') or {}
        local=data['Name']=='/hotel_postgres' and any(m.get('Name')=='project-1-hotel-booking_pg_data' for m in data['Mounts'])
        sandbox=label.get('com.docker.compose.project')=='hotel-m1-acceptance'
        if not data['State']['Running'] or not(local or sandbox): raise ValueError('TARGET_NOT_OWNED_LOCAL')
        if (local and database not in ('hotel_booking_db','m1_analytics_test')) or (sandbox and database not in ('hotel_acceptance_db','m1_analytics_test')): raise ValueError('DATABASE_NOT_ALLOWLISTED')
        env=dict(s.split('=',1) for s in data['Config']['Env'] if '=' in s)
        self.user=env.get('POSTGRES_USER','postgres'); self.password=env.get('POSTGRES_PASSWORD')
        if not self.password: raise ValueError('PROVISIONER_AUTH_UNAVAILABLE')

    @staticmethod
    def command(args, text=None, json_output=False, env=None):
        r=subprocess.run(args,input=text,text=True,encoding='utf-8',capture_output=True,env=env,timeout=120)
        if r.returncode: raise ValueError('DATABASE_OPERATION_FAILED')
        return json.loads(r.stdout) if json_output else r.stdout.strip()

    def query(self, statement):
        return self.command(['docker','exec','-i',self.container,'psql','-X','-qAt','-U',self.user,'-d',self.database,'-v','ON_ERROR_STOP=1'],statement)

    def backup(self, out):
        dump=self.command(['docker','exec',self.container,'pg_dump','-U',self.user,'-d',self.database,'--schema=m1_demo'])
        if 'PostgreSQL database dump complete' not in dump: raise ValueError('BACKUP_INCOMPLETE')
        (out/'before-m1-demo.sql').write_text(dump,encoding='utf-8')

    def migrate(self):
        self.query("CREATE SCHEMA IF NOT EXISTS m1_demo; REVOKE ALL ON SCHEMA m1_demo FROM PUBLIC;")
        env=os.environ.copy(); env['LIQUIBASE_COMMAND_PASSWORD']=self.password
        self.command(['docker','run','--rm','--network','container:'+self.container,
            '--mount',f'type=bind,source={DB_RESOURCE},target=/liquibase/demo,readonly',
            '-e','LIQUIBASE_COMMAND_PASSWORD','liquibase/liquibase:4.31-alpine',
            '--search-path=/liquibase/demo','--changelog-file=demo/m1-analytics.xml',
            '--url=jdbc:postgresql://localhost:5432/'+self.database,'--username='+self.user,
            '--liquibase-schema-name=m1_demo','update'],env=env)

def inspect(db, sources):
    raw=db.query("BEGIN READ ONLY; SET LOCAL statement_timeout='5s'; SELECT coalesce(json_agg(row_to_json(t)),'[]') FROM (SELECT dataset_key,version,data_mode,reference_instant,source_sha256,content_digest,state,encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex') actual_digest,m1_demo.dataset_payload(dataset_key) payload FROM m1_demo.datasets ORDER BY dataset_key) t; COMMIT;")
    rows=json.loads(raw)
    for row in rows:
        k=row['dataset_key']
        if k not in sources or row['version']!=1 or row['state']!='READY' or row['data_mode']!='SYNTHETIC' or row['source_sha256']!=SOURCES[k][1] or row['content_digest']!=row['actual_digest'] or row['payload']!=source_payload(k,sources[k]) or datetime.fromisoformat(row['reference_instant'])!=datetime.fromisoformat(sources[k]['referenceInstant'].replace('Z','+00:00')):
            raise ValueError('DATASET_DRIFT')
    return rows

def main():
    p=argparse.ArgumentParser(); p.add_argument('--action',choices=['seed','inspect','rollback'],default='inspect')
    p.add_argument('--container',default='hotel_postgres'); p.add_argument('--database',choices=['hotel_booking_db','hotel_acceptance_db','m1_analytics_test'],default='hotel_booking_db')
    p.add_argument('--dataset',choices=['all','occupancy-v1','group-v1'],default='all'); p.add_argument('--evidence-directory',required=True)
    p.add_argument('--new-reader',action='store_true',help='Explicitly provision a new read-only account without changing an existing account')
    args=p.parse_args(); out=Path(args.evidence_directory)
    if out.exists(): raise ValueError('EVIDENCE_EXISTS')
    out.mkdir(parents=True)
    sources=load_sources(); db=LocalDatabase(args.container,args.database)
    exists=db.query("BEGIN READ ONLY; SELECT to_regclass('m1_demo.datasets') IS NOT NULL; COMMIT;")=='t'
    schema_exists=db.query("BEGIN READ ONLY; SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname='m1_demo'); COMMIT;")=='t'
    if schema_exists:
        objects=db.query("BEGIN READ ONLY; SELECT coalesce(string_agg(tablename,',' ORDER BY tablename),'') FROM pg_tables WHERE schemaname='m1_demo'; COMMIT;")
        if set(filter(None,objects.split(','))) - set(TABLES) - {'databasechangelog','databasechangeloglock'}: raise ValueError('NAMESPACE_NOT_OWNED')
        db.backup(out)
    if args.action!='seed' and not exists: raise ValueError('DATASET_UNAVAILABLE')
    before=inspect(db,sources) if exists else []
    if args.action=='seed':
        db.migrate()
        chosen={k:v for k,v in sources.items() if (args.dataset=='all' or k==args.dataset) and k not in {r['dataset_key'] for r in before}}
        if chosen:
            statements=seed_sql(chosen)
            (out/'seed.sql').write_text(statements,encoding='utf-8')
            # Lock and recheck inside the same transaction; no upsert or destructive repair.
            db.query("BEGIN; SET LOCAL lock_timeout='5s'; SET LOCAL statement_timeout='15s'; SELECT pg_advisory_xact_lock(61102,201); LOCK TABLE m1_demo.datasets IN EXCLUSIVE MODE;"+statements+'COMMIT;')
        rows=inspect(db,sources)
        if not all(k in {r['dataset_key'] for r in rows} for k in sources if args.dataset=='all' or k==args.dataset): raise ValueError('INCOMPLETE_SEED')
        if chosen or args.new_reader:
            # Dedicated new role: never change privileges/password of an existing account.
            role='m1_demo_reader_'+secrets.token_hex(5); password=secrets.token_hex(24)
            db.query('BEGIN; CREATE ROLE '+role+' LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '+sql(password)+'; GRANT CONNECT ON DATABASE '+args.database+' TO '+role+'; GRANT USAGE ON SCHEMA m1_demo TO '+role+'; GRANT SELECT ON ALL TABLES IN SCHEMA m1_demo TO '+role+'; COMMIT;')
            runtime={'M1_DEMO_DB_USER':role,'M1_DEMO_DB_PASSWORD':password,'HOTEL_AI_DEMO_DATA_SOURCE':'db'}
            # Host URL is assigned explicitly by caller; never guessed from container networking.
            fd=os.open(out/'runtime.private.json',os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
            with os.fdopen(fd,'w',encoding='utf-8') as f: json.dump(runtime,f)
            if os.name=='nt':
                principal=os.environ['USERDOMAIN']+'\\'+os.environ['USERNAME']
                self_acl=['icacls',str(out/'runtime.private.json'),'/inheritance:r','/grant:r',principal+':(F)','/grant:r','*S-1-5-18:(F)']
                LocalDatabase.command(self_acl)
    elif args.action=='rollback':
        selected=[r for r in before if args.dataset=='all' or r['dataset_key']==args.dataset]
        guards=[]
        deletions=[]
        for row in selected:
            guards.append("DO $guard$ BEGIN IF NOT EXISTS(SELECT 1 FROM m1_demo.datasets WHERE dataset_key="+sql(row['dataset_key'])+' AND content_digest='+sql(row['content_digest'])+" AND content_digest=encode(sha256(convert_to(m1_demo.dataset_payload(dataset_key)::text,'UTF8')),'hex')) THEN RAISE EXCEPTION 'Dataset changed'; END IF; END $guard$;")
            deletions.append('DELETE FROM m1_demo.datasets WHERE dataset_key='+sql(row['dataset_key'])+';')
        db.query("BEGIN; SET LOCAL lock_timeout='5s'; SET LOCAL statement_timeout='15s'; SELECT pg_advisory_xact_lock(61102,201); LOCK TABLE m1_demo.datasets,m1_demo.occupancy_samples,m1_demo.group_scenarios,m1_demo.group_candidates,m1_demo.group_quotas,m1_demo.group_night_eligibility IN EXCLUSIVE MODE;"+''.join(guards+deletions)+'COMMIT;')
        rows=inspect(db,sources)
    else: rows=before
    count_sql='SELECT json_build_object('+','.join(sql(t)+',(SELECT count(*) FROM m1_demo.'+t+')' for t in TABLES)+');'
    counts=json.loads(db.query("BEGIN READ ONLY; SET LOCAL statement_timeout='5s';"+count_sql+'COMMIT;'))
    backup=out/'before-m1-demo.sql'
    receipt={'status':'PASS','action':args.action,'database':args.database,'dataMode':'SYNTHETIC','productionReady':False,'timestamp':datetime.now(timezone.utc).isoformat(),'datasets':[{k:v for k,v in r.items() if k!='payload'} for r in rows], 'tables':TABLES,'counts':counts,'requestedDataset':args.dataset,'schemaPreviouslyExisted':schema_exists,'backupSha256':hashlib.sha256(backup.read_bytes()).hexdigest() if backup.exists() else None}
    if args.action=='seed': receipt['runtimeRole']=role if chosen or args.new_reader else None
    (out/'receipt.json').write_text(json.dumps(receipt,indent=2),encoding='utf-8')
    print(json.dumps({'status':'PASS','action':args.action,'datasets':len(rows),'productionReady':False}))

if __name__=='__main__':
    try: main()
    except Exception as error:
        code=str(error) if isinstance(error,ValueError) and re.fullmatch('[A-Z_]{1,64}',str(error)) else 'PROVISIONING_FAILED'
        print(json.dumps({'status':'FAIL','reason':code})); sys.exit(1)
