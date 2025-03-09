# JavaThreads

*   The core of the project is the `Solution.java` file, which implements the `CommandRunner` interface as required by the assignment.
*   The `runCommand` method within `Solution.java` handles various commands (`start`, `cancel`, `running`, `get`, `after`, `finish`, `abort`) based on user input. The input to `runCommand` is a space-separated string representing the command and its arguments.
*   The `SlowCalculator.java` file, also part of the submission, performs the actual calculations. It has been modified to support interruption and return results.
*   This project focuses on running independent calculations in parallel using threads.
*   It adheres to all the specifications of the assignment, including input/output formats, handling of invalid commands, and submission guidelines.
