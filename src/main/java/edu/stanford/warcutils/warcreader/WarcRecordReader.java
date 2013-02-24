package edu.stanford.warcutils.warcreader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * @author paepcke
 *
 */


/**
 * Given either a WARC file (gzipped or clear), or an array of WARC
 * file paths, or a directory containing WARC files (gzipped, 
 * clear, or mixed), return attribute/value pairs,
 * going through the files and extracting one WARC record at a time
 * Keys are offset into the file and values are Warc records, i.e.
 * WarcRecord instances. In case of a directory being processed,
 * the offsets continue across file limits, as if all files were
 * one single stream of WARC records.
 * 
 * Note that the sequence of files when processing a directory
 * is not defined. However, the constructor taking an array of
 * paths to WARC file is guaranteed to be processed in the given
 * order.
 * 
 * There is currently no ability for random access.
 */

public class WarcRecordReader {

	private static final boolean DO_READ_CONTENT = true;
	private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
	private long pos;
	private LineAndChunkReader warcLineReader;
	private DataInputStream warcInStream; 
	private long keyWarcStreamPos = (long) 0;
	private WarcRecord valueWarcRecord = null;
	private FileInputStream fileIn = null;
	private Logger logger = null;
	private StringBuilder errMsgs = null;
	private Formatter strFormatter = new Formatter(errMsgs);
	private LinkedList<File> allFiles = null;

	/**
	 * Provide a single WARC file. 
	 * @param warcPath: WARC file, gzipped or clear.
	 */
	public WarcRecordReader(File warcPath) {

		if (warcPath.isDirectory()) {
			File[] dirFilePaths = warcPath.listFiles();
			allFiles = new LinkedList<File>();
			for (File filePath : dirFilePaths) {
				allFiles.add(filePath);
			}
			initForOneFile(allFiles.remove());
		} else {
			initForOneFile(warcPath);
		}
	}

	/**
	 * Array of paths to WARC files 
	 * @param warcPaths: Array of WARC file paths, clear of gzipped.
	 */
	public WarcRecordReader(String[] warcPaths) {
		allFiles = new LinkedList<File>();
		for (String fileName : warcPaths) {
			allFiles.add(new File(fileName));
		}
		initForOneFile(allFiles.remove());
	}

	/**
	 * Request that the next stream position/WARC record pair be read. 
	 * If multiple WARC files were provided in the constructor, method
	 * will automatically begin reading the next file, when one is exhausted. 
	 * @return true if a pair was available, false if end of file(s) reached.
	 * @throws IOException when read error other than end of file(s) occurs.
	 */
	public boolean nextKeyValue() throws IOException {
		return nextKeyValue(DO_READ_CONTENT);
	}

	/**
	 * Variant of nextKeyValue() that enables caller to specify whether only
	 * the WARC record metadata is to be read, or the record content as well.
	 * @param readContents determines whether WARC record content is read in addition to metadata, or not.
	 * @return true if a pair was available, false if end of file(s) occurs.
	 * @throws IOException when read error other than end of file(s) occurs.
	 */
	public boolean nextKeyValue(boolean readContents) throws IOException {
		keyWarcStreamPos = pos;
		valueWarcRecord = null;
		while (valueWarcRecord == null) {
			// Grab next record in current WARC file:
			valueWarcRecord = WarcRecord.readNextWarcRecord(warcLineReader, readContents);
			if (valueWarcRecord == null) {
				// File is done:
				// Another WARC file in queue?
				try {
					initForOneFile(allFiles.remove());
				} catch (NoSuchElementException e) {
					// No, processed all files.
					keyWarcStreamPos = 0;
					return false;
				}
			}
		}

		//System.out.println("Pulled another WARC record.");

		// Update position wbRecordReader the Data stream
		pos += valueWarcRecord.getTotalRecordLength();
		return true;
	}

	public long getCurrentKey() {
		return keyWarcStreamPos;
	}

	public WarcRecord getCurrentValue() {
		return valueWarcRecord;
	}

	/**
	 * Get the progress within the file:
	 */
	/*  public float getProgress() {
    if (start == end) {
      return 0.0f;
    } else {
      return Math.min(1.0f, (pos - start) / (float)(end - start));
    }
  }
	 */ 
	public synchronized void close() throws IOException {
		if (warcLineReader	 != null) {
			warcLineReader.close(); 
		}
	}

	private void initForOneFile(File warcFilePath) {
		try {
			fileIn = new FileInputStream(warcFilePath);
			GZIPInputStream gzWarcInStream = new GZIPInputStream(fileIn,DEFAULT_BUFFER_SIZE);
			warcInStream = new DataInputStream(gzWarcInStream);
		} catch (IOException e) {
			// Not a gzipped file?
			try {
				// The attempt to read using the gzip stream above consumed
				// the first two bytes. Reset:
				fileIn = new FileInputStream(warcFilePath);
				warcInStream = new DataInputStream (fileIn);
			} catch (Exception e1) {
				logger.info(strFormatter.format("Could not open WARC file %s.", warcFilePath.getAbsolutePath()).toString());
				return;
			}
		}
		warcLineReader = new LineAndChunkReader(warcInStream);
		//start = 0;
		pos = 0;
	}
}

