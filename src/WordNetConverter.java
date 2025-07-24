import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class WordNetConverter {

    // Path to raw lex files
    static final String INPUT_FOLDER = "wordnet_raw/";
    static final String OUTPUT_FOLDER = "dict/";

    public static void main(String[] args) throws IOException {
        Files.walk(Paths.get(INPUT_FOLDER))
                .filter(Files::isRegularFile)
                .forEach(WordNetConverter::processFile);
    }

    private static void processFile(Path filePath) {
        try {
            String filename = filePath.getFileName().toString();
            if (!filename.contains(".")) return;

            String pos = filename.split("\\.")[0]; // e.g. verb.weather -> verb
            String category = filename; // full file name as category
            List<String> lines = Files.readAllLines(filePath);

            for (String line : lines) {
                Matcher m = Pattern.compile("\\{(.+?)\\}").matcher(line);
                while (m.find()) {
                    String block = m.group(1);

                    // Gloss
                    int glossStart = block.lastIndexOf('(');
                    int glossEnd = block.lastIndexOf(')');
                    if (glossStart == -1 || glossEnd == -1) continue;

                    String glossRaw = block.substring(glossStart + 1, glossEnd).trim();
                    String[] glossParts = glossRaw.split(";", 2);
                    String def = glossParts[0].trim();
                    String example = (glossParts.length > 1) ? glossParts[1].replaceAll("\"", "").trim() : "";

                    // Words
                    Set<String> allWords = new LinkedHashSet<>();
                    Matcher wordMatcher = Pattern.compile("[a-zA-Z_\\-]+").matcher(block);
                    while (wordMatcher.find()) {
                        String w = wordMatcher.group();
                        if (!w.equals("frames") && !w.equals("Definition") && !w.equals("Example")) {
                            allWords.add(w);
                        }
                    }

                    // Frames
                    Matcher frameMatcher = Pattern.compile("frames:\\s*([\\d,\\s]+)").matcher(block);
                    String frames = frameMatcher.find() ? frameMatcher.group(1).trim() : "";

                    // Main word = first in list
                    String mainWord = allWords.iterator().next();
                    allWords.remove(mainWord);

                    // Output
                    Path outDir = Paths.get(OUTPUT_FOLDER, pos);
                    Files.createDirectories(outDir);
                    Path outFile = outDir.resolve(mainWord);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Definition: ").append(def).append("\n");
                    if (!example.isEmpty()) sb.append("Example: ").append(example).append("\n");
                    if (!allWords.isEmpty()) sb.append("Synonyms: ").append(String.join(", ", allWords)).append("\n");
                    if (!frames.isEmpty()) sb.append("Frame: ").append(frames).append("\n");
                    sb.append("Category: ").append(category).append("\n");

                    Files.write(outFile, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }

            System.out.println("Processed: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
