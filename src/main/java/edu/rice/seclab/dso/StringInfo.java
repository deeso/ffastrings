package edu.rice.seclab.dso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

public class StringInfo implements Comparable<StringInfo> {
	long myHash;
	byte[] myBytes;
	HashMap<String, HashSet<String>> myLocations = new HashMap<String, HashSet<String>>();
	IStringInterpreter myStringMethods;
	Long myLocation;
	String myFilename;
	static Integer cum = 0;
	
	
	public StringInfo(String filename, Long location, byte [] value, IStringInterpreter stringMethods) throws Exception {
		myBytes = value.clone();
		myStringMethods = stringMethods;
		myHash = stringMethods.createHash(myBytes);
		myLocation = location;
		myFilename = filename;
	}

	public Long getLocation() {
		return myLocation;
	}
	public long length(){
		return myStringMethods.getLength(myBytes);
	}
	
	public String encoding(){
		return myStringMethods.getEncoding();
	}
	
	public boolean addFileOffset(String filename, Long fileoffset) {
		boolean added = false;
		synchronized (myLocations) {
			
			if (!myLocations.containsKey(filename)) {
				myLocations.put(filename, new HashSet<String>());
			}
			String offset = Utils.unsigned_long_xstr(fileoffset.longValue());
			if (!myLocations.get(filename).contains(offset)){
				myLocations.get(filename).add(offset);
				added = true;
			}
							
		}
		return added;
	}
	
	public boolean presentInFile(String filename) {
		return myLocations.containsKey(filename);
	}
	
	public ArrayList<String> grepableFormatList() {
		HashSet<String> _results = new HashSet<String>();
		ArrayList<String> results = new ArrayList<String>();
		synchronized (myLocations) {
			for (String filename : myLocations.keySet()) {
				for (String location : myLocations.get(filename)) {
					_results.add(String.format("%s:%s %s", filename, location, myStringMethods.noNewLineString(myBytes)));
				}
			}
		}
		results.addAll(_results);
		return results;
	}
	
	public ArrayList<String> accumulateFileLocations() {
		HashSet<String> _results = new HashSet<String>();
		//
		synchronized (myLocations) {
			for (String filename : myLocations.keySet()) {
				_results.add(String.format("%s %s: \"%s\"", myStringMethods.noNewLineString(myBytes, "_"), filename, StringUtils.join(myLocations.get(filename), ", ")));
			}
		}
		ArrayList<String> results = new ArrayList<String>();
		results.addAll(_results);
		return results;
	}
	
	public int numFileHits() {
		return myLocations.size();
	}
	
	public String toGreppableString(){
		if (numFileHits() == 0) return new String("");
		return StringUtils.join(grepableFormatList(), "\n");
	}
	
//	public String toByFilenameString(){
//		if (numFileHits() == 0) return new String("");
//		return StringUtils.join(accumulateFileLocations(), "\n");
//	}

	public HashSet<String> getFileHits() {
		HashSet<String> res = new HashSet<String>();
		if (myLocations.isEmpty()) return res;
		for (String filename : myLocations.keySet()) {
			res.add(filename);
		}
		return res;
	}

	public int numLocationsHits() {
		int total = 0;
		for (String filename : myLocations.keySet()) {
			total += numLocationsHits(filename);
		}
		return total;
	}
	public int numLocationsHits(String fileName) {
		if (myLocations.containsKey(fileName)) {
			synchronized (myLocations) {
				return myLocations.get(fileName).size();
			}
		}
		return 0;
	}

	public byte[] getBytes() {
		return myBytes;
	}

	public String getHash() {
		return myStringMethods.defaultHash(myBytes);
	}

	public String getFilename() {
		return myFilename;
	}

	public IStringInterpreter getStringType() {
		return myStringMethods;
	}
	@Override
	public String toString() {
		return myStringMethods.printableString(myBytes);
	}
	
	public String toNoNewLineString() {
		return myStringMethods.noNewLineString(myBytes);
	}
	
	public String toBasicOutput() {
		String offset_str = Utils.unsigned_long_xstr(myLocation);
		//String sz_str = Utils.unsigned_long_xstr(myBytes.length);
		String string = myStringMethods.noNewLineString(myBytes);
		cum += string.length();
		System.out.println(String.format("String length = 0x%08x cum = 0x%08x", string.length(), cum));	
		return String.format("%s: %s ", myFilename, offset_str)+  string;
	}
	
	public String toOutputStringWithLength() {
		String offset_str = Utils.unsigned_long_xstr(myLocation);
		String sz_str = Utils.unsigned_long_xstr(myBytes.length);
		String string = myStringMethods.noNewLineString(myBytes);
		return String.format("%s: %s %s %s", myFilename, offset_str, sz_str, string);
	}
	
	@Override
	public int compareTo(StringInfo o2) {
		if (getLocation() == o2.getLocation()) return 0;
		if (getLocation() < o2.getLocation()) return -1;
		return 1;

	}

	
}
