package com.mdimension.woinstaller;

public class NullProgressMonitor implements IProgressMonitor {
  public void done() {
    // DO NOTHING
  }

  public void progress(long amount, long totalSize) {
    // DO NOTHING
  }
}
