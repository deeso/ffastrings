package edu.rice.seclab.dso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.primitives.UnsignedLong;

@SuppressWarnings("deprecation")
public class Ffaststrings {
	@SuppressWarnings("deprecation")
		ArrayList<File> myTargetFiles = null;
		public static final String KEY_COUNT_OUTPUT = "keyCountOutput";
		public static final String BINARY_FILE = "binaryFile";
		public static final String LIVE_UPDATE = "liveUpdate";
		public static final String GREP_OUTPUT = "grepOutput";
		public static final String BY_FILENAME_OUTPUT = "byFilenameOutput";
		public static final String ENABLE_UNCONSTRAINED_READ = "enableUnconstrainedRead";
		
		private static final String START_OFFSET = "startOffset";

		public static final String NUM_THREADS = "numThreads";

		public static final String BINARY_STRING_FILE = "binaryStrings";
		public static final String HELP_ME = "help";

		
		static Option myHelpOption = new Option(HELP_ME, "print the help message" );
		static Option myLiveUpdateOption = new Option(LIVE_UPDATE, "produce a live update on each hit" );

		
		@SuppressWarnings("static-access")
		static Option myBinaryStringFileOption = OptionBuilder.withArgName( "file" )
	    .hasArg()
	    .withDescription(  "use given file for log" )
	    .create( BINARY_STRING_FILE );
		
		@SuppressWarnings("static-access")
		static Option myNumThreadsOption = OptionBuilder.withArgName( "num_threads" )
			    .hasArg()
			    .withDescription(  "number of threads for scanning (specified in hex), 1 is the default" )
			    .create( NUM_THREADS );
		
		@SuppressWarnings("static-access")
		static Option myMemDumpFileOption = OptionBuilder.withArgName( "file" )
			    .hasArg()
			    .withDescription(  "binary memory dump file" )
			    .create( BINARY_FILE );

		@SuppressWarnings("static-access")
		static Option myDisableUnconstrainedReadOption = OptionBuilder
			    .withDescription(  "memory is plentiful, read all a chunk before scanning it (read chunks as needed)" )
			    .create( ENABLE_UNCONSTRAINED_READ );

		@SuppressWarnings("static-access")
		static Option myKeyCountFileOption = OptionBuilder.withArgName( "file" )
			    .hasArg()
			    .withDescription(  "output key counts file" )
			    .create( KEY_COUNT_OUTPUT );
		
		@SuppressWarnings("static-access")
		static Option myGrepableOutput = OptionBuilder.withArgName( "file" )
			    .hasArg()
			    .withDescription(  "grepable output file" )
			    .create( GREP_OUTPUT );
		
		@SuppressWarnings("static-access")
		static Option myOffsetOption = OptionBuilder.withArgName( "offset" )
			    .hasArg()
			    .withDescription(  "start at given offset (specified in hex), 0 is Default" )
			    .create( START_OFFSET );
		
		@SuppressWarnings("static-access")
		static Option myByFilenameOption = OptionBuilder.withArgName( "file" )
			    .hasArg()
			    .withDescription(  "listing key-filename hits on a single line" )
			    .create( BY_FILENAME_OUTPUT);
		
		public static Options myOptions = new Options().addOption(myBinaryStringFileOption)
				.addOption(myNumThreadsOption).addOption(myOffsetOption).addOption(myMemDumpFileOption)
				.addOption(myHelpOption).addOption(myLiveUpdateOption).addOption(myGrepableOutput)
				.addOption(myByFilenameOption).addOption(myKeyCountFileOption).addOption(myDisableUnconstrainedReadOption);
		
		// Hash Table mapping hash values to key bytes
		HashMap<String, HashMap<Long, StringInfo>> myStringInfoMap = new HashMap<String, HashMap<Long, StringInfo>>();
		HashMap<String, ArrayList<StringInfo>> msToKeys = new HashMap<String, ArrayList<StringInfo>>();

		HashMap<String, ArrayList<String>> keysToFileOffset = new HashMap<String, ArrayList<String>>();
		
		Integer myNumThreads = 1;
		Long myStartOffset;
		private String myMemDump;

		
		ExecutorService myExecutor = null;
		
		ArrayList<Future<?>> myThreadFutures = new ArrayList<Future<?>>();
		private boolean myLiveUpdate = false;
		
		
		public Ffaststrings(String memory_dump_file, Long offset, Integer numThreads, boolean liveUpdate) {
			
			myMemDump = memory_dump_file;
			myNumThreads = numThreads;
			myStartOffset = offset;
			
			myExecutor = Executors.newFixedThreadPool(numThreads);
			myTargetFiles = Utils.readDirectoryFilenames(myMemDump);
			myLiveUpdate = liveUpdate;
			for (File file : myTargetFiles)
				myStringInfoMap.put(file.getAbsolutePath(), new HashMap<Long, StringInfo>());
		}
				
		static void liveUpdate(String filename, long offset, String key) {
			String offset_str = UnsignedLong.valueOf(offset).toString(16);
			System.out.println(String.format("%s %s %s", key, offset_str, filename));
		}
		
		void addStringInfo(String filename, Long offset, byte [] value, IStringInterpreter stringMethods) {
			long checkValue = offset + value.length;
			
			synchronized (myStringInfoMap) {
				HashMap<Long, StringInfo> si_hm = myStringInfoMap.containsKey(filename) ? myStringInfoMap.get(filename) : null;
				if (si_hm == null) {
					si_hm = new HashMap<Long, StringInfo>();
					myStringInfoMap.put(filename, si_hm);
				}
				synchronized (si_hm) {
					StringInfo sr;
					try {
						sr = new StringInfo(filename, offset, value, stringMethods);
						si_hm.put(offset, sr);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}		
		}
		
		
		void executeFileScans() {
			//System.out.println("Starting the binary string search");
			for (File file : myTargetFiles) {
				try {
					performFileScan(file);
				} catch (FileNotFoundException e) {
					// this should not happen but if it does, oh well.
					e.printStackTrace();
				}
			}
			
			while(!myThreadFutures.isEmpty()) {
				Future<?> p = myThreadFutures.get(0);
				if (p.isDone()) {
					myThreadFutures.remove(0);
				} else {
					//System.out.println(String.format("Waiting on %d threads to complete.", myThreadFutures.size()));
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// Dont know if i care about this
						e.printStackTrace();
					}
				}
			}  
			
			// perform string merge
			for (HashMap<Long, StringInfo> si_hm : myStringInfoMap.values()) {
				ArrayList<Long> locations = new ArrayList<Long>();
				locations.addAll(si_hm.keySet());
				Collections.sort(locations);
				for (int i = 0; i < locations.size(); i++) {
					Long offset = locations.get(i);
					StringInfo si = si_hm.containsKey(offset) ? si_hm.get(offset) : null;
					
					if (si == null) continue; // chances are it was removed and merged
					while (si_hm.containsKey(offset+si.length())) {
						StringInfo next_si = si_hm.get(offset+si.length());
						byte [] newValue = new byte[(int)si.length() + (int)next_si.length()];
						System.arraycopy(si.getBytes(), 0, newValue, 0, (int)si.length());
						System.arraycopy(next_si.getBytes(), 0, newValue, (int) si.length(), (int)next_si.length());
						try {
							si = new StringInfo(si.getFilename(), offset, newValue, si.getStringType());
							si_hm.put(offset, si);
							si_hm.remove(next_si.getLocation());
						} catch (Exception e) { 
							e.printStackTrace();
						}
					}
				}
				
			}
			
			
			
			int totalHits = 0, uniqueKeys = 0;
			HashSet<String> containedFiles = new HashSet<String>();
			
			for (HashMap<Long, StringInfo> si_hm : myStringInfoMap.values()) {
				for (StringInfo bsi : si_hm.values()) {
					HashSet<String> fileHits = bsi.getFileHits();
					containedFiles.addAll(fileHits);
					int t = fileHits.size();
					if (t > 0) uniqueKeys++;
					totalHits += bsi.numLocationsHits();
					
				}				
			}
			long totalFileHits = containedFiles.size();
			System.out.println(String.format("File contains %d hits in %d files and %d unique keys", totalHits, totalFileHits, uniqueKeys));
			myExecutor.shutdown();
			while (!myExecutor.isTerminated()) {}
			
		}
		
		public String getGreppableOutput() {
			ArrayList<String> output = new ArrayList<String>();
			for (HashMap<Long, StringInfo> si_hm : myStringInfoMap.values()) {
				for (StringInfo bsi : si_hm.values()) {

				String s = bsi.toGreppableString();
				if (s.length() > 0) output.add(s);
				}
			}
			return StringUtils.join(output, "\n")+"\n";
		}
		
		public String getByFilenameOutput() {
			ArrayList<String> output = new ArrayList<String>();
			for (HashMap<Long, StringInfo> si_hm : myStringInfoMap.values()) {
				for (StringInfo bsi : si_hm.values()) {

				String s = bsi.toByFilenameString();
				if (s.length() > 0) output.add(s);
				}
				}

			return StringUtils.join(output, "\n")+"\n";
		}
		
		public String getByCountsOutput() {
			ArrayList<String> output = new ArrayList<String>();
			for (HashMap<Long, StringInfo> si_hm : myStringInfoMap.values()) {
				for (StringInfo bsi : si_hm.values()) {

				int numHits = bsi.numLocationsHits();
				if (numHits == 0) continue;
				String s = String.format("%s %x", bsi.getHash(), numHits);
				if (s.length() > 0) output.add(s);
				}
			}
			return StringUtils.join(output, "\n")+"\n";
		}
		
		void performFileScan(File file) throws FileNotFoundException {
			
			if (myStringInfoMap.isEmpty() || 
				!file.exists() || 
				!file.isFile()){
				// nothing to do there there are no patterns
				// or this is not a valid file
				return;
			}
			
			ArrayList<IStringInterpreter> stringTypes = Utils.allStringTypes();
			long fileSize = file.length();
			long chunkSz = fileSize/myNumThreads;
			RandomAccessFile fhandle = new RandomAccessFile(file, "r");
			
			for (long offset = myStartOffset; offset < fileSize; offset += chunkSz) {
				chunkSz = offset + chunkSz > fileSize ? fileSize - offset : chunkSz;
				byte[] res = null;
				try {
					fhandle.seek(offset);
					res = new byte[(int) chunkSz];
					long a_sz = fhandle.read(res);
					if (a_sz != chunkSz) {
						System.err.println(String
								.format("Warning: attempted to read %d bytes but got %d", 
										chunkSz, a_sz));
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
				for (IStringInterpreter stype : stringTypes) {
					Runnable cp = null;
					cp = new ChunkProcessor(file,offset,res,myStringInfoMap.get(file.getAbsolutePath()),stype, myLiveUpdate);
					Future<?> p = myExecutor.submit(cp);
					myThreadFutures.add(p);
				}
			}
		}
		
		public static Options getOptions() {
			return myOptions;
		}
		
		
		public static void main(String[] args) throws FileNotFoundException {
			Ffaststrings fbs = null;		
			CommandLineParser parser = new DefaultParser();
			CommandLine cli;
			String binary_strings_file = null,
				   memory_dump_file = null, 
				   num_scanning_threads = "1",
				   offset_start = "0";
			boolean liveUpdate = false;
			try {
				cli = parser.parse(Ffaststrings.getOptions(), args);
				memory_dump_file = cli.hasOption(BINARY_FILE) ? cli.getOptionValue(BINARY_FILE) : null;
				if (cli.hasOption(NUM_THREADS)) num_scanning_threads =  cli.getOptionValue(NUM_THREADS);
				if (cli.hasOption(START_OFFSET)) offset_start = cli.getOptionValue(START_OFFSET);
				if (cli.hasOption(LIVE_UPDATE)) liveUpdate = true;

			} catch (ParseException e) {
				
				e.printStackTrace();
				return;
			}
			
			if (cli.hasOption(HELP_ME)) {
				HelpFormatter hf = new HelpFormatter();
				hf.printHelp("ffastrings", Ffaststrings.getOptions());
				return;
			} else if (!cli.hasOption(BINARY_FILE)) {
				System.err.println(String.format("ERROR: %s parameter is required to run jbgrep.", BINARY_FILE));
				return;
			}
			
			Integer numThreads = Utils.tryParseHexNumber(num_scanning_threads);
			Long offset = Utils.tryParseHexLongNumber(offset_start);

			fbs = new Ffaststrings(memory_dump_file, offset, numThreads, liveUpdate);			
			
			
			if (fbs != null) {
				fbs.executeFileScans();
				if (cli.hasOption(GREP_OUTPUT)) {
					File f = new File (cli.getOptionValue(GREP_OUTPUT));
					Utils.writeOutputFile(f, fbs.getGreppableOutput());
				}
				if (cli.hasOption(BY_FILENAME_OUTPUT)) {
					File f = new File (cli.getOptionValue(BY_FILENAME_OUTPUT));
					Utils.writeOutputFile(f, fbs.getByFilenameOutput());
				}
				if (cli.hasOption(KEY_COUNT_OUTPUT)) {
					File f = new File (cli.getOptionValue(KEY_COUNT_OUTPUT));
					Utils.writeOutputFile(f, fbs.getByCountsOutput());
				}
			}
		}

		

	}