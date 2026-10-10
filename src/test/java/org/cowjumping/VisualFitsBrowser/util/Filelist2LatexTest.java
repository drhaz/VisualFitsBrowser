package org.cowjumping.VisualFitsBrowser.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Filelist2LatexTest extends TestCase {

    public void testPlainTextIsUnchanged() {
        assertEquals("M51 r-band 300s", Filelist2Latex.EscapeForLatex("M51 r-band 300s"));
    }

    public void testNullBecomesEmpty() {
        assertEquals("", Filelist2Latex.EscapeForLatex(null));
    }

    public void testSimpleSpecialCharacters() {
        assertEquals("\\# \\$ \\% \\& \\_ \\{ \\}", Filelist2Latex.EscapeForLatex("# $ % & _ { }"));
    }

    public void testCharactersNeedingCommands() {
        assertEquals("\\textbackslash{} \\textasciicircum{} \\textasciitilde{} \\textless{} \\textgreater{}",
                Filelist2Latex.EscapeForLatex("\\ ^ ~ < >"));
    }

    public void testBackslashIsNotEscapedTwice() {
        // The braces produced for \textbackslash{} must not themselves be escaped.
        assertEquals("a\\textbackslash{}b", Filelist2Latex.EscapeForLatex("a\\b"));
    }

    public void testTypicalComment() {
        assertEquals("seeing \\textasciitilde{}1\", clouds 50\\% \\{thin\\}",
                Filelist2Latex.EscapeForLatex("seeing ~1\", clouds 50% {thin}"));
    }

    public void testFormatTime() {
        // Regression: log sheets failed since 1.9.0 because LocalDateTime was passed to SimpleDateFormat.
        assertEquals("03:04:05", Filelist2Latex.formatTime(LocalDateTime.of(2024, 3, 15, 3, 4, 5)));
        assertEquals("", Filelist2Latex.formatTime(null));
    }

    // --- PDF viewer lookup -------------------------------------------------------------------------------------

    private static final List<File> NONE = Collections.emptyList();

    private static File tmpDir() {
        File d = new File(System.getProperty("java.io.tmpdir"), "viewer-test-" + System.nanoTime());
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

    private static boolean isUnix() {
        return new File("/bin/sh").exists();
    }

    public void testViewerCandidatesPerOs() {
        assertEquals(Arrays.asList("open"), Filelist2Latex.pdfViewerCandidates("Mac OS X"));
        List<String> linux = Filelist2Latex.pdfViewerCandidates("Linux");
        assertEquals("xdg-open", linux.get(0));
        assertTrue(linux.contains("gio open"));
        assertTrue(linux.contains("evince"));
        assertTrue(linux.contains("okular"));
    }

    public void testConfiguredViewerIsKept() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            File viewer = executable(dir, "myviewer");
            executable(dir, "xdg-open");
            assertEquals(viewer.getAbsolutePath() + " --fullscreen", Filelist2Latex.findPdfViewer(
                    viewer.getAbsolutePath() + " --fullscreen", "Linux", Arrays.asList(dir), NONE));
        } finally {
            delete(dir);
        }
    }

    public void testConfiguredViewerFoundInOtherDirectory() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            // Configured /usr/bin/okular does not exist, but okular is installed elsewhere on PATH.
            File okular = executable(dir, "okular");
            executable(dir, "xdg-open");
            assertEquals(okular.getAbsolutePath(), Filelist2Latex.findPdfViewer(
                    "/nonexistent/bin/okular", "Linux", Arrays.asList(dir), NONE));
        } finally {
            delete(dir);
        }
    }

    public void testMissingViewerFallsBackToXdgOpenOnLinux() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            File xdg = executable(dir, "xdg-open");
            executable(dir, "evince");
            assertEquals(xdg.getAbsolutePath(), Filelist2Latex.findPdfViewer(
                    "/nonexistent/okular", "Linux", Arrays.asList(dir), NONE));
        } finally {
            delete(dir);
        }
    }

    public void testGioOpenKeepsSubcommand() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            File gio = executable(dir, "gio");
            assertEquals(gio.getAbsolutePath() + " open", Filelist2Latex.findPdfViewer(
                    "/nonexistent/okular", "Linux", Arrays.asList(dir), NONE));
        } finally {
            delete(dir);
        }
    }

    public void testStandAloneViewerAsLastResort() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            File zathura = executable(dir, "zathura");
            assertEquals(zathura.getAbsolutePath(), Filelist2Latex.findPdfViewer(
                    null, "Linux", Arrays.asList(dir), NONE));
        } finally {
            delete(dir);
        }
    }

    public void testMacUsesOpen() throws Exception {
        if (!isUnix())
            return;
        File dir = tmpDir();
        try {
            File open = executable(dir, "open");
            executable(dir, "xdg-open"); // must not be used on macOS
            assertEquals(open.getAbsolutePath(), Filelist2Latex.findPdfViewer(
                    "/usr/bin/okular-not-here", "Mac OS X", NONE, Arrays.asList(dir)));
        } finally {
            delete(dir);
        }
    }

    public void testNoViewerFound() {
        assertNull(Filelist2Latex.findPdfViewer("/nonexistent/okular", "Linux", NONE, NONE));
    }
}
