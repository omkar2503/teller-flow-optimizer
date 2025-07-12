package simulator;

import java.util.ArrayList;
import java.util.List;

public class Teller
{
    // start time and end time of current interval
    private int startTime;
    private int endTime;

    // teller id and current customer which is served by this teller
    private int tellerID;
    private Customer currentCustomer;

    // for keeping statistical data
    private int totalFreeTime;
    private int totalBusyTime;
    private int totalCustomers;

    // Enhanced state tracking
    private List<Integer> idlePeriods;
    private List<Integer> busyPeriods;

    public Teller()
    {
        this(1);
    }

    public Teller(int tellerId)
    {
        tellerID = tellerId;
        idlePeriods = new ArrayList<>();
        busyPeriods = new ArrayList<>();
    }

    // accessor methods

    public int getTellerID()
    {
        return tellerID;
    }

    public Customer getCustomer()
    {
        return currentCustomer;
    }

    public int getEndBusyIntervalTime()
    {
        // return end time of busy interval
        return endTime;
    }

    public int getTotalBusyTime() {
        return totalBusyTime;
    }
    public int getTotalFreeTime() {
        return totalFreeTime;
    }

    // functions for state transition

    public void freeToBusy (Customer currentCustomer, int currentTime)
    {
        // Main goal : switch from free interval to busy interval
        //
        // end free interval, start busy interval
        // steps : update totalFreeTime
        //         set startTime, endTime, currentCustomer
        //         update totalCustomers

        int idlePeriod = currentTime - startTime;
        if (idlePeriod > 0) idlePeriods.add(idlePeriod);
        totalFreeTime += idlePeriod;
        startTime = currentTime;
        endTime = startTime + currentCustomer.getTransactionTime();
        this.currentCustomer = currentCustomer;
        totalCustomers++;
    }

    public Customer busyToFree ()
    {
        // Main goal : switch from busy interval to free interval
        //
        // steps : update totalBusyTime
        //         set startTime
        //         return currentCustomer

        int busyPeriod = endTime - startTime;
        if (busyPeriod > 0) busyPeriods.add(busyPeriod);
        totalBusyTime += busyPeriod;
        startTime = endTime;
        return currentCustomer;
    }

    // need this method at the end of simulation to update teller data
    // intervalType: 0 for FREE interval, 1 for BUSY interval
    public void setEndIntervalTime (int endsimulationtime, int intervalType)
    {
        // for end of simulation
        // set endTime,
        // for FREE interval, update totalFreeTime
        // for BUSY interval, update totalBusyTime

        endTime = endsimulationtime;

        if (intervalType == 0) {
            totalFreeTime += endTime - startTime;
        } else {
            totalBusyTime += endTime - startTime;
        }
    }

    // functions for printing statistics :
    
    public void printStatistics ()
    {
        // print teller statistics, see project statement

        System.out.println("\t\tTeller ID                : "+tellerID);
        System.out.println("\t\tTotal free time          : "+totalFreeTime);
        System.out.println("\t\tTotal busy time          : "+totalBusyTime);
        System.out.println("\t\tTotal # of customers     : "+totalCustomers);

        if (totalCustomers > 0) {
            System.out.format("\t\tAverage transaction time : %.2f\n",
                    (totalBusyTime*1.0)/totalCustomers);
        }
        // Enhanced state tracking
        if (!idlePeriods.isEmpty()) {
            System.out.format("\t\tAverage idle period      : %.2f\n", avg(idlePeriods));
            System.out.println("\t\tMax idle period          : " + max(idlePeriods));
        }
        if (!busyPeriods.isEmpty()) {
            System.out.format("\t\tAverage busy period      : %.2f\n", avg(busyPeriods));
            System.out.println("\t\tMax busy period          : " + max(busyPeriods));
        }
        double utilization = (totalBusyTime + totalFreeTime) > 0 ? (100.0 * totalBusyTime / (totalBusyTime + totalFreeTime)) : 0.0;
        System.out.format("\t\tUtilization              : %.2f%%\n", utilization);
        System.out.println();
    }

    private double avg(List<Integer> list) {
        if (list.isEmpty()) return 0.0;
        int sum = 0;
        for (int v : list) sum += v;
        return (double) sum / list.size();
    }
    private int max(List<Integer> list) {
        if (list.isEmpty()) return 0;
        int m = list.get(0);
        for (int v : list) if (v > m) m = v;
        return m;
    }

    @Override
    public String toString()
    {
        return "Teller:"+tellerID+":"+startTime+"-"+endTime+":Customer:"+currentCustomer;
    }

    public static void main(String[] args)
    {
        // quick check
        Customer mycustomer = new Customer(20,30,40);
        Teller myteller = new Teller(5);
        myteller.freeToBusy (mycustomer, 13);
        myteller.printStatistics();
    }

}
