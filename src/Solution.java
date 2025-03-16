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
            // System.err.println(tasks.get(N).getStatus());
            this.handleCancel(N);
            return "cancelled " + N;
        } else if (cmd.equals("running")) {
            StringBuilder runningMsg = new StringBuilder();
            int count = 0; 
            for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()){
                // Thread thread = entry.getValue();
                SlowCalculator calculator = entry.getValue(); // Get the associated SlowCalculator (one with the same key)
                if ( calculator != null && calculator.getStatus() == SlowCalculator.STATE.CALCULATING) {
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
            // Create a SlowCalculator instance for M if it doesn't exist
            if (!tasks.containsKey(M)) {
                SlowCalculator calculatorM = new SlowCalculator(M);
                tasks.put(M, calculatorM);
                calculatorM.setStatus(SlowCalculator.STATE.WAITING);
            }

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
        
                    // System.out.println("Waiting for thread with N=" + entry.getKey() + ", status=" + task.getStatus());
        
                    if (thread.isAlive()) {
                        thread.join();  // Wait for the thread to finish
                    }
        
                    // System.out.println("Thread with N=" + entry.getKey() + " has finished, status=" + task.getStatus());
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
                
                // Also update the corresponding task's status to CANCELLED
                Long taskId = entry.getKey();
                SlowCalculator task = tasks.get(taskId);
                if (task != null && 
                    (task.getStatus() == SlowCalculator.STATE.CALCULATING || 
                     task.getStatus() == SlowCalculator.STATE.WAITING)) {
                    task.setStatus(SlowCalculator.STATE.CANCELLED);
                }
            }
        
            // Give threads a moment to respond to interruption
            try {
                Thread.sleep(100); // Small delay to allow threads to process interrupts
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        
            // Verify all tasks are now either CANCELLED or COMPLETED
            for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()) {
                SlowCalculator.STATE status = entry.getValue().getStatus();
                if (status != SlowCalculator.STATE.CANCELLED && status != SlowCalculator.STATE.COMPLETED) {
                    // Force cancel any remaining tasks
                    entry.getValue().setStatus(SlowCalculator.STATE.CANCELLED);
                }
            }
        
            return "aborted";
        } else {
            return "Invalid command";
        }
    }


      private boolean checkCircularDependency(long m, long n) { // backtracking
        //  try to see if n depends on m while m depends on n to prove circular dependency. n becomes starting point, m is ending point, if n can reach m, circular dependency
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
        SlowCalculator calculatorM = tasks.get(M);
        dependencies.put(M, N);
    
        if (threadN != null && calculatorN != null && calculatorM != null) {
            new Thread(() -> {
                try {
                    threadN.join(); // Wait for N to finish
                    
                    // Only start M if N completed successfully
                    // and M is not cancelled
                    if (calculatorN.getStatus() == SlowCalculator.STATE.COMPLETED &&
                        calculatorM.getStatus() != SlowCalculator.STATE.CANCELLED) {
                        
                        Thread threadM = new Thread(calculatorM);
                        tasks.put(M, calculatorM);  
                        threads.put(M, threadM);    
                        this.runCommand("start " + M);
                    }
                } catch (InterruptedException e) {
                    // Thread N was interrupted, do NOT start M
                    Thread.currentThread().interrupt();
                    System.err.println("Thread " + N + " was interrupted. NOT starting " + M);
                }
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

    private void handleCancel(Long N) {
        Thread t = threads.get(N);
        SlowCalculator calculator = tasks.get(N);

        if (t != null && calculator != null) {
            synchronized (threads) {
                if (t.isAlive()) {
                    t.interrupt(); // Interrupt the thread
                    
                }
            }
            
            synchronized (tasks) {
                if (calculator.getStatus()!=SlowCalculator.STATE.CANCELLED){
                    calculator.setStatus(SlowCalculator.STATE.CANCELLED); // Update status
                    // System.err.println("Set state to waiting");
                }
            }

            // Remove all entries where N is the value (dependency)
            dependencies.entrySet().removeIf(entry -> entry.getValue().equals(N));

            synchronized (tasks) {
                if(calculator.getStatus() == SlowCalculator.STATE.WAITING){
                    calculator.setStatus(SlowCalculator.STATE.CANCELLED);
                }
            }
            

            // May not be necessary due to how after works, it automatically starts the thread M if thead N is interrupted
            // for (Map.Entry<Long, Long> entry : dependencies.entrySet()) {
            //     if (entry.getValue().equals(N)) {
            //         Long dependentTaskId = entry.getKey();
            //         SlowCalculator dependentCalculator = tasks.get(dependentTaskId);

            //         if (dependentCalculator != null && dependentCalculator.getStatus().equals(SlowCalculator.STATE.WAITING)) {
            //             // Create a new thread for the dependent task
                        
            //             Thread dependentThread = new Thread(dependentCalculator);
            //             dependentThread.start(); // Start the dependent task

            //             // Update the threads map
            //             threads.put(dependentTaskId, dependentThread);

            //             // Update the status of the dependent task
            //             dependentCalculator.setStatus(SlowCalculator.STATE.CALCULATING);;
            //         }
            //     }
            // }
        }
    }
}
