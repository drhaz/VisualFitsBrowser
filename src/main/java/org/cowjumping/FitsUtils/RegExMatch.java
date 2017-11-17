package org.cowjumping.FitsUtils;

import java.io.File;

public class RegExMatch implements java.io.FilenameFilter {
  private String matchRegex;

  public RegExMatch(String regex) {
    matchRegex = regex;
  }

  public boolean accept(File dir, String name) {
    return name.matches(matchRegex);
  }

}