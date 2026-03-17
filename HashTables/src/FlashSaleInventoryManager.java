import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventoryManager {


    private ConcurrentHashMap<String, AtomicInteger> stockMap;

    private ConcurrentHashMap<String, Queue<Integer>> waitingList;

    public FlashSaleInventoryManager() {
        stockMap = new ConcurrentHashMap<>();
        waitingList = new ConcurrentHashMap<>();
    }


    public void addProduct(String productId, int stock) {
        stockMap.put(productId, new AtomicInteger(stock));
        waitingList.put(productId, new ConcurrentLinkedQueue<>());
    }


    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        return (stock != null) ? stock.get() : 0;
    }


    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);

        if (stock == null) {
            return "Product not found";
        }

        while (true) {
            int currentStock = stock.get();

            if (currentStock <= 0) {
                Queue<Integer> queue = waitingList.get(productId);
                queue.offer(userId);
                return "Out of stock. Added to waiting list. Position #" + queue.size();
            }


            if (stock.compareAndSet(currentStock, currentStock - 1)) {
                return "Success! Remaining stock: " + (currentStock - 1);
            }
        }
    }

    public void restock(String productId, int quantity) {
        AtomicInteger stock = stockMap.get(productId);
        Queue<Integer> queue = waitingList.get(productId);

        if (stock == null) return;

        stock.addAndGet(quantity);

        while (stock.get() > 0 && !queue.isEmpty()) {
            int userId = queue.poll();
            stock.decrementAndGet();
            System.out.println("Allocated to waiting user: " + userId +
                    ", Remaining stock: " + stock.get());
        }
    }


    public static void main(String[] args) {
        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();

        String product = "IPHONE15_256GB";


        manager.addProduct(product, 3);


        System.out.println("Stock: " + manager.checkStock(product));


        System.out.println(manager.purchaseItem(product, 101));
        System.out.println(manager.purchaseItem(product, 102));
        System.out.println(manager.purchaseItem(product, 103));


        System.out.println(manager.purchaseItem(product, 104));
        System.out.println(manager.purchaseItem(product, 105));


        System.out.println("\nRestocking...");
        manager.restock(product, 2);


        System.out.println("Final Stock: " + manager.checkStock(product));
    }
}