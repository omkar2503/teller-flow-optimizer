import java.util.*;

public class test_enhanced_stats {
    public static void main(String[] args) {
        // Test enhanced statistics tracking
        List<Integer> customerWaitTimes = new ArrayList<>();
        int totalCustomerWaitTime = 0;
        int maxWaitTime = 0;
        
        // Simulate some wait times
        int[] testWaitTimes = {5, 10, 3, 15, 8};
        
        for (int waitTime : testWaitTimes) {
            customerWaitTimes.add(waitTime);
            totalCustomerWaitTime += waitTime;
            if (waitTime > maxWaitTime) {
                maxWaitTime = waitTime;
            }
        }
        
        // Calculate average
        double avgWaitTime = (double)totalCustomerWaitTime / customerWaitTimes.size();
        
        System.out.println("Enhanced Statistics Test:");
        System.out.println("Average wait time: " + String.format("%.2f", avgWaitTime));
        System.out.println("Maximum wait time: " + maxWaitTime);
        System.out.println("Total customers: " + customerWaitTimes.size());
        
        // Test utilization calculation
        int totalTellerBusyTime = 15;
        int totalTellerIdleTime = 5;
        double utilization = (double)totalTellerBusyTime / (totalTellerBusyTime + totalTellerIdleTime) * 100;
        
        System.out.println("Teller utilization: " + String.format("%.2f", utilization) + "%");
    }
} 