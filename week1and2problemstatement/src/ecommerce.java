import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ecommerce {

    // productId -> stock count
    private ConcurrentHashMap<String, AtomicInteger> stockMap;

    // productId -> waiting list (FIFO)
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> waitingListMap;

    public ecommerce() {
        stockMap = new ConcurrentHashMap<>();
        waitingListMap = new ConcurrentHashMap<>();
    }

    // Add product with stock
    public void addProduct(String productId, int stock) {
        stockMap.put(productId, new AtomicInteger(stock));
        waitingListMap.put(productId, new ConcurrentLinkedQueue<>());
    }

    // Check stock in O(1)
    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) return 0;
        return stock.get();
    }

    // Purchase item (O(1) atomic operation)
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);

        if (stock == null) {
            return "Product not found";
        }

        while (true) {
            int currentStock = stock.get();

            if (currentStock > 0) {
                // Atomic decrement
                if (stock.compareAndSet(currentStock, currentStock - 1)) {
                    return "Success for user " + userId +
                            ", Remaining stock: " + (currentStock - 1);
                }
            } else {
                // Add to waiting list (FIFO)
                ConcurrentLinkedQueue<Integer> queue = waitingListMap.get(productId);
                queue.add(userId);
                return "Out of stock. Added to waiting list, position #" + queue.size();
            }
        }
    }

    // View waiting list position
    public int getWaitingListSize(String productId) {
        return waitingListMap.get(productId).size();
    }

    // Simulate flash sale load
    public static void main(String[] args) throws InterruptedException {

        ecommerce system = new ecommerce();
        system.addProduct("IPHONE15_256GB", 100);

        System.out.println("Initial Stock: "
                + system.checkStock("IPHONE15_256GB") + " units available");

        int totalUsers = 50000;
        ExecutorService executor = Executors.newFixedThreadPool(200);

        for (int i = 1; i <= totalUsers; i++) {
            final int userId = i;
            executor.execute(() -> {
                String result = system.purchaseItem("IPHONE15_256GB", userId);
                if (userId <= 105) { // Print first few only
                    System.out.println(result);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Final Stock: "
                + system.checkStock("IPHONE15_256GB"));

        System.out.println("Waiting List Size: "
                + system.getWaitingListSize("IPHONE15_256GB"));
    }
}