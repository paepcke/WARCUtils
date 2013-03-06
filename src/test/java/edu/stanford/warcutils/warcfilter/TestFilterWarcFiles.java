package edu.stanford.warcutils.warcfilter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.stanford.warcutils.warcfilter.FilterWarcFiles.FilterSense;
import edu.stanford.warcutils.warcfilter.FilterWarcFiles.WarcHeaderRetention;

public class TestFilterWarcFiles {

	FilterWarcFiles fileFilterWithTmpDir = null;
	FilterWarcFiles fileFilterNoTmpDir = null;
	FilterWarcFiles fileFilterWarcs = null;
	String tmpDirPath = null;
	Collection<File> filesToFilter = null;
	
	@Before
	public void setUp() throws Exception {
		//FileUtils.deleteQuietly(arg0)
		tmpDirPath = FileUtils.getTempDirectoryPath();
		
		// For testing output file construction:
		fileFilterWithTmpDir = new FilterWarcFiles(tmpDirPath, "filter_test_");
		fileFilterNoTmpDir = new FilterWarcFiles(null, "filter_test_");

		// For file filtering test:
		Map<String, String> env = System.getenv();
		// Get the file names:
		File resourcesDir = FileUtils.getFile(env.get("HOME"),"EclipseWorkspaces/WARCUtils/src/test/resources");
		filesToFilter = FileUtils.listFiles(resourcesDir, null, false);
		// Delete all output files in preparation to creating them:
		deleteTestOutputFiles();
	}
		
	private void deleteTestOutputFiles() {
		FilterWarcFiles tmpFilter = new FilterWarcFiles(tmpDirPath, "filteredTest_");
		for (File inFile : filesToFilter) {
			String outFile = tmpFilter.constructOutFileName(inFile.getAbsolutePath());
			FileUtils.deleteQuietly(new File(outFile));
		}
	}

	@Test
	@Ignore
	public void testConstructOutFileName() {
		String outFileName = fileFilterWithTmpDir.constructOutFileName("/foo/bar/test.warc");
		assertEquals("/tmp/filter_test_test.warc", outFileName);
		outFileName = fileFilterNoTmpDir.constructOutFileName("/foo/bar/test.warc");
		assertEquals("/foo/bar/filter_test_test.warc", outFileName);
	}
	
	@Test
	public void testFiltering() throws IOException {
		fileFilterWarcs = new FilterWarcFiles(filesToFilter, 
											  "WARC-Target-URI", 
											  ".*?/smasher\\.html$", 
											  FilterSense.DISCARD_IF_MATCHES,
											  tmpDirPath, 
											  "filteredTest_",
											  WarcHeaderRetention.RETAIN_WARC_HEADERS);
	}
	
}
