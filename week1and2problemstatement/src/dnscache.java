import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class dnscache {

    // ===== DNS Entry Class =====
    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime;

        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // ===== LRU Cache using LinkedHashMap =====
    private final int MAX_SIZE;
    private final Map<String, DNSEntry> cache;

    // Metrics
    private AtomicLong hits = new AtomicLong(0);
    private AtomicLong misses = new AtomicLong(0);
    private AtomicLong totalLookupTime = new AtomicLong(0);

    // Constructor
    public dnscache(int maxSize) {
        this.MAX_SIZE = maxSize;

        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<String, DNSEntry>(maxSize, 0.75f, true) {
                    protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                        return size() > MAX_SIZE;
                    }
                }
        );

        // Background cleanup thread
        startCleanupThread();
    }

    // ===== Resolve Method =====
    public String resolve(String domain) {
        long startTime = System.nanoTime();

        synchronized (cache) {
            DNSEntry entry = cache.get(domain);

            if (entry != null) {
                if (!entry.isExpired()) {
                    hits.incrementAndGet();
                    recordLookupTime(startTime);
                    System.out.println("Cache HIT → " + entry.ipAddress);
                    return entry.ipAddress;
                } else {
                    cache.remove(domain);
                    System.out.println("Cache EXPIRED → " + domain);
                }
            }
        }

        // Cache MISS
        misses.incrementAndGet();
        String ip = queryUpstreamDNS(domain);
        recordLookupTime(startTime);
        return ip;
    }

    // ===== Simulated Upstream DNS Query =====
    private String queryUpstreamDNS(String domain) {
        try {
            Thread.sleep(100); // simulate 100ms DNS lookup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulated dynamic IP
        String ip = "172.217.14." + new Random().nextInt(255);

        DNSEntry entry = new DNSEntry(domain, ip, 300); // TTL 300 sec

        synchronized (cache) {
            cache.put(domain, entry);
        }

        System.out.println("Cache MISS → Queried upstream → " + ip);
        return ip;
    }

    // ===== Cleanup Thread =====
    private void startCleanupThread() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // check every 5 seconds

                    synchronized (cache) {
                        Iterator<Map.Entry<String, DNSEntry>> iterator = cache.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<String, DNSEntry> entry = iterator.next();
                            if (entry.getValue().isExpired()) {
                                iterator.remove();
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        cleaner.setDaemon(true);
        cleaner.start();
    }

    // ===== Metrics =====
    private void recordLookupTime(long startTime) {
        long duration = System.nanoTime() - startTime;
        totalLookupTime.addAndGet(duration);
    }

    public void getCacheStats() {
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests == 0 ? 0 :
                (hits.get() * 100.0 / totalRequests);

        double avgLookupMs = totalRequests == 0 ? 0 :
                (totalLookupTime.get() / totalRequests) / 1_000_000.0;

        System.out.println("Hit Rate: " + String.format("%.2f", hitRate) + "%");
        System.out.println("Average Lookup Time: " +
                String.format("%.2f", avgLookupMs) + " ms");
    }

    // ===== Main Test =====
    public static void main(String[] args) throws InterruptedException {

        dnscache dnsCache = new dnscache(5);

        dnsCache.resolve("google.com");  // MISS
        dnsCache.resolve("google.com");  // HIT

        Thread.sleep(1000);

        dnsCache.resolve("openai.com");  // MISS
        dnsCache.resolve("google.com");  // HIT

        dnsCache.getCacheStats();
    }
}