package vn.edu.utc.hotel_booking.aiassistant.knowledge;

public final class VectorLiteral {
    private VectorLiteral() {}

    public static String of(float[] vector) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) throw new IllegalArgumentException("Non-finite embedding");
            if (i > 0) result.append(',');
            result.append(Float.toString(vector[i]));
        }
        return result.append(']').toString();
    }
}
