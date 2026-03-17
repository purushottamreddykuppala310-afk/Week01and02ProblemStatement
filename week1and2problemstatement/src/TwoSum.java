import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class TwoSum {

    // ===== Transaction Class =====
    static class Transaction {
        int id;
        double amount;
        String merchant;
        String account;
        LocalDateTime time;

        Transaction(int id, double amount,
                    String merchant,
                    String account,
                    LocalDateTime time) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.account = account;
            this.time = time;
        }

        public String toString() {
            return "id:" + id;
        }
    }

    private List<Transaction> transactions;

    public TwoSum(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // =====================================================
    // 1️⃣ Classic Two-Sum (O(n))
    // =====================================================
    public List<List<Transaction>> findTwoSum(double target) {

        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;

            if (map.containsKey(complement)) {
                result.add(Arrays.asList(map.get(complement), t));
            }
            map.put(t.amount, t);
        }

        return result;
    }

    // =====================================================
    // 2️⃣ Two-Sum within 1 Hour Window
    // =====================================================
    public List<List<Transaction>> findTwoSumWithTimeWindow(double target) {

        Map<Double, List<Transaction>> amountMap = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {

            double complement = target - t.amount;

            if (amountMap.containsKey(complement)) {
                for (Transaction prev : amountMap.get(complement)) {

                    long minutes =
                            ChronoUnit.MINUTES.between(prev.time, t.time);

                    if (Math.abs(minutes) <= 60) {
                        result.add(Arrays.asList(prev, t));
                    }
                }
            }

            amountMap
                    .computeIfAbsent(t.amount, k -> new ArrayList<>())
                    .add(t);
        }

        return result;
    }

    // =====================================================
    // 3️⃣ K-Sum (Recursive Backtracking + Sorting)
    // =====================================================
    public List<List<Transaction>> findKSum(int k, double target) {

        List<List<Transaction>> result = new ArrayList<>();

        transactions.sort(Comparator.comparingDouble(t -> t.amount));

        kSumHelper(0, k, target, new ArrayList<>(), result);

        return result;
    }

    private void kSumHelper(int start, int k, double target,
                            List<Transaction> current,
                            List<List<Transaction>> result) {

        if (k == 0 && target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        if (k == 0 || start >= transactions.size())
            return;

        for (int i = start; i < transactions.size(); i++) {

            Transaction t = transactions.get(i);

            if (t.amount > target) break;

            current.add(t);
            kSumHelper(i + 1, k - 1,
                    target - t.amount,
                    current, result);
            current.remove(current.size() - 1);
        }
    }

    // =====================================================
    // 4️⃣ Duplicate Detection
    // Same amount + same merchant + different accounts
    // =====================================================
    public Map<String, Set<String>> detectDuplicates() {

        Map<String, Set<String>> duplicateMap = new HashMap<>();

        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;

            duplicateMap
                    .computeIfAbsent(key, k -> new HashSet<>())
                    .add(t.account);
        }

        // Filter only suspicious duplicates
        Map<String, Set<String>> suspicious = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry :
                duplicateMap.entrySet()) {

            if (entry.getValue().size() > 1) {
                suspicious.put(entry.getKey(),
                        entry.getValue());
            }
        }

        return suspicious;
    }

    // =====================================================
    // MAIN METHOD (Sample Execution)
    // =====================================================
    public static void main(String[] args) {

        List<Transaction> list = new ArrayList<>();

        list.add(new Transaction(1, 500,
                "Store A", "acc1",
                LocalDateTime.of(2024,1,1,10,0)));

        list.add(new Transaction(2, 300,
                "Store B", "acc2",
                LocalDateTime.of(2024,1,1,10,15)));

        list.add(new Transaction(3, 200,
                "Store C", "acc3",
                LocalDateTime.of(2024,1,1,10,30)));

        list.add(new Transaction(4, 500,
                "Store A", "acc2",
                LocalDateTime.of(2024,1,1,11,0)));

        TwoSum system =
                new TwoSum(list);

        System.out.println("Two-Sum (500): "
                + system.findTwoSum(500));

        System.out.println("Two-Sum with 1-hour window (500): "
                + system.findTwoSumWithTimeWindow(500));

        System.out.println("K-Sum (k=3, target=1000): "
                + system.findKSum(3, 1000));

        System.out.println("Duplicate Detection: "
                + system.detectDuplicates());
    }
}
