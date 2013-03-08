/**
 * 
 */
package edu.stanford.warcutils.warcfilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import edu.stanford.warcutils.warcreader.WarcRecord;
import edu.stanford.warcutils.warcreader.WarcRecordReader;

/**
 * @author paepcke
 *
 */
public class WarcFileFilter {
	
	enum FilterSense {
		DISCARD_IF_MATCHES,      // for regex type filters
		DISCARD_IF_NOT_MATCHES,  // for regex type filters
		STRIP_HTML               // HTML detag filter
	}
	
	enum WarcHeaderRetention {
		RETAIN_WARC_HEADERS,
		DISCARD_WARC_HEADERS
	}
	
	WarcRecordReader recReader = null;
	WarcFilter filter = null;
	String outPrefix = null;
	String currInFileName = null;
	String currOutFileName = null;
	File outFile = null;
	File outDir  = null;
	BufferedWriter gzipWriter = null;
	WarcHeaderRetention keepWarcHeaders = null;
	FilterSense filterSense = null;
	
	boolean DO_APPEND = true;
	
	static HelpFormatter helpFormatter;
	
	/**
	 * Create file filter where output paths go to
	 * same place as input files, the file names are
	 * prefixed with "filtered_", and the WARC headers
	 * are retained.
	 * placed, and any prefix that is prepended to the output file names.
	 * @param pathFiles
	 * @param warcKey
	 * @param regexPattern
	 * @throws IOException
	 */
	public WarcFileFilter(Collection<File> pathFiles, 
						   String warcKey, 
						   String regexPattern
						   ) throws IOException {
		// Use defaults; the null is for the output directory.
		// This value will cause outputs to go to same directory
		// as inputs:
		this(pathFiles, warcKey, regexPattern, FilterSense.DISCARD_IF_NOT_MATCHES, null, "filtered_", WarcHeaderRetention.RETAIN_WARC_HEADERS);
	}

	/**
	 * Create file filter with choice of where output files are
	 * placed, and any prefix that is prepended to the output file names.
	 * Source of WARC files may be a directory or an individual WARC file.
	 * @param warcPath
	 * @param warcKey
	 * @param regexPattern
	 * @param theFilterSense
	 * @param theOutDirPath
	 * @param theOutPrefix
	 * @param headerRetention
	 * @throws IOException
	 */
	public WarcFileFilter(File warcPath,
						   String warcKey, 
						   String regexPattern,
						   FilterSense theFilterSense,
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention) throws IOException {
		LinkedList<File> allFiles = new LinkedList<File>();
		if (warcPath.isDirectory()) {
			File[] dirFilePaths = warcPath.listFiles();
			for (File filePath : dirFilePaths) {
				allFiles.add(filePath);
			}
		} else
			allFiles.add(warcPath);

		initAll(allFiles, warcKey, regexPattern, theFilterSense,
				theOutDirPath, theOutPrefix, headerRetention);
		processFiles();
	}
	
	
	/**
	 * Create file filter with choice of where output files are
	 * placed, and any prefix that is prepended to the output file names.
	 * @param pathNames
	 * @param warcKey
	 * @param regexPattern
	 * @param filterSense 
	 * @param theOutDirPath
	 * @param theOutPrefix
	 * @param doRetainWarcHeaders
	 * @throws IOException
	 */
	public WarcFileFilter(Collection<File> pathFiles, 
						   String warcKey, 
						   String regexPattern,
						   FilterSense theFilterSense,
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention) throws IOException {
		initAll(pathFiles, warcKey, regexPattern, theFilterSense,
				theOutDirPath, theOutPrefix, headerRetention);
		processFiles();
	}

	
	public WarcFileFilter(File warcPath,
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention) throws IOException {
		LinkedList<File> allFiles = new LinkedList<File>();
		if (warcPath.isDirectory()) {
			File[] dirFilePaths = warcPath.listFiles();
			for (File filePath : dirFilePaths) {
				allFiles.add(filePath);
			}
		} else
			allFiles.add(warcPath);

		initAll(allFiles, "", "", FilterSense.STRIP_HTML,
				theOutDirPath, theOutPrefix, headerRetention);
		processFiles();
	}
	
	
	/**
	 * Stripping HTML from all WARC contents, which is also a kind of filter.
	 * @param pathFiles
	 * @param theOutDirPath
	 * @param theOutPrefix
	 * @param headerRetention
	 * @throws IOException
	 */
	public WarcFileFilter(Collection<File> pathFiles, 
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention) throws IOException {
		initAll(pathFiles, "", "", FilterSense.STRIP_HTML,
				theOutDirPath, theOutPrefix, headerRetention);
		processFiles();
	}
	
	private void initAll(Collection<File> pathFiles, 
					     String warcKey,
					     String regexPattern, 
					     FilterSense theFilterSense,
					     String theOutDirPath, 
					     String theOutPrefix,
					     WarcHeaderRetention headerRetention) {
		recReader = new WarcRecordReader(pathFiles);
		// We need to close our output files, and start
		// a new output files whenever the reader is done
		// with one input file, and is moving on to the
		// next. So install a callback:
		recReader.setCallback(this, "oneInFileProcessed");
		currInFileName = recReader.getCurrentFilePath();
		filter = new WarcFilter(regexPattern, warcKey);
		outPrefix = theOutPrefix;
		keepWarcHeaders = headerRetention;
		filterSense = theFilterSense;
		if (theOutDirPath != null)
			outDir = new File(theOutDirPath);
	}
	
	/**
	 * This constructor ONLY FOR UNIT TESTING !!!
	 * @param theOutDirPath
	 * @param theOutPrefix
	 */
	public WarcFileFilter(String theOutDirPath, String theOutPrefix) {
		outPrefix = theOutPrefix;
		if (theOutDirPath != null)
				outDir = new File(theOutDirPath);
	}
	
	private void processFiles() throws IOException {
		boolean filesLeft = true;
		String content = null;
		WarcRecord rec = null;
		prepareOutputFile();
		
		while (filesLeft) {
			if (! recReader.nextKeyValue()) {
				filesLeft = false;
				continue;
			}
			rec = recReader.getCurrentValue();
			if (filterSense == FilterSense.STRIP_HTML)
				content = detagFilter(rec);
			else
				content = regexFilter(rec);
			if (content == null)
				// Record not wanted: matches the filter regexp:
				continue;
			if (gzipWriter != null)
				gzipWriter.write(content);
			else
				FileUtils.write(outFile, content, DO_APPEND); 
		}
	}

	private String regexFilter(WarcRecord rec) {
		String content = null;
		// Grab record with or without the WARC header, depending
		// on the regex match:
		if (keepWarcHeaders == WarcHeaderRetention.RETAIN_WARC_HEADERS)
			if (filterSense == FilterSense.DISCARD_IF_MATCHES)
				content = filter.allIfNot(recReader.getCurrentValue());
			else
				content = filter.allIf(recReader.getCurrentValue());
		else
			if (filterSense == FilterSense.DISCARD_IF_MATCHES)
				content = filter.contentsIfNot(recReader.getCurrentValue());
			else
				content = filter.contentsIf(recReader.getCurrentValue());
		return content;
	}
	
	private String detagFilter(WarcRecord rec) {
		WarcRecord strippedRec = HTMLStripper.extractText(rec);
		// Turn to string:
		if (keepWarcHeaders == WarcHeaderRetention.RETAIN_WARC_HEADERS)
			return strippedRec.toString(true) + "\n\n";
		else
			return strippedRec.get("content") + "\n\n";
	}

	/**
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void prepareOutputFile() throws FileNotFoundException, IOException {
		currInFileName = recReader.getCurrentFilePath();
		currOutFileName = constructOutFileName(currInFileName);
		outFile = new File(currOutFileName);
		if (isGzipped(currInFileName))
			prepareGZipOutWriter(currOutFileName);
		else
			// Ensure that processFiles knows we are not
			// dealing with gzipped files any more:
			gzipWriter = null;
	}
	
	/**
	 * Callback method: One input file was processed by the
	 * WarcRecordReader. Close the previous out file, if necessary,
	 * and open the next output file. 
	 * @param finishedFilePath
	 * @param nextFilePath input file next to be processed, or empty string, if all inputs have been processed.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void oneInFileProcessed(String finishedFilePath, String nextFilePath) throws FileNotFoundException, IOException {
		if (isGzipped(currOutFileName))
			try {
				gzipWriter.close();
			} catch (IOException e) {
				// best effort
			}
		// Get a new output file to hold the new input file info:
		if (!nextFilePath.isEmpty())
			prepareOutputFile();
	}
	
	public String constructOutFileName(String fullFilePath) {
		String path     = FilenameUtils.getPath(fullFilePath);
		String fileName = FilenameUtils.getName(fullFilePath);
		String res = "";
		// If no output dir was specified, out files go 
		// to directory where infiles are:
		if (outDir == null) {
			// Root spec is for Windows: C:, etc:
			String rootSpec = FilenameUtils.getPrefix(fullFilePath);
			File newFileName = FileUtils.getFile(rootSpec,path, outPrefix + fileName);
			res = newFileName.getAbsolutePath();
			return res;
		}
		File newFileName = FileUtils.getFile(outDir, outPrefix + fileName);
		return newFileName.getAbsolutePath();
	}
	
	public boolean isGzipped(String fileName) {
		return (FilenameUtils.getExtension(fileName).equals("gz")); 
	}
	
	public void oneFileProcessed(String absFilePath) {
		if (isGzipped(currInFileName)) {
            if (gzipWriter != null) {
                try {
                    gzipWriter.close();
                } catch (IOException e) {
                    System.err.println("Could not close gzip output for " + currOutFileName);
                }			
            }
		}
	}

	private void prepareGZipOutWriter(String fileName) throws FileNotFoundException, IOException {
		//Construct the BufferedWriter object
		gzipWriter = new BufferedWriter(
				new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(fileName))));
	}
	
	
	private static void printHelp(Options theOptionsObj, HelpFormatter formatter) {
	    formatter.printHelp( "WarcFileFilter [options] warcRecordFldName regexPattern files", theOptionsObj );		
	}
	
	/**
	 * @param args
						   String warcKey, 
						   String regexPattern,
						   FilterSense theFilterSense,
						   String theOutDirPath,
						   String theOutPrefix,
						   WarcHeaderRetention headerRetention
						   String file
						   
requiredArgs, 
	    				   warcKeyArg, 
	    				   patternArg, 
	    				   filterSenseArg, 
	    				   outDirArg, 
	    				   outPrefixArg, 
	    				   dropWarcHeadersArg);						   
	 */
	/**
	 * @param args: options,warcKey,regexPattern,file1,file2,...
	 * @throws IOException 
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {
		
		HelpFormatter formatter = new HelpFormatter();
		
		Option help 			= new Option( "help", "Print help message" );
		Option dropWarcHeaders  = new Option( "dropWarcHeaders", "Remove WARC headers from qualifying records" );
		Option rejectMatches    = new Option( "rejectMatches", "Keep matching records out of target copy. Exclusive with includeMatches" );
		Option includeMatches   = new Option( "includeMatches", "Only include matching records in target copy. Exclusive with rejectMatches" );
		Option outPrefix		= OptionBuilder.withArgName( "outPrefix" )
											   .hasArg()
											   .withDescription("Prefix to use for target file names" )
											   .create( "outPrefix" );
		Option outDir			= OptionBuilder.withArgName( "outDir" )
											   .hasArg()
											   .withDescription("Destination directory for filtered WARC files" )
											   .create( "outDir" );

		Options options = new Options();
		options.addOption( help );
		options.addOption( dropWarcHeaders );
		options.addOption( rejectMatches );
		options.addOption( includeMatches );
		options.addOption( outPrefix );
		options.addOption( outDir );

		helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine cmdLine = null;
	    try {
	        // Parse the command line arguments
	        cmdLine = parser.parse( options, args );
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	    }
	    
	    // Ensure only either rejectMatches or includeMatches is specified:
	    if (cmdLine.hasOption("rejectMatches") && cmdLine.hasOption("includeMatches")) {
	    	System.err.println("Can only have either rejectMatches, or includeMatches, but not both.");
	    	System.exit(-1);
	    }
	    if (cmdLine.hasOption("help")) {
			formatter.printHelp( "WarcFileFilter", options );		
			System.exit(0);
	    }
	    	
	    String outPrefixArg  = cmdLine.getOptionValue("outPrefix", "filtered_");
	    String outDirArg	 = cmdLine.getOptionValue("outDir", null);
	    WarcHeaderRetention headerRetentionArg = WarcHeaderRetention.RETAIN_WARC_HEADERS;
	    if (cmdLine.hasOption("dropWarcHeaders"))
	    	headerRetentionArg = WarcHeaderRetention.DISCARD_WARC_HEADERS;

	    FilterSense filterSenseArg = null;
	    if (cmdLine.hasOption("rejectMatches"))
	    	filterSenseArg = FilterSense.DISCARD_IF_MATCHES;
	    else
	    	filterSenseArg = FilterSense.DISCARD_IF_NOT_MATCHES;
	    
	    @SuppressWarnings("unchecked")
		List<String> requiredArgs = cmdLine.getArgList();
	    if (requiredArgs.size()< 3) {
	    	printHelp(options, helpFormatter);
	    	System.exit(-1);
	    }
	    String warcKeyArg    = requiredArgs.get(0);
	    String patternArg    = requiredArgs.get(1);
	    requiredArgs.remove(0);
	    requiredArgs.remove(0);
	    LinkedList<File> fileList = new LinkedList<File>();
	    for (String filePath : requiredArgs)
	    	fileList.add(new File(filePath));
	    
	    //*******************
/*	    System.out.println(warcKeyArg +
	    				   ", " + patternArg +
	    				   ", " + filterSenseArg +
	    				   ", " + outDirArg  +
	    				   ", " + outPrefixArg +
	    				   ", " + headerRetentionArg);
	    for (File fileObj : fileList)
	    	System.out.println(fileObj.getAbsolutePath());
	    System.exit(0);
*/	    //*******************	    
	    
	    new WarcFileFilter(fileList, 
	    				   warcKeyArg, 
	    				   patternArg, 
	    				   filterSenseArg, 
	    				   outDirArg, 
	    				   outPrefixArg, 
	    				   headerRetentionArg);
	}
}
