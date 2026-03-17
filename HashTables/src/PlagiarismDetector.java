import java.util.*;

public class PlagiarismDetector {


    private static final int N = 5;


    private Map<String, Set<String>> ngramIndex;


    private Map<String, List<String>> documentNgrams;

    public PlagiarismDetector() {
        ngramIndex = new HashMap<>();
        documentNgrams = new HashMap<>();
    }


    public void addDocument(String docId, String content) {
        List<String> ngrams = generateNgrams(content);
        documentNgrams.put(docId, ngrams);

        for (String ngram : ngrams) {
            ngramIndex
                    .computeIfAbsent(ngram, k -> new HashSet<>())
                    .add(docId);
        }
    }


    public void analyzeDocument(String docId, String content) {
        List<String> newNgrams = generateNgrams(content);

        Map<String, Integer> matchCount = new HashMap<>();

        for (String ngram : newNgrams) {
            if (ngramIndex.containsKey(ngram)) {
                for (String existingDoc : ngramIndex.get(ngram)) {
                    matchCount.put(existingDoc,
                            matchCount.getOrDefault(existingDoc, 0) + 1);
                }
            }
        }

        System.out.println("Analyzing: " + docId);
        System.out.println("Extracted " + newNgrams.size() + " n-grams\n");

        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            String existingDoc = entry.getKey();
            int matches = entry.getValue();

            int total = newNgrams.size();
            double similarity = (matches * 100.0) / total;

            String result = similarity > 60 ? "PLAGIARISM DETECTED"
                    : similarity > 15 ? "SUSPICIOUS"
                    : "LOW SIMILARITY";

            System.out.println("Matched with: " + existingDoc);
            System.out.println("Matching n-grams: " + matches);
            System.out.printf("Similarity: %.2f%% → %s\n\n", similarity, result);
        }
    }


    private List<String> generateNgrams(String text) {
        List<String> ngrams = new ArrayList<>();

        String[] words = text.toLowerCase().split("\\s+");

        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();

            for (int j = 0; j < N; j++) {
                sb.append(words[i + j]).append(" ");
            }

            ngrams.add(sb.toString().trim());
        }

        return ngrams;
    }


    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector();


        detector.addDocument("essay_089",
                "machine learning is a method of data analysis that automates analytical model building");

        detector.addDocument("essay_092",
                "machine learning is a method of data analysis that automates analytical model building using algorithms");

        detector.analyzeDocument("essay_123",
                "machine learning is a method of data analysis that automates analytical model building");
    }
}
