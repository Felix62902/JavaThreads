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
        String cmd;
        Long N;
        Long M;


        if(tokens.length == 0) return "Invalid command";
        if (tokens.length == 2){
            cmd = tokens[0];
            N = Long.valueOf(tokens[1]);
        } else if (tokens.length == 3){
            cmd = tokens[0];
            N = Long.valueOf(tokens[1]);
            M = Long.valueOf(tokens[2]);
        }
         
        // operataions
        if (cmd.equals("start")) {
            // start a new Thread
            SlowCalculator task = new SlowCalculator(N); // task to be executed
            Thread t = new Thread(task); // worker to executer the
            if(tokens.length == 3){ // run by after
                threads.put(M, t);  
                tasks.put(M, task);
                t.start();
                return "started " + M;
            } 
            // length of 2, not run by after
            threads.put(N, t);  
            tasks.put(N, task);
            t.start();
            return "started " + N;
        } else if (cmd.equals("cancel")) {
            //find the thread based on N, check its status and cancel accordingly
            Thread t = threads.get(N);
            SlowCalculator calculator = tasks.get(N);
            if ( t.isAlive()) {  
                t.interrupt(); // sets interrupt flag
                calculator.setStatus("cancelled");  
                dependencies.remove(N);
            }
            return "cancelled " + N;
        } else if (cmd.equals("running")) {
            StringBuilder runningMsg = new StringBuilder();
            int count = 0; 
            synchronized (tasks) {  // prevent running condition: thread finish before for loop, wont slow down as much due to concurrenthashmap
                for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()){
                    SlowCalculator calculator = entry.getValue();
                    if(calculator.getStatus().equals("calculating")){ // if thread is currently running
                        if (count > 0 ){
                            runningMsg.append(" ");
                        }
                        runningMsg.append(entry.getKey());
                        count++;
                    }
            }
            return count == 0 ? "no calculations running" : count + " calculations running: " + runningMsg;
            //total number of threads running
        } else if (cmd.equals("get")) {
            // get status of particular thread
            // Thread t = threads.get(N);
            SlowCalculator calculator = tasks.get(N);
            if(calculator.getStatus().equals("completed")){ // meaning it has completed
                return "result is " + calculator.getResult();
            } else if (calculator.getStatus().equals("calculating")){
                return "calculating";
            } else if(calculator.getStatus().equals("cancelled")){
                return "cancelled";
            } else if (calculator.getStatus().equals("waiting")){ // scheduled with after but not yet started
                return "waiting";
            }
            return "error";
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
            // wait for all threads to finish
            
            return "finished";
        } else if (cmd.equals("abort")) {
            // stop all threads
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
        // Thread threadN = threads.get(N);
        // SlowCalculator calculator = tasks.get(N);
        // if (threadN != null) {
        //     new Thread(() -> {
        //         try {
        //             threadN.join(); // Wait for N to finish
        //             if (!calculator.getStatus().equals("cancelled")) {
        //                 runCommand("start " + M); // Start M after N finishes
        //             }
        //         } catch (InterruptedException e) {
        //             // Ignore
        //         }
        //     }).start();
        // }
    }
}
