package edu.stanford.warcutils.warcreader;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class WarcRecordTest {

	WarcRecord record = null;
	String testRecord = 
			"WARC/1.0\n" +
			"WARC-Type: warcinfo\n" +
			"WARC-Date: 2012-12-07T18:54:56Z\n" +
			"WARC-Filename: WEB-20121207185456922-00000-23301~mono.stanford.edu~8443.warc.gz\n" +
			"WARC-Record-ID: <urn:uuid:155c7215-756f-4609-b10d-104b7ce86a69>\n" +
			"Content-Type: application/warc-fields\n" +
			"Content-Length: 394\n" +
			"\n" +
			"software: Heritrix/3.1.0 http://crawler.archive.org\n" +
			"ip: 127.0.1.1\n" +
			"hostname: mono.stanford.edu\n" +
			"format: WARC File Format 1.0\n" +
			"conformsTo: http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf\n" +
			"isPartOf: small_crawls\n" +
			"description: Basic crawl starting with useful defaults\n" +
			"robots: obey\n" +
			"http-header-user-agent: Mozilla/5.0 (compatible; heritrix/3.1.0 +http://infolab.stanford.edu/~paepcke)\n"; 

	@Before
	public void setUp() throws Exception {
		// System.out.println(testRecord);
    	// Convert String record into InputStream
		InputStream recordStream = new ByteArrayInputStream(testRecord.getBytes());
		LineAndChunkReader lineReader = new LineAndChunkReader(recordStream);
		record = WarcRecord.readNextWarcRecord(lineReader, true);
	}

	@Test
	public void testToString() {
		String str = record.toString();
		//System.out.println(str);
		assertTrue(str.startsWith("WARC/1.0"));
		assertTrue(str.endsWith("[Record content suppressed. Use toString(INCLUDE_CONTENT) to see the content string.]\n"));
	}

}
