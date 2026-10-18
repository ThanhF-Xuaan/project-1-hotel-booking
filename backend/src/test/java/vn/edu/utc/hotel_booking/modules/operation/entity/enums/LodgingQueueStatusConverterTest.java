package vn.edu.utc.hotel_booking.modules.operation.entity.enums;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LodgingQueueStatusConverterTest {

    private LodgingQueueStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LodgingQueueStatusConverter();
    }

    @Test
    void convertToDatabaseColumn_mapsErrorToFailed() {
        assertEquals("FAILED", converter.convertToDatabaseColumn(LodgingQueueStatus.ERROR));
    }

    @Test
    void convertToDatabaseColumn_mapsStandardStatuses() {
        assertEquals("FAILED", converter.convertToDatabaseColumn(LodgingQueueStatus.FAILED));
        assertEquals("PENDING", converter.convertToDatabaseColumn(LodgingQueueStatus.PENDING));
        assertEquals("PROCESSING", converter.convertToDatabaseColumn(LodgingQueueStatus.PROCESSING));
        assertEquals("EXPORTED", converter.convertToDatabaseColumn(LodgingQueueStatus.EXPORTED));
        assertEquals("CANCELLED", converter.convertToDatabaseColumn(LodgingQueueStatus.CANCELLED));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_mapsFailedToErrorForApiCompatibility() {
        assertEquals(LodgingQueueStatus.ERROR, converter.convertToEntityAttribute("FAILED"));
        assertEquals(LodgingQueueStatus.ERROR, converter.convertToEntityAttribute("ERROR"));
    }

    @Test
    void convertToEntityAttribute_mapsStandardStatuses() {
        assertEquals(LodgingQueueStatus.PENDING, converter.convertToEntityAttribute("PENDING"));
        assertEquals(LodgingQueueStatus.PROCESSING, converter.convertToEntityAttribute("PROCESSING"));
        assertEquals(LodgingQueueStatus.EXPORTED, converter.convertToEntityAttribute("EXPORTED"));
        assertEquals(LodgingQueueStatus.CANCELLED, converter.convertToEntityAttribute("CANCELLED"));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }
}
