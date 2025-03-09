public class SlowCalculator implements Runnable {

    private final long N;

    public SlowCalculator(final long N) {
        this.N = N;
    }

    public void run() {
        final int result = calculateNumFactors(N);
        System.out.println(result);  // you'll be changing this
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