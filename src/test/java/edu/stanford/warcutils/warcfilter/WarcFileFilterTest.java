package edu.stanford.warcutils.warcfilter;

import static org.grep4j.core.Grep4j.constantExpression;
import static org.grep4j.core.Grep4j.grep;
import static org.grep4j.core.fluent.Dictionary.executing;
import static org.grep4j.core.fluent.Dictionary.on;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.grep4j.core.model.Profile;
import org.grep4j.core.model.ProfileBuilder;
import org.junit.Before;
import org.junit.Test;

import edu.stanford.warcutils.warcfilter.WarcFileFilter.FilterSense;
import edu.stanford.warcutils.warcfilter.WarcFileFilter.WarcHeaderRetention;
import edu.stanford.warcutils.warcreader.WarcCounter;


public class WarcFileFilterTest {

	WarcFileFilter fileFilterWithTmpDir = null;
	WarcFileFilter fileFilterNoTmpDir = null;
	WarcFileFilter fileFilterWarcs = null;
	String tmpDirPath = null;
	Collection<File> filesToFilter = null;
	
	Profile warc0_18Profile = null;
	Profile warc1_0Profile = null;
	Profile	warc1_0ProfileInitial = null;	
	
	@Before
	public void setUp() throws Exception {
		tmpDirPath = FileUtils.getTempDirectoryPath();
		
		// For testing output file name construction:
		fileFilterWithTmpDir = new WarcFileFilter(tmpDirPath, "filter_test_");
		fileFilterNoTmpDir = new WarcFileFilter(null, "filter_test_");

		// For file filtering test:
		// Get the file names:
		File resourcesDir = FileUtils.getFile("src/test/resources");
		filesToFilter = FileUtils.listFiles(resourcesDir, null, false);
		// Delete all output files in preparation to creating them:
		deleteTestOutputFiles();
		
		// Prepare grep4j to ensure output files are correct:
		warc0_18Profile = ProfileBuilder.newBuilder()
				.name("WarcFileFilter Test Output Warc0_18.warc")
				.filePath("/tmp/filteredTest_tinyWarc0_18.warc")
				.onLocalhost()
				.build();
		warc1_0Profile = ProfileBuilder.newBuilder()
				.name("WarcFileFilter Test Output Warc1_0.warc")
				.filePath("/tmp/filteredTest_tinyWarc1_0.warc")
				.onLocalhost()
				.build();
		
		warc1_0ProfileInitial = ProfileBuilder.newBuilder()
				.name("WarcFileFilter Test input Warc1_0.warc")
				.filePath("src/test/resources/tinyWarc1_0.warc")
				.onLocalhost()
				.build();
		
	}
		
	private void deleteTestOutputFiles() {
		WarcFileFilter tmpFilter = new WarcFileFilter(tmpDirPath, "filteredTest_");
		for (File inFile : filesToFilter) {
			String outFile = tmpFilter.constructOutFileName(inFile.getAbsolutePath());
			FileUtils.deleteQuietly(new File(outFile));
		}
	}

	@Test
	public void testConstructOutFileName() {
		String outFileName = fileFilterWithTmpDir.constructOutFileName("/foo/bar/test.warc");
		assertEquals("/tmp/filter_test_test.warc", outFileName);
		outFileName = fileFilterNoTmpDir.constructOutFileName("/foo/bar/test.warc");
		assertEquals("/foo/bar/filter_test_test.warc", outFileName);
	}
	
	@Test
	public void testFilteringDiscardIfMatches() throws IOException {
		fileFilterWarcs = new WarcFileFilter(filesToFilter, 
											  "WARC-Target-URI", 
											  ".*?/smasher\\.html$", 
											  FilterSense.DISCARD_IF_MATCHES,
											  tmpDirPath, 
											  "filteredTest_",
											  WarcHeaderRetention.RETAIN_WARC_HEADERS);
		assertEquals(0, (executing(grep(constantExpression("smasher"), on(warc0_18Profile, warc1_0Profile))).totalLines()));
	}
		
	@Test
	public void testFilteringDiscardIfNotMatches() throws IOException {
		fileFilterWarcs = new WarcFileFilter(filesToFilter, 
											  "content-length", 
											  "406", 
											  FilterSense.DISCARD_IF_NOT_MATCHES,
											  tmpDirPath, 
											  "filteredTest_",
											  WarcHeaderRetention.RETAIN_WARC_HEADERS);
		
		assertEquals(1, (executing(grep(constantExpression("Content-Length"), on(warc1_0Profile))).totalLines()));
	}

	
	@Test
	public void testFilteringContent() throws IOException {
		File origFile = new File("src/test/resources/tinyWarc1_0.warc");
		assertEquals(1, (executing(grep(constantExpression("small_crawls"), on(warc1_0ProfileInitial))).totalLines()));
		assertEquals(45, WarcCounter.count(origFile));
		File fileToFilter = FileUtils.getFile("src/test/resources/tinyWarc1_0.warc");
		fileFilterWarcs = new WarcFileFilter(fileToFilter, 
											  "content", 
											  "(?s).*small_crawls.*", 
											  FilterSense.DISCARD_IF_MATCHES,
											  tmpDirPath, 
											  "filteredTest_",
											  WarcHeaderRetention.RETAIN_WARC_HEADERS);

		File filteredFile = new File("/tmp/filteredTest_tinyWarc1_0.warc");
		assertEquals(0, (executing(grep(constantExpression("small_crawls"), on(warc1_0Profile))).totalLines()));
		assertEquals(44, WarcCounter.count(filteredFile));
	}	
	
}
