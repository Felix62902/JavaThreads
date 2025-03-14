public class SlowCalculator implements Runnable {

    private final long N;
    private int result;
    private volatile String status; // volatile for thread safety, always read from memory not cache, changes immediately visible to other threads
    // private boolean isComplete;

    public SlowCalculator(final long N) {
        this.N = N;
        this.result = -1;
        this.status = "waiting"; // waiting, calculating, completed,cancelled
    }

    
    @Override
    public void run() {
        status = "calculating";
        System.out.println("Started calculation for N=" + N);

        try {
            result = calculateNumFactors(N);  // Perform calculation
            status = "completed";
            System.out.println("Calculation completed for N=" + N + ", result=" + result); 
        } catch (InterruptedException e) {
            status = "cancelled";  
            Thread.currentThread().interrupt();  // Preserve the interrupt flag
            System.out.println("Task " + N + " was interrupted.");
        }
    }
    

    public String getStatus() {
        return status;  // Return current status
    }

    public int getResult() {
        return result;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    private static int calculateNumFactors(final long N) throws InterruptedException{
        // This (very inefficiently) finds and returns the number of unique prime factors of |N|
        // You don't need to think about the mathematical details; what's important is that it does some slow calculation taking N as input
        // You should NOT modify the calculation performed by this class, but you may want to add support for interruption
        int count = 0;
        for (long candidate = 2; candidate < Math.abs(N); ++candidate) {
            if(Thread.interrupted()){
                throw new InterruptedException();
            }
            if (isPrime(candidate)) {
                if (Math.abs(N) % candidate == 0) {
                    count++;
                }
            }
            //REMOVE:  Print progress every 1 million iterations
            if (candidate % 1_000_000 == 0) {
                System.out.println("Progress for N=" + N + ": candidate=" + candidate);
            }
        }
        return count;
    }

    private static boolean isPrime(final long n) {
        // This (very inefficiently) checks whether n is prime
        // You should NOT modify this method
        for (long candidate = 2; candidate < Math.sqrt(n) + 1; ++candidate) {
            if (n % candidate == 0) {
                return false;
            }
        }
        return true;
    }
}