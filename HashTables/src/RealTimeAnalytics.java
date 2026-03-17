import java.util.*;
import java.util.concurrent.*;

public class RealTimeAnalytics {

    // pageUrl -> total visit count
    private ConcurrentHashMap<String, Integer> pageViews;

    // pageUrl -> unique users
    private ConcurrentHashMap<String, Set<String>> uniqueVisitors;

    // traffic source -> count
    private ConcurrentHashMap<String, Integer> sourceCount;

    public RealTimeAnalytics() {
        pageViews = new ConcurrentHashMap<>();
        uniqueVisitors = new ConcurrentHashMap<>();
        sourceCount = new ConcurrentHashMap<>();

        startDashboardUpdater();
    }

    // Process incoming event
    public void processEvent(String url, String userId, String source) {

        // Update page views
        pageViews.merge(url, 1, Integer::sum);

        // Update unique visitors
        uniqueVisitors
                .computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        // Update traffic source
        sourceCount.merge(source, 1, Integer::sum);
    }

    // Get top 10 pages
    private List<Map.Entry<String, Integer>> getTopPages() {
        PriorityQueue<Map.Entry<String, Integer>> minHeap =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : pageViews.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > 10) {
                minHeap.poll();
            }
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> b.getValue() - a.getValue());
        return result;
    }

    // Generate dashboard
    public void getDashboard() {
        System.out.println("\n===== REAL-TIME DASHBOARD =====");

        // Top pages
        System.out.println("\nTop Pages:");
        List<Map.Entry<String, Integer>> topPages = getTopPages();

        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPages) {
            String url = entry.getKey();
            int views = entry.getValue();
            int unique = uniqueVisitors.getOrDefault(url, Collections.emptySet()).size();

            System.out.println(rank + ". " + url +
                    " - " + views + " views (" + unique + " unique)");
            rank++;
        }

        // Traffic sources
        System.out.println("\nTraffic Sources:");
        int total = sourceCount.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : sourceCount.entrySet()) {
            double percent = (entry.getValue() * 100.0) / total;
            System.out.printf("%s: %.2f%%\n", entry.getKey(), percent);
        }

        System.out.println("================================\n");
    }

    // Auto update dashboard every 5 seconds
    private void startDashboardUpdater() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            getDashboard();
        }, 5, 5, TimeUnit.SECONDS);
    }

    // Main method (Simulation)
    public static void main(String[] args) throws InterruptedException {
        RealTimeAnalytics analytics = new RealTimeAnalytics();

        String[] urls = {
                "/article/breaking-news",
                "/sports/championship",
                "/tech/ai",
                "/health/tips"
        };

        String[] sources = {"google", "facebook", "direct", "twitter"};

        Random rand = new Random();

        // Simulate live traffic
        for (int i = 1; i <= 100; i++) {
            String url = urls[rand.nextInt(urls.length)];
            String userId = "user_" + rand.nextInt(50);
            String source = sources[rand.nextInt(sources.length)];

            analytics.processEvent(url, userId, source);

            Thread.sleep(50); // simulate stream
        }

        // Keep program alive to see dashboard updates
        Thread.sleep(20000);
    }
}