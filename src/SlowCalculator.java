public class SlowCalculator implements Runnable {

    private final long N;
    private int result;
    private String status;

    public SlowCalculator(final long N) {
        this.N = N;
        this.result = -1;
        this.status = "waiting"; // waiting, running, completed, interrupted
    }

    @Override
    public void run() {
        status = "running";
        
        try {
            if(Thread.interrupted()){ // check if current thread (executing this method) is interrupted, sets interrupted flag back to false
                throw new InterruptedException();
            }
            result = calculateNumFactors(N);
            status = "completed";
            System.out.println(result);  // Print the result
        } catch (InterruptedException e) {
            status = "interrupted";  // Set status to interrupted if interrupted during execution
            Thread.currentThread().interrupt();  // set interrupt flag to ture, preserve the flag
            System.out.println("Task " + N + " was interrupted.");
        }
    }
    

    public String getStatus() {
        return status;  // Return current status
    }

    public int getResult() {
        return result;
    }

    private static int calculateNumFactors(final long N) {
        // This (very inefficiently) finds and returns the number of unique prime factors of |N|
        // You don't need to think about the mathematical details; what's important is that it does some slow calculation taking N as input
        // You should NOT modify the calculation performed by this class, but you may want to add support for interruption
        int count = 0;
        for (long candidate = 2; candidate < Math.abs(N); ++candidate) {
            if (isPrime(candidate)) {
                if (Math.abs(N) % candidate == 0) {
                    count++;
                }
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