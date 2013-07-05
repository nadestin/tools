/**
 * 
 */
package com.riag.tools.MavenCacheCleanup;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 *
 */
public class CacheWalker
{
  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(\\.)?(\\d+)?(\\.)?(.+)?(\\-)?(.+)?");
  private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("(\\d{8})\\.(\\d{6})\\-(\\d+)(.+)");
  private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
  private static final int SNAPSHOT_LEN = "SNAPSHOT".length();
  
  private long m_deleted;
  private long m_failedToDelete;
  private long m_reclaimedSpace;
  
  CacheWalker()
  {
    m_deleted = 0L;
    m_reclaimedSpace = 0L;
    m_failedToDelete = 0L;
  }
  
  /**
   * @return NUmber of deleted files
   */
  public long getDeleted()
  {
    return m_deleted;
  }
  
  /**
   * @return Total size in Bytes of all deleted files
   */
  public long getReclaimedSpace()
  {
    return m_reclaimedSpace;
  }
  
  /**
   * @return Number of files that failed to be deleted
   */
  public long getFailedToDelete()
  {
    return m_failedToDelete;
  }
  
  public int processDirectory(File cacheDir)
  {
    int retval = 0;
    // Search Versions sub-dirs first.
    File[] versions = cacheDir.listFiles(new DirPatternFilter(VERSION_PATTERN, false));
    
    for(File versionDir : versions)
    {
      if(versionDir.getName().endsWith(SNAPSHOT_SUFFIX))  // Only process snapshot version
      {
        cleanSnapshotDir(versionDir);
      }
    }
    
    // Recursively search all sub-dirs which are not Versions
    File[] subdirs = cacheDir.listFiles(new DirPatternFilter(VERSION_PATTERN, true));
    
    for(File subdir : subdirs)
    {
      retval = Math.max(retval, processDirectory(subdir));
    }
    
    return retval;
  }

  
  private void cleanSnapshotDir(File versionDir)
  {
    // Guess Artifact name prefix. VersionDir is a snapshot version directory
    // So ArtifactId is Name of the parent Dir
    // Then comes "-" and then name of versionDisr without trailing "SNAPSHOT".
    // Then we are interested in the pattern "yyyyMMdd.HHmmss" and then "-n" rest is not important
    // All files that are patterned like this should be sorted by DateTime and buildNumber ("-n") in the reverse order
    // and as last step we should delete all but latest.
    // Files without pattern should not be touched
    
    String artifactId = versionDir.getParentFile().getName();
    String artifactBaseVersion = versionDir.getName().substring(0, versionDir.getName().length() - SNAPSHOT_LEN);
    
    String filenamePrefix = artifactId + "-" + artifactBaseVersion;
    
    File[] timestampedFiles = versionDir.listFiles(new TimestampedFileFilter(filenamePrefix));
    if(timestampedFiles.length > 0)
    {
      String versionToKeep = getLatestVersion(timestampedFiles, filenamePrefix);
      if(versionToKeep != null)
      {
        String filePrifixToKeep = filenamePrefix + versionToKeep;
        
        // Delete all files from the list that do not start from 'filePrefixToKeep'
        for(File file : timestampedFiles)
        {
          if(file.getName().startsWith(filePrifixToKeep)) continue;
          
          long fileSize = file.length();
          
          if(file.delete())
          {
            m_deleted++;
            m_reclaimedSpace += fileSize;
          }
          else
          {
            m_failedToDelete++;
            try
            {
              System.err.println("Failed to delete file '" + file.getCanonicalPath() + "'");
            }
            catch(IOException e)
            {
              // Should never occur, ignore
            }
          }
        }
      }
    }
  }


  private String getLatestVersion(File[] timestampedFiles, String filenamePrefix)
  {
    TreeSet<SnapshotUniqueVersion> versions = new TreeSet<SnapshotUniqueVersion>();
    int prefixLen = filenamePrefix.length();
    
    for(File file : timestampedFiles)
    {
      String filenamerest = file.getName().substring(prefixLen);
      Matcher m = SNAPSHOT_VERSION_PATTERN.matcher(filenamerest);
      if(m.matches()) // It should always match
      {
        String dateString = m.group(1);
        String timeString = m.group(2);
        String buildString = m.group(3);
        try
        {
          SnapshotUniqueVersion ver = new SnapshotUniqueVersion(dateString, timeString, buildString);
          versions.add(ver);
        }
        catch(ParseException e)
        {
          System.err.println("Failed to parse filename '" + file.getName() + "'.");
        }
      }
    }
    
    // Treeset should be sorted, so we can just take last element which is latest version
    if(versions.size() > 0)
    {
      SnapshotUniqueVersion latestVersion = versions.last();
      return latestVersion.toString();
    }
    
    // No versions in set, return null
    return null;
  }


  private static final class DirPatternFilter implements FileFilter
  {
    private final Pattern m_pattern;
    private final boolean m_not;
    
    DirPatternFilter(Pattern pattern, boolean notPattern)
    {
      m_pattern = pattern;
      m_not = notPattern;
    }
    
    public boolean accept(File pathname)
    {
      if(pathname.isDirectory())
      {
        Matcher m = m_pattern.matcher(pathname.getName());
        boolean matches = m.matches();
        return (m_not) ? !matches : matches;
      }
      
      return false;
    }
  }
  
  private static final class TimestampedFileFilter implements FileFilter
  {
    private final String m_filePrifix;
    
    TimestampedFileFilter(String prefix)
    {
      m_filePrifix = prefix;
    }
    
    public boolean accept(File pathname)
    {
      if(pathname.isFile())
      {
        String fileName = pathname.getName();
        
        if(fileName.startsWith(m_filePrifix))
        {
          String nameRest = fileName.substring(m_filePrifix.length());
          Matcher m = SNAPSHOT_VERSION_PATTERN.matcher(nameRest);
          return m.matches();
        }
      }
      return false;
    }
    
  }
}
