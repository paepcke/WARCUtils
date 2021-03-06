package edu.stanford.warcutils.warcfilter;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import edu.stanford.warcutils.warcreader.WarcRecord;

 /**
  *  Remove HTML tags, and JavaScript from a string, or from the content
  *  field of a WarcRecord instance.
  * 
 * @author paepcke
 *
 */
public class HTMLStripper {
	
	public HTMLStripper(String htmlPage) {
		
	}
	
	public static String extractText(String webPage)  {
		String bodyHtml = webPage;
		String cleanText = Jsoup.clean(bodyHtml, Whitelist.none());
		return cleanText;
	}
	
	public static WarcRecord extractText(WarcRecord warcRecord) {
		String content = HTMLStripper.extractText(warcRecord.get("content"));
		warcRecord.put("content", content);
		warcRecord.put("content-length", Integer.toString(content.length()));
		return warcRecord;
	}
}
