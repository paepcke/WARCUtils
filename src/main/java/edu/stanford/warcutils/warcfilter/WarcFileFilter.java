/**
 * 
 */
package edu.stanford.warcutils.warcfilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import edu.stanford.warcutils.warcreader.WarcRecordReader;

/**
 * @author paepcke
 *
 */
public class WarcFileFilter {
	
	enum FilterSense {
		DISCARD_IF_MATCHES,
		DISCARD_IF_NOT_MATCHES
	}
	
	enum WarcHeaderRetention {
		RETAIN_WARC_HEADERS,
		DISCARD_WARC_HEADERS
	}
	
	WarcRecordReader recReader = null;
	WarcFilter filter = null;
	String outPrefix = null;
	String currInFileName = null;
	String currOutFileName = null;
	File outFile = null;
	File outDir  = null;
	BufferedWriter gzipWriter = null;
	WarcHeaderRetention keepWarcHeaders = null;
	FilterSense filterSense = null;
	
	boolean DO_APPEND = true;
	
	/**
	 * Create file filter where output paths go to
	 * same place as input files, the file names are
	 * prefixed with "filtered_", and the WARC headers
	 * are retained.
	 * placed, and any prefix that is prepended to the output file names.
	 * @param pathFiles
	 * @param warcKey
	 * @param regexPattern
	 * @throws IOException
	 */
	public WarcFileFilter(Collection<File> pathFiles, 
						   String warcKey, 
						   String regexPattern
						   ) throws IOException {
		// Use defaults; the null is for the output directory.
		// This value will cause outputs to go to same directory
		// as inputs:
		this(pathFiles, warcKey, regexPattern, FilterSense.DISCARD_IF_NOT_MATCHES, null, "filtered_", WarcHeaderRetention.RETAIN_WARC_HEADERS);
	}
	
	
	/**
	 * Create file filter with choice of where output files are
	 * placed, and any prefix that is prepended to the output file names.
	 * @param pathNames
	 * @param warcKey
	 * @param regexPattern
	 * @param filterSense 
	 * @param theOutDirPath
	 * @param theOutPrefix
	 * @param doRetainWarcHeaders
	 * @throws IOException
	 */
	public WarcFileFilter(Collection<File> pathFiles, 
						   String warcKey, 
						   String regexPattern,
						   FilterSense theFilterSense,
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention) throws IOException {
		recReader = new WarcRecordReader(pathFiles);
		// We need to close our output files, and start
		// a new output files whenever the reader is done
		// with one input file, and is moving on to the
		// next. So install a callback:
		recReader.setCallback(this, "oneInFileProcessed");
		currInFileName = recReader.getCurrentFilePath();
		filter = new WarcFilter(regexPattern, warcKey);
		outPrefix = theOutPrefix;
		keepWarcHeaders = headerRetention;
		filterSense = theFilterSense;
		if (theOutDirPath != null)
			outDir = new File(theOutDirPath);
		processFiles();
	}
	
	/**
	 * This constructor ONLY FOR UNIT TESTING !!!
	 * @param theOutDirPath
	 * @param theOutPrefix
	 */
	public WarcFileFilter(String theOutDirPath, String theOutPrefix) {
		outPrefix = theOutPrefix;
		if (theOutDirPath != null)
				outDir = new File(theOutDirPath);
	}
	
	public void processFiles() throws IOException {
		boolean filesLeft = true;
		String content = null;
		prepareOutputFile();
		
		while (filesLeft) {
			if (! recReader.nextKeyValue()) {
				filesLeft = false;
				continue;
			}
			// Grab record with or without the WARC header, depending
			// on the regex match:
			if (keepWarcHeaders == WarcHeaderRetention.RETAIN_WARC_HEADERS)
				if (filterSense == FilterSense.DISCARD_IF_MATCHES)
					content = filter.allIfNot(recReader.getCurrentValue());
				else
					content = filter.allIf(recReader.getCurrentValue());
			else
				if (filterSense == FilterSense.DISCARD_IF_MATCHES)
					content = filter.contentsIfNot(recReader.getCurrentValue());
				else
					content = filter.contentsIf(recReader.getCurrentValue());
			if (content == null)
				// Record not wanted: matches the filter regexp:
				continue;
			if (gzipWriter != null)
				gzipWriter.write(content);
			else
				FileUtils.write(outFile, content, DO_APPEND); 
		}
	}


	/**
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void prepareOutputFile() throws FileNotFoundException, IOException {
		currInFileName = recReader.getCurrentFilePath();
		currOutFileName = constructOutFileName(currInFileName);
		outFile = new File(currOutFileName);
		if (isGzipped(currInFileName))
			prepareGZipOutWriter(currOutFileName);
		else
			// Ensure that processFiles knows we are not
			// dealing with gzipped files any more:
			gzipWriter = null;
	}
	
	/**
	 * Callback method: One input file was processed by the
	 * WarcRecordReader. Close the previous out file, if necessary,
	 * and open the next output file. 
	 * @param finishedFilePath
	 * @param nextFilePath input file next to be processed, or empty string, if all inputs have been processed.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void oneInFileProcessed(String finishedFilePath, String nextFilePath) throws FileNotFoundException, IOException {
		if (isGzipped(currOutFileName))
			try {
				gzipWriter.close();
			} catch (IOException e) {
				// best effort
			}
		// Get a new output file to hold the new input file info:
		if (!nextFilePath.isEmpty())
			prepareOutputFile();
	}
	
	public String constructOutFileName(String fullFilePath) {
		String path     = FilenameUtils.getPath(fullFilePath);
		String fileName = FilenameUtils.getName(fullFilePath);
		String res = "";
		// If no output dir was specified, out files go 
		// to directory where infiles are:
		if (outDir == null) {
			// Root spec is for Windows: C:, etc:
			String rootSpec = FilenameUtils.getPrefix(fullFilePath);
			File newFileName = FileUtils.getFile(rootSpec,path, outPrefix + fileName);
			res = newFileName.getAbsolutePath();
			return res;
		}
		File newFileName = FileUtils.getFile(outDir, outPrefix + fileName);
		return newFileName.getAbsolutePath();
	}
	
	public boolean isGzipped(String fileName) {
		return (FilenameUtils.getExtension(fileName).equals("gz")); 
	}
	
	public void oneFileProcessed(String absFilePath) {
		if (isGzipped(currInFileName)) {
            if (gzipWriter != null) {
                try {
                    gzipWriter.close();
                } catch (IOException e) {
                    System.err.println("Could not close gzip output for " + currOutFileName);
                }			
            }
		}
	}

	private void prepareGZipOutWriter(String fileName) throws FileNotFoundException, IOException {
		//Construct the BufferedWriter object
		gzipWriter = new BufferedWriter(
				new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(fileName))));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
