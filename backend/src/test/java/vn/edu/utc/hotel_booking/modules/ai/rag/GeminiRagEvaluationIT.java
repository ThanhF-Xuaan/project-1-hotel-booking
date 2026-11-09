package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Network-free audit of reviewed captures. Does not manufacture live traces or semantic judgments. */
class GeminiRagEvaluationIT {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DATA = Path.of("../eval/t12/r2/gemini-v1");
    private static final List<String> REPEATS = java.util.stream.IntStream.rangeClosed(1,10)
            .mapToObj(i -> String.format(Locale.ROOT,"ACC-%03d",i)).toList();

    @Test void immutableDraftHasFortyCasesAndSeparateDevelopmentSplit() throws Exception {
        var cases = cases(); assertEquals(40,cases.size()); assertEquals(10,REPEATS.size());
        var manifest = JSON.readTree(DATA.resolve("manifest.json").toFile());
        for (var files = manifest.path("fileHashes").fields(); files.hasNext();) {
            var item = files.next(); assertEquals(item.getValue().path("sha256").asText(),hash(DATA.resolve(item.getKey())),"Frozen artifact hash mismatch");
        }
        assertEquals(20,Files.readAllLines(DATA.resolve("dev-cases.jsonl")).size());
        for(var item:cases.values()) {
            assertFalse(item.path("scoring").path("exactAnswerTextRequired").asBoolean());
            assertTrue(manifest.path("scenarios").has(item.path("scenarioId").asText()),"Scenario missing");
        }
    }

    @Test void fallbackCannotPassAsGeneratedAndCitationCannotReplaceRetrieval() throws Exception {
        var expected=cases().get("ACC-001");
        var sample=JSON.readTree("""
          {"httpStatus":200,"apiCode":200,"resultStatus":"GROUNDED_ANSWER","answer":"Thông tin synthetic",
           "businessWriteCalls":0,"generatorCalls":1,"generationOutcome":"EXTRACTIVE_FALLBACK",
           "durationMillis":10,"liveMeasured":true,"retrievedEvidence":[],"citationEvidence":["H1-CHECKIN-v2"],
           "warnings":["GENERATION_FALLBACK_EXTRACTIVE"],"retryAfterSeconds":null}
          """);
        assertFalse(check(expected,sample).errors().isEmpty());
        assertEquals(0,check(expected,sample).recall());
    }

    @Test void percentileUsesNearestRankAndNeverTreatsAnEmptySampleAsZeroLatency() {
        assertEquals(95,p95(java.util.stream.LongStream.rangeClosed(1,100).boxed().toList()));
        assertThrows(IllegalArgumentException.class,()->p95(List.of()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"valid","foreign-citation","missing-trace","write","wrong-status","old-version"})
    void scorerRejectsCriticalTraceDefectsWithoutExactTextMatching(String fault) throws Exception {
        var sample=(com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree("""
          {"httpStatus":200,"apiCode":200,"resultStatus":"GROUNDED_ANSWER","answer":"Diễn đạt synthetic khác câu gold",
           "businessWriteCalls":0,"generatorCalls":1,"generationOutcome":"GENERATED","durationMillis":10,
           "retrievedEvidence":["H1-CHECKIN-v2"],"citationEvidence":["H1-CHECKIN-v2"],"warnings":[],"retryAfterSeconds":null}
          """);
        switch(fault){
            case "foreign-citation" -> sample.set("citationEvidence",JSON.readTree("[\"H2-CHECKIN-v1\"]"));
            case "missing-trace" -> sample.remove("retrievedEvidence");
            case "write" -> sample.put("businessWriteCalls",1);
            case "wrong-status" -> sample.put("resultStatus","OUT_OF_SCOPE");
            case "old-version" -> sample.set("citationEvidence",JSON.readTree("[\"H1-CHECKIN-v1\"]"));
            default -> { }
        }
        var result=check(cases().get("ACC-001"),sample);
        assertEquals("valid".equals(fault),result.errors().isEmpty());
    }

    @Test
    @EnabledIfEnvironmentVariable(named="T12_RAG_GEMINI_EVAL_REPLAY",matches="true")
    void evaluateLockedHumanReviewedCaptureWithoutCallingAnyProvider() throws Exception {
        try { auditCapture(); }
        catch(Exception | AssertionError failure) { throw new AssertionError("Evaluation capture audit failed; inspect private inputs, not provider bodies"); }
    }

    private void auditCapture() throws Exception {
        Path capture=required("T12_RAG_EVAL_CAPTURE"), approval=required("T12_RAG_EVAL_APPROVAL");
        requireHash(capture,"T12_RAG_EVAL_CAPTURE_SHA256"); requireHash(approval,"T12_RAG_EVAL_APPROVAL_SHA256");
        JsonNode bundle=read(capture), signed=read(approval);
        assertEquals("rag-eval-capture-v1",bundle.path("schemaVersion").asText());
        assertEquals(hash(capture),signed.path("captureSha256").asText());
        for(String gate:List.of("G04","G05","G06","G07","G09")) {
            var record=signed.path("gates").path(gate);assertEquals("APPROVED",record.path("status").asText(),"Missing owner decision");
            assertFalse(record.path("reviewer").asText().isBlank());assertFalse(record.path("reference").asText().isBlank());
        }
        assertEquals("APPROVED_UNCHANGED_PROPOSED_THRESHOLDS",signed.path("thresholdDecision").asText());
        for(String name:List.of("manifest.json","corpus.json","acceptance-cases.jsonl","dev-cases.jsonl"))
            assertEquals(hash(DATA.resolve(name)),bundle.path("artifactHashes").path(name).asText(),"Artifact changed");
        for(String name:List.of("gitSha","dirtyHashesSha256","dependencyTreeSha256","chatModel","embeddingModel",
                "promptSha256","configurationSha256","traceSha256","calibrationSha256","ledgerSha256","persistentReceiptSha256"))
            assertFalse(bundle.path("provenance").path(name).asText().isBlank(),"Missing reproducibility metadata");
        assertTrue(bundle.path("provenance").path("dimension").asInt()>0);
        // External artifacts must be actual files and independently hash-locked, not labels in a report.
        for(String name:List.of("trace","calibration","ledger","persistentReceipt","dirtyHashes","dependencyTree","prompt","configuration")) {
            Path file=Path.of(bundle.path("files").path(name).asText());assertTrue(file.isAbsolute());
            assertEquals(hash(file),bundle.path("provenance").path(name+"Sha256").asText(),"Evidence artifact mismatch");
        }
        var persistent=read(Path.of(bundle.path("files").path("persistentReceipt").asText()));
        assertEquals("PASS_TECHNICAL_ONLY",persistent.path("status").asText());
        var calibration=read(Path.of(bundle.path("files").path("calibration").asText()));
        assertEquals(bundle.path("provenance").path("embeddingModel").asText(),calibration.path("embeddingModel").asText());
        assertEquals(bundle.path("provenance").path("dimension").asInt(),calibration.path("dimension").asInt());
        assertEquals(hash(DATA.resolve("corpus.json")),calibration.path("corpusSha256").asText());
        assertEquals(hash(DATA.resolve("dev-cases.jsonl")),calibration.path("devSha256").asText());
        double threshold=calibration.path("selectedThreshold").asDouble(-1);assertTrue(List.of(.3,.4,.5,.6).contains(threshold));
        assertEquals(threshold,bundle.path("retrievalThreshold").asDouble(-1));
        var ledgerSummary=checkLedger(Path.of(bundle.path("files").path("ledger").asText()));
        var traces=read(Path.of(bundle.path("files").path("trace").asText()));
        Map<String,JsonNode> traceByKey=new HashMap<>();
        for(var trace:traces){String key=trace.path("caseId").asText()+":"+trace.path("iteration").asInt(-1);assertNull(traceByKey.put(key,trace));}
        var expected=cases(); var seen=new HashSet<String>();var failed=new TreeSet<String>();var latencies=new ArrayList<Long>();
        double recall=0;int eligible=0,quality=0,qualityEligible=0,generated=0,fallback=0,live=0;
        assertEquals(60,bundle.path("samples").size(),"40 originals plus 10 cases repeated two additional times required");
        for(var sample:bundle.path("samples")) {
            String id=sample.path("caseId").asText();int iteration=sample.path("iteration").asInt(-1);
            assertTrue(expected.containsKey(id));assertTrue(iteration==0 || (REPEATS.contains(id) && (iteration==1||iteration==2)));
            String key=id+":"+iteration;assertTrue(seen.add(key),"Duplicate sample");
            assertTrue(sample.equals(traceByKey.get(key)),"Capture sample does not match independent locked trace");
            var spec=expected.get(id);var verdict=check(spec,sample);if(!verdict.errors.isEmpty())failed.add(key);
            if(iteration==0 && spec.path("scoring").path("retrievalEligible").asBoolean()){recall+=verdict.recall;eligible++;}
            var review=signed.path("semanticReviews").path(key);
            assertEquals(hashBytes(JSON.writeValueAsBytes(sample)),review.path("sampleSha256").asText(),"Review not bound to exact sample");
            assertFalse(review.path("reviewer").asText().isBlank());assertFalse(review.path("rationale").asText().isBlank());
            if(!review.path("factsSupported").asBoolean() || !review.path("noInventedClaims").asBoolean()) failed.add(key);
            if(iteration==0 && spec.path("scoring").path("answerQualityEligible").asBoolean()){
                qualityEligible++;if(review.path("correctAndComplete").asBoolean() && review.path("factsSupported").asBoolean()
                    && review.path("noInventedClaims").asBoolean() && "GENERATED".equals(sample.path("generationOutcome").asText())
                    && !strings(sample.path("warnings")).contains("GENERATION_FALLBACK_EXTRACTIVE"))quality++;
            }
            if("baseline".equals(spec.path("scenarioId").asText()) && "GENERATED".equals(spec.path("expected").path("generationOutcome").asText()))
                assertTrue(sample.path("liveMeasured").asBoolean(),"Baseline generation must be observed live");
            if(sample.path("liveMeasured").asBoolean()){
                assertEquals("baseline",spec.path("scenarioId").asText());assertEquals("HTTP_END_TO_END",sample.path("measurement").asText());
                assertTrue(List.of("cold","warm").contains(sample.path("thermalState").asText()));live++;latencies.add(sample.path("durationMillis").asLong());
            }
            if("GENERATED".equals(sample.path("generationOutcome").asText()))generated++;
            if("EXTRACTIVE_FALLBACK".equals(sample.path("generationOutcome").asText()))fallback++;
        }
        for(String id:expected.keySet())assertTrue(seen.contains(id+":0"),"Missing base case");
        for(String id:REPEATS)for(int n=1;n<=2;n++)assertTrue(seen.contains(id+":"+n));
        assertTrue(eligible>0 && qualityEligible>0 && live>0);
        boolean pass=failed.isEmpty() && recall/eligible>=.9 && (double)quality/qualityEligible>=.9 && p95(latencies)<=12000;
        Map<String,Object> report=new LinkedHashMap<>();report.put("status",pass?"PASS_REVIEWED_SYNTHETIC_CAPTURE_ONLY":"FAIL");
        report.put("captureSha256",hash(capture));report.put("approvalSha256",hash(approval));report.put("sampleCount",seen.size());
        report.put("failedSamples",failed);report.put("retrievalRecallAt5",recall/eligible);report.put("reviewedQuality",(double)quality/qualityEligible);
        report.put("liveMeasuredSamples",live);report.put("p95Millis",p95(latencies));report.put("generated",generated);report.put("extractiveFallback",fallback);
        report.put("usageAndCost",ledgerSummary);
        var variability=new LinkedHashMap<String,List<Object>>();
        for(String id:REPEATS){var observations=new ArrayList<Object>();for(var sample:bundle.path("samples"))if(id.equals(sample.path("caseId").asText()))
            observations.add(Map.of("iteration",sample.path("iteration").asInt(),"answerSha256",hashBytes(sample.path("answer").asText().getBytes(StandardCharsets.UTF_8)),
                "outcome",sample.path("generationOutcome").asText(),"durationMillis",sample.path("durationMillis").asLong()));variability.put(id,observations);}
        report.put("repeatObservations",variability);
        var roundLatency=new LinkedHashMap<Integer,Long>();
        for(int round=0;round<=2;round++){
            var timings=new ArrayList<Long>();for(var sample:bundle.path("samples"))
                if(sample.path("iteration").asInt(-1)==round && sample.path("liveMeasured").asBoolean())timings.add(sample.path("durationMillis").asLong());
            if(!timings.isEmpty())roundLatency.put(round,p95(timings));
        }
        report.put("p95MillisByIteration",roundLatency);
        requireHash(capture,"T12_RAG_EVAL_CAPTURE_SHA256");requireHash(approval,"T12_RAG_EVAL_APPROVAL_SHA256");
        for(String name:List.of("trace","calibration","ledger","persistentReceipt","dirtyHashes","dependencyTree","prompt","configuration"))
            assertEquals(hash(Path.of(bundle.path("files").path(name).asText())),bundle.path("provenance").path(name+"Sha256").asText(),"Evidence changed during audit");
        report.put("limitation","Read-only audit; capture provenance and human decisions remain external trust boundaries. No live requests executed by this auditor.");
        Path output=Path.of("target/rag-gemini-evaluation-"+UUID.randomUUID()+".json");Files.createDirectories(output.getParent());
        Files.writeString(output,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report),StandardOpenOption.CREATE_NEW);
        assertTrue(pass,"Acceptance failed; retain all failed samples and immutable expectations");
    }

    private record Verdict(List<String> errors,double recall) { }
    private static Verdict check(JsonNode spec,JsonNode actual) {
        List<String> errors=new ArrayList<>();var expected=spec.path("expected");
        for(String field:List.of("httpStatus","apiCode","resultStatus","generationOutcome"))
            if(!expected.path(field).equals(actual.path(field)))errors.add(field);
        if(!actual.path("businessWriteCalls").isIntegralNumber() || actual.path("businessWriteCalls").asInt()!=0)errors.add("businessWrite");
        int calls=actual.path("generatorCalls").asInt(-1);
        if(!actual.path("generatorCalls").isIntegralNumber() || calls<expected.path("generatorCalls").path("min").asInt()||calls>expected.path("generatorCalls").path("max").asInt())errors.add("generatorCalls");
        Set<String> retrieved=strings(actual.path("retrievedEvidence")),cited=strings(actual.path("citationEvidence"));
        Set<String> required=strings(expected.path("requiredEvidence")),allowed=strings(expected.path("allowedEvidence"));
        if(!actual.path("retrievedEvidence").isArray() || actual.path("retrievedEvidence").size()>5)errors.add("retrievalTrace");
        if(!actual.path("citationEvidence").isArray() || !allowed.containsAll(cited)||!retrieved.containsAll(cited))errors.add("citation");
        if("GROUNDED_ANSWER".equals(actual.path("resultStatus").asText()) && cited.isEmpty())errors.add("missingCitation");
        Set<String> warnings=strings(actual.path("warnings"));
        if(!warnings.containsAll(strings(expected.path("requiredWarnings"))))errors.add("warning");
        if("GENERATED".equals(actual.path("generationOutcome").asText()) && warnings.contains("GENERATION_FALLBACK_EXTRACTIVE"))errors.add("falseGenerationSuccess");
        if(!expected.path("retryAfterSeconds").isNull() && !expected.path("retryAfterSeconds").equals(actual.path("retryAfterSeconds")))errors.add("retryAfter");
        String answer=actual.path("answer").asText();
        if(!answer.equals(PiiRedactionUtil.redact(answer)))errors.add("pii");
        for(var fragment:expected.path("forbiddenAnswerFragments"))if(answer.contains(fragment.asText()))errors.add("forbiddenFragment");
        if(!actual.path("durationMillis").isIntegralNumber() || actual.path("durationMillis").asLong()<0)errors.add("latencyMissing");
        double recall=required.isEmpty()?0:(double)required.stream().filter(retrieved::contains).count()/required.size();
        return new Verdict(errors,recall);
    }

    private static Map<String,Object> checkLedger(Path path) throws Exception {
        String previous="0".repeat(64);var pending=new HashMap<String,java.math.BigDecimal>();
        java.math.BigDecimal spent=java.math.BigDecimal.ZERO;int inputs=0,calls=0;long tokens=0;
        String content=Files.readString(path);assertTrue(!content.isEmpty() && content.endsWith("\n"));
        for(String line:content.lines().toList()) {
            var wrapper=JSON.readTree(line);String event=wrapper.path("event").asText();
            assertEquals(previous,wrapper.path("previous").asText());assertEquals(hashBytes((previous+"\n"+event).getBytes(StandardCharsets.UTF_8)),wrapper.path("hash").asText());
            var row=JSON.readTree(event);String id=row.path("id").asText();
            if("RESERVE".equals(row.path("type").asText())){var cost=new java.math.BigDecimal(row.path("maxCost").asText());assertTrue(cost.signum()>=0);assertTrue(row.path("inputs").isIntegralNumber() && row.path("inputs").asInt()>=0 && row.path("generations").isIntegralNumber() && row.path("generations").asInt()>=0);assertNull(pending.put(id,cost));spent=spent.add(cost);inputs+=row.path("inputs").asInt();calls+=row.path("generations").asInt();}
            else if("SETTLE".equals(row.path("type").asText())){var max=pending.remove(id);assertNotNull(max);var cost=new java.math.BigDecimal(row.path("actualCost").asText());assertTrue(cost.signum()>=0 && cost.compareTo(max)<=0);spent=spent.subtract(max).add(cost);assertTrue(row.path("inputTokens").asLong(-1)>=0 && row.path("totalTokens").asLong(-1)>=0);tokens+=row.path("totalTokens").asLong();}
            else fail("Ledger halted or invalid");
            assertTrue(inputs<=200 && calls<=120 && spent.compareTo(new java.math.BigDecimal("5"))<=0);previous=wrapper.path("hash").asText();
        }
        assertTrue(pending.isEmpty(),"Unresolved paid reservations");
        return Map.of("embeddingInputs",inputs,"generationCalls",calls,"totalTokens",tokens,"chargedUsd",spent.toPlainString());
    }
    private static Map<String,JsonNode> cases() throws Exception {var result=new LinkedHashMap<String,JsonNode>();for(String line:Files.readAllLines(DATA.resolve("acceptance-cases.jsonl"))){var row=JSON.readTree(line);assertNull(result.put(row.path("id").asText(),row));}return result;}
    private static Set<String> strings(JsonNode array){Set<String> result=new HashSet<>();array.forEach(item->result.add(item.asText()));return result;}
    private static long p95(List<Long> values){if(values.isEmpty())throw new IllegalArgumentException("No live latency samples");var sorted=values.stream().sorted().toList();return sorted.get((int)Math.ceil(sorted.size()*.95)-1);}
    private static Path required(String env){String value=System.getenv(env);assertNotNull(value,"Missing evaluation artifact path");Path p=Path.of(value);assertTrue(p.isAbsolute());return p;}
    private static JsonNode read(Path p)throws Exception{assertTrue(Files.size(p)<=8_388_608);return JSON.readTree(p.toFile());}
    private static void requireHash(Path p,String env)throws Exception{assertEquals(System.getenv(env),hash(p),"Unapproved evaluation artifact bytes");}
    private static String hash(Path p)throws Exception{return hashBytes(Files.readAllBytes(p));}
    private static String hashBytes(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
}
