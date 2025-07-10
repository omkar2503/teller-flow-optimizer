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

    // internal data
    private int customerIDCounter;
    private ServiceArea servicearea; // service area object
    private Scanner dataFile;        // get customer data from file
    private Random dataRandom;       // get customer data using random function

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

        // Time driver simulation loop
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {

            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime+1));
            System.out.println("Queue : " + servicearea.numWaitingCustomers()
                    + "/" + customerQLimit);

            totalWaitingTime = (servicearea.numWaitingCustomers() > 0) ? totalWaitingTime+1 : 0;

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


        System.out.println("\n\n\t*** Current Tellers info. ***\n\n");
        servicearea.printStatistics();

        System.out.println("\n\n\t\tTotal waiting time   : " + totalWaitingTime);
        double averageWaitingTime = ( servicearea.emptyCustomerQ() )
                ? 0.0 : (double)totalWaitingTime / servicearea.numWaitingCustomers();
        System.out.printf("\t\tAverage waiting time : %.2f\n", averageWaitingTime);

        System.out.println("\n\n\t*** Busy Tellers info. ***\n\n");
        if (!servicearea.emptyBusyTellerQ()) {
            while (servicearea.numBusyTellers() > 0) {
                Teller teller = servicearea.removeBusyTellerQ();
                teller.setEndIntervalTime(simulationTime, 1);
                teller.printStatistics();
            }
        } else {
            System.out.println("\t\tNo busy tellers.\n");
        }

        System.out.println("\n\t*** Free Tellers Info. ***\n\n");
        if (!servicearea.emptyFreeTellerQ()) {
            while (servicearea.numFreeTellers() > 0) {
                Teller teller = servicearea.removeFreeTellerQ();
                teller.setEndIntervalTime(simulationTime, 0);
                teller.printStatistics();
            }
        } else {
            System.out.println("\t\tNo free tellers.\n");
        }
        System.out.println();
    }

    private void runGreedy() {
        // Reuse the current doSimulation() logic
        doSimulation();
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
        Queue<Customer> customerQueue = new ArrayDeque<>();
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime + 1));
            System.out.println("Queue : " + customerQueue.size() + "/" + customerQLimit);
            totalWaitingTime = (customerQueue.size() > 0) ? totalWaitingTime + 1 : 0;
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
    }

    private void runLeastWorkLeft() {
        System.out.println("\n\t*** Start Simulation (Least Work Left) ***\n");
        // Initialize ServiceArea
        servicearea = new ServiceArea(numTellers, customerQLimit, 1);
        List<Teller> tellers = new ArrayList<>();
        for (int i = 0; i < numTellers; i++) {
            tellers.add(new Teller(i + 1));
        }
        Queue<Customer> customerQueue = new ArrayDeque<>();
        int[] tellerFinishTimes = new int[numTellers];
        for (int currentTime = 0; currentTime < simulationTime; currentTime++) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Time  : " + (currentTime + 1));
            System.out.println("Queue : " + customerQueue.size() + "/" + customerQLimit);
            totalWaitingTime = (customerQueue.size() > 0) ? totalWaitingTime + 1 : 0;
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
    }

    // *** main method to run simulation ***

    public static void main(String[] args)
    {
        TellerFlowOptimizer runTellerFlowOptimizer = new TellerFlowOptimizer();
        runTellerFlowOptimizer.setupParameters();
        Scanner menuScanner = new Scanner(System.in);
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
        menuScanner.close();
    }

}
