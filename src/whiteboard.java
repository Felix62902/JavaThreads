import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class whiteboard {

    private final Map<Integer, Thread> threads = new ConcurrentHashMap<>();
    private final Map<Integer, SlowCalculator> tasks = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> dependencies = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();


    public void start() {
        SlowCalculator cal1 = new SlowCalculator(5349125);
        Thread t1 = new Thread(cal1);
        SlowCalculator cal2 = new SlowCalculator(7234568);
        Thread t2 = new Thread(cal2);
        SlowCalculator cal3 = new SlowCalculator(1045606);
        Thread t3 = new Thread(cal3);

        t1.start();
        t2.start();
        t3.start();

        threads.put(1045606, t3);
        tasks.put(1045606, cal3);
        threads.put(5349125, t1);
        tasks.put(5349125, cal1);
        threads.put(7234568, t2);
        tasks.put(7234568, cal2);
    }

    public void finish() {
        for (Map.Entry<Integer, Thread> entry : threads.entrySet()) {
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
        System.out.println("Finished");
    
    }



    public static void main(String[] args) {
        whiteboard wb = new whiteboard();
        wb.start();
        wb.finish(); // Call finish to ensure all threads complete execution
    }
}
