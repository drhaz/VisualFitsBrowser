package org.cowjumping.guiUtils;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProcessRunnerTest extends TestCase {

    private static final Logger log = LogManager.getLogger(ProcessRunnerTest.class);

    private static boolean isUnix() {
        return new File("/bin/sh").exists();
    }

    public void testCommandSplitsProgramButKeepsArguments() {
        assertEquals(Arrays.asList("open", "-a", "Preview", "/tmp/my log.pdf"),
                ProcessRunner.command("  open -a  Preview ", "/tmp/my log.pdf"));
    }

    public void testExitCodeIsReturned() {
        if (!isUnix())
            return;
        assertEquals(0, ProcessRunner.run(ProcessRunner.command("/bin/sh", "-c", "exit 0"), null, log));
        assertEquals(3, ProcessRunner.run(ProcessRunner.command("/bin/sh", "-c", "exit 3"), null, log));
    }

    public void testMissingProgramReturnsMinusOne() {
        assertEquals(-1, ProcessRunner.run(ProcessRunner.command("/nonexistent/program"), null, log));
    }

    public void testLargeStderrOutputDoesNotBlock() throws Exception {
        if (!isUnix())
            return;
        // About 1 MB on stderr: more than a pipe buffer. With separate, sequentially read streams this
        // used to block forever.
        final int[] exit = {Integer.MIN_VALUE};
        Thread t = new Thread(new Runnable() {
            public void run() {
                exit[0] = ProcessRunner.run(ProcessRunner.command("/bin/sh", "-c",
                        "i=0; while [ $i -lt 20000 ]; do echo 'some error output to fill the pipe buffer' >&2; i=$((i+1)); done; exit 0"),
                        null, log);
            }
        });
        t.start();
        t.join(30000);
        assertFalse("process runner blocked on stderr output", t.isAlive());
        assertEquals(0, exit[0]);
    }

    public void testWorkingDirectoryIsUsed() throws Exception {
        if (!isUnix())
            return;
        File dir = new File(System.getProperty("java.io.tmpdir"), "process runner test " + System.nanoTime());
        assertTrue(dir.mkdir());
        try {
            assertEquals(0, ProcessRunner.run(ProcessRunner.command("/bin/sh", "-c", "touch marker"), dir, log));
            assertTrue(new File(dir, "marker").exists());
        } finally {
            new File(dir, "marker").delete();
            dir.delete();
        }
    }

    // --- findExecutable ----------------------------------------------------------------------------------------

    private File tmpDir(String name) {
        File d = new File(System.getProperty("java.io.tmpdir"), "pr-test-" + name + "-" + System.nanoTime());
        assertTrue(d.mkdirs());
        return d;
    }

    private static File executable(File dir, String name) throws IOException {
        File f = new File(dir, name);
        assertTrue(f.createNewFile());
        assertTrue(f.setExecutable(true));
        return f;
    }

    private static void delete(File dir) {
        File[] files = dir.listFiles();
        if (files != null)
            for (File f : files)
                f.delete();
        dir.delete();
    }

    public void testConfiguredExecutableIsKept() throws Exception {
        if (!isUnix())
            return;
        File conf = tmpDir("conf");
        File fallback = tmpDir("fallback");
        try {
            File configured = executable(conf, "pdflatex");
            executable(fallback, "pdflatex");
            List<File> none = Collections.emptyList();
            assertEquals(configured.getAbsolutePath(), ProcessRunner.findExecutable(configured.getAbsolutePath(),
                    "pdflatex", none, Arrays.asList(fallback)));
            // Options after the program are preserved.
            assertEquals(configured.getAbsolutePath() + " -draftmode", ProcessRunner.findExecutable(
                    configured.getAbsolutePath() + " -draftmode", "pdflatex", none, Arrays.asList(fallback)));
        } finally {
            delete(conf);
            delete(fallback);
        }
    }

    public void testMissingConfiguredExecutableFallsBackToPathThenFallbackDirs() throws Exception {
        if (!isUnix())
            return;
        File path = tmpDir("path");
        File fallback = tmpDir("fallback");
        try {
            File inFallback = executable(fallback, "pdflatex");
            assertEquals(inFallback.getAbsolutePath(), ProcessRunner.findExecutable("/usr/bin/does-not-exist",
                    "pdflatex", Arrays.asList(path), Arrays.asList(fallback)));

            File inPath = executable(path, "pdflatex");
            assertEquals("PATH is searched before the fallback directories", inPath.getAbsolutePath(),
                    ProcessRunner.findExecutable("/usr/bin/does-not-exist", "pdflatex", Arrays.asList(path),
                            Arrays.asList(fallback)));
        } finally {
            delete(path);
            delete(fallback);
        }
    }

    public void testBareProgramNameIsResolvedOnPath() throws Exception {
        if (!isUnix())
            return;
        File path = tmpDir("path");
        try {
            File f = executable(path, "xelatex");
            List<File> none = Collections.emptyList();
            assertEquals(f.getAbsolutePath() + " -foo",
                    ProcessRunner.findExecutable("xelatex -foo", "pdflatex", Arrays.asList(path), none));
        } finally {
            delete(path);
        }
    }

    public void testNonExecutableFileIsSkipped() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir("noexec");
        try {
            assertTrue(new File(dir, "pdflatex").createNewFile()); // not executable
            List<File> none = Collections.emptyList();
            assertNull(ProcessRunner.findExecutable(null, "pdflatex", none, Arrays.asList(dir)));
        } finally {
            delete(dir);
        }
    }

    public void testNothingFoundReturnsNull() {
        List<File> none = Collections.emptyList();
        assertNull(ProcessRunner.findExecutable("/usr/bin/does-not-exist", "no-such-program-xyz", none, none));
    }

    public void testPathDirectories() {
        assertEquals(Arrays.asList(new File("/a"), new File("/b")),
                ProcessRunner.pathDirectories("/a" + File.pathSeparator + File.pathSeparator + "/b"));
        assertTrue(ProcessRunner.pathDirectories(null).isEmpty());
    }
}
