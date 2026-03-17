import java.util.*;

public class Plagiarism {

    private static final int N = 5; // 5-grams

    // nGramHash → Set of document IDs containing it
    private Map<Long, Set<String>> invertedIndex;

    // documentId → total n-gram count
    private Map<String, Integer> documentGramCount;

    public Plagiarism() {
        invertedIndex = new HashMap<>();
        documentGramCount = new HashMap<>();
    }

    // ===== Add Existing Document to Database =====
    public void addDocument(String docId, String content) {
        List<Long> ngrams = extractNGrams(content);
        documentGramCount.put(docId, ngrams.size());

        for (Long hash : ngrams) {
            invertedIndex
                    .computeIfAbsent(hash, k -> new HashSet<>())
                    .add(docId);
        }
    }

    // ===== Analyze New Document =====
    public void analyzeDocument(String docId, String content) {
        List<Long> ngrams = extractNGrams(content);
        System.out.println("Extracted " + ngrams.size() + " n-grams");

        Map<String, Integer> matchCount = new HashMap<>();

        for (Long hash : ngrams) {
            if (invertedIndex.containsKey(hash)) {
                for (String matchedDoc : invertedIndex.get(hash)) {
                    matchCount.put(matchedDoc,
                            matchCount.getOrDefault(matchedDoc, 0) + 1);
                }
            }
        }

        // Calculate similarity
        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            String comparedDoc = entry.getKey();
            int matches = entry.getValue();

            int baseCount = documentGramCount.get(comparedDoc);
            double similarity = (matches * 100.0) / baseCount;

            System.out.println("Found " + matches +
                    " matching n-grams with \"" + comparedDoc + "\"");
            System.out.printf("Similarity: %.2f%% ", similarity);

            if (similarity > 60)
                System.out.println("(PLAGIARISM DETECTED)");
            else if (similarity > 15)
                System.out.println("(Suspicious)");
            else
                System.out.println("(Low similarity)");
        }
    }

    // ===== Extract N-Grams =====
    private List<Long> extractNGrams(String content) {
        List<Long> ngrams = new ArrayList<>();

        String[] words = content
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .split("\\s+");

        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                sb.append(words[i + j]).append(" ");
            }
            String gram = sb.toString().trim();

            long hash = computeHash(gram);
            ngrams.add(hash);
        }

        return ngrams;
    }

    // ===== Rolling Hash Function =====
    private long computeHash(String text) {
        long hash = 0;
        long prime = 31;

        for (int i = 0; i < text.length(); i++) {
            hash = hash * prime + text.charAt(i);
        }

        return hash;
    }

    // ===== Main Method =====
    public static void main(String[] args) {

        Plagiarism detector = new Plagiarism();

        // Add previous essays (database of 100k simulated)
        detector.addDocument("essay_089.txt",
                "Artificial intelligence is transforming the world "
                        + "with machine learning and data science techniques.");

        detector.addDocument("essay_092.txt",
                "Machine learning and data science techniques are transforming "
                        + "the world with artificial intelligence systems.");

        // Analyze new submission
        detector.analyzeDocument("essay_123.txt",
                "Machine learning and data science techniques are transforming "
                        + "the world with artificial intelligence.");
    }
}