import java.util.*;

public class MultiLevelCache {

    static class VideoData {
        String videoId;
        String content;

        public VideoData(String videoId, String content) {
            this.videoId = videoId;
            this.content = content;
        }
    }

    // LRU Cache using LinkedHashMap
    static class LRUCache<K,V> extends LinkedHashMap<K,V> {
        private int capacity;

        public LRUCache(int capacity) {
            super(capacity, 0.75f, true); // access-order
            this.capacity = capacity;
        }

        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > capacity;
        }
    }

    private LRUCache<String, VideoData> l1Cache; // memory
    private Map<String, VideoData> l2Cache;       // SSD simulation
    private Map<String, VideoData> l3Database;    // slow DB

    private Map<String, Integer> accessCount;     // for promotion
    private int promoteThreshold = 3;

    private int l1Hits = 0, l2Hits = 0, l3Hits = 0, totalRequests = 0;
    private double l1Time = 0, l2Time = 0, l3Time = 0;

    public MultiLevelCache() {
        l1Cache = new LRUCache<>(10000);
        l2Cache = new HashMap<>();
        l3Database = new HashMap<>();
        accessCount = new HashMap<>();
    }

    // Add video to DB
    public void addVideoToDB(VideoData video) {
        l3Database.put(video.videoId, video);
    }

    // Fetch video with multi-level caching
    public VideoData getVideo(String videoId) {
        totalRequests++;

        // L1 Cache
        if (l1Cache.containsKey(videoId)) {
            l1Hits++;
            l1Time += 0.5;
            return l1Cache.get(videoId);
        }

        // L2 Cache
        if (l2Cache.containsKey(videoId)) {
            l2Hits++;
            l2Time += 5;
            VideoData video = l2Cache.get(videoId);

            // Increase access count & promote if needed
            accessCount.put(videoId, accessCount.getOrDefault(videoId, 0) + 1);
            if (accessCount.get(videoId) >= promoteThreshold) {
                l1Cache.put(videoId, video);
            }

            return video;
        }

        // L3 Database
        if (l3Database.containsKey(videoId)) {
            l3Hits++;
            l3Time += 150;
            VideoData video = l3Database.get(videoId);

            // Add to L2 with access count 1
            l2Cache.put(videoId, video);
            accessCount.put(videoId, 1);

            return video;
        }

        return null; // video not found
    }

    // Invalidate cache for updated video
    public void invalidate(String videoId) {
        l1Cache.remove(videoId);
        l2Cache.remove(videoId);
        accessCount.remove(videoId);
    }

    // Get cache statistics
    public void getStatistics() {
        System.out.printf("L1: Hit Rate %.2f%%, Avg Time %.2fms\n",
                l1Hits*100.0/totalRequests, l1Time/totalRequests);
        System.out.printf("L2: Hit Rate %.2f%%, Avg Time %.2fms\n",
                l2Hits*100.0/totalRequests, l2Time/totalRequests);
        System.out.printf("L3: Hit Rate %.2f%%, Avg Time %.2fms\n",
                l3Hits*100.0/totalRequests, l3Time/totalRequests);

        double overallHitRate = (l1Hits + l2Hits + l3Hits) * 100.0 / totalRequests;
        double overallTime = (l1Time + l2Time + l3Time) / totalRequests;

        System.out.printf("Overall: Hit Rate %.2f%%, Avg Time %.2fms\n",
                overallHitRate, overallTime);
    }

    // Testing
    public static void main(String[] args) {
        MultiLevelCache cache = new MultiLevelCache();

        // Populate DB
        for (int i=1; i<=5; i++) {
            cache.addVideoToDB(new VideoData("video_"+i, "Content for video "+i));
        }

        // Access videos
        cache.getVideo("video_1"); // L3 → L2
        cache.getVideo("video_1"); // L2
        cache.getVideo("video_1"); // L2 → promote to L1
        cache.getVideo("video_1"); // L1

        cache.getVideo("video_2"); // L3 → L2
        cache.getVideo("video_3"); // L3 → L2
        cache.getVideo("video_3"); // L2

        // Stats
        cache.getStatistics();

        // Invalidate video_1 (updated)
        cache.invalidate("video_1");
        System.out.println("After invalidation:");
        cache.getVideo("video_1"); // Fetch again → L3 → L2
        cache.getStatistics();
    }
}