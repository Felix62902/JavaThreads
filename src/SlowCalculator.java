public class SlowCalculator implements Runnable {

    private final long N;
    private int result;
    private volatile STATE status; // volatile for thread safety, always read from memory not cache, changes immediately visible to other threads
    // private boolean isComplete;

    enum STATE{
        WAITING,
        CALCULATING,
        COMPLETED,
        CANCELLED
    }

    public SlowCalculator(final long N) {
        this.N = N;
        this.result = -1;
        this.status = STATE.WAITING; // waiting, calculating, completed,cancelled
    }

    
    @Override
    public void run() {
        status = STATE.CALCULATING;
        try {
            result = calculateNumFactors(N);  // Perform calculation
            status = STATE.COMPLETED;
        } catch (InterruptedException e) {
            status = STATE.CANCELLED;  
            Thread.currentThread().interrupt();  // Preserve the interrupt flag            
        }
    }
    

    public STATE getStatus() {
        return status;  // Return current status
    }

    public int getResult() {
        return result;
    }

    public void setStatus(STATE status) {
        this.status = status;
    }

    public void scheduleafter(Thread n){
        try{
            n.join();
        
        } catch (InterruptedException e){}
    }


    private static int calculateNumFactors(final long N) throws InterruptedException{
        // This (very inefficiently) finds and returns the number of unique prime factors of |N|
        // You don't need to think about the mathematical details; what's important is that it does some slow calculation taking N as input
        // You should NOT modify the calculation performed by this class, but you may want to add support for interruption
        int count = 0;
        for (long candidate = 2; candidate < Math.abs(N); ++candidate) {
            if(Thread.interrupted()){
                throw new InterruptedException(); // will be caught in the run method, changing status to "cancelled"
            }
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