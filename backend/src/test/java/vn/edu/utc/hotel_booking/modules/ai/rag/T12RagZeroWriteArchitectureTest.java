package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.PublicRagKnowledgeController;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagGroundedAnswerService;

class T12RagZeroWriteArchitectureTest {

    private static final Set<String> FORBIDDEN_TYPE_NAMES = Set.of(
            "BookingService",
            "BookingSessionService",
            "PublicBookingController",
            "PublicBookingSessionController",
            "BookingController",
            "RoomInventoryHoldService",
            "RoomAvailabilityService",
            "PublicAvailabilityController",
            "PaymentService",
            "InvoiceService",
            "PaymentController",
            "InvoiceController",
            "StayGuestService",
            "LodgingQueueService",
            "ServiceOrderService",
            "BookingRepository",
            "RoomInventoryHoldRepository",
            "PaymentRepository",
            "StayGuestRepository",
            "HotelRepository",
            "RoomTypeRepository",
            "EntityManager",
            "JpaRepository",
            "CrudRepository"
    );

    @Test
    @DisplayName("Zero-Write Architecture (RAG R2): No class in modules.ai.rag references forbidden write components or JPA repositories")
    void shouldNotReferenceAnyForbiddenWriteComponentsInRag() throws Exception {
        List<Class<?>> ragClasses = scanRagClasses();
        assertThat(ragClasses).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : ragClasses) {
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
                .withFailMessage("Zero-Write architecture violations found in modules.ai.rag:\n" + String.join("\n", violations))
                .isEmpty();
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

    private List<Class<?>> scanRagClasses() throws Exception {
        String packageName = "vn.edu.utc.hotel_booking.modules.ai.rag";
        String path = packageName.replace('.', '/');
        // Inspect production bytecode only. Test spies must never count as production dependencies.
        File output = new File(RagGroundedAnswerService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return findClasses(new File(output, path), packageName);
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
                classes.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
            }
        }
        return classes;
    }

    @Test void publicReadSliceCannotReferenceLifecycleBusinessWritesOrStoreMutators() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> type : scanRagClasses()) {
            String name = type.getName();
            if (!(name.contains(".service.") && !name.contains("RagKnowledgeLifecycleService"))
                    && !name.contains(".embedding.") && !name.contains("PublicRagKnowledgeController")) continue;
            violations.addAll(publicReadViolations(type));
        }
        assertThat(violations).isEmpty();
    }

    @Test void publicControllerHasOnlyReadServiceEligibilityAndBudgetDependencies() {
        assertThat(Arrays.stream(PublicRagKnowledgeController.class.getDeclaredFields()).map(Field::getType)
                .filter(type -> !type.getName().startsWith("org.slf4j.")).map(Class::getName))
                .containsExactlyInAnyOrder(RagGroundedAnswerService.class.getName(),
                        "vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader",
                        "vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget");
    }

    @Test void bytecodeRuleDetectsCallsHiddenInsideMethodBodies() throws Exception {
        assertThat(publicReadViolations(DeliberatelyForbiddenFixture.class)).anyMatch(s -> s.contains("stageDocument"));
    }

    private static final class DeliberatelyForbiddenFixture {
        void forbidden(vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort store) {
            store.stageDocument(null, List.of());
        }
    }

    /** Constant-pool invocation references catch method-body calls missed by field/constructor reflection. */
    private List<String> publicReadViolations(Class<?> type) throws Exception {
        List<String> violations = new ArrayList<>();
        var pool = constantPool(type);
        Set<String> reads = Set.of("findCandidateChunks", "getExpectedDimension", "getDocument", "getActiveDocument");
        for (Object entry : pool) {
            if (entry instanceof ClassEntry clazz) {
                String target = (String) pool[clazz.name()];
                if (target.contains("RagKnowledgeLifecycleService") || target.contains("AdminRagLifecycleController")
                        || target.equals("org/springframework/ai/tool/ToolCallback")
                        || target.contains("FunctionToolCallback") || target.contains("ToolCallbackResolver")
                        || target.contains("/jdbc/") || target.endsWith("/DataSource")) {
                    violations.add(type.getName() + " -> forbidden public dependency " + target);
                }
                if (target.startsWith("vn/edu/utc/hotel_booking/modules/")
                        && !target.startsWith("vn/edu/utc/hotel_booking/modules/ai/rag/")
                        && !target.equals("vn/edu/utc/hotel_booking/modules/organization/service/PublicHotelEligibilityReader")
                        && !target.equals("vn/edu/utc/hotel_booking/modules/ai/advisor/llm/PiiRedactionUtil")) {
                    violations.add(type.getName() + " -> cross-module public dependency " + target);
                }
            }
            if (entry instanceof Invocation invocation) {
                ClassEntry owner = (ClassEntry) pool[invocation.owner()];
                NameEntry method = (NameEntry) pool[invocation.name()];
                String ownerName = (String) pool[owner.name()], methodName = (String) pool[method.name()];
                if (ownerName.endsWith("/RagVectorStorePort") && !reads.contains(methodName)) {
                    violations.add(type.getName() + " -> forbidden store invocation " + methodName);
                }
            }
        }
        return violations;
    }

    private record ClassEntry(int name) { }
    private record NameEntry(int name) { }
    private record Invocation(int owner, int name) { }

    private Object[] constantPool(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream resourceStream = type.getResourceAsStream(resource);
             var input = new DataInputStream(Objects.requireNonNull(resourceStream))) {
            assertThat(input.readInt()).isEqualTo(0xCAFEBABE);
            input.readUnsignedShort(); input.readUnsignedShort();
            Object[] pool = new Object[input.readUnsignedShort()];
            for (int i = 1; i < pool.length; i++) {
                switch (input.readUnsignedByte()) {
                    case 1 -> pool[i] = input.readUTF();
                    case 3, 4 -> input.readInt();
                    case 5, 6 -> { input.readLong(); i++; }
                    case 7 -> pool[i] = new ClassEntry(input.readUnsignedShort());
                    case 8, 16, 19, 20 -> input.readUnsignedShort();
                    case 9, 17, 18 -> { input.readUnsignedShort(); input.readUnsignedShort(); }
                    case 10, 11 -> pool[i] = new Invocation(input.readUnsignedShort(), input.readUnsignedShort());
                    case 12 -> { pool[i] = new NameEntry(input.readUnsignedShort()); input.readUnsignedShort(); }
                    case 15 -> { input.readUnsignedByte(); input.readUnsignedShort(); }
                    default -> throw new AssertionError("Unknown JVM constant-pool tag");
                }
            }
            return pool;
        }
    }
}
