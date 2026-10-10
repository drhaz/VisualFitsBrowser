package org.cowjumping.VisualFitsBrowser.util;

import junit.framework.TestCase;

import java.time.LocalDateTime;

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
}
