package vn.edu.utc.hotel_booking.aiassistant;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeIngestionWorker;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.VectorLiteral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeIngestionWorkerTest {
    @Test
    void vectorLiteralRejectsNonFiniteCoordinates() {
        assertEquals("[0.0,1.5]", VectorLiteral.of(new float[]{0f, 1.5f}));
        assertThrows(IllegalArgumentException.class,
                () -> VectorLiteral.of(new float[]{Float.NaN}));
    }
}
