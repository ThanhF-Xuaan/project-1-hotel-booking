package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityServiceImpl;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Room Availability Service Constructor & Spring Wiring Test")
class RoomAvailabilityServiceConstructorTest {

    @Test
    @DisplayName("Kiểm tra RoomAvailabilityServiceImpl có duy nhất 1 constructor và có annotation @Autowired")
    void testSingleAutowiredConstructorExists() {
        Constructor<?>[] constructors = RoomAvailabilityServiceImpl.class.getDeclaredConstructors();

        // Xác nhận class có duy nhất 1 constructor để Spring không bao giờ tìm default constructor
        assertThat(constructors).hasSize(1);

        Constructor<?> mainConstructor = constructors[0];
        assertThat(mainConstructor.getParameterCount()).isEqualTo(6);
        assertThat(mainConstructor.isAnnotationPresent(Autowired.class)).isTrue();
    }
}
