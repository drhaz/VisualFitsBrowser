package org.cowjumping.guiUtils;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;

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
}
