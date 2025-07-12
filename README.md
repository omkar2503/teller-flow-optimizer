# TellerFlowOptimizer

## Introduction

This program simulates a bank with multiple tellers and customers.
A bank service area consists of several tellers and a customer queue.
In each time unit, at most one new customer arrives at the queue.
If the queue is too long, the customer leaves without completing their
transaction; otherwise, the customer gets into the queue.
If all tellers are busy, customers in the queue must wait for a teller.
If a teller is free and customers are waiting, the first customer in the queue
advances to the teller's counter and begins their transaction.
When a customer is done, they depart and the teller becomes free.
The simulation is run through many units of time.
At the end of each time unit, the program prints out a snapshot of the queues,
customers, and tellers.
The program ends with printing out statistics of the simulation.

## JavaFX Graphical User Interface with AI Recommender (NEW!)

You can now run the simulation with a modern JavaFX GUI that includes an AI-powered algorithm recommender:
- **AI Algorithm Recommender:** Describe your banking scenario and get AI-powered algorithm suggestions
- **Dropdown** to select scheduling algorithm (Greedy, Round Robin, Least Work Left)
- **Input fields** for all simulation parameters
- **Start Simulation** button
- **Real-time log** of simulation steps
- **BarChart** showing teller utilization after simulation
- **Background threading** for a responsive UI (no freezing during simulation)
- **Input validation and error dialogs** for user-friendly experience

## Groq AI Key Setup

You can provide your Groq API key in one of two ways:

1. **Environment Variable (Recommended for production):**
   - Set `GROQ_API_KEY` in your environment
2. **Local File (Easy for development):**
   - Create a file named `groq_api_key.txt` in the project root (or `simulator/` directory) and paste your API key as the first line.
   - This file is ignored by git and is safe for local use.

If both are present, the environment variable takes precedence.

### How to Get a Groq API Key
1. Go to [https://console.groq.com/](https://console.groq.com/)
2. Sign up (Google, GitHub, or email)
3. Verify your email
4. Log in, go to **API Keys**, and create a new key
5. Copy your key and use as above

---

## AI-Powered Algorithm Recommendation (Groq)

The application now includes an AI recommender that uses Groq's Llama 3 model to suggest the best scheduling algorithm for your specific banking scenario:

1. **Describe Your Scenario:** Enter details about your bank branch, customer patterns, and goals
2. **Ask AI:** Click "Ask AI to Choose Algorithm" to get a recommendation
3. **Automatic Selection:** The recommended algorithm is automatically selected
4. **Run Simulation:** Start the simulation with the AI-recommended algorithm

---

### How to Run the JavaFX UI

1. **Compile:**
   ```sh
   javac --module-path /path/to/javafx-sdk-XX/lib --add-modules javafx.controls,javafx.fxml simulator/*.java
   ```
2. **Run:**
   ```sh
   java --module-path /path/to/javafx-sdk-XX/lib --add-modules javafx.controls,javafx.fxml simulator.TellerSimulatorApp
   ```
   *(Replace `/path/to/javafx-sdk-XX/lib` with your actual JavaFX SDK path)*

3. **Use the UI:**
   - Enter your simulation parameters
   - Select the algorithm
   - Click **Start Simulation**
   - View the log and utilization chart

### Features
- **Responsive UI:** Simulation runs in the background
- **Error Handling:** Invalid input shows a dialog, not a crash
- **Visualization:** See teller utilization and simulation log instantly

## Scheduling Algorithms

When you run the program, you can choose which teller scheduling algorithm to use:

1. **Greedy (Least Finish Time):**
   - Assigns each waiting customer to the next available teller (the one who becomes free the earliest).
   - This is the original/default logic.

2. **Round Robin:**
   - Assigns customers to tellers in a circular order (teller 1, 2, ..., N, then back to 1, etc.).
   - Each teller only takes a new customer if they are free.

3. **Least Work Left:**
   - Assigns each new customer to the teller with the least total finish time so far (the teller who will be free the soonest, considering all current assignments).

## Enhanced Features

### 🎯 **Better Teller State Tracking**
- **Individual teller statistics:** Average/maximum idle periods, average/maximum busy periods
- **Workload distribution:** Detailed breakdown of each teller's performance
- **Utilization analysis:** Percentage of time each teller was busy vs. idle
- **Customer service metrics:** Average transaction time per teller

### 📊 **Real-time Metrics**
- **Queue trends:** Track queue length at each time step
- **Status updates:** Real-time summary of busy/free tellers and queue status
- **Performance monitoring:** Min/avg/max queue length reporting
- **Toggle option:** Enable/disable real-time status updates

### 🔄 **Algorithm Comparison**
- **Performance metrics storage:** Captures key metrics for each algorithm run
- **Comparison mode:** Run all algorithms with same parameters for fair comparison
- **Side-by-side analysis:** Comprehensive comparison table with all metrics
- **Best performers:** Automatic identification of top-performing algorithms

## Output Features

### **Enhanced Performance Metrics**
- Average customer wait time
- Maximum wait time
- Peak queue length
- Average teller utilization
- Queue efficiency percentage
- Average service time

### **Real-time Status Updates**
```
[Time 5] Queue: 2, Busy Tellers: 3, Free Tellers: 2
```

### **Algorithm Comparison Table**
```
==========================================
*** ALGORITHM COMPARISON TABLE ***
==========================================
Algorithm       Avg Wait    Max Wait    Utilization   Queue Eff.    Peak Queue   Avg Queue
-------------------------------------------------------------------------------
Greedy          2.50        8           75.50        95.00         5            1.20
Round Robin     3.20        10          70.30        92.00         6            1.50
Least Work Left 2.80        9           78.20        96.00         4            1.10
-------------------------------------------------------------------------------

*** BEST PERFORMERS ***
Lowest avg wait time: Greedy
Highest utilization: Least Work Left
Best queue efficiency: Least Work Left
```

### **Detailed Teller Statistics**
```
Teller ID                : 1
Total free time          : 15
Total busy time          : 85
Total # of customers     : 8
Average transaction time : 10.63
Average idle period      : 3.75
Max idle period          : 8
Average busy period      : 10.63
Max busy period          : 15
Utilization              : 85.00%
```

## Assumptions and Requirements

### Assumptions

* At most one customer arrives per time unit
* All numbers are positive integer numbers (>=0), except average values
should be displayed to two decimal places
* No time lost in between the following events:
    * a customer arriving and entering the queue
    * a customer arriving and leaving without banking
    * a customer completing their transaction and departing
    * a customer leaving the queue, advancing to a teller and beginning their
    transaction

### The limits of simulation parameters

* Maximum number of tellers     10
* Maximum simulation length     10000
* Maximum transaction time      500
* Maximum customer queue limit  50
* Probability of a new customer 1% - 100%

### Input parameters and customer (random/file) data

The following data are read at the beginning of the simulation:

* int numTellers;         // number of tellers
* int simulationTime;     // time to run simulation
* int customerQLimit;     // customer queue limit
* int chancesOfArrival;   // probability of a new customer (1 - 100)
* int maxTransactionTime; // maximum transaction time per customer
* int dataSource;         // data source: from file or random

##### Sample input layout:
```
$ java simulator.TellerFlowOptimizer

	***  Get Simulation Parameters  ***

Enter simulation time (max is 10000): 10
Enter maximum transaction time of customers (max is 500): 5
Enter chances (0% < & <= 100%) of new customer: 75
Enter the number of tellers (max is 10): 3
Enter customer queue limit (max is 50): 2
Enter 1/0 to get data from file/Random: 1
Reading data from file. Enter file name: DataFile
```

In each time unit of the simulation, the program needs two positive integers
to compute: (i) boolean anyNewArrival and (ii) int transactionTime.

A user has two options (1 or 0) to specify the source of those numbers:

For user input 1, numbers are read from a file. A filename should be provided
at the beginning of the simulation. Each line in a datafile should contain two
positive numbers (> 0). A datafile should contain sufficient data for
simulationTime up to 500 units, i.e., at least 500 lines. In each time unit,
anyNewArrival & transactionTime are computed as follows:

    read data1 and data2 from the file;
    anyNewArrival = (((data1 % 100) + 1) <= chancesOfArrival);
    transactionTime = (data2 % maxTransactionTime) + 1;

For user input 0, numbers are generated by method nextInt() in a Random object,
dataRandom, which is constructed at the beginning of the simulation. In
each time unit, anyNewArrival & transactionTime are computed as follows:

    anyNewArrival = ((dataRandom.nextInt(100) + 1) <= chancesOfArrival);
    transactionTime = dataRandom.nextInt(maxTransactionTime) + 1;

### Output information
##### Sample output layout:
```

	*** Start Simulation ***

---------------------------------------------------------------
Time  : 1
Queue : 0/2
	Customer #1 arrives with transaction time 5 unit(s).
	Customer #1 waits in the customer queue.
	Customer #1 gets teller #1 for 5 unit(s).
---------------------------------------------------------------
Time  : 2
Queue : 0/2
	Customer #2 arrives with transaction time 2 unit(s).
	Customer #2 waits in the customer queue.
	Customer #2 gets teller #2 for 2 unit(s).
---------------------------------------------------------------
Time  : 3
Queue : 0/2
	Customer #3 arrives with transaction time 5 unit(s).
	Customer #3 waits in the customer queue.
	Customer #3 gets teller #3 for 5 unit(s).
---------------------------------------------------------------
Time  : 4
Queue : 0/2
	No new customer!
	Customer #2 is done.
	Teller #2 is free.
---------------------------------------------------------------
Time  : 5
Queue : 0/2
	Customer #4 arrives with transaction time 3 unit(s).
	Customer #4 waits in the customer queue.
	Customer #4 gets teller #2 for 3 unit(s).
---------------------------------------------------------------
Time  : 6
Queue : 0/2
	No new customer!
	Customer #1 is done.
	Teller #1 is free.
---------------------------------------------------------------
Time  : 7
Queue : 0/2
	Customer #5 arrives with transaction time 3 unit(s).
	Customer #5 waits in the customer queue.
	Customer #5 gets teller #1 for 3 unit(s).
---------------------------------------------------------------
Time  : 8
Queue : 0/2
	Customer #6 arrives with transaction time 5 unit(s).
	Customer #6 waits in the customer queue.
	Customer #4 is done.
	Teller #2 is free.
	Customer #3 is done.
	Teller #3 is free.
	Customer #6 gets teller #2 for 5 unit(s).
---------------------------------------------------------------
Time  : 9
Queue : 0/2
	No new customer!
---------------------------------------------------------------
Time  : 10
Queue : 0/2
	No new customer!
	Customer #5 is done.
	Teller #1 is free.

===============================================================

	*** End of simulation report ***


		# total arrival customers : 6
		# customers gone away     : 0
		# customers served        : 6


	*** Current Tellers info. ***


		# waiting customers : 0
		# busy tellers      : 1
		# free tellers      : 2


		Total waiting time   : 0
		Average waiting time : 0.00


	*** Busy Tellers info. ***


		Teller ID                : 2
		Total free time          : 2
		Total busy time          : 8
		Total # of customers     : 3
		Average transaction time : 2.67


	*** Free Tellers Info. ***


		Teller ID                : 3
		Total free time          : 5
		Total busy time          : 5
		Total # of customers     : 1
		Average transaction time : 5.00

		Teller ID                : 1
		Total free time          : 2
		Total busy time          : 8
		Total # of customers     : 2
		Average transaction time : 4.00


```

## Compile and run program

```
javac simulator/*.java && java simulator.TellerFlowOptimizer
```
"# teller-flow-optimizer" 
