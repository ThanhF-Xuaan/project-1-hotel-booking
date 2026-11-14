package vn.edu.utc.hotel_booking.modules.ai.ocr.service;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Optional CCCD data-region retry. All decoded pixels and encoded bytes stay in memory. */
public final class OcrImagePreparation {
    private OcrImagePreparation() { }
    public static boolean hasIdentityCardLabel(String text) {
        String normalized = IdentityTextLabels.fold(text);
        return normalized.contains("can cuoc") || normalized.contains("citizen identity");
    }
    public static byte[] identityRegion(byte[] bytes) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            BufferedImage source;
            try {
                reader.setInput(input, true, true);
                source = reader.read(0);
            } finally { reader.dispose(); }
            if (source == null || source.getWidth() < source.getHeight() * 1.2) return null;
            int x = (int) (source.getWidth() * .27), y = (int) (source.getHeight() * .30);
            int width = source.getWidth() - x, height = source.getHeight() - y;
            int targetWidth = 1700, targetHeight = (int) ((long) height * targetWidth / width);
            if (targetHeight <= 0 || targetHeight > 2200) return null;
            BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
            var graphics = result.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                // Lighten colored security patterns while retaining dark printed text.
                BufferedImage region = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                for (int row = 0; row < height; row++) for (int col = 0; col < width; col++) {
                    int rgb = source.getRGB(x + col, y + row);
                    int light = Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
                    int value = Math.max(0, Math.min(255, (light - 40) * 255 / 180));
                    region.getRaster().setSample(col, row, 0, value);
                }
                graphics.drawImage(region, 0, 0, targetWidth, targetHeight, null);
            } finally { graphics.dispose(); }
            var output = new ByteArrayOutputStream();
            ImageIO.write(result, "png", output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ignored) {
            return null; // Original OCR draft remains usable when a codec cannot decode this format.
        }
    }

    /** Crop an OCR-detected text row without removing the small marks above Vietnamese letters. */
    public static byte[] textLine(byte[] bytes, TesseractTsvParser.Region bounds) {
        return cropText(bytes, bounds, 2400, 110, true);
    }

    public static byte[] textBlock(byte[] bytes, TesseractTsvParser.Region bounds) {
        return cropText(bytes, bounds, 1700, 800, false);
    }

    private static byte[] cropText(byte[] bytes, TesseractTsvParser.Region bounds,
                                   int maxWidth, int maxHeight, boolean singleLine) {
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return null;
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            var reader = readers.next();
            BufferedImage source;
            try { reader.setInput(input, true, true); source = reader.read(0); }
            finally { reader.dispose(); }
            // TSV already bounds the accents. A quarter-line margin also captured the caption
            // immediately above real CCCD names, which breaks single-line recognition.
            int margin = singleLine ? Math.max(2, bounds.height() / 20) : 4;
            int left = Math.max(0, bounds.left() - margin), top = Math.max(0, bounds.top() - margin);
            int right = Math.min(source.getWidth(), bounds.left() + bounds.width() + margin);
            int bottom = Math.min(source.getHeight(), bounds.top() + bounds.height() + margin);
            if (right <= left || bottom <= top) return null;
            double scale = Math.min((double) maxWidth / (right - left), (double) maxHeight / (bottom - top));
            int width = Math.max(1, (int) ((right - left) * scale));
            int height = Math.max(1, (int) ((bottom - top) * scale));
            var result = new BufferedImage(width + 40, height + 40, BufferedImage.TYPE_INT_RGB);
            var graphics = result.createGraphics();
            try {
                graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(source, 20, 20, width + 20, height + 20, left, top, right, bottom, null);
            } finally { graphics.dispose(); }
            if (!singleLine) {
                // Blue printed captions remain dark in the green channel, while the cyan
                // security background becomes lighter. Max(R,G,B) erased those captions.
                for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < result.getWidth(); x++) {
                    int green = (result.getRGB(x, y) >> 8) & 255;
                    int level = Math.max(0, Math.min(255, (green - 35) * 255 / 190));
                    result.setRGB(x, y, (level << 16) | (level << 8) | level);
                }
            }
            var output = new ByteArrayOutputStream(); ImageIO.write(result, "png", output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ignored) { return null; }
    }
}
