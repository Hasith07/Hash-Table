import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedRateLimiter {


    static class TokenBucket {
        private final int maxTokens;
        private final int refillRatePerSec;

        private AtomicInteger tokens;
        private volatile long lastRefillTime;

        public TokenBucket(int maxTokens, int refillRatePerSec) {
            this.maxTokens = maxTokens;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = new AtomicInteger(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
        }


        public synchronized boolean allowRequest() {
            refill();

            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTime;

            int tokensToAdd = (int) (elapsedTime / 1000 * refillRatePerSec);

            if (tokensToAdd > 0) {
                int newTokenCount = Math.min(maxTokens, tokens.get() + tokensToAdd);
                tokens.set(newTokenCount);
                lastRefillTime = now;
            }
        }

        public int getRemainingTokens() {
            return tokens.get();
        }

        public long getRetryAfterSeconds() {
            if (tokens.get() > 0) return 0;

            long now = System.currentTimeMillis();
            long nextRefill = lastRefillTime + 1000;
            return Math.max(1, (nextRefill - now) / 1000);
        }
    }


    private ConcurrentHashMap<String, TokenBucket> clientBuckets;

    private final int MAX_TOKENS = 1000;
    private final int REFILL_RATE = 1000 / 3600; // ~0.27 tokens/sec

    public DistributedRateLimiter() {
        clientBuckets = new ConcurrentHashMap<>();
    }


    public String checkRateLimit(String clientId) {

        TokenBucket bucket = clientBuckets.computeIfAbsent(
                clientId,
                k -> new TokenBucket(MAX_TOKENS, REFILL_RATE)
        );

        boolean allowed = bucket.allowRequest();

        if (allowed) {
            return "Allowed (" + bucket.getRemainingTokens() + " requests remaining)";
        } else {
            return "Denied (0 remaining, retry after " +
                    bucket.getRetryAfterSeconds() + "s)";
        }
    }


    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);

        if (bucket == null) {
            return "No data for client";
        }

        int used = MAX_TOKENS - bucket.getRemainingTokens();

        return "{used: " + used +
                ", limit: " + MAX_TOKENS +
                ", remaining: " + bucket.getRemainingTokens() + "}";
    }


    public static void main(String[] args) throws InterruptedException {
        DistributedRateLimiter limiter = new DistributedRateLimiter();

        String client = "abc123";


        for (int i = 0; i < 5; i++) {
            System.out.println(limiter.checkRateLimit(client));
        }


        for (int i = 0; i < 1000; i++) {
            limiter.checkRateLimit(client);
        }

        System.out.println(limiter.checkRateLimit(client));

        System.out.println(limiter.getRateLimitStatus(client));
    }
}