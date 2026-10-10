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
     * Find an executable program, preferring the configured one.
     * <p>
     * The configured value may be an absolute path or a bare program name, optionally followed by options. If it
     * does not point to an executable, the program {@code name} is searched in the directories of the
     * {@code PATH} environment variable and then in {@code fallbackDirs}.
     *
     * @param configured   configured program (may be null or empty).
     * @param name         program file name to search for, e.g. {@code pdflatex}.
     * @param fallbackDirs additional directories to search after {@code PATH}, in order.
     * @return the configured value if usable, otherwise the absolute path of the program found, or null.
     */
    public static String findExecutable(String configured, String name, List<File> fallbackDirs) {
        return findExecutable(configured, name, pathDirectories(System.getenv("PATH")), fallbackDirs);
    }

    /**
     * As {@link #findExecutable(String, String, List)}, but with the {@code PATH} directories given explicitly
     * (for tests, or callers that want to control the search).
     */
    public static String findExecutable(String configured, String name, List<File> pathDirs,
                                        List<File> fallbackDirs) {
        if (configured != null && !configured.trim().isEmpty()) {
            String trimmed = configured.trim();
            String[] parts = trimmed.split("\\s+", 2);
            String program = parts[0];
            String options = parts.length > 1 ? " " + parts[1] : "";

            if (program.contains(File.separator)) {
                if (isExecutable(new File(program)))
                    return trimmed;
            } else {
                File found = search(program, pathDirs);
                if (found != null)
                    return found.getAbsolutePath() + options;
            }
        }

        File found = search(name, pathDirs);
        if (found == null)
            found = search(name, fallbackDirs);
        return found != null ? found.getAbsolutePath() : null;
    }

    public static List<File> pathDirectories(String path) {
        List<File> dirs = new ArrayList<File>();
        if (path != null)
            for (String entry : path.split(File.pathSeparator))
                if (!entry.isEmpty())
                    dirs.add(new File(entry));
        return dirs;
    }

    private static File search(String name, List<File> dirs) {
        if (dirs != null)
            for (File dir : dirs) {
                File candidate = new File(dir, name);
                if (isExecutable(candidate))
                    return candidate;
            }
        return null;
    }

    private static boolean isExecutable(File f) {
        return f.isFile() && f.canExecute();
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
