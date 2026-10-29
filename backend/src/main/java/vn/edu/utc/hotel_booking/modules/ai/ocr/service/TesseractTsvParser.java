package vn.edu.utc.hotel_booking.modules.ai.ocr.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parser TSV của Tesseract; không giữ hay log dữ liệu ảnh hoặc OCR. */
@Component
public class TesseractTsvParser {

    public record ParsedTsv(String text, double meanWordConfidence, int wordCount) { }

    public ParsedTsv parse(String tsv) {
        if (tsv == null || tsv.isBlank()) throw new IllegalArgumentException("Tesseract TSV is empty");
        Map<String, StringBuilder> lines = new LinkedHashMap<>();
        double confidenceSum = 0.0;
        int words = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(tsv))) {
            String row = reader.readLine();
            if (row == null || !row.startsWith("level\t")) {
                throw new IllegalArgumentException("Tesseract TSV header is invalid");
            }
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) continue;
                String[] columns = row.split("\\t", 12);
                if (columns.length < 12) throw new IllegalArgumentException("Tesseract TSV row is invalid");
                if (!"5".equals(columns[0])) continue;
                String text = columns[11].trim();
                if (text.isEmpty()) continue;
                double confidence;
                try {
                    confidence = Double.parseDouble(columns[10]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 100.0) continue;
                String key = String.join(":", columns[1], columns[2], columns[3], columns[4]);
                lines.computeIfAbsent(key, ignored -> new StringBuilder()).append(text).append(' ');
                confidenceSum += confidence;
                words++;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Tesseract TSV could not be parsed", e);
        }
        StringBuilder text = new StringBuilder();
        lines.values().forEach(line -> text.append(line.toString().trim()).append('\n'));
        return new ParsedTsv(text.toString().trim(), words == 0 ? 0.0 : confidenceSum / words / 100.0, words);
    }
}
