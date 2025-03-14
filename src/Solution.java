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

public class Solution implements CommandRunner{
    // data structure
    //get count of available CPU cores
    // private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, Thread> threads = new ConcurrentHashMap<>();  // to track state of thread identified by N e.g. <1497, t1>
    private final Map<Long, SlowCalculator> tasks = new ConcurrentHashMap<>(); // to track results/ status of thread identified by Ne.g. <1497, 21235667>
    private final Map<Long, Long> dependencies = new ConcurrentHashMap<>(); 
    // a queue for after?
    
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
                SlowCalculator calculator = tasks.get(entry.getKey()); // Get the associated SlowCalculator
                if (thread.isAlive() && calculator != null && calculator.getStatus().equals("calculating")) {
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
                case "completed": return "result is " + calculator.getResult();
                case "calculating": return "calculating";
                case "cancelled": return "cancelled";
                case "waiting": return "waiting";
                default: return "error";
            }
        } else if (cmd.equals("after")) {
            if (tokens.length != 3) return "Invalid command";
            //schedule thread M to start when that of N finishes ( or is cancelled )
            // check circular dependency condition
            if (checkCircularDependency(M, N)) {
                List<Long> dependencyChain = getDependencyChain(N);
                dependencyChain.add(M);
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
                entry.getValue().interrupt();
            }
            Boolean notAborted = true;
            while (notAborted) {
                notAborted = false;
                for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()) {
                    SlowCalculator t = entry.getValue();
                    if (!t.getStatus().equals("cancelled")) {
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
        Set<Long> visited = new HashSet<>();
        while (dependencies.containsKey(n)) { // always true unless n is not in the dependency
            if (n == m) return true;            //if reaches starting point, it's a cycle
            if (visited.contains(n)) return false; // if we've visited the node, no cycle
            visited.add(n);
            n = dependencies.get(n);  // move to next dependency 
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
                chain.add(n);
                return chain;
            }
            chain.add(n);
            visited.add(n);
            n = dependencies.get(n);
        }
        chain.add(n); // add lask task to chain
        return chain;
    }

    private void scheduleAfter(long N, long M) {
        Thread threadN = threads.get(N);
        SlowCalculator calculator = tasks.get(N);
        if (threadN != null) {
            new Thread(new Runnable() { // create runnable object on the fly
                @Override
                public void run() {
                    try {
                        threadN.join(); // Wait for N to finish
                        if (calculator.getStatus().equals("completed")) {
                            runCommand("start " + M); // Start M after N finishes
                        }
                    } catch (InterruptedException e) {
                        // Ignore
                    }
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
        task.setStatus("started");
    }

    private void handleCancel(Long N){
        Thread t = threads.get(N);
        SlowCalculator calculator = tasks.get(N);
        if ( t.isAlive()) {  
            t.interrupt(); // sets interrupt flag
            calculator.setStatus("cancelled");  
            dependencies.remove(N);
        }
    }
}
