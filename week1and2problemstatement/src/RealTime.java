import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RealTime {

    // pageUrl -> total visits
    private ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();

    // pageUrl -> unique visitors
    private ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();

    // traffic source -> count
    private ConcurrentHashMap<String, AtomicInteger> trafficSources = new ConcurrentHashMap<>();

    // Constructor: start dashboard updater
    public RealTime() {
        startDashboardUpdater();
    }

    // ===== Process Incoming Event =====
    public void processEvent(String url, String userId, String source) {

        // Update page view count (O(1))
        pageViews
                .computeIfAbsent(url, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Update unique visitors (O(1) average)
        uniqueVisitors
                .computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        // Update traffic source count (O(1))
        trafficSources
                .computeIfAbsent(source, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    // ===== Get Dashboard =====
    public void getDashboard() {

        System.out.println("\n===== REAL-TIME DASHBOARD =====");

        // Top 10 pages using Min-Heap
        PriorityQueue<Map.Entry<String, AtomicInteger>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(e -> e.getValue().get()));

        for (Map.Entry<String, AtomicInteger> entry : pageViews.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > 10) {
                minHeap.poll();
            }
        }

        List<Map.Entry<String, AtomicInteger>> topPages = new ArrayList<>(minHeap);
        topPages.sort((a, b) -> b.getValue().get() - a.getValue().get());

        System.out.println("Top Pages:");
        int rank = 1;
        for (Map.Entry<String, AtomicInteger> entry : topPages) {
            String page = entry.getKey();
            int views = entry.getValue().get();
            int unique = uniqueVisitors.get(page).size();

            System.out.println(rank++ + ". " + page +
                    " - " + views + " views (" + unique + " unique)");
        }

        // Traffic Source %
        System.out.println("\nTraffic Sources:");
        int totalSourceCount = trafficSources.values()
                .stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        for (Map.Entry<String, AtomicInteger> entry : trafficSources.entrySet()) {
            double percent = (entry.getValue().get() * 100.0) / totalSourceCount;
            System.out.printf("%s: %.2f%%\n", entry.getKey(), percent);
        }

        System.out.println("================================\n");
    }

    // ===== Auto Dashboard Update Every 5 Seconds =====
    private void startDashboardUpdater() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            getDashboard();
        }, 5, 5, TimeUnit.SECONDS);
    }

    // ===== Main Method (Simulation) =====
    public static void main(String[] args) throws InterruptedException {

        RealTime analytics = new RealTime();

        String[] pages = {
                "/article/breaking-news",
                "/sports/championship",
                "/tech/ai-future",
                "/entertainment/movies"
        };

        String[] sources = {"google", "facebook", "direct", "twitter"};

        Random random = new Random();

        // Simulate 10,000 page views
        for (int i = 0; i < 10000; i++) {

            String url = pages[random.nextInt(pages.length)];
            String userId = "user_" + random.nextInt(5000);
            String source = sources[random.nextInt(sources.length)];

            analytics.processEvent(url, userId, source);

            Thread.sleep(1); // simulate streaming delay
        }
    }
}