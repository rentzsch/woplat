package com.mdimension.woinstaller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.mdimension.archiver.CPIO;
import com.mdimension.archiver.XarFile;

import er.extensions.ERXFileUtilities;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("usage: java -jar WOInstaller.jar [5.3.3|5.4.3] [destinationFolder]");
      
      System.out.println("\nExample:");
      System.out.println("WO 5.3.3 on Windows");
      System.out.println("       java -jar WOInstaller.jar 5.3.3 C:\\Apple");
      
      System.out.println();
      System.out.println("WO 5.3.3 on OS X (in alternate folder)");
      System.out.println("       java -jar WOInstaller.jar 5.3.3 /opt");
      System.exit(1);
    }

    URL woDmgUrl;
    int woVersion = 0;
    long rawLength = 0;
    long fileLength = 0;
    String version = args[0];
    List<BlockEntry> blockList = new ArrayList<BlockEntry>();
    if ("5.3.3".equals(version)) {
      woDmgUrl = new URL("http://supportdownload.apple.com/download.info.apple.com/Apple_Support_Area/Apple_Software_Updates/Mac_OS_X/downloads/061-2998.20070215.33woU/WebObjects5.3.3Update.dmg");
      BlockEntry entry = new BlockEntry();
      entry.offset = 11608064L;
      entry.length = 29672581L;
      blockList.add(entry);
      rawLength = 51252394;
      woVersion = 53;
      fileLength = 42321716L;
    } 
    else if ("5.4.3".equals(version)) {
      woDmgUrl = new URL("http://download.info.apple.com/Mac_OS_X/061-4634.20080915.3ijd0/WebObjects543.dmg");
      BlockEntry entry = new BlockEntry();
      entry.offset = 58556928L;
      entry.length = 107601091L;
      blockList.add(entry);
      rawLength = 153786259L;
      fileLength = 166167249L;
    }
    else if ("dev".equals(version)) {
      woDmgUrl = new URL("file:///tmp/WebObjectsUpdate.dmg");
      BlockEntry entry = new BlockEntry();
      entry.offset = 11608064L;
      entry.length = 29672581L;
      blockList.add(entry);
      rawLength = 51252394;
      woVersion = 53;
      fileLength = 42321716L;
    }
    else if ("dev54".equals(version)) {
      woDmgUrl = new URL("file:///tmp/WebObjects543.dmg");
      BlockEntry entry = new BlockEntry();
      entry.offset = 58556928L;
      entry.length = 107601091L;
      blockList.add(entry);
      rawLength = 153786259L;
      woVersion = 54;
      fileLength = 166167249L;
    }
    else {
      throw new IllegalArgumentException("Unknown WebObjects version '" + version + "'.");
    }
        
    File destinationFolder = new File(args[1]);
    if (destinationFolder.exists()) {
      if (!destinationFolder.canWrite()) {
        throw new IOException("You do not have permission to write to the folder '" + destinationFolder + "'.");
      }
    }
    else if (!destinationFolder.mkdirs()) {
      throw new IOException("Failed to create the directory '" + destinationFolder + "'.");
    }

    String osName = System.getProperty("os.name");
    boolean windows = osName.toLowerCase().indexOf("windows") != -1;
    boolean osx = osName.toLowerCase().indexOf("os x") != -1;
    boolean symbolicLinksSupported = !windows;
    
    File woDmgFile = new File("WebObjectsUpdate.dmg");
    if (!woDmgFile.exists() || woDmgFile.length() != fileLength) {
    	woDmgFile = File.createTempFile("WebObjects.", ".dmg");
        woDmgFile.deleteOnExit();
        ERXFileUtilities.writeUrlToFile(woDmgUrl, woDmgFile, new ConsoleProgressMonitor("Downloading WebObjects"));
    }

    InputStream woPaxGZIs;
    if (woVersion == 53) {
      woPaxGZIs = new MultiBlockInputStream(new BufferedInputStream(new FileInputStream(woDmgFile)), blockList);
    } else {
      //woVersion = 54
      InputStream woPkgIs = new MultiBlockInputStream(new BufferedInputStream(new FileInputStream(woDmgFile)), blockList);
      XarFile xarfile = new XarFile(woPkgIs);
      woPaxGZIs = xarfile.getInputStream("Payload");
    }
    GZIPInputStream woPaxFileIs = new GZIPInputStream(woPaxGZIs);
    CPIO cpio = new CPIO(woPaxFileIs);
    cpio.setLength(rawLength);
    cpio.extractTo(destinationFolder, symbolicLinksSupported, new ConsoleProgressMonitor("Extracting WebObjects Runtime"));    	
    
    
    if (!osx) {
      System.out.print("Shuffling file structure for " + osName + ": ");
      File localFolder = new File(destinationFolder, "Local");
      if (localFolder.exists()) {
        throw new IOException("The folder '" + localFolder + "' already exists.");
      }
      if (!localFolder.mkdirs()) {
        throw new IOException("Failed to create the directory '" + localFolder + "'.");
      }

      File localLibraryFolder = new File(localFolder, "Library");

      File originalLibraryFolder = new File(destinationFolder, "Library");
      if (!originalLibraryFolder.renameTo(localLibraryFolder)) {
        throw new IOException("Failed to move '" + originalLibraryFolder + "' to '" + localLibraryFolder + "'.");
      }

      File originalSystemFolder = new File(destinationFolder, "System");

      File originalSystemLibraryFolder = new File(originalSystemFolder, "Library");
      File libraryFolder = new File(destinationFolder, "Library");
      if (libraryFolder.exists()) {
        throw new IOException("The folder '" + libraryFolder + "' already exists.");
      }
      if (!originalSystemLibraryFolder.renameTo(libraryFolder)) {
        throw new IOException("Failed to move '" + originalSystemLibraryFolder + "' to '" + libraryFolder + "'.");
      }

      if (!originalSystemFolder.delete()) {
        throw new IOException("Failed to delete '" + originalSystemFolder + ".");
      }
      System.out.println("Done");
    }
    
    System.out.println("Installation Complete");
  }
}
