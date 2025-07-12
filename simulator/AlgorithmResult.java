package simulator;

public class AlgorithmResult {
    private String algorithmName;
    private double avgWaitTime;
    private int maxWaitTime;
    private double avgTellerUtilization;
    private double queueEfficiency;
    private int totalCustomers;
    private int customersServed;
    private int customersGoneAway;
    private double avgServiceTime;
    private int peakQueueLength;
    private double avgQueueLength;
    
    public AlgorithmResult(String algorithmName) {
        this.algorithmName = algorithmName;
    }
    
    // Getters and setters
    public String getAlgorithmName() { return algorithmName; }
    public double getAvgWaitTime() { return avgWaitTime; }
    public void setAvgWaitTime(double avgWaitTime) { this.avgWaitTime = avgWaitTime; }
    public int getMaxWaitTime() { return maxWaitTime; }
    public void setMaxWaitTime(int maxWaitTime) { this.maxWaitTime = maxWaitTime; }
    public double getAvgTellerUtilization() { return avgTellerUtilization; }
    public void setAvgTellerUtilization(double avgTellerUtilization) { this.avgTellerUtilization = avgTellerUtilization; }
    public double getQueueEfficiency() { return queueEfficiency; }
    public void setQueueEfficiency(double queueEfficiency) { this.queueEfficiency = queueEfficiency; }
    public int getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
    public int getCustomersServed() { return customersServed; }
    public void setCustomersServed(int customersServed) { this.customersServed = customersServed; }
    public int getCustomersGoneAway() { return customersGoneAway; }
    public void setCustomersGoneAway(int customersGoneAway) { this.customersGoneAway = customersGoneAway; }
    public double getAvgServiceTime() { return avgServiceTime; }
    public void setAvgServiceTime(double avgServiceTime) { this.avgServiceTime = avgServiceTime; }
    public int getPeakQueueLength() { return peakQueueLength; }
    public void setPeakQueueLength(int peakQueueLength) { this.peakQueueLength = peakQueueLength; }
    public double getAvgQueueLength() { return avgQueueLength; }
    public void setAvgQueueLength(double avgQueueLength) { this.avgQueueLength = avgQueueLength; }
} 