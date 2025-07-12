import java.util.*;
import simulator.Teller;
import simulator.Customer;

public class test_teller_stats {
    public static void main(String[] args) {
        // Test teller statistics tracking
        System.out.println("Testing Teller Statistics Tracking:");
        
        // Create a teller
        Teller teller = new Teller(1);
        
        // Simulate customer service
        Customer customer1 = new Customer(1, 5, 10);
        Customer customer2 = new Customer(2, 3, 20);
        
        // Teller serves customers
        teller.freeToBusy(customer1, 10);
        System.out.println("Teller #" + teller.getTellerID() + " serves customer #" + customer1.getCustomerID());
        
        // Customer finishes
        teller.busyToFree();
        System.out.println("Customer #" + customer1.getCustomerID() + " finished");
        
        // Teller serves another customer
        teller.freeToBusy(customer2, 20);
        System.out.println("Teller #" + teller.getTellerID() + " serves customer #" + customer2.getCustomerID());
        
        // Set end time and print statistics
        teller.setEndIntervalTime(30, 1);
        System.out.println("\nFinal Teller Statistics:");
        teller.printStatistics();
        
        System.out.println("Test completed successfully!");
    }
} 