import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// import SlowCalculator.STATE;



public class Solution implements CommandRunner{
    // data structure
    //get count of available CPU cores
    // private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, Thread> threads = new ConcurrentHashMap<>();  // to track state of thread identified by N e.g. <1497, t1>
    private final Map<Long, SlowCalculator> tasks = new ConcurrentHashMap<>(); // to track results/ status of thread identified by Ne.g. <1497, 21235667>
    private final Map<Long, Long> dependencies = new ConcurrentHashMap<>(); 

    
    public Solution(){}

    public String runCommand(String command) {
        // Input
        String[] tokens = command.split(" ");
        String cmd = tokens[0];
        Long N = null;
        Long M = null;


        if (tokens.length == 2) {
            // Command has 2 tokens: cmd and N
            N = Long.valueOf(tokens[1]);
        } else if (tokens.length == 3) {
            // Command has 3 tokens: cmd, N, and M
            N = Long.valueOf(tokens[1]);
            M = Long.valueOf(tokens[2]);
        }
        
         
        // operataions
        if (cmd.equals("start")) {
            this.handleStart(N);
            return "started " + N;
            // start a new Thread
           
        } else if (cmd.equals("cancel")) {
            //find the thread based on N, check its status and cancel accordingly
            this.handleCancel(N);
            return "cancelled " + N;
        } else if (cmd.equals("running")) {
            StringBuilder runningMsg = new StringBuilder();
            int count = 0; 
            for (Map.Entry<Long, Thread> entry : threads.entrySet()){
                Thread thread = entry.getValue();
                SlowCalculator calculator = tasks.get(entry.getKey()); // Get the associated SlowCalculator (one with the same key)
                if ( calculator != null && calculator.getStatus()== SlowCalculator.STATE.CALCULATING) {
                    if (count > 0) {
                        runningMsg.append(" ");
                    }
                    runningMsg.append(entry.getKey());
                    count++;
                }
            }
            return count == 0 ? "no calculations running" : count + " calculations running: " + runningMsg;
        } else if (cmd.equals("get")) {
            SlowCalculator calculator = tasks.get(N);
            if (calculator == null) { 
                return "cancelled";
            }
            switch (calculator.getStatus()) {
                case COMPLETED: return "result is " + calculator.getResult();
                case CALCULATING: return "calculating";
                case CANCELLED: return "cancelled";
                case WAITING: return "waiting";
                default: return "error";
            }
        } else if (cmd.equals("after")) {
            if (tokens.length != 3) return "Invalid command";

            // check circular dependency condition
            if (checkCircularDependency(M, N)) {
                List<Long> dependencyChain = getDependencyChain(N);
                StringBuilder circularDependencyMsg = new StringBuilder("circular dependency ");
                for (Long num : dependencyChain) {
                    circularDependencyMsg.append(num).append(" ");
                }
                return circularDependencyMsg.toString().trim();
            }
            dependencies.put(M, N);
            scheduleAfter(N,M);
            return M + " will start after " + N;
        } else if (cmd.equals("finish")) {
            for (Map.Entry<Long, Thread> entry : threads.entrySet()) {
                try {
                    Thread thread = entry.getValue();
                    SlowCalculator task = tasks.get(entry.getKey());
        
                    // Add null check for task
                    if (task == null) {
                        System.err.println("Task for N=" + entry.getKey() + " is null. Skipping.");
                        continue;
                    }
        
                    System.out.println("Waiting for thread with N=" + entry.getKey() + ", status=" + task.getStatus());
        
                    if (thread.isAlive()) {
                        thread.join();  // Wait for the thread to finish
                    }
        
                    System.out.println("Thread with N=" + entry.getKey() + " has finished, status=" + task.getStatus());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }
            return("finished");
        } else if (cmd.equals("abort")) {
            // stop all threads
            for (Map.Entry<Long, Thread> entry : threads.entrySet()) {
                entry.getValue().interrupt(); // Interrupt the thread
            }

            // Check if all tasks are cancelled or completed
            boolean notAborted = true;
            while(notAborted){
                notAborted = false;
                for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()) {
                    SlowCalculator.STATE status = entry.getValue().getStatus();
                    if (status != SlowCalculator.STATE.CANCELLED && status != SlowCalculator.STATE.COMPLETED) {
                        notAborted = true;
                        break;
                    }
                }
            }

            return "aborted";
        } else {
            return "Invalid command";
        }
    }


      private boolean checkCircularDependency(long m, long n) { // backtracking
        //  try to see if n depends on m while m depends on n to prove circular dependency. n becomes starting point, m is ending point, if n can reach m, circular dependency
        // Set<Long> visited = new HashSet<>();
        // while (dependencies.containsKey(n)) { // always true unless n is not in the dependency
        //     if (n == m) return true;            //if reaches starting point, it's a cycle
        //     if (visited.contains(n)) return false; // if we've visited the node, no cycle
        //     visited.add(n);
        //     n = dependencies.get(n);  // move to next dependency 
        // }
        // return false;
        ////////////
        /// 
        // Check if n depends on m (directly or indirectly)
        Long current = n;
        Set<Long> visited = new HashSet<>();
        
        while (dependencies.containsKey(current)) {
            if (current == m) {
                return true; // Found a cycle
            }
            
            if (visited.contains(current)) {
                break; // We've visited this node before, avoid infinite loop
            }
            
            visited.add(current);
            current = dependencies.get(current);
        }
        
        return false;
    }

    /**
     * Returns the list of numbers scheduled after N (directly or indirectly).
     */
    private List<Long> getDependencyChain(long n) {
        List<Long> chain = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        while (dependencies.containsKey(n)) {
            if(visited.contains(n)){
                // chain.add(n);
                return chain;
            }
            chain.add(n);
            visited.add(n);
            n = dependencies.get(n);
        }
        // chain.add(n); // add lask task to chain
        return chain;
    }

    private void scheduleAfter(long N, long M) {
        Thread threadN = threads.get(N);
        SlowCalculator calculatorN = tasks.get(N);
        // SlowCalculator calculatorM = tasks.get(M);
    
        if (threadN != null && calculatorN != null) {
            new Thread(() -> {
                try {
                    threadN.join(); // Wait for N to finish
                } catch (InterruptedException e) {
                    // Restore the interrupt flag
                    Thread.currentThread().interrupt();
                    System.err.println("Thread " + N + " was interrupted. Starting " + M + " immediately.");
                }
        
                // Start M regardless of how N finished
                SlowCalculator calculatorM = new SlowCalculator(M);
                Thread threadM = new Thread(calculatorM);
                
                tasks.put(M, calculatorM);  // ✅ Store calculatorM
                threads.put(M, threadM);    // ✅ Store threadM
                this.runCommand("start " + M); // ✅ Ensure command execution
        
            }).start();    
        }
    }

    private void handleStart(long N){
        SlowCalculator task = new SlowCalculator(N); // task to be executed
        Thread t = new Thread(task); // worker to executer the
        threads.put(N, t);  
        tasks.put(N, task);
        t.start();
        task.setStatus(SlowCalculator.STATE.CALCULATING);
    }

    // private void handleCancel(Long N) {
    //     // Null checks for thread and task
    //     Thread t = threads.get(N);
    //     SlowCalculator calculator = tasks.get(N);
    //     if (t == null || calculator == null) {
    //         return;
    //     }
    
    //     // Ensure thread safety when accessing shared resources
    //     synchronized (threads) {
    //         if (t.isAlive()) {
    //             t.interrupt(); // Interrupt the thread
    //             // Remove N as a key from dependencies (if it exists)
    //             dependencies.remove(N);
    
    //             // Remove N from the values of dependencies (i.e., remove N from other tasks' dependency lists)
    //             for (Map.Entry<Long, Long> entry : dependencies.entrySet()) {
    //                 if (entry.getValue().equals(N)) {
    //                     dependencies.remove(entry.getKey()); // Remove the task that depends on N
    //                 }
    //             }
    //         }
    //     }
    //     for (Map.Entry<Long, Long> entry : dependencies.entrySet()){
    //         if (entry.getValue().equals(N)) {
    //             Thread thread = threads.get(entry.getKey());
    //             SlowCalculator task = tasks.get(entry.getKey());
    //             if(task != null && thread != null && task.getStatus().equals("waiting")){
    //                 thread.start();
    //                 task.setStatus("started");
    //             }
    //         }
    //     }
    // }
    private void handleCancel(Long N) {
    Thread t = threads.get(N);
    SlowCalculator calculator = tasks.get(N);

    if (t != null && calculator != null) {
        synchronized (threads) {
            if (t.isAlive()) {
                t.interrupt(); // Interrupt the thread
                calculator.setStatus(SlowCalculator.STATE.CANCELLED); // Update status
            }
        }

        // Start all tasks that depend on N
        for (Map.Entry<Long, Long> entry : dependencies.entrySet()) {
            if (entry.getValue().equals(N)) {
                Long dependentTaskId = entry.getKey();
                SlowCalculator dependentCalculator = tasks.get(dependentTaskId);

                if (dependentCalculator != null && dependentCalculator.getStatus().equals(SlowCalculator.STATE.WAITING)) {
                    // Create a new thread for the dependent task
                    Thread dependentThread = new Thread(dependentCalculator);
                    dependentThread.start(); // Start the dependent task

                    // Update the threads map
                    threads.put(dependentTaskId, dependentThread);

                    // Update the status of the dependent task
                    dependentCalculator.setStatus(SlowCalculator.STATE.CALCULATING);;
                }
            }
        }
    }
}
}
