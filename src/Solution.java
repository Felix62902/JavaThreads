import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Solution implements CommandRunner {
    // Thread-safe maps to track calculations and dependencies
    private final Map<Long, Thread> threads = new ConcurrentHashMap<>();
    private final Map<Long, SlowCalculator> calculators = new ConcurrentHashMap<>();
    private final Map<Long, Long> dependencies = new ConcurrentHashMap<>();

    public Solution() {
        // No-arg constructor as required
    }

    @Override
    public String runCommand(String command) {
        String[] tokens = command.split(" ");
        String cmd = tokens[0];

        try {
            // Handle commands that don't require additional arguments
            if (cmd.equals("running")) {
                return handleRunning();
            } else if (cmd.equals("finish")) {
                return handleFinish();
            } else if (cmd.equals("abort")) {
                return handleAbort();
            }

            // Commands that require at least one argument (N)
            if (tokens.length < 2) {
                return "Invalid command";
            }

            long N = Long.parseLong(tokens[1]);

            if (cmd.equals("start")) {
                return handleStart(N);
            } else if (cmd.equals("cancel")) {
                return handleCancel(N);
            } else if (cmd.equals("get")) {
                return handleGet(N);
            } else if (cmd.equals("after")) {
                long M = Long.parseLong(tokens[2]);

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
                if (!calculators.containsKey(M)) {
                    SlowCalculator calculatorM = new SlowCalculator(M);
                    calculators.put(M, calculatorM);
                    // calculatorM.setStatus(SlowCalculator.STATE.WAITING);
                }

                handleAfter(N,M);
                return M + " will start after " + N;
            }
        } catch (NumberFormatException e) {
            return "Invalid command";
        }

        return "Invalid command";
    }

    private String handleStart(long N) {
        // Create a new calculator for N
        SlowCalculator calculator = new SlowCalculator(N);
        Thread thread = new Thread(calculator);
        
        // Store references to the thread and calculator
        calculators.put(N, calculator);
        threads.put(N, thread);
        
        // Start the thread
        thread.start();
        
        return "started " + N;
    }

    private String handleCancel(long N) {
        Thread thread = threads.get(N);
        SlowCalculator calculator = calculators.get(N);
        
        if (thread == null || calculator == null) {
            return "cancelled " + N; // Already cancelled or never started
        }

        if(calculators.get(N).getStatus()==SlowCalculator.STATE.COMPLETED || calculators.get(N).getStatus()==SlowCalculator.STATE.CANCELLED ) {
            return "";
        }

        dependencies.entrySet().removeIf(entry -> entry.getValue().equals(N));

        // Interrupt the thread and set status to cancelled
        thread.interrupt();
        calculator.setStatus(SlowCalculator.STATE.CANCELLED);        
        return "cancelled " + N;
    }

    private String handleGet(long N) {
        SlowCalculator calculator = calculators.get(N);
        
        if (calculator == null) {
            return "cancelled"; // Never started or already cancelled
        }
        
        SlowCalculator.STATE status = calculator.getStatus();
        
        switch (status) {
            case COMPLETED:
                return "result is " + calculator.getResult();
            case CALCULATING:
                return "calculating";
            case CANCELLED:
                return "cancelled";
            case WAITING:
                return "waiting";
            default:
                return "Invalid command";
        }
    }

    private String handleRunning() {
        List<Long> runningCalculations = new ArrayList<>();
        
        for (Map.Entry<Long, Thread> entry : threads.entrySet()) {
            Long N = entry.getKey();
            Thread thread = entry.getValue();
            SlowCalculator calculator = calculators.get(N);
            
            if (thread.isAlive() && calculator != null && 
                calculator.getStatus() == SlowCalculator.STATE.CALCULATING) {
                runningCalculations.add(N);
            }
        }
        
        if (runningCalculations.isEmpty()) {
            return "no calculations running";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(runningCalculations.size()).append(" calculations running: ");
        for (int i = 0; i < runningCalculations.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(runningCalculations.get(i));
        }
        
        return sb.toString();
    }

    private String handleFinish() {
        // Wait for all threads to complete
        for (Thread thread : threads.values()) {
            try {
                if (thread.isAlive()) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return "finished";
    }

    private String handleAbort() {
        for (Map.Entry<Long, Long> entry : dependencies.entrySet()) {
            Long waitingTask = entry.getKey();
            if (calculators.containsKey(waitingTask)) {
                calculators.get(waitingTask).setStatus(SlowCalculator.STATE.CANCELLED);
            }
        }
        dependencies.clear(); // Remove all dependencies
        // Interrupt all running threads
        for (Map.Entry<Long, Thread> entry : threads.entrySet()) {
            Long N = entry.getKey();
            Thread thread = entry.getValue();
            SlowCalculator calculator = calculators.get(N);
            
            // && calculator != null ?
            if (thread.isAlive()  && (calculator.getStatus() != SlowCalculator.STATE.COMPLETED && calculator.getStatus() != SlowCalculator.STATE.CANCELLED) ) {
                thread.interrupt();
                calculator.setStatus(SlowCalculator.STATE.CANCELLED);
            }
        }        
        return "aborted";
    }

    private String handleAfter(long N, long M) {
        // Check if N exists
        Thread threadN = threads.get(N);
        SlowCalculator calculatorN = calculators.get(N);
        // will not be null as it is created before coming into this method
        SlowCalculator calculatorM = calculators.get(M);
        // System.err.println("checkpoint 1 : Calculator: " + M  + calculatorM.getStatus());
        
        
        // If N doesn't exist or is completed/cancelled, start M immediately
        if (calculatorN == null || calculatorN.getStatus() == SlowCalculator.STATE.COMPLETED || calculatorN.getStatus() == SlowCalculator.STATE.CANCELLED) {
            return handleStart(M);
            // System.err.println("checkpoint 2 : Calculator : " + M + " " + calculatorM.getStatus());
            // if (!calculators.containsKey(M)) {
                
            // }
        } else {
            // Create a waiting task for M
            if (!calculators.containsKey(M)) {
                // SlowCalculator calculatorM = new SlowCalculator(M);
                // calculatorM.setStatus(SlowCalculator.STATE.WAITING);
                // calculators.put(M, calculatorM);
            }
            
            // Add dependency
            dependencies.put(M, N);
            
            // Create a watcher thread that will start M when N completes
            if (threadN != null && calculatorN != null && calculatorM != null) {
            new Thread(() -> {
                try {
                    // Thread threadN = threads.get(N);
                    if (threadN != null && calculatorN.getStatus()!=SlowCalculator.STATE.COMPLETED && calculatorN.getStatus()!=SlowCalculator.STATE.CANCELLED ) {
                        threadN.join();
                    }
                    
                    // Only start M if it's still waiting and N completed successfully
                    // calculatorM = calculators.get(M);
                    // && && dependencies.get(M) != null?
                    if (calculatorM.getStatus() == SlowCalculator.STATE.WAITING ) {
                
                        // Remove dependency
                       
                        
                        // Start M
                        Thread threadM = new Thread(calculatorM);
                        threads.put(M, threadM);
                        dependencies.remove(M);
                        threadM.start();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
            // watcher.start();
        }
        
        return M + " will start after " + N;
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
}