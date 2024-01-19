import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    // Define the threshold values
    private static final int consecutiveDaysThreshold = 7;
    private static final int timeBetweenShiftsMin = 60;  // in minutes
    private static final int timeBetweenShiftsMax = 600;  // in minutes
    private static final int singleShiftThreshold = 840;  // in minutes (14 hours)

    // Map to store employee data
    private static Map<String, EmployeeData> employees = new HashMap<>();

    public static void main(String[] args) {
        analyzeEmployeeSchedule("sheet.csv");
    }

    private static void analyzeEmployeeSchedule(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;  // Skip the header row
                }

                String[] columns = line.split(",");

                // Assuming columns order is: Position ID, Position Status, Time, Time Out, ...
                String employeeName = columns[8]; // Change index to match the actual column index
                String positionId = columns[0]; // Adjust the index accordingly
                String positionStatus = columns[1]; // Adjust the index accordingly

                // Skip rows where time data is missing
                if (columns[2].isEmpty() || columns[3].isEmpty()) {
                    continue;
                }

                // Convert time strings to Date objects
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                Date timeIn = dateFormat.parse(columns[2]);
                Date timeOut = dateFormat.parse(columns[3]);

                employees.computeIfAbsent(employeeName, k -> new EmployeeData());

                // Check consecutive days
                if (employees.get(employeeName).lastShift != null &&
                        (timeIn.getTime() - employees.get(employeeName).lastShift.getTime()) / (1000 * 60 * 60 * 24) == 1) {
                    employees.get(employeeName).consecutiveDays++;
                } else {
                    employees.get(employeeName).consecutiveDays = 1;
                }

                // Check time between shifts
                if (employees.get(employeeName).lastShiftEnd != null) {
                    long timeBetweenShifts = (timeIn.getTime() - employees.get(employeeName).lastShiftEnd.getTime()) / (1000 * 60);
                    if (timeBetweenShifts > timeBetweenShiftsMin && timeBetweenShifts < timeBetweenShiftsMax) {
                        System.out.println(employeeName + " has less than 10 hours between shifts but greater than 1 hour.");
                    }
                }

                // Check single shift duration
                long shiftDuration = (timeOut.getTime() - timeIn.getTime()) / (1000 * 60);
                if (shiftDuration > singleShiftThreshold) {
                    System.out.println(employeeName + " has worked for more than 14 hours in a single shift.");
                }

                // Update employee data
                employees.get(employeeName).lastShift = timeIn;
                employees.get(employeeName).lastShiftEnd = timeOut;

                // Store position information
                Position position = new Position(positionId, positionStatus);
                employees.get(employeeName).positions.add(position);

                // Print employees with 7 consecutive days
                if (employees.get(employeeName).consecutiveDays == consecutiveDaysThreshold) {
                    System.out.println(employeeName + " has worked for 7 consecutive days.");
                }
            }

            // Write the console output to output.txt
            try (FileWriter writer = new FileWriter("output.txt")) {
                for (Map.Entry<String, EmployeeData> entry : employees.entrySet()) {
                    writer.write(entry.getKey() + " - " + entry.getValue().positions + "\n");
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static class EmployeeData {
        Date lastShift;
        Date lastShiftEnd;
        int consecutiveDays;
        List<Position> positions = new ArrayList<>();
    }

    private static class Position {
        String positionId;
        String positionStatus;

        public Position(String positionId, String positionStatus) {
            this.positionId = positionId;
            this.positionStatus = positionStatus;
        }

        @Override
        public String toString() {
            return "(" + positionId + ", " + positionStatus + ")";
        }
    }
}
