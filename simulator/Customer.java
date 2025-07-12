package simulator;

public class Customer
{
    private int customerID;
    private int transactionTime;
    private int arrivalTime;

    public Customer()
    {
        this(1,1,1);
    }

    public Customer(int customerid, int transactionduration, int arrivaltime)
    {
        customerID = customerid;
        transactionTime = transactionduration;
        arrivalTime = arrivaltime;
    }

    public int getTransactionTime()
    {
        return transactionTime;
    }

    public int getArrivalTime()
    {
        return arrivalTime;
    }

    public int getCustomerID()
    {
        return customerID;
    }

    public String toString()
    {
        return ""+customerID+":"+transactionTime+":"+arrivalTime;
    }

    public static void main(String[] args)
    {
        // quick check!
        Customer mycustomer = new Customer(20,30,40);
        System.out.println("Customer Info:"+mycustomer);
    }

}
