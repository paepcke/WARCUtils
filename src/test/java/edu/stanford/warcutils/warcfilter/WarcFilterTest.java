package edu.stanford.warcutils.warcfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import edu.stanford.warcutils.warcreader.WarcRecord;
import edu.stanford.warcutils.warcreader.WarcRecordReader;

public class WarcFilterTest {

	File testWarcFile;
	WarcRecordReader warcReader0_18GZipped;
	
	@Before
	public void setUp() throws Exception {
		//---------- WARC/0.18GZipped -------------		
		testWarcFile = new File("src/test/resources/tinyWarc0_18.warc");
		if (!testWarcFile.exists()) {
			System.out.println("File " + testWarcFile + " does not exist.");
		} else {
			//System.out.println("File " + testWarcFile0_18GZipped + " exists.");
			//System.out.println("File length is " + testWarcFile0_18GZipped.length());
		}
		warcReader0_18GZipped = new WarcRecordReader(testWarcFile);
	}
	
	@Test
	public void test() throws IOException {
		WarcFilter filter = new WarcFilter(".*-0400", "warc-date");
		WarcRecord rec = null;
		WarcRecord matchingRec = null;
		WarcRecord notMatchingRec = null;
		while (warcReader0_18GZipped.nextKeyValue(true)) {
			rec = warcReader0_18GZipped.getCurrentRecord();
			if (filter.matches(rec)) {
				assertEquals("Failed on date match", "2009-04-119T12:48:17-0400", rec.get("warc-date"));
				matchingRec = rec;
			}
			else
				notMatchingRec = rec;
		}
		String content = filter.contentsIf(matchingRec);
		long strIndex = content.indexOf("modified for clueweb09");
		assertNotEquals(strIndex, -1);
		assertNull(filter.contentsIf(notMatchingRec));
	}
}
