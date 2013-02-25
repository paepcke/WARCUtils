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
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import edu.stanford.warcutils.warcreader.WarcRecordReader;

/**
 * @author paepcke
 *
 */
public class FilterWarcFiles {
	
	WarcRecordReader recReader = null;
	WarcFilter filter = null;
	String outPrefix = null;
	String currInFileName = null;
	String currOutFileName = null;
	File outFile = null;
	BufferedWriter gzipWriter = null;
	
	boolean DO_APPEND = true;
	
	public FilterWarcFiles(String[] pathNames, String warcKey, String regexPattern) throws IOException {
		this(pathNames, warcKey, regexPattern, "filtered");
	}
	
	public FilterWarcFiles(String[] pathNames, String warcKey, String regexPattern, String theOutPrefix) throws IOException {
		recReader = new WarcRecordReader(pathNames);
		currInFileName = recReader.getCurrentFilePath();
		filter = new WarcFilter(regexPattern, warcKey);
		outPrefix = theOutPrefix;
		processFiles();
	}
	
	public void processFiles() throws IOException {
		boolean filesLeft = true;
		String content = null;
		currInFileName = recReader.getCurrentFilePath();
		currOutFileName = constructOutFileName(currInFileName);
		outFile = new File(currOutFileName);
		if (isGzipped(currInFileName))
			prepareGZipOutWriter(currOutFileName);
		
		while (filesLeft) {
			if (! recReader.nextKeyValue()) {
				filesLeft = false;
				continue;
			}
			if ((content = filter.contentsIf(recReader.getCurrentValue())) == null)
				// Record not wanted: matches the filter regexp:
				continue;
			if (gzipWriter != null)
				gzipWriter.write(content);
			else
				FileUtils.write(outFile, content, DO_APPEND); 
		}
	}
	
	public String constructOutFileName(String fullFilePath) {
		String path     = FilenameUtils.getPath(fullFilePath);
		String rootSpec = FilenameUtils.getPrefix(fullFilePath);
		String fileName = FilenameUtils.getName(fullFilePath);
		String res = rootSpec + path + outPrefix + fileName;
		return res;
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
