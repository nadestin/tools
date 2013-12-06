package ch.ringler.tools.m2cachecleanup;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which deletes outdated timestamped snapshot artifacts from the local
 * Maven cache
 * 
 */
@Mojo(name = "cleanup-cache", defaultPhase = LifecyclePhase.CLEAN)
public class CleanupMavenCache extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${settings.localRepository}", property = "directory", required = true)
	private File directory;

	public void execute() throws MojoExecutionException {
		
	    try {
			//
			// Sanity checks
			//
			if(!isValidCache(directory)) throw new MojoExecutionException("Directory '" + directory.getCanonicalPath() + "' is not a maven cache");
			
			CacheWalker walker = new CacheWalker(getLog());
			getLog().info("Cleaning Maven local cache at '" + directory.getCanonicalPath() + "'");
			walker.processDirectory(directory);
			
			// Print statistics
			getLog().info("Totally deleted " + walker.getDeleted() + " file(s).");
			getLog().info("Reclaimed space " + getHrSize(walker.getReclaimedSpace()));
			if(walker.getFailedToDelete() > 0)
			{
				getLog().info("Failed to delete " + walker.getFailedToDelete() + " file(s).");
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unexpected exception during cache cleanup", e);
		}
	}

	private boolean isValidCache(File cacheDir) throws IOException {

	    // TODO Implement tests that cacheDir is really maven cache directory. At the moment I have no any particular rules for that.
	    
	    return true;
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
