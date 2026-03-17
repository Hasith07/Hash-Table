import java.util.*;

public class ParkingLotSystem {

    // Slot status
    enum Status {
        EMPTY, OCCUPIED, DELETED
    }

    // Parking slot
    static class Slot {
        String licensePlate;
        long entryTime;
        Status status;

        Slot() {
            this.status = Status.EMPTY;
        }
    }

    private Slot[] table;
    private int capacity;
    private int size;

    // Stats
    private int totalProbes = 0;
    private int totalParks = 0;
    private Map<Integer, Integer> hourlyTraffic;

    public ParkingLotSystem(int capacity) {
        this.capacity = capacity;
        this.table = new Slot[capacity];
        this.size = 0;
        this.hourlyTraffic = new HashMap<>();

        for (int i = 0; i < capacity; i++) {
            table[i] = new Slot();
        }
    }

    // Hash function
    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % capacity;
    }

    // Park vehicle (Linear Probing)
    public String parkVehicle(String licensePlate) {
        if (size >= capacity) {
            return "Parking Full!";
        }

        int index = hash(licensePlate);
        int probes = 0;

        while (table[index].status == Status.OCCUPIED) {
            index = (index + 1) % capacity;
            probes++;
        }

        table[index].licensePlate = licensePlate;
        table[index].entryTime = System.currentTimeMillis();
        table[index].status = Status.OCCUPIED;

        size++;
        totalProbes += probes;
        totalParks++;

        // Track hourly traffic
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        hourlyTraffic.put(hour, hourlyTraffic.getOrDefault(hour, 0) + 1);

        return "Assigned spot #" + index + " (" + probes + " probes)";
    }

    // Exit vehicle
    public String exitVehicle(String licensePlate) {
        int index = hash(licensePlate);
        int probes = 0;

        while (table[index].status != Status.EMPTY) {
            if (table[index].status == Status.OCCUPIED &&
                    table[index].licensePlate.equals(licensePlate)) {

                long durationMillis = System.currentTimeMillis() - table[index].entryTime;
                double hours = durationMillis / (1000.0 * 60 * 60);
                double fee = hours * 5.0; // $5 per hour

                table[index].status = Status.DELETED;
                table[index].licensePlate = null;
                size--;

                return String.format(
                        "Spot #%d freed, Duration: %.2f hrs, Fee: $%.2f",
                        index, hours, fee
                );
            }

            index = (index + 1) % capacity;
            probes++;
        }

        return "Vehicle not found!";
    }

    // Find nearest available spot (from entrance = index 0)
    public int findNearestAvailable() {
        for (int i = 0; i < capacity; i++) {
            if (table[i].status != Status.OCCUPIED) {
                return i;
            }
        }
        return -1;
    }

    // Get statistics
    public String getStatistics() {
        double occupancy = (size * 100.0) / capacity;
        double avgProbes = totalParks == 0 ? 0 : (double) totalProbes / totalParks;

        // Find peak hour
        int peakHour = -1, max = 0;
        for (Map.Entry<Integer, Integer> entry : hourlyTraffic.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        return String.format(
                "Occupancy: %.2f%%, Avg Probes: %.2f, Peak Hour: %d-%d",
                occupancy, avgProbes, peakHour, (peakHour + 1) % 24
        );
    }

    // Main method (Testing)
    public static void main(String[] args) throws InterruptedException {
        ParkingLotSystem parking = new ParkingLotSystem(10);

        System.out.println(parking.parkVehicle("ABC-1234"));
        System.out.println(parking.parkVehicle("ABC-1235"));
        System.out.println(parking.parkVehicle("XYZ-9999"));

        Thread.sleep(2000);

        System.out.println(parking.exitVehicle("ABC-1234"));

        System.out.println("Nearest available: Spot #" + parking.findNearestAvailable());

        System.out.println(parking.getStatistics());
    }
}