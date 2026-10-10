package org.cowjumping.guiUtils;

import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs external programs (pdflatex, funpack, a PDF viewer, ...).
 * <p>
 * The program's stderr is merged into stdout and read by a single reader, so a program that writes a lot to
 * either stream cannot block. The configured program is split on whitespace, so a setting such as
 * {@code open -a Preview} still works, while file arguments are passed unchanged and may contain spaces.
 */
public class ProcessRunner {

    private ProcessRunner() {
    }

    /**
     * Build a command line from a configured program string and arguments.
     *
     * @param program program path, optionally followed by fixed options separated by whitespace.
     * @param args    arguments passed as they are, e.g. file names that may contain spaces.
     */
    public static List<String> command(String program, String... args) {
        List<String> cmd = new ArrayList<String>();
        for (String token : program.trim().split("\\s+")) {
            if (!token.isEmpty())
                cmd.add(token);
        }
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    /**
     * Run a command, log its output at debug level, and wait for it to finish.
     *
     * @param command    program and arguments, see {@link #command(String, String...)}.
     * @param workingDir working directory, or null for the current one.
     * @param log        logger receiving the program's output.
     * @return the program's exit code, or -1 if it could not be started or was interrupted.
     */
    public static int run(List<String> command, File workingDir, Logger log) {
        Process proc;
        try {
            proc = start(command, workingDir, log);
        } catch (IOException e) {
            log.error("Could not start " + command + ": " + e.getMessage());
            return -1;
        }

        try {
            drain(proc, log);
            int exit = proc.waitFor();
            log.debug("Command " + command + " finished with exit code " + exit);
            return exit;
        } catch (InterruptedException e) {
            proc.destroy();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for " + command);
            return -1;
        }
    }

    /**
     * Start a command without waiting for it, e.g. a viewer that stays open. Its output is logged at debug
     * level by a background thread.
     *
     * @return true if the program was started.
     */
    public static boolean launch(List<String> command, File workingDir, final Logger log) {
        final Process proc;
        try {
            proc = start(command, workingDir, log);
        } catch (IOException e) {
            log.error("Could not start " + command + ": " + e.getMessage());
            return false;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                drain(proc, log);
            }
        }, "Output of " + command.get(0));
        t.setDaemon(true);
        t.start();
        return true;
    }

    private static Process start(List<String> command, File workingDir, Logger log) throws IOException {
        log.info("Executing " + command + (workingDir != null ? " in " + workingDir : ""));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        if (workingDir != null)
            pb.directory(workingDir);
        return pb.start();
    }

    private static void drain(Process proc, Logger log) {
        try {
            BufferedReader out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                String line;
                while ((line = out.readLine()) != null)
                    log.debug(line);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            log.debug("While reading process output: " + e.getMessage());
        }
    }
}
