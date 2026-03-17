import java.util.*;
import java.time.*;

public class TransactionAnalyzer {

    static class Transaction {
        int id;
        double amount;
        String merchant;
        String account;
        LocalDateTime time;

        public Transaction(int id, double amount, String merchant, String account, LocalDateTime time) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.account = account;
            this.time = time;
        }
    }

    private List<Transaction> transactions;

    public TransactionAnalyzer(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // Classic Two-Sum
    public List<int[]> findTwoSum(double target) {
        Map<Double, Transaction> map = new HashMap<>();
        List<int[]> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(new int[]{map.get(complement).id, t.id});
            }
            map.put(t.amount, t);
        }
        return result;
    }

    // Two-Sum within 1-hour window
    public List<int[]> findTwoSumTimeWindow(double target, int windowMinutes) {
        Map<Double, List<Transaction>> map = new HashMap<>();
        List<int[]> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;

            if (map.containsKey(complement)) {
                for (Transaction other : map.get(complement)) {
                    long minutes = Duration.between(other.time, t.time).toMinutes();
                    if (Math.abs(minutes) <= windowMinutes) {
                        result.add(new int[]{other.id, t.id});
                    }
                }
            }

            map.computeIfAbsent(t.amount, k -> new ArrayList<>()).add(t);
        }

        return result;
    }

    // K-Sum (recursive)
    public List<List<Integer>> findKSum(int k, double target) {
        List<List<Integer>> result = new ArrayList<>();
        transactions.sort(Comparator.comparingDouble(a -> a.amount));
        kSumHelper(0, k, target, new ArrayList<>(), result);
        return result;
    }

    private void kSumHelper(int start, int k, double target, List<Integer> path, List<List<Integer>> result) {
        if (k == 2) {
            int left = start, right = transactions.size() - 1;
            while (left < right) {
                double sum = transactions.get(left).amount + transactions.get(right).amount;
                if (Math.abs(sum - target) < 1e-6) {
                    List<Integer> res = new ArrayList<>(path);
                    res.add(transactions.get(left).id);
                    res.add(transactions.get(right).id);
                    result.add(res);
                    left++; right--;
                } else if (sum < target) left++;
                else right--;
            }
            return;
        }

        for (int i = start; i < transactions.size(); i++) {
            path.add(transactions.get(i).id);
            kSumHelper(i + 1, k - 1, target - transactions.get(i).amount, path, result);
            path.remove(path.size() - 1);
        }
    }

    // Detect duplicates: same amount, same merchant, different accounts
    public List<Map<String, Object>> detectDuplicates() {
        Map<String, Map<Double, Set<String>>> map = new HashMap<>();
        List<Map<String, Object>> duplicates = new ArrayList<>();

        for (Transaction t : transactions) {
            map.putIfAbsent(t.merchant, new HashMap<>());
            Map<Double, Set<String>> merchantMap = map.get(t.merchant);
            merchantMap.putIfAbsent(t.amount, new HashSet<>());
            merchantMap.get(t.amount).add(t.account);
        }

        for (String merchant : map.keySet()) {
            for (Double amount : map.get(merchant).keySet()) {
                Set<String> accounts = map.get(merchant).get(amount);
                if (accounts.size() > 1) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("merchant", merchant);
                    entry.put("amount", amount);
                    entry.put("accounts", accounts);
                    duplicates.add(entry);
                }
            }
        }

        return duplicates;
    }

    // Testing
    public static void main(String[] args) {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(1, 500, "Store A", "acc1", LocalDateTime.of(2026,3,17,10,0)));
        transactions.add(new Transaction(2, 300, "Store B", "acc2", LocalDateTime.of(2026,3,17,10,15)));
        transactions.add(new Transaction(3, 200, "Store C", "acc3", LocalDateTime.of(2026,3,17,10,30)));
        transactions.add(new Transaction(4, 500, "Store A", "acc2", LocalDateTime.of(2026,3,17,11,0)));

        TransactionAnalyzer analyzer = new TransactionAnalyzer(transactions);

        System.out.println("Classic Two-Sum (target=500): " + analyzer.findTwoSum(500));
        System.out.println("Two-Sum 1-hour window (target=500): " + analyzer.findTwoSumTimeWindow(500, 60));
        System.out.println("K-Sum k=3, target=1000: " + analyzer.findKSum(3, 1000));
        System.out.println("Duplicates: " + analyzer.detectDuplicates());
    }
}