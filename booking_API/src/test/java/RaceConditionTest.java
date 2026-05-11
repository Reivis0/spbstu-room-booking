import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Race Condition Test for Booking System
 * Tests: 100% exclusion of race conditions using concurrent booking requests
 * 
 * Compile: javac -cp .:jackson-databind-2.15.0.jar:jackson-core-2.15.0.jar:jackson-annotations-2.15.0.jar RaceConditionTest.java
 * Run: java -cp .:jackson-databind-2.15.0.jar:jackson-core-2.15.0.jar:jackson-annotations-2.15.0.jar RaceConditionTest
 */
public class RaceConditionTest {
    
    private static final String BASE_URL = "http://127.0.0.1:8081/api/v1";
    private static final String TEST_EMAIL = "student@arch.local";
    private static final String TEST_PASSWORD = "password";
    
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger conflictCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static class BookingRequest {
        public String roomId;
        public String startsAt;
        public String endsAt;
        public String title;
        
        public BookingRequest(String roomId, String startsAt, String endsAt, String title) {
            this.roomId = roomId;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.title = title;
        }
    }
    
    private static String getAuthToken() throws Exception {
        String loginPayload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}", 
            TEST_EMAIL, TEST_PASSWORD
        );
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Login failed with status " + response.statusCode());
        }
        
        JsonNode jsonNode = objectMapper.readTree(response.body());
        return jsonNode.get("access_token").asText();
    }
    
    private static void createBooking(String token, String roomId, OffsetDateTime startsAt, 
                                     OffsetDateTime endsAt, CountDownLatch latch, 
                                     List<String> results) {
        try {
            // Wait for all threads to be ready
            latch.countDown();
            latch.await();
            
            BookingRequest booking = new BookingRequest(
                roomId,
                startsAt.toString(),
                endsAt.toString(),
                "Race Test " + System.nanoTime()
            );
            
            String payload = objectMapper.writeValueAsString(booking);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/bookings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            synchronized (results) {
                switch (response.statusCode()) {
                    case 201:
                        successCount.incrementAndGet();
                        results.add("SUCCESS: Booking created (status " + response.statusCode() + ")");
                        break;
                    case 409:
                        conflictCount.incrementAndGet();
                        results.add("CONFLICT: Booking conflict detected (status " + response.statusCode() + ")");
                        break;
                    default:
                        errorCount.incrementAndGet();
                        results.add("ERROR: Unexpected status " + response.statusCode());
                }
            }
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            synchronized (results) {
                results.add("ERROR: " + e.getMessage());
            }
        }
    }
    
    private static void testRaceCondition(int concurrency, String roomId) throws Exception {
        System.out.println("\n=== Testing Race Condition with " + concurrency + " concurrent requests ===");
        System.out.println("Target: Same room, same time slot");
        System.out.println("Expected: Exactly 1 success, " + (concurrency - 1) + " conflicts (409)\n");
        
        // Get auth token
        String token = getAuthToken();
        
        // Reset counters
        successCount.set(0);
        conflictCount.set(0);
        errorCount.set(0);
        
        // Same time slot for all requests
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endsAt = startsAt.plusHours(2);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(concurrency);
        List<String> results = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Launch concurrent requests
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> createBooking(token, roomId, startsAt, endsAt, latch, results));
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Print results
        System.out.println("\nDetailed Results:");
        for (String result : results) {
            System.out.println("  - " + result);
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("Total Requests: " + concurrency);
        System.out.println("Successful (201): " + successCount.get());
        System.out.println("Conflicts (409): " + conflictCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Avg Latency: " + (duration / concurrency) + "ms");
        
        // Validation
        int success = successCount.get();
        int conflicts = conflictCount.get();
        int errors = errorCount.get();
        
        System.out.println("\n=== Validation ===");
        if (success == 1 && conflicts == (concurrency - 1) && errors == 0) {
            System.out.println("✅ PASS: Race condition handled correctly");
            System.out.println("   - Exactly 1 booking succeeded");
            System.out.println("   - All other requests received 409 Conflict");
            System.out.println("   - No double bookings detected");
        } else {
            System.out.println("❌ FAIL: Race condition NOT handled correctly");
            if (success > 1) {
                System.out.println("   - CRITICAL: " + success + " bookings succeeded (expected 1)");
                System.out.println("   - This indicates a DOUBLE BOOKING bug!");
            }
            if (errors > 0) {
                System.out.println("   - " + errors + " requests resulted in errors");
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            String testRoomId = "550e8400-e29b-41d4-a716-446655440000";
            
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║   RACE CONDITION TEST - Booking System                ║");
            System.out.println("║   Testing: 100% exclusion of race conditions           ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            
            // Test 1: 10 concurrent requests
            testRaceCondition(10, testRoomId);
            Thread.sleep(2000);
            
            // Test 2: 50 concurrent requests
            testRaceCondition(50, testRoomId);
            Thread.sleep(2000);
            
            // Test 3: 100 concurrent requests (stress test)
            testRaceCondition(100, testRoomId);
            
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║   Test Complete                                        ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
