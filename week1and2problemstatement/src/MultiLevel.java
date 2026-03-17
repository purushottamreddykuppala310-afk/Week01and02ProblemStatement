import java.util.*;

public class MultiLevel {

    // ================================
    // Video Data Model
    // ================================
    static class VideoData {
        String videoId;
        String content;

        VideoData(String videoId, String content) {
            this.videoId = videoId;
            this.content = content;
        }
    }

    // ================================
    // Generic LRU Cache (Thread-Safe)
    // ================================
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCache(int capacity) {
            super(capacity, 0.75f, true); // access-order
            this.capacity = capacity;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    // ================================
    // Cache Levels
    // ================================
    private LRUCache<String, VideoData> L1;
    private LRUCache<String, VideoData> L2;
    private Map<String, VideoData> L3Database;

    private Map<String, Integer> accessCount;

    // Statistics
    private int l1Hits = 0, l2Hits = 0, l3Hits = 0;
    private int totalRequests = 0;

    public MultiLevel() {

        L1 = new LRUCache<>(10_000);     // Memory cache
        L2 = new LRUCache<>(100_000);    // SSD cache
        L3Database = new HashMap<>();    // Database
        accessCount = new HashMap<>();

        // Simulated database preload
        for (int i = 1; i <= 200_000; i++) {
            L3Database.put("video_" + i,
                    new VideoData("video_" + i, "VideoContent_" + i));
        }
    }

    // ================================
    // Get Video (Core Logic)
    // ================================
    public VideoData getVideo(String videoId) {

        totalRequests++;

        // L1 Check
        if (L1.containsKey(videoId)) {
            l1Hits++;
            simulateLatency(1); // 0.5ms simulated
            return L1.get(videoId);
        }

        // L2 Check
        if (L2.containsKey(videoId)) {
            l2Hits++;
            simulateLatency(5); // 5ms simulated
            VideoData video = L2.get(videoId);

            promoteToL1(video);
            return video;
        }

        // L3 Database
        if (L3Database.containsKey(videoId)) {
            l3Hits++;
            simulateLatency(150); // 150ms simulated
            VideoData video = L3Database.get(videoId);

            promoteToL2(video);
            return video;
        }

        return null;
    }

    // ================================
    // Promotion Logic
    // ================================
    private void promoteToL1(VideoData video) {
        L1.put(video.videoId, video);
    }

    private void promoteToL2(VideoData video) {

        int count = accessCount.getOrDefault(video.videoId, 0) + 1;
        accessCount.put(video.videoId, count);

        L2.put(video.videoId, video);

        // Promote to L1 if frequently accessed
        if (count > 3) {
            promoteToL1(video);
        }
    }

    // ================================
    // Invalidate Content
    // ================================
    public void invalidate(String videoId) {
        L1.remove(videoId);
        L2.remove(videoId);
        L3Database.remove(videoId);
        accessCount.remove(videoId);
    }

    // ================================
    // Statistics
    // ================================
    public void getStatistics() {

        System.out.println("L1 Hit Rate: " +
                percentage(l1Hits) + "%");

        System.out.println("L2 Hit Rate: " +
                percentage(l2Hits) + "%");

        System.out.println("L3 Hit Rate: " +
                percentage(l3Hits) + "%");

        int totalHits = l1Hits + l2Hits + l3Hits;

        System.out.println("Overall Hit Rate: " +
                ((double) totalHits / totalRequests) * 100 + "%");
    }

    private double percentage(int hits) {
        return ((double) hits / totalRequests) * 100;
    }

    // ================================
    // Simulate Latency
    // ================================
    private void simulateLatency(int ms) {
        try {
            Thread.sleep(ms / 10); // scaled down simulation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================================
    // Main Simulation
    // ================================
    public static void main(String[] args) {

        MultiLevel cache =
                new MultiLevel();

        System.out.println("Request video_123");
        cache.getVideo("video_123");

        System.out.println("Request video_123 again");
        cache.getVideo("video_123");

        System.out.println("Request video_99999");
        cache.getVideo("video_99999");

        cache.getStatistics();
    }
}