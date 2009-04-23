package com.mdimension.woinstaller;

public interface IProgressMonitor {
  public void progress(long amount, long totalSize);
  
  public void done();
}
