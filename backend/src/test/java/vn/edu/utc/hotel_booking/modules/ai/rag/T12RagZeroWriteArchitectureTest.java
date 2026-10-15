package vn.edu.utc.hotel_booking.modules.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

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
