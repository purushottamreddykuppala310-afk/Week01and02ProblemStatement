import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SocialMedia {

    // username -> userId
    private ConcurrentHashMap<String, Integer> usernameMap;

    // username -> attempt count
    private ConcurrentHashMap<String, AtomicInteger> attemptMap;

    private volatile String mostAttempted;
    private volatile int maxAttempts;

    public SocialMedia() {
        usernameMap = new ConcurrentHashMap<>();
        attemptMap = new ConcurrentHashMap<>();
        mostAttempted = "";
        maxAttempts = 0;
    }

    // Check availability in O(1)
    public boolean checkAvailability(String username) {
        updateAttempts(username);
        return !usernameMap.containsKey(username);
    }

    // Register username
    public boolean registerUser(String username, int userId) {
        if (checkAvailability(username)) {
            usernameMap.put(username, userId);
            return true;
        }
        return false;
    }

    // Update attempt frequency
    private void updateAttempts(String username) {
        attemptMap.putIfAbsent(username, new AtomicInteger(0));
        int count = attemptMap.get(username).incrementAndGet();

        synchronized (this) {
            if (count > maxAttempts) {
                maxAttempts = count;
                mostAttempted = username;
            }
        }
    }

    // Suggest alternatives
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();

        // Append numbers
        for (int i = 1; i <= 5; i++) {
            String newUsername = username + i;
            if (!usernameMap.containsKey(newUsername)) {
                suggestions.add(newUsername);
            }
        }

        // Replace underscore with dot
        if (username.contains("_")) {
            String dotVersion = username.replace("_", ".");
            if (!usernameMap.containsKey(dotVersion)) {
                suggestions.add(dotVersion);
            }
        }

        // Add random 3-digit suffix
        String randomSuffix = username + new Random().nextInt(999);
        if (!usernameMap.containsKey(randomSuffix)) {
            suggestions.add(randomSuffix);
        }

        return suggestions;
    }

    // Get most attempted username
    public String getMostAttempted() {
        return mostAttempted + " (" + maxAttempts + " attempts)";
    }

    // Main method for testing
    public static void main(String[] args) {
        SocialMedia system = new SocialMedia();

        // Pre-register some usernames
        system.registerUser("john_doe", 1001);
        system.registerUser("admin", 1002);

        System.out.println("checkAvailability(\"john_doe\") → "
                + system.checkAvailability("john_doe"));

        System.out.println("checkAvailability(\"jane_smith\") → "
                + system.checkAvailability("jane_smith"));

        System.out.println("Suggestions for john_doe → "
                + system.suggestAlternatives("john_doe"));

        // Simulate multiple attempts for "admin"
        for (int i = 0; i < 10543; i++) {
            system.checkAvailability("admin");
        }

        System.out.println("Most Attempted → "
                + system.getMostAttempted());
    }
}