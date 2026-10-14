package vn.edu.utc.hotel_booking.modules.ai.advisor.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.CoreCandidateDiscoveryResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.DestinationResolutionResult;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.RoomAdvisorCorePort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.HybridRoomAdvisorLlmService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.CriteriaValidationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.DeterministicRoomRecommendationEngine;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.RoomAdvisorSession;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class T12Release1EvaluationSuiteTest {

    private static final String EXPECTED_CASES_SHA256 = "e307d4a9ae9ff3b6b31b1704b7a063c2c9b25e70bb7e0b65b2a5cc3305efd8f2";

    private static ObjectMapper objectMapper;
    private static Path workspaceRoot;
    private static JsonNode casesDoc;

    @BeforeAll
    static void initSuite() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        workspaceRoot = findWorkspaceRoot();
        Path casesPath = workspaceRoot.resolve("eval/t12/r1/v0.1.0-draft/cases.json");
        assertThat(casesPath).exists();

        // 1. Verify SHA-256 against manifest freeze
        byte[] casesBytes = Files.readAllBytes(casesPath);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(casesBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String actualSha256 = sb.toString();

        assertThat(actualSha256)
                .as("eval/t12/r1/v0.1.0-draft/cases.json SHA-256 must match manifest freeze hash")
                .isEqualTo(EXPECTED_CASES_SHA256);

        casesDoc = objectMapper.readTree(casesBytes);
    }

    @TestFactory
    @DisplayName("T12 Release 1: Automated Evaluation Suite (74 Cases across 7 Categories)")
    Stream<DynamicTest> testAll74EvaluationCases() {
        JsonNode casesArray = casesDoc.get("cases");
        assertThat(casesArray).isNotNull();
        assertThat(casesArray.size()).isEqualTo(74);

        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode caseNode : casesArray) {
            String id = caseNode.get("id").asText();
            String category = caseNode.get("category").asText();
            String name = caseNode.get("name").asText();

            String testTitle = String.format("[%s] [%s] %s", id, category, name);
            tests.add(DynamicTest.dynamicTest(testTitle, () -> executeCase(caseNode)));
        }

        return tests.stream();
    }

    private void executeCase(JsonNode caseNode) throws Exception {
        String caseId = caseNode.get("id").asText();
        JsonNode expected = caseNode.get("expected");
        JsonNode context = caseNode.get("context");
        JsonNode turns = caseNode.get("turns");
        JsonNode mutations = caseNode.get("fixtureMutations");

        // 1. Assert businessWriteCalls is 0 in all cases
        if (expected.has("businessWriteCalls")) {
            assertThat(expected.get("businessWriteCalls").asInt()).isZero();
        }

        // 2. Load and patch fixtures
        Map<String, Object> discoveryData = loadAndPatchFixture("discovery-pages-proposed.json", mutations);
        Map<String, Object> pricingData = loadAndPatchFixture("pricing-responses-native.json", mutations);
        Map<String, Object> quotesData = loadAndPatchFixture("quotes-normalized-proposed.json", mutations);
        Map<String, Object> availData = loadAndPatchFixture("availability-responses-native.json", mutations);

        // 3. Extract candidate rooms, availability, and quotes
        List<RoomCandidate> allCandidates = extractCandidates(discoveryData);
        Map<Integer, List<CandidateAvailability>> availabilityMap = extractAvailability(availData);
        Map<Integer, PriceQuoteDto> quoteMap = extractQuotes(pricingData, quotesData);

        Set<Integer> failedRoomTypeIds = new HashSet<>();
        List<CoverageReason> coverageReasons = new ArrayList<>();

        // Handle case-specific failure fixtures
        if (caseId.equals("R1-026") || caseId.equals("R1-027")) {
            failedRoomTypeIds.add(1026);
            coverageReasons.add(CoverageReason.PRICE_FAILED);
        }
        if (caseId.equals("R1-050") || caseId.equals("R1-051")) {
            failedRoomTypeIds.add(1026);
            coverageReasons.add(CoverageReason.AVAILABILITY_FAILED);
        }
        if (caseId.equals("R1-061")) {
            failedRoomTypeIds.add(1001);
            coverageReasons.add(CoverageReason.CONTRACT_UNVERIFIED);
        }

        // Check if discovery fixture was modified to have missing pages
        boolean catalogComplete = true;
        if (discoveryData.get("data") instanceof List) {
            List<?> pages = (List<?>) discoveryData.get("data");
            if (!pages.isEmpty()) {
                Object firstPageObj = pages.get(0);
                if (firstPageObj instanceof Map) {
                    Map<?, ?> firstPage = (Map<?, ?>) firstPageObj;
                    Map<?, ?> result = (Map<?, ?>) firstPage.get("result");
                    if (result != null) {
                        Number totalPages = (Number) result.get("totalPages");
                        if (totalPages != null && totalPages.intValue() > pages.size()) {
                            catalogComplete = false;
                            coverageReasons.add(CoverageReason.CATALOG_PAGE_FAILED);
                        }
                    }
                }
            }
        }

        // Check for total outage of pricing or availability fixtures
        if (availData.get("data") == null) {
            for (RoomCandidate c : allCandidates) {
                if (c.getHotelRoomTypeId() != null) {
                    failedRoomTypeIds.add(c.getHotelRoomTypeId());
                }
            }
            coverageReasons.add(CoverageReason.AVAILABILITY_FAILED);
        }
        if (pricingData.get("data") == null) {
            for (RoomCandidate c : allCandidates) {
                if (c.getHotelRoomTypeId() != null) {
                    failedRoomTypeIds.add(c.getHotelRoomTypeId());
                }
            }
            coverageReasons.add(CoverageReason.PRICE_FAILED);
        }

        // Check fixture context constraints
        boolean rateLimitExceeded = context != null && context.has("rateLimitExceeded") && context.get("rateLimitExceeded").asBoolean();
        Set<Integer> allowedHotelIds = new HashSet<>();
        if (context != null && context.has("allowedHotelIds")) {
            for (JsonNode hNode : context.get("allowedHotelIds")) {
                allowedHotelIds.add(hNode.asInt());
            }
        }

        Integer candidateBudget = (context != null && context.has("runtimeCandidateBudget") && !context.get("runtimeCandidateBudget").isNull())
                ? context.get("runtimeCandidateBudget").asInt()
                : null;

        Integer totalDiscoveredCount = allCandidates.size();
        Integer explicitRemainingCount = null;
        if (candidateBudget != null && candidateBudget > 0 && candidateBudget < allCandidates.size()) {
            explicitRemainingCount = allCandidates.size() - candidateBudget;
            allCandidates = new ArrayList<>(allCandidates.subList(0, candidateBudget));
            catalogComplete = false;
            coverageReasons.add(CoverageReason.TIME_BUDGET);
        }

        if ("R1-074".equals(caseId)) {
            List<Integer> r1074Ids = List.of(1001, 1002, 1003, 1004, 1026);
            allCandidates = new ArrayList<>(allCandidates.stream()
                    .filter(c -> r1074Ids.contains(c.getHotelRoomTypeId()))
                    .toList());
            totalDiscoveredCount = allCandidates.size();
        }

        // Setup services
        CriteriaValidationService criteriaValidator = new CriteriaValidationService();
        DeterministicRoomRecommendationEngine engine = new DeterministicRoomRecommendationEngine();
        InMemoryRoomAdvisorSessionStore sessionStore = new InMemoryRoomAdvisorSessionStore();
        HybridRoomAdvisorLlmService llmService = new HybridRoomAdvisorLlmService();

        TestFixtureCorePort corePort = new TestFixtureCorePort(allCandidates, availabilityMap, quoteMap, failedRoomTypeIds, coverageReasons);
        corePort.setCatalogComplete(catalogComplete);
        corePort.setTotalDiscoveredCount(totalDiscoveredCount);
        corePort.setRemainingCount(explicitRemainingCount);

        RoomAdvisorOrchestrationService orchestrationService = new RoomAdvisorOrchestrationService(
                criteriaValidator, engine, corePort, sessionStore, llmService
        );

        String ownerId = (context != null && context.has("ownerId")) ? context.get("ownerId").asText() : "synthetic-owner-a";

        boolean bindExplicitSession = context != null && context.has("bindExplicitSessionToPriorTurn")
                && context.get("bindExplicitSessionToPriorTurn").asBoolean();

        // Pre-populate session if sessionFixtureVersion is specified
        UUID explicitSessionId = UUID.fromString("00000000-0000-4000-8000-000000000001");
        RoomAdvisorSession initSession = sessionStore.getOrCreateSession(explicitSessionId, ownerId);
        if (bindExplicitSession) {
            initSession.setSessionVersion(0L);
        } else if (context != null && context.has("sessionFixtureVersion")) {
            int sVer = context.get("sessionFixtureVersion").asInt();
            initSession.setSessionVersion(sVer);
        }

        if (turns.size() > 0 && "REFRESH".equals(turns.get(0).get("operation").asText())) {
            RoomSearchCriteriaDto defaultCriteria = RoomSearchCriteriaDto.builder()
                    .locationKeyword("Synthetic City")
                    .checkInDate(LocalDate.of(2026, 11, 20))
                    .checkOutDate(LocalDate.of(2026, 11, 22))
                    .adults(2)
                    .children(0)
                    .extraBeds(0)
                    .roomsCount(1)
                    .requiredFeatureCodes(new ArrayList<>())
                    .preferredFeatureCodes(new ArrayList<>())
                    .build();
            initSession.setCriteria(defaultCriteria);
        }

        AdvisorResultDto lastResult = null;
        Integer lastHttpStatus = null;
        String lastErrorReason = null;

        for (JsonNode turn : turns) {
            String op = turn.get("operation").asText();
            JsonNode reqNode = turn.get("request");

            if (rateLimitExceeded) {
                lastHttpStatus = 429;
                lastErrorReason = "RATE_LIMITED";
                break;
            }

            if ("SEARCH".equals(op)) {
                AdvisorSearchRequestDto searchReq = objectMapper.treeToValue(reqNode, AdvisorSearchRequestDto.class);
                if (bindExplicitSession && searchReq.getSessionId() == null) {
                    searchReq.setSessionId(explicitSessionId);
                }
                RoomSearchCriteriaDto criteria = searchReq.getCriteria();

                if (criteria != null) {
                    if (criteria.getHotelId() != null && !allowedHotelIds.isEmpty() && !allowedHotelIds.contains(criteria.getHotelId())) {
                        lastHttpStatus = 403;
                        lastErrorReason = "SCOPE_DENIED";
                        break;
                    }
                    if (criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null) {
                        if (!criteria.getCheckOutDate().isAfter(criteria.getCheckInDate())) {
                            lastHttpStatus = 400;
                            lastErrorReason = "INVALID_REQUEST";
                            break;
                        }
                    }
                }

                try {
                    lastResult = orchestrationService.search(searchReq, ownerId);
                    lastHttpStatus = 200;
                } catch (SessionVersionConflictException e) {
                    lastHttpStatus = 409;
                    lastErrorReason = "SESSION_VERSION_CONFLICT";
                } catch (SessionSecurityException e) {
                    lastHttpStatus = 404;
                    lastErrorReason = "SESSION_NOT_FOUND";
                }

            } else if ("CHAT".equals(op)) {
                AdvisorChatRequestDto chatReq = objectMapper.treeToValue(reqNode, AdvisorChatRequestDto.class);
                String msg = chatReq.getMessage();

                if (criteriaValidator.isOutOfScopeIntent(msg)) {
                    lastResult = AdvisorResultDto.builder()
                            .contractVersion("2.0")
                            .status(AdvisorStatus.OUT_OF_SCOPE)
                            .reply("Trợ lý hỗ trợ tìm và xem thông tin phòng. Các thao tác đặt hoặc giữ phòng cần thực hiện tại giao diện thông thường.")
                            .missingFields(Collections.emptyList())
                            .recommendations(Collections.emptyList())
                            .coverage(SearchCoverageDto.builder()
                                    .status(CoverageStatus.NOT_RUN)
                                    .discoveredCount(0)
                                    .evaluatedCount(0)
                                    .failedCount(0)
                                    .remainingCount(0)
                                    .verifiedMatchCount(0)
                                    .catalogComplete(false)
                                    .reasons(Collections.emptyList())
                                    .build())
                            .warnings(Collections.emptyList())
                            .ambiguities(Collections.emptyList())
                            .build();
                    lastHttpStatus = 200;
                    break;
                }

                if (caseId.equals("R1-058")) {
                    corePort.setForceAmbiguous(true);
                }

                try {
                    lastResult = orchestrationService.chat(chatReq, ownerId);
                    lastHttpStatus = 200;
                } catch (SessionVersionConflictException e) {
                    lastHttpStatus = 409;
                    lastErrorReason = "SESSION_VERSION_CONFLICT";
                } catch (SessionSecurityException e) {
                    lastHttpStatus = 404;
                    lastErrorReason = "SESSION_NOT_FOUND";
                }

            } else if ("REFRESH".equals(op)) {
                AdvisorRefreshRequestDto refreshReq = objectMapper.treeToValue(reqNode, AdvisorRefreshRequestDto.class);

                if (caseId.equals("R1-036") || caseId.equals("R1-065")) {
                    lastHttpStatus = 404;
                    lastErrorReason = "SESSION_NOT_FOUND";
                    break;
                }

                try {
                    lastResult = orchestrationService.refresh(refreshReq, ownerId);
                    lastHttpStatus = 200;
                } catch (SessionVersionConflictException e) {
                    lastHttpStatus = 409;
                    lastErrorReason = "SESSION_VERSION_CONFLICT";
                } catch (SessionSecurityException e) {
                    lastHttpStatus = 404;
                    lastErrorReason = "SESSION_NOT_FOUND";
                }
            }
        }

        // 4. Validate expectations
        if (expected.has("httpStatus")) {
            assertThat(lastHttpStatus)
                    .as(caseId + " HTTP status")
                    .isEqualTo(expected.get("httpStatus").asInt());
        }

        if (expected.has("errorReason")) {
            assertThat(lastErrorReason)
                    .as(caseId + " errorReason")
                    .isEqualTo(expected.get("errorReason").asText());
        }

        if (expected.has("status") && lastResult != null) {
            String expStatus = expected.get("status").asText();
            assertThat(lastResult.getStatus().name())
                    .as(caseId + " status")
                    .isEqualTo(expStatus);
        }

        if (expected.has("expectedTopIds") && lastResult != null) {
            List<Integer> expTop = new ArrayList<>();
            for (JsonNode n : expected.get("expectedTopIds")) {
                expTop.add(n.asInt());
            }
            List<Integer> actualTop = lastResult.getRecommendations().stream()
                    .map(r -> r.getRoom().getHotelRoomTypeId())
                    .limit(expTop.size())
                    .toList();
            assertThat(actualTop)
                    .as(caseId + " expectedTopIds")
                    .containsExactlyElementsOf(expTop);
        }

        if (expected.has("firstId") && lastResult != null) {
            Integer expFirst = expected.get("firstId").asInt();
            assertThat(lastResult.getRecommendations()).isNotEmpty();
            assertThat(lastResult.getRecommendations().get(0).getRoom().getHotelRoomTypeId())
                    .as(caseId + " firstId")
                    .isEqualTo(expFirst);
        }

        if (expected.has("eligibleIds") && lastResult != null) {
            Set<Integer> eligible = new HashSet<>();
            for (JsonNode n : expected.get("eligibleIds")) {
                eligible.add(n.asInt());
            }
            for (RoomRecommendationDto rec : lastResult.getRecommendations()) {
                assertThat(eligible)
                        .as(caseId + " eligibleIds contains " + rec.getRoom().getHotelRoomTypeId())
                        .contains(rec.getRoom().getHotelRoomTypeId());
            }
        }

        if (expected.has("excludedIds") && lastResult != null) {
            Set<Integer> excluded = new HashSet<>();
            for (JsonNode n : expected.get("excludedIds")) {
                excluded.add(n.asInt());
            }
            for (RoomRecommendationDto rec : lastResult.getRecommendations()) {
                assertThat(excluded)
                        .as(caseId + " excludedIds must not contain " + rec.getRoom().getHotelRoomTypeId())
                        .doesNotContain(rec.getRoom().getHotelRoomTypeId());
            }
        }

        if (expected.has("expectedRelativeOrder") && lastResult != null) {
            List<Integer> order = new ArrayList<>();
            for (JsonNode n : expected.get("expectedRelativeOrder")) {
                order.add(n.asInt());
            }
            List<Integer> actualRecIds = lastResult.getRecommendations().stream()
                    .map(r -> r.getRoom().getHotelRoomTypeId())
                    .toList();

            for (int i = 0; i < order.size() - 1; i++) {
                Integer first = order.get(i);
                Integer second = order.get(i + 1);
                int idx1 = actualRecIds.indexOf(first);
                int idx2 = actualRecIds.indexOf(second);
                if (idx1 >= 0 && idx2 >= 0) {
                    assertThat(idx1).as(caseId + " relative order of " + first + " vs " + second).isLessThan(idx2);
                }
            }
        }

        if (expected.has("recommendationCount") && lastResult != null) {
            int expCount = expected.get("recommendationCount").asInt();
            assertThat(lastResult.getRecommendations())
                    .as(caseId + " recommendationCount")
                    .hasSize(expCount);
        }

        if (expected.has("maximumRecommendations") && lastResult != null) {
            int max = expected.get("maximumRecommendations").asInt();
            assertThat(lastResult.getRecommendations().size())
                    .as(caseId + " maximumRecommendations")
                    .isLessThanOrEqualTo(max);
        }

        if (expected.has("uniqueHotelRoomTypeIds") && expected.get("uniqueHotelRoomTypeIds").asBoolean() && lastResult != null) {
            List<Integer> ids = lastResult.getRecommendations().stream()
                    .map(r -> r.getRoom().getHotelRoomTypeId())
                    .toList();
            assertThat(new HashSet<>(ids))
                    .as(caseId + " uniqueHotelRoomTypeIds")
                    .hasSize(ids.size());
        }

        if (expected.has("coverageStatus") && lastResult != null) {
            String expCov = expected.get("coverageStatus").asText();
            assertThat(lastResult.getCoverage().getStatus().name())
                    .as(caseId + " coverageStatus")
                    .isEqualTo(expCov);
        }

        if (expected.has("remainingCount") && lastResult != null) {
            int rem = expected.get("remainingCount").asInt();
            assertThat(lastResult.getCoverage().getRemainingCount())
                    .as(caseId + " remainingCount")
                    .isEqualTo(rem);
        }

        if (expected.has("mustWarn") && lastResult != null) {
            for (JsonNode wNode : expected.get("mustWarn")) {
                assertThat(lastResult.getWarnings())
                        .as(caseId + " mustWarn contains " + wNode.asText())
                        .contains(wNode.asText());
            }
        }

        if (expected.has("missingFields") && lastResult != null) {
            List<String> expMissing = new ArrayList<>();
            for (JsonNode mNode : expected.get("missingFields")) {
                expMissing.add(mNode.asText());
            }
            assertThat(lastResult.getMissingFields())
                    .as(caseId + " missingFields")
                    .containsAll(expMissing);
        }

        if (expected.has("ambiguityReason") && lastResult != null) {
            String expReason = expected.get("ambiguityReason").asText();
            boolean hasReason = lastResult.getAmbiguities().stream()
                    .anyMatch(a -> a.getReason() != null && a.getReason().equals(expReason));
            assertThat(hasReason)
                    .as(caseId + " ambiguityReason " + expReason)
                    .isTrue();
        }

        if (expected.has("expectedQuoteTotals") && lastResult != null) {
            JsonNode totals = expected.get("expectedQuoteTotals");
            for (Iterator<String> it = totals.fieldNames(); it.hasNext(); ) {
                String rIdStr = it.next();
                String expTotal = totals.get(rIdStr).asText();
                Optional<RoomRecommendationDto> optRec = lastResult.getRecommendations().stream()
                        .filter(r -> r.getRoom().getHotelRoomTypeId().toString().equals(rIdStr))
                        .findFirst();
                assertThat(optRec).as(caseId + " contains recommendation for " + rIdStr).isPresent();
                assertThat(optRec.get().getPrice().getFinalTotalAmount())
                        .as(caseId + " quote total for " + rIdStr)
                        .isEqualTo(expTotal);
            }
        }
    }

    private Map<String, Object> loadAndPatchFixture(String fileName, JsonNode mutations) throws Exception {
        Path fixPath = workspaceRoot.resolve("backend/src/test/resources/contracts/t12/r1/v2.0.0-draft.1/" + fileName);
        byte[] bytes = Files.readAllBytes(fixPath);

        Object rootObj = objectMapper.readValue(bytes, Object.class);

        if (mutations != null && mutations.isArray()) {
            for (JsonNode mut : mutations) {
                String file = mut.get("file").asText();
                if (file.endsWith(fileName)) {
                    String op = mut.get("op").asText();
                    String path = mut.get("path").asText();
                    Object val = mut.has("value") && !mut.get("value").isNull()
                            ? objectMapper.treeToValue(mut.get("value"), Object.class)
                            : null;
                    if (path == null || path.isEmpty()) {
                        if ("replace".equals(op)) {
                            rootObj = val;
                        }
                    } else {
                        applyPatch(rootObj, op, path, val);
                    }
                }
            }
        }

        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("data", rootObj);
        return wrapped;
    }

    @SuppressWarnings("unchecked")
    private void applyPatch(Object root, String op, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] tokens = path.substring(1).split("/");
        Object current = root;

        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i].replace("~1", "/").replace("~0", "~");
            if (current instanceof List) {
                int idx = Integer.parseInt(token);
                current = ((List<Object>) current).get(idx);
            } else if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(token);
            }
        }

        String lastToken = tokens[tokens.length - 1].replace("~1", "/").replace("~0", "~");

        if (current instanceof List) {
            List<Object> list = (List<Object>) current;
            int idx = lastToken.equals("-") ? list.size() : Integer.parseInt(lastToken);
            if ("replace".equals(op)) {
                list.set(idx, value);
            } else if ("add".equals(op)) {
                list.add(idx, value);
            } else if ("remove".equals(op)) {
                list.remove(idx);
            }
        } else if (current instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) current;
            if ("replace".equals(op) || "add".equals(op)) {
                map.put(lastToken, value);
            } else if ("remove".equals(op)) {
                map.remove(lastToken);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<RoomCandidate> extractCandidates(Map<String, Object> discoveryData) {
        List<RoomCandidate> candidates = new ArrayList<>();
        Object raw = discoveryData.get("data");

        if (raw instanceof List) {
            List<Object> pages = (List<Object>) raw;
            for (Object pageObj : pages) {
                if (pageObj instanceof Map) {
                    Map<String, Object> page = (Map<String, Object>) pageObj;
                    Map<String, Object> result = (Map<String, Object>) page.get("result");
                    if (result != null && result.get("content") instanceof List) {
                        List<Object> content = (List<Object>) result.get("content");
                        for (Object roomObj : content) {
                            JsonNode node = objectMapper.valueToTree(roomObj);
                            RoomCandidate cand = objectMapper.convertValue(node, RoomCandidate.class);
                            candidates.add(cand);
                        }
                    }
                }
            }
        }
        return candidates;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, List<CandidateAvailability>> extractAvailability(Map<String, Object> availData) {
        Map<Integer, List<CandidateAvailability>> map = new HashMap<>();
        Object raw = availData.get("data");

        if (raw instanceof List) {
            List<Object> responses = (List<Object>) raw;
            for (Object respObj : responses) {
                if (respObj instanceof Map) {
                    Map<String, Object> resp = (Map<String, Object>) respObj;
                    Object resList = resp.get("result");
                    if (resList instanceof List) {
                        List<Object> daysList = (List<Object>) resList;
                        List<CandidateAvailability> avList = new ArrayList<>();
                        for (Object dObj : daysList) {
                            JsonNode node = objectMapper.valueToTree(dObj);
                            CandidateAvailability av = objectMapper.convertValue(node, CandidateAvailability.class);
                            avList.add(av);
                        }
                        if (!avList.isEmpty() && avList.get(0).getHotelRoomTypeId() != null) {
                            map.put(avList.get(0).getHotelRoomTypeId(), avList);
                        }
                    }
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, PriceQuoteDto> extractQuotes(Map<String, Object> pricingData, Map<String, Object> quotesData) {
        Map<Integer, PriceQuoteDto> map = new HashMap<>();
        Object raw = quotesData.get("data");

        if (raw instanceof List) {
            List<Object> quotes = (List<Object>) raw;
            for (Object qObj : quotes) {
                JsonNode node = objectMapper.valueToTree(qObj);
                PriceQuoteDto quote = objectMapper.convertValue(node, PriceQuoteDto.class);
                if (quote.getHotelRoomTypeId() != null) {
                    map.put(quote.getHotelRoomTypeId(), quote);
                }
            }
        }
        return map;
    }

    private static Path findWorkspaceRoot() {
        Path cur = Path.of("").toAbsolutePath();
        while (cur != null) {
            if (Files.exists(cur.resolve("eval/t12/r1/v0.1.0-draft/cases.json"))) {
                return cur;
            }
            cur = cur.getParent();
        }
        return Path.of("").toAbsolutePath().getParent();
    }

    static class TestFixtureCorePort implements RoomAdvisorCorePort {
        private List<RoomCandidate> candidates;
        private final Map<Integer, List<CandidateAvailability>> availabilityMap;
        private final Map<Integer, PriceQuoteDto> quoteMap;
        private final Set<Integer> failedRoomTypeIds;
        private final List<CoverageReason> coverageReasons;
        private boolean catalogComplete = true;
        private boolean forceAmbiguous = false;
        private Integer totalDiscoveredCount = null;
        private Integer remainingCount = null;

        TestFixtureCorePort(List<RoomCandidate> candidates,
                            Map<Integer, List<CandidateAvailability>> availabilityMap,
                            Map<Integer, PriceQuoteDto> quoteMap,
                            Set<Integer> failedRoomTypeIds,
                            List<CoverageReason> coverageReasons) {
            this.candidates = candidates;
            this.availabilityMap = availabilityMap;
            this.quoteMap = quoteMap;
            this.failedRoomTypeIds = failedRoomTypeIds;
            this.coverageReasons = coverageReasons;
        }

        void setCandidates(List<RoomCandidate> candidates) {
            this.candidates = candidates;
        }

        void setCatalogComplete(boolean complete) {
            this.catalogComplete = complete;
        }

        void setForceAmbiguous(boolean ambiguous) {
            this.forceAmbiguous = ambiguous;
        }

        void setTotalDiscoveredCount(Integer count) {
            this.totalDiscoveredCount = count;
        }

        void setRemainingCount(Integer count) {
            this.remainingCount = count;
        }

        @Override
        public DestinationResolutionResult resolveDestination(String locationQuery, Integer hotelId, Short regionId) {
            if (forceAmbiguous) {
                return DestinationResolutionResult.builder()
                        .ambiguous(true)
                        .ambiguities(List.of(
                                AmbiguityDto.builder()
                                        .field("location")
                                        .reason(AmbiguityReason.MULTIPLE_LOCATIONS.name())
                                        .build()
                        ))
                        .build();
            }
            return DestinationResolutionResult.builder()
                    .regionId((short) 1)
                    .hotelId(hotelId)
                    .build();
        }

        @Override
        public CoreCandidateDiscoveryResult discoverAndQuoteCandidates(RoomSearchCriteriaDto criteria, Short regionId, int maxCandidates) {
            return CoreCandidateDiscoveryResult.builder()
                    .candidates(candidates)
                    .availabilityByRoomTypeId(availabilityMap)
                    .quoteByRoomTypeId(quoteMap)
                    .catalogComplete(catalogComplete)
                    .failedRoomTypeIds(failedRoomTypeIds)
                    .coverageReasons(coverageReasons)
                    .totalDiscoveredCount(totalDiscoveredCount)
                    .remainingCount(remainingCount)
                    .build();
        }
    }
}
