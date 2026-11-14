package vn.edu.utc.hotel_booking.modules.ai.ocr.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/** Parser TSV của Tesseract; không giữ hay log dữ liệu ảnh hoặc OCR. */
@Component
public class TesseractTsvParser {

    public record Word(String text, int left, int top, int width, int height, double confidence) { }
    public record Region(int left, int top, int width, int height) { }
    public record Line(List<Word> words) {
        public String text() { return String.join(" ", words.stream().map(Word::text).toList()); }
        public int top() { return words.stream().mapToInt(Word::top).min().orElse(0); }
    }
    public record ParsedTsv(String text, double meanWordConfidence, int wordCount, List<Line> lines) {
        public Region nameRegion(String name) {
            return nameBounds(nameWords(name));
        }

        public Region surnameRegion(String name) {
            var words = nameWords(name);
            if (words.isEmpty()) return null;
            var row = nameBounds(words);
            var first = words.getFirst();
            return new Region(first.left(), row.top(), first.width(), row.height());
        }

        private List<Word> nameWords(String name) {
            if (name == null) return List.of();
            String target = IdentityTextLabels.fold(name).replaceAll("[^a-z]", "");
            if (target.isEmpty()) return List.of();
            for (var line : lines) {
                for (int start = 0; start < line.words().size(); start++) {
                    if (IdentityTextLabels.fold(line.words().get(start).text()).replaceAll("[^a-z]", "").isEmpty()) continue;
                    StringBuilder candidate = new StringBuilder();
                    for (int end = start; end < line.words().size(); end++) {
                        candidate.append(IdentityTextLabels.fold(line.words().get(end).text()).replaceAll("[^a-z]", ""));
                        if (candidate.toString().equals(target)) return line.words().subList(start, end + 1);
                        if (!target.startsWith(candidate.toString())) break;
                    }
                }
            }
            return List.of();
        }

        public Region addressRegion() {
            for (var line : lines) {
                int left = residenceLeft(line, lines);
                if (left < 0) continue;
                int[] heights = line.words().stream().filter(word -> word.text().matches(".*[\\p{L}].*"))
                        .mapToInt(Word::height).filter(h -> h > 0).sorted().toArray();
                int height = heights.length == 0 ? 20 : heights[heights.length / 2];
                return bounds(lines.stream().flatMap(candidate -> candidate.words().stream())
                        .filter(word -> word.left() >= left - 8 && word.top() >= line.top() - height / 4
                                && word.top() <= line.top() + height * 3)
                        .filter(word -> word.confidence() >= .4 && word.height() >= height * .6
                                && word.text().matches("[\\p{L}\\p{M}\\p{N}\\h,.:/()'\\-]+")
                                && word.text().replaceAll("[^\\p{L}]", "").length() >= 2
                                && word.width() <= word.text().length() * word.height() * 1.8)
                        .toList());
            }
            return null;
        }
    }

    public ParsedTsv parse(String tsv) {
        if (tsv == null || tsv.isBlank()) throw new IllegalArgumentException("Tesseract TSV is empty");
        Map<String, List<Word>> lines = new LinkedHashMap<>();
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
                try {
                    int left = Integer.parseInt(columns[6]), top = Integer.parseInt(columns[7]);
                    int width = Integer.parseInt(columns[8]), height = Integer.parseInt(columns[9]);
                    if (left < 0 || top < 0 || width < 0 || height < 0) continue;
                    lines.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(new Word(text, left, top, width, height, confidence / 100.0));
                } catch (NumberFormatException e) { continue; }
                confidenceSum += confidence;
                words++;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Tesseract TSV could not be parsed", e);
        }
        List<Line> ordered = lines.values().stream().map(value -> new Line(List.copyOf(value))).toList();
        return new ParsedTsv(identityText(ordered), words == 0 ? 0.0 : confidenceSum / words / 100.0,
                words, ordered);
    }

    private String identityText(List<Line> lines) {
        StringBuilder text = new StringBuilder();
        for (var line : lines) {
            var label = IdentityTextLabels.addressLabel(line.text());
            if (!label.find()) { text.append(line.text()).append('\n'); continue; }
            int left = residenceLeft(line, lines);
            if (left < 0) continue;
            int boundary = left - 8;
            // Preserve the right-hand address column. Expiry captions on the left must not be joined in.
            lines.stream().filter(candidate -> candidate.top() >= line.top())
                    .sorted(Comparator.comparingInt(Line::top))
                    .map(candidate -> candidate.words().stream().filter(w -> w.left() >= boundary)
                            .sorted(Comparator.comparingInt(Word::left)).map(Word::text).toList())
                    .filter(value -> !value.isEmpty()).forEach(value -> text.append(String.join(" ", value)).append('\n'));
            return text.toString().trim();
        }
        return text.toString().trim();
    }

    private static int residenceLeft(Line line, List<Line> lines) {
        var label = IdentityTextLabels.addressLabel(line.text());
        if (!label.find()) return -1;
        int offset = 0;
        for (var word : line.words()) {
            if (offset + word.text().length() > label.start()) {
                int left = word.left();
                // An English-only caption can follow a damaged Vietnamese caption. Use the
                // independently detected name column; never walk backwards across the expiry column.
                if (IdentityTextLabels.fold(line.text()).substring(label.start()).startsWith("place")) {
                    String text = String.join("\n", lines.stream().map(Line::text).toList());
                    var name = new ParsedTsv(text, 0, 0, lines).nameRegion(IdentityTextLabels.name(text));
                    if (name != null && name.left() < left) left = Math.max(0, name.left() - name.height() / 3);
                }
                return left;
            }
            offset += word.text().length() + 1;
        }
        return -1;
    }

    private static Region bounds(List<Word> words) {
        if (words.isEmpty()) return null;
        int left = words.stream().mapToInt(Word::left).min().orElse(0);
        int top = words.stream().mapToInt(Word::top).min().orElse(0);
        int right = words.stream().mapToInt(w -> w.left() + w.width()).max().orElse(left);
        int bottom = words.stream().mapToInt(w -> w.top() + w.height()).max().orElse(top);
        return new Region(left, top, right - left, bottom - top);
    }

    private static Region nameBounds(List<Word> words) {
        var box = bounds(words);
        if (box == null || words.size() < 2) return box;
        int capitalHeight = words.stream().mapToInt(Word::height).filter(h -> h > 0).min().orElse(box.height());
        int baselineTop = words.stream().mapToInt(Word::top).max().orElse(box.top());
        // Some word boxes absorb the smaller caption above the name. Keep accent headroom
        // relative to the other capital letters, rather than padding that contaminated box.
        int top = Math.max(box.top(), baselineTop - (int) Math.ceil(capitalHeight * .4));
        return new Region(box.left(), top, box.width(), box.top() + box.height() - top);
    }
}
