import java.util.*;

public class AutocompleteSystem {


    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        Map<String, Integer> freqMap = new HashMap<>(); // query -> frequency
    }

    private TrieNode root;
    private Map<String, Integer> globalFreq; // query -> frequency

    public AutocompleteSystem() {
        root = new TrieNode();
        globalFreq = new HashMap<>();
    }


    public void insert(String query) {
        globalFreq.put(query, globalFreq.getOrDefault(query, 0) + 1);

        TrieNode node = root;

        for (char c : query.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);

            // update frequency at each prefix node
            node.freqMap.put(query, globalFreq.get(query));
        }
    }


    public List<String> search(String prefix) {
        TrieNode node = root;

        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return Collections.emptyList();
            }
            node = node.children.get(c);
        }


        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : node.freqMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > 10) {
                minHeap.poll();
            }
        }

        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().getKey());
        }

        Collections.reverse(result);
        return result;
    }


    public List<String> suggestWithTypo(String input) {
        List<String> results = new ArrayList<>();

        for (String query : globalFreq.keySet()) {
            if (isEditDistanceOne(input, query)) {
                results.add(query);
            }
        }


        results.sort((a, b) -> globalFreq.get(b) - globalFreq.get(a));

        return results.subList(0, Math.min(5, results.size()));
    }


    private boolean isEditDistanceOne(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        if (Math.abs(m - n) > 1) return false;

        int i = 0, j = 0, edits = 0;

        while (i < m && j < n) {
            if (s1.charAt(i) != s2.charAt(j)) {
                if (++edits > 1) return false;

                if (m > n) i++;
                else if (m < n) j++;
                else {
                    i++; j++;
                }
            } else {
                i++; j++;
            }
        }

        return true;
    }


    public static void main(String[] args) {
        AutocompleteSystem system = new AutocompleteSystem();


        system.insert("java tutorial");
        system.insert("javascript");
        system.insert("java download");
        system.insert("java tutorial");
        system.insert("java tutorial");
        system.insert("javascript");


        System.out.println("Search results for 'jav':");
        List<String> results = system.search("jav");

        int rank = 1;
        for (String r : results) {
            System.out.println(rank++ + ". " + r);
        }


        System.out.println("\nTypo suggestions for 'jva':");
        System.out.println(system.suggestWithTypo("jva"));
    }
}
