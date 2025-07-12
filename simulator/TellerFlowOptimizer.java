package simulator;

import java.util.*;
import java.io.*;

class TellerFlowOptimizer
{
    // input parameters
    private int numTellers, customerQLimit;
    private int simulationTime, dataSource;
    private int chancesOfArrival, maxTransactionTime;

    // statistical data
    private int numGoaway, numServed, totalWaitingTime;

    // enhanced statistics for detailed analysis
    private int maxWaitTime;
    private int totalCustomerWaitTime;
    private int totalTellerBusyTime;
    private int totalTellerIdleTime;
    private List<Integer> customerWaitTimes;
    private List<Integer> tellerUtilization;
    private int peakQueueLength;
    private int totalQueueTime;
    // Real-time metrics
    private List<Integer> queueLengths;
    private boolean printStepSummary = true; // Set to false to disable per-step status
    
    // Algorithm comparison
    private List<AlgorithmResult> algorithmResults;
    private boolean comparisonMode = false;

    // internal data
    private int customerIDCounter;
    private ServiceArea servicearea; // service area object
    private Scanner dataFile;        // get customer data from file
    private Random dataRandom;       // get customer data using random function
    
    // Store actual teller objects for accurate final statistics
    private List<Teller> simulationTellers;

    // most recent customer arrival info, see getCustomerData()
    private boolean anyNewArrival;
    private int transactionTime;

    // initialize data fields
    private TellerFlowOptimizer()
    {
        numGoaway = 0;
        numServed = 0;
        totalWaitingTime = 0;
        customerIDCounter = 0;
        
        // Initialize enhanced statistics
        maxWaitTime = 0;
        totalCustomerWaitTime = 0;
        totalTellerBusyTime = 0;
        totalTellerIdleTime = 0;
        customerWaitTimes = new ArrayList<>();
        tellerUtilization = new ArrayList<>();
        peakQueueLength = 0;
        totalQueueTime = 0;
        
        // Initialize teller storage
        simulationTellers = new ArrayList<>();
        
        // Real-time metrics
        queueLengths = new ArrayList<>();
        
        // Algorithm comparison
        algorithmResults = new ArrayList<>();
    }

    private void setupParameters()
    {
        // read input parameters
        // setup dataFile or dataRandom

        Scanner input = new Scanner(System.in);
        System.out.println("\n\t***  Get Simulation Parameters  ***\n");

        do {
            System.out.print("Enter simulation time (max is 10000): ");
            simulationTime = input.nextInt();
        } while (simulationTime > 10000 || simulationTime < 0);
        do {
            System.out.print("Enter maximum transaction time of customers (max is 500): ");
            maxTransactionTime = input.nextInt();
        } while (maxTransactionTime > 500 || maxTransactionTime < 0);
        do {
            System.out.print("Enter chances (0% < & <= 100%) of new customer: ");
            chancesOfArrival = input.nextInt();
        } while (chancesOfArrival > 100 || chancesOfArrival <= 0);
        do {
            System.out.print("Enter the number of tellers (max is 10): ");
            numTellers = input.nextInt();
        } while (numTellers > 10 || numTellers < 0);
        do {
            System.out.print("Enter customer queue limit (max is 50): ");
            customerQLimit = input.nextInt();
        } while (customerQLimit > 50 || customerQLimit < 0);
        do {
            System.out.print("Enter 1/0 to get data from file/Random: ");
            dataSource = input.nextInt();
        } while (dataSource > 1 || dataSource < 0);

        if (dataSource == 1) {
            System.out.print("Reading data from file. Enter file name: ");
            try {
                dataFile = new Scanner( new File(input.next()) );
            } catch (FileNotFoundException ex) {
                System.out.println("File not found. Randomizing data instead.");
                dataSource = 0;
            }
        } else {
            System.out.println("Randomizing data.");
        }

        dataRandom = new Random();
    }

    private void getCustomerData()
    {
        // get next customer data : from file or random number generator
        // set anyNewArrival and transactionTime

        if (dataSource == 1) {
            int data1, data2;
            data1 = data2 = 0;

            // assign 2 integers from file to data1 & data2
            if (dataFile.hasNextInt()) {
                data1 = dataFile.nextInt();
                data2 = dataFile.nextInt();
            }

            anyNewArrival = (((data1%100)+1) <= chancesOfArrival);
            transactionTime = (data2%maxTransactionTime)+1;

        } else {
            anyNewArrival = ((dataRandom.nextInt(100)+1) <= chancesOfArrival);
            transactionTime = dataRandom.nextInt(maxTransactionTime)+1;
        }
    }

    private void doSimulation()
    {
        System.out.println("\n\t*** Start Simulation ***\n");

        // Initialize ServiceArea
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        
        // Store teller objects for accurate final statistics
        simulationTellers.clear();
        for (int i = 0; i < numTellers; i++) {
            simulationTellers.add(new Teller(i + 1));
        }

        // Time driver simulation loop
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {

            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime+1));
            int currentQueueLen = servicearea.numWaitingCustomers();
            queueLengths.add(currentQueueLen);
            if (printStepSummary) {
                int busyTellers = servicearea.numBusyTellers();
                int freeTellers = servicearea.numFreeTellers();
                System.out.printf("[Time %d] Queue: %d, Busy Tellers: %d, Free Tellers: %d\n", currentTime+1, currentQueueLen, busyTellers, freeTellers);
            }
            System.out.println("Queue : " + currentQueueLen
                    + "/" + customerQLimit);

            totalWaitingTime = (servicearea.numWaitingCustomers() > 0) ? totalWaitingTime+1 : 0;
            
            // Update enhanced statistics
            updateQueueStatistics(servicearea.numWaitingCustomers(), currentTime);
            updateTellerUtilization(servicearea.numBusyTellers(), numTellers, currentTime);

            // Step 1: any new customer enters the bank?
            getCustomerData();

            if (anyNewArrival) {

                // Step 1.1: setup customer data
                customerIDCounter++;
                System.out.println("\tCustomer #" + customerIDCounter
                        + " arrives with transaction time " + transactionTime + " unit(s).");

                // Step 1.2: check customer waiting queue too long?
                if (servicearea.isCustomerQTooLong()) {
                    System.out.println("\tCustomer queue full. Customer #" + customerIDCounter + " leaves...");
                    numGoaway++;
                } else {
                    System.out.println("\tCustomer #" + customerIDCounter + " waits in the customer queue.");
                    servicearea.insertCustomerQ( new Customer(customerIDCounter, transactionTime, currentTime) );
                }

            } else {
                System.out.println("\tNo new customer!");
            }

            // Step 2: free busy tellers, add to free tellerQ
            while (servicearea.numBusyTellers() > 0 && servicearea.getFrontBusyTellerQ().getEndBusyIntervalTime() == currentTime) {
                Teller teller = servicearea.removeBusyTellerQ();
                teller.busyToFree();
                servicearea.insertFreeTellerQ(teller);
                
                // Update corresponding teller in our stored list
                int tellerIndex = teller.getTellerID() - 1;
                if (tellerIndex >= 0 && tellerIndex < simulationTellers.size()) {
                    simulationTellers.get(tellerIndex).busyToFree();
                }

                System.out.println("\tCustomer #" + teller.getCustomer().getCustomerID() + " is done.");
                System.out.println("\tTeller #" + teller.getTellerID() + " is free.");
            }

            // Step 3: get free tellers to serve waiting customers
            while (servicearea.numFreeTellers() > 0 && servicearea.numWaitingCustomers() > 0) {
                Customer customer = servicearea.removeCustomerQ();
                Teller teller = servicearea.removeFreeTellerQ();
                teller.freeToBusy(customer, currentTime);
                servicearea.insertBusyTellerQ(teller);
                numServed++;
                
                // Track customer wait time
                int waitTime = currentTime - customer.getArrivalTime();
                updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                
                // Update corresponding teller in our stored list
                int tellerIndex = teller.getTellerID() - 1;
                if (tellerIndex >= 0 && tellerIndex < simulationTellers.size()) {
                    simulationTellers.get(tellerIndex).freeToBusy(customer, currentTime);
                }

                System.out.println("\tCustomer #" + customer.getCustomerID() + " gets teller #"
                        + teller.getTellerID() + " for " + customer.getTransactionTime() + " unit(s).");
            }

        } // end simulation loop
    }

    private void printStatistics()
    {
        // print out simulation results

        System.out.println("\n===============================================================\n");
        System.out.println("\t*** End of simulation report ***\n\n");
        System.out.println("\t\t# total arrival customers : " + customerIDCounter);
        System.out.println("\t\t# customers gone away     : " + numGoaway);
        System.out.println("\t\t# customers served        : " + numServed);

        // Enhanced statistics
        System.out.println("\n\t*** Enhanced Performance Metrics ***\n");
        
        // Wait time statistics
        double avgWaitTime = customerWaitTimes.isEmpty() ? 0.0 : 
            (double)totalCustomerWaitTime / customerWaitTimes.size();
        System.out.printf("\t\tAverage customer wait time : %.2f units\n", avgWaitTime);
        System.out.println("\t\tMaximum wait time          : " + maxWaitTime + " units");
        System.out.println("\t\tPeak queue length          : " + peakQueueLength + " customers");
        
        // Teller utilization
        double avgTellerUtilization = tellerUtilization.isEmpty() ? 0.0 :
            (double)totalTellerBusyTime / (totalTellerBusyTime + totalTellerIdleTime) * 100;
        System.out.printf("\t\tAverage teller utilization : %.2f%%\n", avgTellerUtilization);
        
        // Queue efficiency
        double queueEfficiency = customerIDCounter == 0 ? 0.0 :
            (double)numServed / customerIDCounter * 100;
        System.out.printf("\t\tQueue efficiency           : %.2f%%\n", queueEfficiency);
        
        // Service quality metrics
        if (numServed > 0) {
            double avgServiceTime = (double)totalTellerBusyTime / numServed;
            System.out.printf("\t\tAverage service time       : %.2f units\n", avgServiceTime);
        }

        System.out.println("\n\n\t*** Current Tellers info. ***\n\n");
        servicearea.printStatistics();

        System.out.println("\n\n\t\tTotal waiting time   : " + totalWaitingTime);
        double averageWaitingTime = ( servicearea.emptyCustomerQ() )
                ? 0.0 : (double)totalWaitingTime / servicearea.numWaitingCustomers();
        System.out.printf("\t\tAverage waiting time : %.2f\n", averageWaitingTime);

        System.out.println("\n\n\t*** Final Teller Statistics ***\n\n");
        if (!simulationTellers.isEmpty()) {
            for (Teller teller : simulationTellers) {
                teller.setEndIntervalTime(simulationTime, teller.getCustomer() != null ? 1 : 0);
                teller.printStatistics();
            }
        } else {
            System.out.println("\t\tNo teller data available.\n");
        }
        System.out.println();

        // Real-time queue trends
        if (!queueLengths.isEmpty()) {
            int minQ = queueLengths.stream().min(Integer::compareTo).orElse(0);
            int maxQ = queueLengths.stream().max(Integer::compareTo).orElse(0);
            double avgQ = queueLengths.stream().mapToInt(i->i).average().orElse(0.0);
            System.out.printf("\n\tQueue length (min/avg/max): %d / %.2f / %d\n", minQ, avgQ, maxQ);
        }
    }

    // Enhanced statistics tracking methods
    private void updateQueueStatistics(int currentQueueLength, int currentTime) {
        // Track peak queue length
        if (currentQueueLength > peakQueueLength) {
            peakQueueLength = currentQueueLength;
        }
        
        // Track total queue time
        if (currentQueueLength > 0) {
            totalQueueTime++;
        }
    }

    private void updateCustomerWaitTime(int customerID, int waitTime) {
        customerWaitTimes.add(waitTime);
        totalCustomerWaitTime += waitTime;
        if (waitTime > maxWaitTime) {
            maxWaitTime = waitTime;
        }
    }

    private void updateTellerUtilization(int busyTellers, int totalTellers, int currentTime) {
        int utilization = totalTellers == 0 ? 0 : (busyTellers * 100) / totalTellers;
        tellerUtilization.add(utilization);
        
        // Track total busy and idle time
        totalTellerBusyTime += busyTellers;
        totalTellerIdleTime += (totalTellers - busyTellers);
    }
    
    // Algorithm comparison methods
    private void captureAlgorithmResult(String algorithmName) {
        AlgorithmResult result = new AlgorithmResult(algorithmName);
        
        // Calculate metrics
        double avgWaitTime = customerWaitTimes.isEmpty() ? 0.0 : 
            (double)totalCustomerWaitTime / customerWaitTimes.size();
        double avgTellerUtilization = tellerUtilization.isEmpty() ? 0.0 :
            (double)totalTellerBusyTime / (totalTellerBusyTime + totalTellerIdleTime) * 100;
        double queueEfficiency = customerIDCounter == 0 ? 0.0 :
            (double)numServed / customerIDCounter * 100;
        double avgServiceTime = numServed > 0 ? (double)totalTellerBusyTime / numServed : 0.0;
        double avgQueueLength = queueLengths.isEmpty() ? 0.0 : 
            queueLengths.stream().mapToInt(i->i).average().orElse(0.0);
        
        // Set results
        result.setAvgWaitTime(avgWaitTime);
        result.setMaxWaitTime(maxWaitTime);
        result.setAvgTellerUtilization(avgTellerUtilization);
        result.setQueueEfficiency(queueEfficiency);
        result.setTotalCustomers(customerIDCounter);
        result.setCustomersServed(numServed);
        result.setCustomersGoneAway(numGoaway);
        result.setAvgServiceTime(avgServiceTime);
        result.setPeakQueueLength(peakQueueLength);
        result.setAvgQueueLength(avgQueueLength);
        
        algorithmResults.add(result);
    }
    
    private void printComparisonTable() {
        if (algorithmResults.size() < 2) return;
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("\t\t*** ALGORITHM COMPARISON TABLE ***");
        System.out.println("=".repeat(80));
        
        // Print header
        System.out.printf("%-15s %-12s %-12s %-15s %-15s %-12s %-12s\n", 
            "Algorithm", "Avg Wait", "Max Wait", "Utilization", "Queue Eff.", "Peak Queue", "Avg Queue");
        System.out.println("-".repeat(80));
        
        // Print each algorithm's results
        for (AlgorithmResult result : algorithmResults) {
            System.out.printf("%-15s %-12.2f %-12d %-15.2f %-15.2f %-12d %-12.2f\n",
                result.getAlgorithmName(),
                result.getAvgWaitTime(),
                result.getMaxWaitTime(),
                result.getAvgTellerUtilization(),
                result.getQueueEfficiency(),
                result.getPeakQueueLength(),
                result.getAvgQueueLength());
        }
        
        System.out.println("-".repeat(80));
        
        // Find best performers
        AlgorithmResult bestWait = algorithmResults.stream()
            .min((a, b) -> Double.compare(a.getAvgWaitTime(), b.getAvgWaitTime()))
            .orElse(null);
        AlgorithmResult bestUtilization = algorithmResults.stream()
            .max((a, b) -> Double.compare(a.getAvgTellerUtilization(), b.getAvgTellerUtilization()))
            .orElse(null);
        AlgorithmResult bestEfficiency = algorithmResults.stream()
            .max((a, b) -> Double.compare(a.getQueueEfficiency(), b.getQueueEfficiency()))
            .orElse(null);
            
        System.out.println("\n*** BEST PERFORMERS ***");
        if (bestWait != null) System.out.println("Lowest avg wait time: " + bestWait.getAlgorithmName());
        if (bestUtilization != null) System.out.println("Highest utilization: " + bestUtilization.getAlgorithmName());
        if (bestEfficiency != null) System.out.println("Best queue efficiency: " + bestEfficiency.getAlgorithmName());
        System.out.println("=".repeat(80));
    }
    
    private void resetForNextRun() {
        // Reset counters for next algorithm run
        numGoaway = 0;
        numServed = 0;
        totalWaitingTime = 0;
        customerIDCounter = 0;
        maxWaitTime = 0;
        totalCustomerWaitTime = 0;
        totalTellerBusyTime = 0;
        totalTellerIdleTime = 0;
        customerWaitTimes.clear();
        tellerUtilization.clear();
        peakQueueLength = 0;
        totalQueueTime = 0;
        queueLengths.clear();
        simulationTellers.clear();
    }

    private void runGreedy() {
        // Reuse the current doSimulation() logic
        doSimulation();
        captureAlgorithmResult("Greedy");
    }

    private void runRoundRobin() {
        System.out.println("\n\t*** Start Simulation (Round Robin) ***\n");
        // Initialize ServiceArea
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        int tellerIndex = 0;
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < numTellers; i++) {
            tellers.add(new Teller(i + 1));
        }
        
        // Store teller objects for accurate final statistics
        simulationTellers.clear();
        simulationTellers.addAll(tellers);

        Queue<Customer> customerQueue = new ArrayDeque<>();
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime + 1));
            int currentQueueLen = customerQueue.size();
            queueLengths.add(currentQueueLen);
            if (printStepSummary) {
                int busyTellers = (int) tellers.stream().filter(t -> t.getCustomer() != null).count();
                int freeTellers = tellers.size() - busyTellers;
                System.out.printf("[Time %d] Queue: %d, Busy Tellers: %d, Free Tellers: %d\n", currentTime+1, currentQueueLen, busyTellers, freeTellers);
            }
            System.out.println("Queue : " + currentQueueLen + "/" + customerQLimit);
            totalWaitingTime = (customerQueue.size() > 0) ? totalWaitingTime + 1 : 0;
            
            // Update enhanced statistics
            updateQueueStatistics(customerQueue.size(), currentTime);
            updateTellerUtilization(tellers.stream().mapToInt(t -> t.getCustomer() != null ? 1 : 0).sum(), tellers.size(), currentTime);
            
            getCustomerData();
            if (anyNewArrival) {
                customerIDCounter++;
                System.out.println("\tCustomer #" + customerIDCounter + " arrives with transaction time " + transactionTime + " unit(s).");
                if (customerQueue.size() == customerQLimit) {
                    System.out.println("\tCustomer queue full. Customer #" + customerIDCounter + " leaves...");
                    numGoaway++;
                } else {
                    System.out.println("\tCustomer #" + customerIDCounter + " waits in the customer queue.");
                    customerQueue.add(new Customer(customerIDCounter, transactionTime, currentTime));
                }
            } else {
                System.out.println("\tNo new customer!");
            }
            // Assign customers to tellers in round robin
            Iterator<Teller> busyIter = tellers.iterator();
            while (busyIter.hasNext()) {
                Teller t = busyIter.next();
                if (t.getCustomer() != null && t.getEndBusyIntervalTime() == currentTime) {
                    t.busyToFree();
                    System.out.println("\tCustomer #" + t.getCustomer().getCustomerID() + " is done.");
                    System.out.println("\tTeller #" + t.getTellerID() + " is free.");
                }
            }
            for (int i = 0; i < tellers.size(); i++) {
                Teller teller = tellers.get(tellerIndex);
                if (teller.getCustomer() == null && !customerQueue.isEmpty()) {
                    Customer customer = customerQueue.poll();
                    teller.freeToBusy(customer, currentTime);
                    numServed++;
                    
                    // Track customer wait time
                    int waitTime = currentTime - customer.getArrivalTime();
                    updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                    
                    System.out.println("\tCustomer #" + customer.getCustomerID() + " gets teller #" + teller.getTellerID() + " for " + customer.getTransactionTime() + " unit(s).");
                }
                tellerIndex = (tellerIndex + 1) % tellers.size();
            }
        }
        // Print teller stats
        for (Teller teller : tellers) {
            teller.setEndIntervalTime(simulationTime, teller.getCustomer() != null ? 1 : 0);
            teller.printStatistics();
        }
        captureAlgorithmResult("Round Robin");
    }

    private void runLeastWorkLeft() {
        System.out.println("\n\t*** Start Simulation (Least Work Left) ***\n");
        // Initialize ServiceArea
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < numTellers; i++) {
            tellers.add(new Teller(i + 1));
        }
        
        // Store teller objects for accurate final statistics
        simulationTellers.clear();
        simulationTellers.addAll(tellers);

        Queue<Customer> customerQueue = new ArrayDeque<>();
        int[] tellerFinishTimes = new int[numTellers];
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime + 1));
            int currentQueueLen = customerQueue.size();
            queueLengths.add(currentQueueLen);
            if (printStepSummary) {
                int busyTellers = (int) tellers.stream().filter(t -> t.getCustomer() != null).count();
                int freeTellers = tellers.size() - busyTellers;
                System.out.printf("[Time %d] Queue: %d, Busy Tellers: %d, Free Tellers: %d\n", currentTime+1, currentQueueLen, busyTellers, freeTellers);
            }
            System.out.println("Queue : " + currentQueueLen + "/" + customerQLimit);
            totalWaitingTime = (customerQueue.size() > 0) ? totalWaitingTime + 1 : 0;
            
            // Update enhanced statistics
            updateQueueStatistics(customerQueue.size(), currentTime);
            updateTellerUtilization(tellers.stream().mapToInt(t -> t.getCustomer() != null ? 1 : 0).sum(), tellers.size(), currentTime);
            
            getCustomerData();
            if (anyNewArrival) {
                customerIDCounter++;
                System.out.println("\tCustomer #" + customerIDCounter + " arrives with transaction time " + transactionTime + " unit(s).");
                if (customerQueue.size() == customerQLimit) {
                    System.out.println("\tCustomer queue full. Customer #" + customerIDCounter + " leaves...");
                    numGoaway++;
                } else {
                    System.out.println("\tCustomer #" + customerIDCounter + " waits in the customer queue.");
                    customerQueue.add(new Customer(customerIDCounter, transactionTime, currentTime));
                }
            } else {
                System.out.println("\tNo new customer!");
            }
            // Free up tellers who are done
            for (int i = 0; i < tellers.size(); i++) {
                Teller t = tellers.get(i);
                if (t.getCustomer() != null && t.getEndBusyIntervalTime() == currentTime) {
                    t.busyToFree();
                    System.out.println("\tCustomer #" + t.getCustomer().getCustomerID() + " is done.");
                    System.out.println("\tTeller #" + t.getTellerID() + " is free.");
                }
            }
            // Assign customers to teller with least work left
            while (!customerQueue.isEmpty()) {
                int minIndex = 0;
                int minFinish = Integer.MAX_VALUE;
                for (int i = 0; i < tellers.size(); i++) {
                    Teller t = tellers.get(i);
                    int finish = t.getCustomer() == null ? currentTime : t.getEndBusyIntervalTime();
                    if (finish < minFinish) {
                        minFinish = finish;
                        minIndex = i;
                    }
                }
                Teller chosen = tellers.get(minIndex);
                if (chosen.getCustomer() == null) {
                    Customer customer = customerQueue.poll();
                    chosen.freeToBusy(customer, currentTime);
                    numServed++;
                    
                    // Track customer wait time
                    int waitTime = currentTime - customer.getArrivalTime();
                    updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                    
                    System.out.println("\tCustomer #" + customer.getCustomerID() + " gets teller #" + chosen.getTellerID() + " for " + customer.getTransactionTime() + " unit(s).");
                } else {
                    break;
                }
            }
        }
        // Print teller stats
        for (Teller teller : tellers) {
            teller.setEndIntervalTime(simulationTime, teller.getCustomer() != null ? 1 : 0);
            teller.printStatistics();
        }
        captureAlgorithmResult("Least Work Left");
    }

    // Simulation with log for JavaFX UI
    private void doSimulationWithLog(StringBuilder log) {
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        simulationTellers.clear();
        for (int i = 0; i < numTellers; i++) {
            simulationTellers.add(new Teller(i + 1));
        }
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            log.append("Time: ").append(currentTime + 1).append(", Queue: ")
                .append(servicearea.numWaitingCustomers()).append("/" + customerQLimit).append("\n");
            getCustomerData();
            if (anyNewArrival) {
                customerIDCounter++;
                log.append("  Customer #").append(customerIDCounter)
                    .append(" arrives with transaction time ").append(transactionTime).append("\n");
                if (servicearea.isCustomerQTooLong()) {
                    log.append("  Customer queue full. Customer #").append(customerIDCounter).append(" leaves...\n");
                    numGoaway++;
                } else {
                    log.append("  Customer #").append(customerIDCounter).append(" waits in the customer queue.\n");
                    servicearea.insertCustomerQ(new Customer(customerIDCounter, transactionTime, currentTime));
                }
            } else {
                log.append("  No new customer!\n");
            }
            while (servicearea.numBusyTellers() > 0 && servicearea.getFrontBusyTellerQ().getEndBusyIntervalTime() == currentTime) {
                Teller teller = servicearea.removeBusyTellerQ();
                teller.busyToFree();
                servicearea.insertFreeTellerQ(teller);
                int tellerIndex = teller.getTellerID() - 1;
                if (tellerIndex >= 0 && tellerIndex < simulationTellers.size()) {
                    simulationTellers.get(tellerIndex).busyToFree();
                }
                log.append("  Customer #").append(teller.getCustomer().getCustomerID()).append(" is done.\n");
                log.append("  Teller #").append(teller.getTellerID()).append(" is free.\n");
            }
            while (servicearea.numFreeTellers() > 0 && servicearea.numWaitingCustomers() > 0) {
                Customer customer = servicearea.removeCustomerQ();
                Teller teller = servicearea.removeFreeTellerQ();
                teller.freeToBusy(customer, currentTime);
                servicearea.insertBusyTellerQ(teller);
                numServed++;
                int waitTime = currentTime - customer.getArrivalTime();
                updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                int tellerIndex = teller.getTellerID() - 1;
                if (tellerIndex >= 0 && tellerIndex < simulationTellers.size()) {
                    simulationTellers.get(tellerIndex).freeToBusy(customer, currentTime);
                }
                log.append("  Customer #").append(customer.getCustomerID()).append(" gets teller #")
                    .append(teller.getTellerID()).append(" for ")
                    .append(customer.getTransactionTime()).append(" unit(s).\n");
            }
        }
    }

    private void doRoundRobinWithLog(StringBuilder log) {
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < numTellers; i++) {
            tellers.add(new Teller(i + 1));
        }
        simulationTellers.clear();
        simulationTellers.addAll(tellers);
        Queue<Customer> customerQueue = new ArrayDeque<>();
        int tellerIndex = 0;
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            log.append("Time: ").append(currentTime + 1).append(", Queue: ")
                .append(customerQueue.size()).append("/" + customerQLimit).append("\n");
            getCustomerData();
            if (anyNewArrival) {
                customerIDCounter++;
                log.append("  Customer #").append(customerIDCounter)
                    .append(" arrives with transaction time ").append(transactionTime).append("\n");
                if (customerQueue.size() == customerQLimit) {
                    log.append("  Customer queue full. Customer #").append(customerIDCounter).append(" leaves...\n");
                    numGoaway++;
                } else {
                    log.append("  Customer #").append(customerIDCounter).append(" waits in the customer queue.\n");
                    customerQueue.add(new Customer(customerIDCounter, transactionTime, currentTime));
                }
            } else {
                log.append("  No new customer!\n");
            }
            for (Teller t : tellers) {
                if (t.getCustomer() != null && t.getEndBusyIntervalTime() == currentTime) {
                    t.busyToFree();
                    log.append("  Customer #").append(t.getCustomer().getCustomerID()).append(" is done.\n");
                    log.append("  Teller #").append(t.getTellerID()).append(" is free.\n");
                }
            }
            for (int i = 0; i < tellers.size(); i++) {
                Teller teller = tellers.get(tellerIndex);
                if (teller.getCustomer() == null && !customerQueue.isEmpty()) {
                    Customer customer = customerQueue.poll();
                    teller.freeToBusy(customer, currentTime);
                    numServed++;
                    int waitTime = currentTime - customer.getArrivalTime();
                    updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                    log.append("  Customer #").append(customer.getCustomerID()).append(" gets teller #")
                        .append(teller.getTellerID()).append(" for ")
                        .append(customer.getTransactionTime()).append(" unit(s).\n");
                }
                tellerIndex = (tellerIndex + 1) % tellers.size();
            }
        }
    }

    private void doLeastWorkLeftWithLog(StringBuilder log) {
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < numTellers; i++) {
            tellers.add(new Teller(i + 1));
        }
        simulationTellers.clear();
        simulationTellers.addAll(tellers);
        Queue<Customer> customerQueue = new ArrayDeque<>();
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            log.append("Time: ").append(currentTime + 1).append(", Queue: ")
                .append(customerQueue.size()).append("/" + customerQLimit).append("\n");
            getCustomerData();
            if (anyNewArrival) {
                customerIDCounter++;
                log.append("  Customer #").append(customerIDCounter)
                    .append(" arrives with transaction time ").append(transactionTime).append("\n");
                if (customerQueue.size() == customerQLimit) {
                    log.append("  Customer queue full. Customer #").append(customerIDCounter).append(" leaves...\n");
                    numGoaway++;
                } else {
                    log.append("  Customer #").append(customerIDCounter).append(" waits in the customer queue.\n");
                    customerQueue.add(new Customer(customerIDCounter, transactionTime, currentTime));
                }
            } else {
                log.append("  No new customer!\n");
            }
            for (Teller t : tellers) {
                if (t.getCustomer() != null && t.getEndBusyIntervalTime() == currentTime) {
                    t.busyToFree();
                    log.append("  Customer #").append(t.getCustomer().getCustomerID()).append(" is done.\n");
                    log.append("  Teller #").append(t.getTellerID()).append(" is free.\n");
                }
            }
            while (!customerQueue.isEmpty()) {
                int minIndex = 0;
                int minFinish = Integer.MAX_VALUE;
                for (int i = 0; i < tellers.size(); i++) {
                    Teller t = tellers.get(i);
                    int finish = t.getCustomer() == null ? currentTime : t.getEndBusyIntervalTime();
                    if (finish < minFinish) {
                        minFinish = finish;
                        minIndex = i;
                    }
                }
                Teller chosen = tellers.get(minIndex);
                if (chosen.getCustomer() == null) {
                    Customer customer = customerQueue.poll();
                    chosen.freeToBusy(customer, currentTime);
                    numServed++;
                    int waitTime = currentTime - customer.getArrivalTime();
                    updateCustomerWaitTime(customer.getCustomerID(), waitTime);
                    log.append("  Customer #").append(customer.getCustomerID()).append(" gets teller #")
                        .append(chosen.getTellerID()).append(" for ")
                        .append(customer.getTransactionTime()).append(" unit(s).\n");
                } else {
                    break;
                }
            }
        }
    }

    // Static method for JavaFX UI integration
    public static SimulationResult runWithParams(int simTime, int maxTrans, int chance, int tellers, int queueLimit, String algorithm) {
        StringBuilder log = new StringBuilder();
        List<Double> utilizations = new ArrayList<>();
        TellerFlowOptimizer sim = new TellerFlowOptimizer();
        sim.simulationTime = simTime;
        sim.maxTransactionTime = maxTrans;
        sim.chancesOfArrival = chance;
        sim.numTellers = tellers;
        sim.customerQLimit = queueLimit;
        sim.dataSource = 0; // Always use random for UI
        sim.dataRandom = new Random();
        sim.customerIDCounter = 0;
        sim.numGoaway = 0;
        sim.numServed = 0;
        sim.totalWaitingTime = 0;
        sim.maxWaitTime = 0;
        sim.totalCustomerWaitTime = 0;
        sim.totalTellerBusyTime = 0;
        sim.totalTellerIdleTime = 0;
        sim.customerWaitTimes = new ArrayList<>();
        sim.tellerUtilization = new ArrayList<>();
        sim.peakQueueLength = 0;
        sim.totalQueueTime = 0;
        sim.queueLengths = new ArrayList<>();
        sim.simulationTellers = new ArrayList<>();
        // Choose algorithm
        if (algorithm.contains("Greedy")) {
            sim.doSimulationWithLog(log);
        } else if (algorithm.contains("Round Robin")) {
            sim.doRoundRobinWithLog(log);
        } else {
            sim.doLeastWorkLeftWithLog(log);
        }
        // Collect utilization
        for (Teller t : sim.simulationTellers) {
            double util = (t.getTotalBusyTime() + t.getTotalFreeTime()) > 0 ?
                100.0 * t.getTotalBusyTime() / (t.getTotalBusyTime() + t.getTotalFreeTime()) : 0.0;
            utilizations.add(util);
        }
        return new SimulationResult(log.toString(), utilizations);
    }

    // *** main method to run simulation ***

    public static void main(String[] args)
    {
        TellerFlowOptimizer runTellerFlowOptimizer = new TellerFlowOptimizer();
        runTellerFlowOptimizer.setupParameters();
        
        Scanner menuScanner = new Scanner(System.in);
        
        // Ask if user wants to run comparison mode
        System.out.println("\nRun comparison mode? (Compare multiple algorithms)");
        System.out.println("1: Single algorithm run");
        System.out.println("2: Comparison mode (run all algorithms)");
        System.out.print("Enter choice (1-2): ");
        int comparisonChoice = menuScanner.nextInt();
        
        if (comparisonChoice == 2) {
            // Run all algorithms for comparison
            runTellerFlowOptimizer.comparisonMode = true;
            System.out.println("\nRunning all algorithms for comparison...");
            
            // Run Greedy
            System.out.println("\n" + "=".repeat(50));
            System.out.println("RUNNING GREEDY ALGORITHM");
            System.out.println("=".repeat(50));
            runTellerFlowOptimizer.runGreedy();
            runTellerFlowOptimizer.printStatistics();
            
            // Reset counters for next run
            runTellerFlowOptimizer.resetForNextRun();
            
            // Run Round Robin
            System.out.println("\n" + "=".repeat(50));
            System.out.println("RUNNING ROUND ROBIN ALGORITHM");
            System.out.println("=".repeat(50));
            runTellerFlowOptimizer.runRoundRobin();
            runTellerFlowOptimizer.printStatistics();
            
            // Reset counters for next run
            runTellerFlowOptimizer.resetForNextRun();
            
            // Run Least Work Left
            System.out.println("\n" + "=".repeat(50));
            System.out.println("RUNNING LEAST WORK LEFT ALGORITHM");
            System.out.println("=".repeat(50));
            runTellerFlowOptimizer.runLeastWorkLeft();
            runTellerFlowOptimizer.printStatistics();
            
            // Print comparison table
            runTellerFlowOptimizer.printComparisonTable();
            
        } else {
            // Single algorithm run
            int choice = 0;
            do {
                System.out.println("\nChoose scheduling algorithm:");
                System.out.println("1: Greedy (Least Finish Time)");
                System.out.println("2: Round Robin");
                System.out.println("3: Least Work Left");
                System.out.print("Enter choice (1-3): ");
                choice = menuScanner.nextInt();
            } while (choice < 1 || choice > 3);
            
            switch (choice) {
                case 1:
                    runTellerFlowOptimizer.runGreedy();
                    break;
                case 2:
                    runTellerFlowOptimizer.runRoundRobin();
                    break;
                case 3:
                    runTellerFlowOptimizer.runLeastWorkLeft();
                    break;
            }
            runTellerFlowOptimizer.printStatistics();
        }
        
        menuScanner.close();
    }

}
