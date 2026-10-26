package vn.edu.utc.hotel_booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "acceptance.integration", matches = "true")
class HotelBookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
