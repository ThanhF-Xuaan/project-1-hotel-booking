package vn.edu.utc.hotel_booking.modules.ai.advisor.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.PublicBookingCoreAdapter;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class T12ZeroWriteArchitectureTest {

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
            "RoomTypeRepository"
    );

    @Test
    @DisplayName("Zero-Write Architecture: No advisor class references forbidden write services or repositories")
    void shouldNotReferenceAnyForbiddenWriteComponents() throws Exception {
        List<Class<?>> advisorClasses = scanAdvisorClasses();
        assertThat(advisorClasses).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (Class<?> clazz : advisorClasses) {
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
                .withFailMessage("Zero-Write architecture violations found:\n" + String.join("\n", violations))
                .isEmpty();
    }

    @Test
    @DisplayName("Zero-Write Architecture: PublicBookingCoreAdapter has only PublicBookingSearchService as core dependency")
    void shouldOnlyInjectPublicBookingSearchServiceInAdapter() {
        Field[] fields = PublicBookingCoreAdapter.class.getDeclaredFields();
        List<Class<?>> dependencyTypes = new ArrayList<>();

        for (Field field : fields) {
            Class<?> type = field.getType();
            if (!type.isPrimitive() && !type.getPackageName().startsWith("java.") && !type.getName().contains("Logger")) {
                dependencyTypes.add(type);
            }
        }

        assertThat(dependencyTypes)
                .as("PublicBookingCoreAdapter must only depend on PublicBookingSearchService")
                .containsExactly(PublicBookingSearchService.class);
    }

    private void checkType(Class<?> sourceClass, String context, Class<?> targetType, List<String> violations) {
        if (targetType == null) {
            return;
        }

        String simpleName = targetType.getSimpleName();
        String fullName = targetType.getName();

        if (FORBIDDEN_TYPE_NAMES.contains(simpleName)) {
            violations.add(sourceClass.getName() + " -> " + context + " has forbidden type: " + fullName);
        }

        // Generic check for any Spring Data Repository
        if (simpleName.endsWith("Repository") && fullName.contains("vn.edu.utc.hotel_booking")) {
            violations.add(sourceClass.getName() + " -> " + context + " directly accesses repository: " + fullName);
        }

        // Check for any Booking/Hold/Payment write services
        if (fullName.contains("vn.edu.utc.hotel_booking.modules.booking.service")
                && !simpleName.equals("PublicBookingSearchService")) {
            violations.add(sourceClass.getName() + " -> " + context + " references non-search booking service: " + fullName);
        }
    }

    private List<Class<?>> scanAdvisorClasses() throws Exception {
        String basePkg = "vn.edu.utc.hotel_booking.modules.ai.advisor";
        String path = basePkg.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = cl.getResources(path);

        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            File dir = new File(url.toURI());
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir, basePkg, classes);
            }
        }

        return classes;
    }

    private void scanDirectory(File dir, String currentPkg, List<Class<?>> classes) throws ClassNotFoundException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, currentPkg + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = currentPkg + "." + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
    }
}
