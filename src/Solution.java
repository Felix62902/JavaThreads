import java.util.HashMap;
import java.util.Map;
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
    // a queue for after?
    
    public Solution(){}

    public String runCommand(String command) {
        // Input
        String[] tokens = command.split(" ");
        if(tokens.length == 0) return "Invalid command";
        String cmd = tokens[0];
        Long N = Long.valueOf(tokens[1]); 

        // operataions
        if (cmd.equals("start")) {
            // start a new Thread
            SlowCalculator task = new SlowCalculator(N); // task to be executed
            Thread t = new Thread(task); // worker to executer the
            threads.put(N, t);
            tasks.put(N, task);
            t.start();
            return "started " + N;
        } else if (cmd.equals("cancel")) {
            //find the thread based on N, check its status and cancel accordingly
            Thread t = threads.get(N);
            if (t!= null && t.isAlive()) {
                t.interrupt(); // sets interrupt flag
                return "cancelled " + N;
            }
        } else if (cmd.equals("running")) {
            StringBuilder runningMsg = new StringBuilder();
            int count = 0; 
            synchronized (threads) {  // prevent running condition: thread finish before for loop
                for (Map.Entry<Long, SlowCalculator> entry : tasks.entrySet()){
                    SlowCalculator calculator = entry.getValue();
                    if(calculator.getStatus().equals("running")){ // if thread is currently running
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
        } else if (cmd.equals("after")) {
            //schedule thread after
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
}
