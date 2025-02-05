package io.improt.vai.util;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class MeldLauncher {
    public static void launchMeld(Path fileA, Path fileB) throws Exception {
        // Debug statement to indicate that meld is being launched
        System.out.println("Launching meld!");

        ProcessBuilder processBuilder = new ProcessBuilder(
                "meld",
                fileA.toAbsolutePath().toString(),
                fileB.toAbsolutePath().toString()
        );

        processBuilder.inheritIO(); // Pass output and error streams to this process
        Process process = processBuilder.start();
        if (!process.waitFor(60, TimeUnit.DAYS)) {
            process.destroy(); // Kill process if it hangs
            throw new Exception("Launching meld timed out.");
        }
    }
}
