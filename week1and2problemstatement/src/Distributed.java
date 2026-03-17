import java.util.concurrent.*;

public class Distributed {

    // ===== Token Bucket Class =====
    static class TokenBucket {
        private final long maxTokens;
        private final double refillRatePerMillis; // tokens per ms
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long maxTokens, long refillDurationMillis) {
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            this.refillRatePerMillis = (double) maxTokens / refillDurationMillis;
            this.lastRefillTime = System.currentTimeMillis();
        }

        // Try consuming 1 token
        public synchronized boolean allowRequest() {
            refill();

            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        // Refill tokens based on elapsed time
        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            double refillTokens = elapsed * refillRatePerMillis;
            tokens = Math.min(maxTokens, tokens + refillTokens);

            lastRefillTime = now;
        }

        public synchronized long getRemainingTokens() {
            refill();
            return (long) tokens;
        }

        public synchronized long getResetTimeSeconds() {
            if (tokens >= maxTokens) return 0;

            double tokensNeeded = maxTokens - tokens;
            long millisToFull = (long) (tokensNeeded / refillRatePerMillis);
            return millisToFull / 1000;
        }

        public long getMaxTokens() {
            return maxTokens;
        }
    }

    // ===== Rate Limiter Map =====
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets =
            new ConcurrentHashMap<>();

    private final long MAX_REQUESTS = 1000;
    private final long ONE_HOUR_MILLIS = 60 * 60 * 1000;

    // ===== Check Rate Limit =====
    public String checkRateLimit(String clientId) {

        TokenBucket bucket = clientBuckets.computeIfAbsent(
                clientId,
                id -> new TokenBucket(MAX_REQUESTS, ONE_HOUR_MILLIS)
        );

        boolean allowed = bucket.allowRequest();

        if (allowed) {
            return "Allowed (" + bucket.getRemainingTokens() +
                    " requests remaining)";
        } else {
            return "Denied (0 requests remaining, retry after " +
                    bucket.getResetTimeSeconds() + "s)";
        }
    }

    // ===== Get Client Status =====
    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);

        if (bucket == null) {
            return "{used: 0, limit: " + MAX_REQUESTS + ", reset: 0}";
        }

        long remaining = bucket.getRemainingTokens();
        long used = bucket.getMaxTokens() - remaining;
        long reset = bucket.getResetTimeSeconds();

        return "{used: " + used +
                ", limit: " + MAX_REQUESTS +
                ", reset_in_seconds: " + reset + "}";
    }

    // ===== Main Method (Simulation) =====
    public static void main(String[] args) {

        Distributed limiter = new Distributed();

        String client = "abc123";

        // Simulate requests
        for (int i = 0; i < 1005; i++) {
            String result = limiter.checkRateLimit(client);
            if (i >= 995) {  // print last few
                System.out.println(result);
            }
        }

        System.out.println(limiter.getRateLimitStatus(client));
    }
}