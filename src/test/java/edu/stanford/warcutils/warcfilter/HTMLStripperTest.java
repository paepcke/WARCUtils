package edu.stanford.warcutils.warcfilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import edu.stanford.warcutils.warcreader.WarcRecord;
import edu.stanford.warcutils.warcreader.WarcRecordReader;

public class HTMLStripperTest {
	
	String simpleHTML = null;
	String noHTMLBody = null;
	String complexHTML = null;
	String wikipediaHTML = null;
	
	WarcRecordReader warcReader = null;

	@Before
	public void setUp() throws Exception {
		noHTMLBody = "<head><html>This is <b>bold</b> and a <a href='http://test.com'>link anchor</a></html></head>";
		simpleHTML = "<html><head><title>My Title</title></head><body>This is <b>bold</b> and a <a href='http://test.com'>link anchor</a></body></html>";
		complexHTML = FileUtils.readFileToString(new File("src/test/resources/messyHtml.html"));
		wikipediaHTML = FileUtils.readFileToString(new File("src/test/resources/wikipediaHtml.html"));
		
		warcReader = new WarcRecordReader(new File("src/test/resources/tinyWarc0_18.warc"));
	}
	
	@Test
	public void testExtractFromString() {
		String res = HTMLStripper.extractText(simpleHTML);
		//System.out.println(res);
		assertEquals("My TitleThis is bold and a link anchor", res);
		
		res = HTMLStripper.extractText(noHTMLBody);
		//System.out.println(res);
		assertEquals("This is bold and a link anchor", res);
		
		res = HTMLStripper.extractText(complexHTML);
		//System.out.println(res);
		assertEquals(-1, res.indexOf("<"));
		assertEquals(-1, res.indexOf(">"));
		
		res = HTMLStripper.extractText(wikipediaHTML);
		//System.out.println(res);
		assertEquals(-1, res.indexOf("<"));
		assertEquals(-1, res.indexOf(">"));
		
	}
	
	@Test
	public void testExtractFromWarcRecord() throws IOException {
		WarcRecord res = null;
		boolean foundHtmlRecord = false;
		WarcRecord rec = null;
		String recID = null;
		while (!foundHtmlRecord) {
			warcReader.nextKeyValue();
			rec = warcReader.getCurrentValue();
			if (rec == null)
				fail("Did not find recordID");
			
			recID = rec.get("WARC-Record-ID");
			if (recID.contains("<urn:uuid:721f9a28-6b9a-44c1-bccd-8c7accb514cd>"))
				foundHtmlRecord = true;
		}
		// Len before strip
		assertEquals("21064", rec.get("content-length"));
		assertEquals(21064, rec.get("content").length());

		// Len before strip
		res = HTMLStripper.extractText(rec);
		assertEquals("5702", res.get("content-length"));
		assertEquals(5702, res.get("content").length());
	}

}
