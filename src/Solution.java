public class Solution implements CommandRunner{

    public Solution(){}

    public String runCommand(String command) {
        String[] tokens = command.split(" ");
        String cmd = tokens[0];
        Long N = Long.valueOf(tokens[1]); 
        if (cmd.equals("start")) {
            this.start(N);
            return "started " + N;
        } else if (cmd.equals("cancel")) {
        } else if (cmd.equals("running")) {
        } else if (cmd.equals("get")) {
        } else if (cmd.equals("set")) {
        } else if (cmd.equals("after")) {
        } else if (cmd.equals("finish")) {
        } else if (cmd.equals("abort")) {
        } else {
            return "Invalid command";
        }


    }

    public void start(Long N){
        Thread thread = new Thread(new SlowCalculator(N));
        thread.start();
    }
}
