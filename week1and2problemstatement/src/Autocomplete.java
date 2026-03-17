import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Autocomplete {

    // ===== Global Frequency Store =====
    private Map<String, Integer> frequencyMap = new ConcurrentHashMap<>();

    // ===== Trie Node =====
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private TrieNode root = new TrieNode();

    // ===== Insert Query into Trie =====
    public void insert(String query) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEndOfWord = true;
    }

    // ===== Update Frequency =====
    public void updateFrequency(String query) {
        frequencyMap.put(query, frequencyMap.getOrDefault(query, 0) + 1);

        // If new query, insert into trie
        if (frequencyMap.get(query) == 1) {
            insert(query);
        }
    }

    // ===== Search Top 10 Suggestions =====
    public List<String> search(String prefix) {

        long startTime = System.nanoTime();

        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                // Typo correction fallback
                return handleTypo(prefix);
            }
            node = node.children.get(c);
        }

        // Collect matches
        List<String> results = new ArrayList<>();
        dfs(node, new StringBuilder(prefix), results);

        // Min-Heap for Top 10 by frequency
        PriorityQueue<String> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(frequencyMap::get));

        for (String s : results) {
            minHeap.offer(s);
            if (minHeap.size() > 10) {
                minHeap.poll();
            }
        }

        List<String> topResults = new ArrayList<>(minHeap);
        topResults.sort((a, b) -> frequencyMap.get(b) - frequencyMap.get(a));

        long endTime = System.nanoTime();
        System.out.println("Search time: " +
                (endTime - startTime) / 1_000_000.0 + " ms");

        return topResults;
    }

    // ===== DFS to collect prefix matches =====
    private void dfs(TrieNode node, StringBuilder current,
                     List<String> results) {

        if (node.isEndOfWord) {
            results.add(current.toString());
        }

        for (char c : node.children.keySet()) {
            current.append(c);
            dfs(node.children.get(c), current, results);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // ===== Simple Typo Handling (Edit Distance 1) =====
    private List<String> handleTypo(String word) {
        List<String> corrections = new ArrayList<>();

        for (String query : frequencyMap.keySet()) {
            if (editDistanceOne(word, query)) {
                corrections.add(query);
            }
        }

        corrections.sort((a, b) -> frequencyMap.get(b) - frequencyMap.get(a));
        return corrections.size() > 10 ?
                corrections.subList(0, 10) : corrections;
    }

    private boolean editDistanceOne(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;

        int diff = 0, i = 0, j = 0;

        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) != b.charAt(j)) {
                if (++diff > 1) return false;
                if (a.length() > b.length()) i++;
                else if (a.length() < b.length()) j++;
                else { i++; j++; }
            } else {
                i++; j++;
            }
        }

        return true;
    }

    // ===== Main Method =====
    public static void main(String[] args) {

        Autocomplete system = new Autocomplete();

        // Simulated 10M queries (small sample here)
        system.updateFrequency("java tutorial");
        system.updateFrequency("javascript");
        system.updateFrequency("java download");
        system.updateFrequency("java 21 features");
        system.updateFrequency("java 21 features");
        system.updateFrequency("java 21 features");

        System.out.println("Search Results for 'jav':");
        List<String> results = system.search("jav");

        int rank = 1;
        for (String r : results) {
            System.out.println(rank++ + ". " + r +
                    " (" + system.frequencyMap.get(r) + " searches)");
        }

        System.out.println("\nUpdating frequency...");
        system.updateFrequency("java 21 features");
        system.updateFrequency("java 21 features");

        System.out.println("Updated Frequency: " +
                system.frequencyMap.get("java 21 features"));
    }
}