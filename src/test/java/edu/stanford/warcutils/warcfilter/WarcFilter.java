/**
 * 
 */
package edu.stanford.warcutils.warcfilter;

import java.util.regex.Pattern;

import edu.stanford.warcutils.warcreader.WarcRecord;

/**
 * @author paepcke
 *
 * Instances of WarcFilter can test whether a given WarcRecord
 * instance contains a given key, and the corresponding value
 * satisfies a given regular expression. The class is intended
 * to be instantiated once, and the resulting instance used 
 * many times as a stream of WARC records is examined.
 * 
 * WARC record keys are:
 *     warc-type
 *     warc-record-id
 *     warc-date
 *     content-length
 *     content-type
 *     warc-concurrent-To
 *     warc-block-digest
 *     warc-payload-digest
 *     warc-ip-address
 *     warc-refers-to
 *     warc-target-uri
 *     warc-truncated
 *     warc-warcinfo-id
 *     warc-filename
 *     warc-profile
 *     warc-identified-payload-type
 *     warc-segment-origin-id
 *     warc-segment-number
 *     warc-segment-total-length
 * 
 * Only content-length, warc-date, and warc-type are mandatory.
 */
public class WarcFilter {
	
	private Pattern regexPattern = null;
	private String warcFieldKey = null;

	public WarcFilter(Pattern warcKeyRegexPattern, String warcRecKey) {
		regexPattern = warcKeyRegexPattern;
		warcFieldKey = warcRecKey;
	}
	
	public boolean matches(WarcRecord warcRec) {
		String val = warcRec.get(warcFieldKey);
		return val == null;
	}
	
	public String contentsIf(WarcRecord warcRec) {
		String val = warcRec.get(warcFieldKey);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
