package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationResultDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationEvalReportDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.PersonalizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.TravelPatternSegment;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class U14ZeroWriteAndPricingIsolationTest {

    private static final Set<String> FORBIDDEN_TYPE_NAMES = Set.of(
            // Booking & Inventory
            "BookingService",
            "BookingSessionService",
            "PublicBookingController",
            "PublicBookingSessionController",
            "BookingController",
            "RoomInventoryHoldService",
            "RoomAvailabilityService",
            "PublicAvailabilityController",
            "BookingRepository",
            "RoomInventoryHoldRepository",
            // Payment & Billing
            "PaymentService",
            "InvoiceService",
            "PaymentController",
            "InvoiceController",
            "PaymentRepository",
            // Guest & Lodging Mutation
            "StayGuestService",
            "LodgingQueueService",
            "ServiceOrderService",
            "StayGuestRepository",
            // Pricing & Promotion (Strict isolation - personalization must never mutate or apply pricing/coupons)
            "PriceEngine",
            "PricingRuleService",
            "PricingController",
            "CampaignService",
            "CampaignRepository",
            "VoucherService",
            "VoucherRepository",
            "VoucherController",
            "PromotionService",
            "PromotionRepository",
            // Shared JPA / Entity Mutation
            "EntityManager",
            "JpaRepository",
            "CrudRepository"
    );

    @Test
    @DisplayName("U14 Zero-Write & Pricing Isolation: No class in modules.ai.personalization references forbidden write/pricing components")
    void shouldNotReferenceAnyForbiddenComponentsInPersonalization() throws Exception {
        List<Class<?>> personalizationClasses = scanPersonalizationClasses();
        assertThat(personalizationClasses).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : personalizationClasses) {
            // 1. Check Fields
            for (Field field : clazz.getDeclaredFields()) {
                checkType(clazz, "field " + field.getName(), field.getType(), violations);
            }

            // 2. Check Constructor parameters
            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                for (Class<?> paramType : ctor.getParameterTypes()) {
                    checkType(clazz, "constructor parameter", paramType, violations);
                }
            }

            // 3. Check Method parameters and return types
            for (Method method : clazz.getDeclaredMethods()) {
                checkType(clazz, "method return " + method.getName(), method.getReturnType(), violations);
                for (Class<?> paramType : method.getParameterTypes()) {
                    checkType(clazz, "method param " + method.getName(), paramType, violations);
                }
            }
        }

        assertThat(violations)
                .withFailMessage("Zero-Write or Pricing isolation violations found in modules.ai.personalization:\n" + String.join("\n", violations))
                .isEmpty();
    }

    @Test
    @DisplayName("U14 Source File Import Scan: Java source files in modules.ai.personalization do not import pricing/booking/JPA packages")
    void shouldNotImportForbiddenPackagesInSourceFiles() throws Exception {
        Path sourceDir = Files.exists(Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization"))
                ? Path.of("src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization")
                : Path.of("backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization");
        assertThat(Files.exists(sourceDir)).isTrue();

        List<String> forbiddenPackagePrefixes = List.of(
                "vn.edu.utc.hotel_booking.modules.booking",
                "vn.edu.utc.hotel_booking.modules.pricing",
                "vn.edu.utc.hotel_booking.modules.payment",
                "org.springframework.data.jpa",
                "jakarta.persistence.EntityManager"
        );

        List<String> violations = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(sourceDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.startsWith("import ")) {
                        for (String forbidden : forbiddenPackagePrefixes) {
                            if (line.contains(forbidden)) {
                                violations.add(javaFile.getFileName() + ":" + (i + 1) + " imports forbidden package: " + line);
                            }
                        }
                    }
                }
            }
        }

        assertThat(violations)
                .withFailMessage("Forbidden imports detected in U14 source files:\n" + String.join("\n", violations))
                .isEmpty();
    }

    @Test
    @DisplayName("U14 Invariant: Result DTO strictly enforces nonBindingSuggestion=true and pricingOrPromotionMutated=false")
    void shouldEnforceNonBindingAndZeroPricingMutationInResultDto() {
        GuestPersonalizationResultDto result = GuestPersonalizationResultDto.builder()
                .guestId(101L)
                .status(PersonalizationStatus.PERSONALIZED_READY)
                .detectedSegment(TravelPatternSegment.BUSINESS_FREQUENT)
                .roomTypeSuggestions(List.of())
                .featureSuggestions(List.of())
                .serviceSuggestions(List.of())
                .build();

        assertThat(result.isNonBindingSuggestion()).isTrue();
        assertThat(result.isPricingOrPromotionMutated()).isFalse();

        // Verify there is no setter method that could mutate these invariant fields
        Method[] methods = GuestPersonalizationResultDto.class.getDeclaredMethods();
        List<String> setterNames = Arrays.stream(methods)
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertThat(setterNames)
                .doesNotContain("setNonBindingSuggestion")
                .doesNotContain("setPricingOrPromotionMutated");
    }

    @Test
    @DisplayName("U14 Invariant: Eval report strictly enforces leakageFreeTimeSplit=true")
    void shouldEnforceLeakageFreeTimeSplitInEvalReportDto() {
        PersonalizationEvalReportDto report = PersonalizationEvalReportDto.builder()
                .totalEvaluatedStays(25)
                .k(3)
                .timeDecayedHitRateAtK(0.85)
                .baselineFrequencyHitRateAtK(0.65)
                .timeDecayedMrrAtK(0.72)
                .baselineFrequencyMrrAtK(0.50)
                .build();

        assertThat(report.isLeakageFreeTimeSplit()).isTrue();

        Method[] methods = PersonalizationEvalReportDto.class.getDeclaredMethods();
        List<String> setterNames = Arrays.stream(methods)
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertThat(setterNames).doesNotContain("setLeakageFreeTimeSplit");
    }

    private void checkType(Class<?> sourceClass, String context, Class<?> targetType, List<String> violations) {
        if (targetType == null) {
            return;
        }

        String simpleName = targetType.getSimpleName();
        String typeName = targetType.getName();

        if (FORBIDDEN_TYPE_NAMES.contains(simpleName)) {
            violations.add(sourceClass.getName() + " -> " + context + " has forbidden type: " + typeName);
        }

        for (String forbidden : FORBIDDEN_TYPE_NAMES) {
            if (typeName.endsWith("." + forbidden) || typeName.equals(forbidden)) {
                violations.add(sourceClass.getName() + " -> " + context + " matches forbidden name: " + forbidden);
            }
        }
    }

    private List<Class<?>> scanPersonalizationClasses() throws Exception {
        String packageName = "vn.edu.utc.hotel_booking.modules.ai.personalization";
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.toURI()));
        }

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
