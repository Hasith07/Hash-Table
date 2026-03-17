import java.util.*;
import java.util.concurrent.*;

public class DNSCacheSystem {

    // DNS Entry class
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

    private final int capacity;

    // LRU Cache using LinkedHashMap
    private final Map<String, DNSEntry> cache;

    // Stats
    private long hits = 0;
    private long misses = 0;

    public DNSCacheSystem(int capacity) {
        this.capacity = capacity;

        this.cache = new LinkedHashMap<String, DNSEntry>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > DNSCacheSystem.this.capacity;
            }
        };

        startCleanupThread();
    }

    // Resolve domain
    public synchronized String resolve(String domain) {
        DNSEntry entry = cache.get(domain);

        if (entry != null) {
            if (!entry.isExpired()) {
                hits++;
                return "Cache HIT → " + entry.ipAddress;
            } else {
                cache.remove(domain);
            }
        }

        misses++;
        String ip = queryUpstreamDNS(domain);
        cache.put(domain, new DNSEntry(domain, ip, 5)); // TTL = 5 sec (example)

        return "Cache MISS → " + ip;
    }

    // Simulated upstream DNS query
    private String queryUpstreamDNS(String domain) {
        try {
            Thread.sleep(100); // simulate delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "192.168.1." + new Random().nextInt(255);
    }

    // Background cleanup thread
    private void startCleanupThread() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    cleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleaner.setDaemon(true);
        cleaner.start();
    }

    // Remove expired entries
    private synchronized void cleanup() {
        Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DNSEntry> entry = it.next();
            if (entry.getValue().isExpired()) {
                it.remove();
            }
        }
    }

    // Get stats
    public String getStats() {
        long total = hits + misses;
        double hitRate = (total == 0) ? 0 : (hits * 100.0 / total);
        return "Hits: " + hits + ", Misses: " + misses + ", Hit Rate: " + hitRate + "%";
    }

    // Main method for testing
    public static void main(String[] args) throws InterruptedException {
        DNSCacheSystem dnsCache = new DNSCacheSystem(3);

        System.out.println(dnsCache.resolve("google.com"));
        System.out.println(dnsCache.resolve("google.com"));

        Thread.sleep(6000); // wait for TTL expiry

        System.out.println(dnsCache.resolve("google.com"));

        System.out.println(dnsCache.getStats());
    }
}
