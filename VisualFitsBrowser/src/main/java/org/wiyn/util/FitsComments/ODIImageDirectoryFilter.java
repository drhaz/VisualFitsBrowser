package org.wiyn.util.FitsComments;

import java.io.File;

public class ODIImageDirectoryFilter implements java.io.FilenameFilter {
    private String matchRegex = "[odftc].*";

    public ODIImageDirectoryFilter() {

    }

    public boolean accept (File dir, String name) {
	return name.matches (matchRegex);
    }

}