package edu.stanford.warcutils.warcreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class WarcRecordReaderTest {

	File testWarcFile0_18;
	File testWarcFile0_18GZipped;
	File testWarcFile1_0;
	File testWarcDir;
	WarcRecordReader warcReader0_18;
	WarcRecordReader warcReader1_0;
	WarcRecordReader warcReader0_18GZipped;
	WarcRecordReader warcReaderDir;

	ArrayList<String> fileArray = null;
	
	@Before
	public void setUp() throws Exception {
		
		//---------- WARC/0.18 -------------
		
		testWarcFile0_18 = new File("src/test/resources/tinyWarc0_18.warc");
		if (!testWarcFile0_18.exists()) {
			System.out.println("File " + testWarcFile0_18 + " does not exist.");
		} else {
			//System.out.println("File " + testWarcFile0_18 + " exists.");
			//System.out.println("File length is " + testWarcFile0_18.length());
		}
		
		warcReader0_18 = new WarcRecordReader(testWarcFile0_18);
		
		//---------- WARC/1.0 -------------

		testWarcFile1_0 = new File("src/test/resources/tinyWarc1_0.warc");
		if (!testWarcFile1_0.exists()) {
			System.out.println("File " + testWarcFile1_0 + " does not exist.");
		} else {
			//System.out.println("File " + testWarcFile1_0 + " exists.");
			//System.out.println("File length is " + testWarcFile1_0.length());
		}
		warcReader1_0 = new WarcRecordReader(testWarcFile1_0);
		
		//---------- WARC/0.18GZipped -------------		
		testWarcFile0_18GZipped = new File("src/test/resources/tinyWarc0_18.warc.gz");
		if (!testWarcFile0_18GZipped.exists()) {
			System.out.println("File " + testWarcFile0_18GZipped + " does not exist.");
		} else {
			//System.out.println("File " + testWarcFile0_18GZipped + " exists.");
			//System.out.println("File length is " + testWarcFile0_18GZipped.length());
		}
		warcReader0_18GZipped = new WarcRecordReader(testWarcFile0_18GZipped);

		//---------- Directory of WARC files mixed gzipped and clear -------------

		testWarcDir = new File("src/test/resources/warcdir");
		if (!testWarcDir.exists()) {
			System.out.println("Directory " + testWarcDir + " does not exist.");
		}
		warcReaderDir = new WarcRecordReader(testWarcDir);
		
		//---------- Callacks after each file -------------
		fileArray = new ArrayList<String>();
	}

	@Test
	public void testWarc0_18() throws IOException {
		assertTrue(warcReader0_18.nextKeyValue());
		long key = warcReader0_18.getCurrentKey();
		WarcRecord   record = warcReader0_18.getCurrentValue();
		assertEquals(0, key);
		assertEquals("warcinfo", record.get(WarcRecord.WARC_TYPE));
		//assertEquals("WARC/0.18", record.get(WarcRecord.WARC_TYPE));
		
		assertTrue(warcReader0_18.nextKeyValue());
		record = warcReader0_18.getCurrentValue();
		assertEquals("response", record.get(WarcRecord.WARC_TYPE));
		int lenFirstContentRecord = 21064; // Includes HTTP header
		assertEquals(lenFirstContentRecord, Integer.parseInt(record.get(WarcRecord.CONTENT_LENGTH)));
		String content = record.get(WarcRecord.CONTENT);
		assertEquals(lenFirstContentRecord, content.length());
		
		assertTrue(warcReader0_18.nextKeyValue());
		record = warcReader0_18.getCurrentValue();
		assertEquals("response", record.get(WarcRecord.WARC_TYPE));
		int len2ndContentRecord = 21032; // Includes HTTP header
		assertEquals(len2ndContentRecord, Integer.parseInt(record.get(WarcRecord.CONTENT_LENGTH)));
	}

	@Test
	public void testWarc0_18GZipped() throws IOException {
		assertTrue(warcReader0_18GZipped.nextKeyValue());
		long key = warcReader0_18GZipped.getCurrentKey();
		WarcRecord   record = warcReader0_18GZipped.getCurrentValue();
		assertEquals(0, key);
		assertEquals("warcinfo", record.get(WarcRecord.WARC_TYPE));
		//assertEquals("WARC/0.18", record.get(WarcRecord.WARC_TYPE));
		
		assertTrue(warcReader0_18GZipped.nextKeyValue());
		record = warcReader0_18GZipped.getCurrentValue();
		assertEquals("response", record.get(WarcRecord.WARC_TYPE));
		int lenFirstContentRecord = 21064; // Includes HTTP header
		assertEquals(lenFirstContentRecord, Integer.parseInt(record.get(WarcRecord.CONTENT_LENGTH)));
		String content = record.get(WarcRecord.CONTENT);
		assertEquals(lenFirstContentRecord, content.length());
		
		assertTrue(warcReader0_18GZipped.nextKeyValue());
		record = warcReader0_18GZipped.getCurrentValue();
		assertEquals("response", record.get(WarcRecord.WARC_TYPE));
		int len2ndContentRecord = 21032; // Includes HTTP header
		assertEquals(len2ndContentRecord, Integer.parseInt(record.get(WarcRecord.CONTENT_LENGTH)));
	}
	
	@Test
	public void testWarc1_0() throws IOException {
		assertTrue(warcReader1_0.nextKeyValue());
		long key = warcReader1_0.getCurrentKey();
		WarcRecord   record = warcReader1_0.getCurrentValue();
		assertEquals(0, key);
		assertEquals("warcinfo", record.get(WarcRecord.WARC_TYPE));

		assertTrue(warcReader1_0.nextKeyValue());
		record = warcReader1_0.getCurrentValue();
		int lenFirstContentRecord = 62; // Includes HTTP header
		assertEquals(lenFirstContentRecord, Integer.parseInt(record.get(WarcRecord.CONTENT_LENGTH)));
		String content = record.get(WarcRecord.CONTENT);
		assertEquals(lenFirstContentRecord, content.length());
	}

	@Test
	public void testWarcDir() throws IOException {
		// Test whether all 51 entries are present (45 in the clear file, 6 in the .gz file, and 0 in the empty file:
		for (int i=0; i<51; i++) {
			assertTrue(warcReaderDir.nextKeyValue());
		}
		assertFalse(warcReaderDir.nextKeyValue());
	}
	
	public void callbackMethod(String fileNameDone, String fileNameNext) {
		fileArray.add(fileNameDone);
	}
	
	@Test
	public void testCallback() throws IOException {
		assertTrue(fileArray.size() == 0);
		warcReaderDir.setCallback(this, "callbackMethod");
		// Run through all 50 entries. When i=51, no record
		// will be found:
		for (int i=0; i<51; i++) {
			assertTrue(warcReaderDir.nextKeyValue());
		}
		assertFalse(warcReaderDir.nextKeyValue());
		
		assertEquals(fileArray.size(), 3);
		assertTrue(fileArray.contains(new File(testWarcDir, "tinyWarc0_18.warc.gz").getAbsolutePath())); 
		assertTrue(fileArray.contains(new File(testWarcDir, "tinyWarc1_0.warc").getAbsolutePath())); 
		assertTrue(fileArray.contains(new File(testWarcDir, "tinyWarc1_1.warc").getAbsolutePath())); 
	}
}
