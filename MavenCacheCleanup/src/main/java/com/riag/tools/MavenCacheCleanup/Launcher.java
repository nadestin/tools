package com.riag.tools.MavenCacheCleanup;

import java.io.File;
import java.io.IOException;

/**
 * Utility to purge old versions of the timestamped snapshot artefacts from the local maven cache for the current user
 */
public class Launcher
{
  private static final String OPT_DIR = "-dir";
  private static final String OPT_VERBOSE = "-v";
  
  private File m_baseDir;
  private boolean m_verbose;
  
  private Launcher()
  {
    m_baseDir = new File(new File(System.getProperty("user.home"), ".m2"), "repository"); // Deafult location of the maven cache
  }
  
  public static void main(String[] args)
  {
    try
    {
      int status =  new Launcher().perform(args);
      System.exit(status);
    }
    catch(Throwable e)
    {
      System.err.println("Unexpected error during program execution");
      e.printStackTrace(System.err);
      System.exit(2);
    }
  }

  private boolean parseArgs(String[] args) throws IOException
  {
    boolean retval = true;
    
    if(args != null && args.length > 0)
    {
      for(int i=0; i < args.length; i++)
      {
        String key = args[i];
        
        if(OPT_DIR.equals(key))
        {
          // -dir key given
          i++;
          if(i >= args.length)
          {
            System.err.println("Option -dir should be followed by directory path");
            return false;
          }
          String cacheDir = args[i];
          
          File tmp = new File(cacheDir);
          if(!tmp.exists())
          {
            System.err.println("Directory '" + tmp.getCanonicalPath() + "' not exists.");
            return false;
          }
          
          if(!tmp.isDirectory())
          {
            System.err.println("Argument '" + key + "' does not points to a directory.");
            return false;
          }

          m_baseDir = tmp;
          continue;
        }
        else if (OPT_VERBOSE.equals(key))
        {
          m_verbose = true;
          continue;
        }
        
        // Option is unknown, print error message and exit
        System.err.println("Argument '" + key + "' is not a valid option.");
        retval = false;
      }
      
    }
    return retval;
  }

  private boolean isValidCache(File cacheDir) throws IOException
  {
    // Cache directory should contain file "repository.xml"
    
//    File repXml = new File(cacheDir, "repository.xml");
//    
//    if(!repXml.exists())
//    {
//      System.err.println("Directory '" + cacheDir.getCanonicalPath() + "' does not look like maven cache");
//    }
    
    // TODO Implement more advanced test
    
    return true;
  }

  private int perform(String[] args) throws IOException
  {
    if(!parseArgs(args)) return 1;
      
    //
    // Sanity checks
    //
    if(!isValidCache(m_baseDir)) return 3;
    
    CacheWalker walker = new CacheWalker(m_verbose);
    System.out.println("Cleaning Maven local cache at '" + m_baseDir.getCanonicalPath() + "'");
    int retval = walker.processDirectory(m_baseDir);
    
    // Print statistics
    System.out.println("Totally deleted " + walker.getDeleted() + " file(s).");
    System.out.println("Reclaimed space " + getHrSize(walker.getReclaimedSpace()));
    if(walker.getFailedToDelete() > 0)
    {
      System.out.println("Failed to delete " + walker.getFailedToDelete() + " file(s).");
    }
    
    return retval;
  }

  private static long KB = 1024L;
  private static long MB = KB * 1024L;
  private static long GB = MB * 1024L;
  
  private String getHrSize(long reclaimedSpace)
  {
    String unit = "Byte(s)";
    double value = (double) reclaimedSpace;
    
    // Convert Size in Bytes to more human readable form Kb, Mb, Gb.
    if(reclaimedSpace >= GB)
    {
      value = value / (double) GB;
      unit = "GB";
    }
    else if(reclaimedSpace >= MB)
    {
      value = value / (double) MB;
      unit = "MB";
    }
    else if(reclaimedSpace >= KB)
    {
      value = value / (double) KB;
      unit = "KB";
    }
    
    return String.format("%1$.2f %2$s", value, unit);
  }

}
