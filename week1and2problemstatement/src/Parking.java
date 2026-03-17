import java.util.*;

public class Parking {

    private static final int TOTAL_SPOTS = 500;
    private static final double HOURLY_RATE = 5.0;

    enum SpotStatus { EMPTY, OCCUPIED, DELETED }

    static class ParkingSpot {
        String licensePlate;
        long entryTime;
        SpotStatus status;

        ParkingSpot() {
            this.status = SpotStatus.EMPTY;
        }
    }

    private ParkingSpot[] table = new ParkingSpot[TOTAL_SPOTS];

    // Statistics
    private int occupiedCount = 0;
    private long totalProbes = 0;
    private int totalParkingOps = 0;
    private Map<Integer, Integer> hourlyTraffic = new HashMap<>();

    public Parking() {
        for (int i = 0; i < TOTAL_SPOTS; i++) {
            table[i] = new ParkingSpot();
        }
    }

    // ===== Custom Hash Function =====
    private int hash(String licensePlate) {
        int hash = 0;
        for (char c : licensePlate.toCharArray()) {
            hash = (hash * 31 + c) % TOTAL_SPOTS;
        }
        return Math.abs(hash);
    }

    // ===== Park Vehicle (Linear Probing) =====
    public String parkVehicle(String licensePlate) {

        if (occupiedCount >= TOTAL_SPOTS)
            return "Parking Full!";

        int index = hash(licensePlate);
        int probes = 0;

        while (table[index].status == SpotStatus.OCCUPIED) {
            index = (index + 1) % TOTAL_SPOTS;
            probes++;
        }

        table[index].licensePlate = licensePlate;
        table[index].entryTime = System.currentTimeMillis();
        table[index].status = SpotStatus.OCCUPIED;

        occupiedCount++;
        totalProbes += probes;
        totalParkingOps++;

        // Track peak hour
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hourlyTraffic.put(hour,
                hourlyTraffic.getOrDefault(hour, 0) + 1);

        return "Assigned spot #" + index +
                " (" + probes + " probes)";
    }

    // ===== Exit Vehicle =====
    public String exitVehicle(String licensePlate) {

        int index = hash(licensePlate);
        int probes = 0;

        while (table[index].status != SpotStatus.EMPTY) {

            if (table[index].status == SpotStatus.OCCUPIED &&
                    table[index].licensePlate.equals(licensePlate)) {

                long exitTime = System.currentTimeMillis();
                long durationMillis = exitTime - table[index].entryTime;

                double hours = durationMillis / (1000.0 * 60 * 60);
                double fee = Math.ceil(hours) * HOURLY_RATE;

                table[index].status = SpotStatus.DELETED;
                table[index].licensePlate = null;

                occupiedCount--;

                return "Spot #" + index + " freed, Duration: " +
                        String.format("%.2f", hours) +
                        "h, Fee: $" + String.format("%.2f", fee);
            }

            index = (index + 1) % TOTAL_SPOTS;
            probes++;
        }

        return "Vehicle not found!";
    }

    // ===== Find Nearest Available Spot (from entrance = 0) =====
    public int findNearestAvailable() {
        for (int i = 0; i < TOTAL_SPOTS; i++) {
            if (table[i].status != SpotStatus.OCCUPIED) {
                return i;
            }
        }
        return -1;
    }

    // ===== Statistics =====
    public void getStatistics() {

        double occupancyRate =
                (occupiedCount * 100.0) / TOTAL_SPOTS;

        double avgProbes =
                totalParkingOps == 0 ? 0 :
                        (double) totalProbes / totalParkingOps;

        int peakHour = -1;
        int maxTraffic = 0;

        for (Map.Entry<Integer, Integer> entry :
                hourlyTraffic.entrySet()) {

            if (entry.getValue() > maxTraffic) {
                maxTraffic = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        System.out.println("Occupancy: " +
                String.format("%.2f", occupancyRate) + "%");
        System.out.println("Avg Probes: " +
                String.format("%.2f", avgProbes));
        System.out.println("Peak Hour: " +
                peakHour + ":00 - " + (peakHour + 1) + ":00");
    }

    // ===== Main Method =====
    public static void main(String[] args)
            throws InterruptedException {

        Parking parking = new Parking();

        System.out.println(parking.parkVehicle("ABC-1234"));
        System.out.println(parking.parkVehicle("ABC-1235"));
        System.out.println(parking.parkVehicle("XYZ-9999"));

        Thread.sleep(2000); // simulate parking duration

        System.out.println(parking.exitVehicle("ABC-1234"));

        System.out.println("Nearest Available Spot: " +
                parking.findNearestAvailable());

        parking.getStatistics();
    }
}