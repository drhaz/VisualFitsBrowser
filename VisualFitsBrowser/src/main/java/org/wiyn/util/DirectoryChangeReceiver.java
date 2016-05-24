package org.wiyn.util;

import java.io.File;
import java.util.Vector;

public interface DirectoryChangeReceiver {

  public void onDirectoryChanged(File myDirectory);

  public void addSingleNewItem(File newItem);

  public Vector<File> getListedFiles();

}
