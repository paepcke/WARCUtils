package edu.stanford.warcutils.warcreader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

public class WarcCounter {

	public static long count(Collection<File> warcFiles) throws IOException {
		if (warcFiles.size() == 0)
			throw new IOException("WarcCounter received an empty list of files/directories in which to count records.");
		WarcRecordReader recReader = new WarcRecordReader(warcFiles);
		return doCount(recReader);
	}

	public static long count(File warcDir) throws IOException {
		WarcRecordReader recReader = new WarcRecordReader(warcDir);
		return doCount(recReader);
	}

	private static long doCount(WarcRecordReader recReader) throws IOException {
		long numRecs = 0L;
		while (recReader.nextKeyValue(false))
			numRecs++;
		return numRecs;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			File dir = new File(args[0]);
			try {
				System.out.println(WarcCounter.count(dir));
				return;
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} else {
			LinkedList<File> fileObjs = new LinkedList<File>();
			for (String fileOrDirPath : args) {
				//********
				//System.out.println(fileOrDirPath);
				//********
				File fileObj = new File(fileOrDirPath);
				fileObjs.add(fileObj);
				if (!fileObj.canRead()) {
					System.out.println("File " + fileObj.getAbsolutePath() + " does not exist or is not readable. Fix and try again.");
					System.exit(1);
				}
			}
			try {
				System.out.println(WarcCounter.count(fileObjs));
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
