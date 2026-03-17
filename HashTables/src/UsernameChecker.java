import java.util.*;

public class UsernameChecker {


    private Map<String, Integer> usernameMap;


    private Map<String, Integer> attemptCount;

    public UsernameChecker() {
        usernameMap = new HashMap<>();
        attemptCount = new HashMap<>();
    }


    public boolean checkAvailability(String username) {
        attemptCount.put(username, attemptCount.getOrDefault(username, 0) + 1);
        return !usernameMap.containsKey(username);
    }


    public boolean register(String username, int userId) {
        if (usernameMap.containsKey(username)) {
            return false;
        }
        usernameMap.put(username, userId);
        return true;
    }


    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();


        for (int i = 1; i <= 5; i++) {
            String newName = username + i;
            if (!usernameMap.containsKey(newName)) {
                suggestions.add(newName);
            }
        }


        if (username.contains("_")) {
            String alt = username.replace("_", ".");
            if (!usernameMap.containsKey(alt)) {
                suggestions.add(alt);
            }
        }


        String prefix = "the_" + username;
        if (!usernameMap.containsKey(prefix)) {
            suggestions.add(prefix);
        }

        String suffix = username + "_official";
        if (!usernameMap.containsKey(suffix)) {
            suggestions.add(suffix);
        }

        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }


    public String getMostAttempted() {
        String result = null;
        int max = 0;

        for (Map.Entry<String, Integer> entry : attemptCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                result = entry.getKey();
            }
        }

        return result;
    }


    public static void main(String[] args) {
        UsernameChecker checker = new UsernameChecker();

        checker.register("john_doe", 101);

        System.out.println(checker.checkAvailability("john_doe"));   // false
        System.out.println(checker.checkAvailability("jane_smith")); // true

        System.out.println(checker.suggestAlternatives("john_doe"));

        System.out.println(checker.getMostAttempted());
    }
}