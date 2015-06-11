package edu.rice.seclab.dso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;

public class ChunkProcessor extends Thread {
	public static final Long CHUNK_SCAN_SIZE = 4096L * 10;
	Boolean keepRunning = true;
	Boolean stillRunning = false;
	RandomAccessFile fhandle = null;
	HashMap<Long, StringInfo> myKeyInfo = null;
	private Long myChunkSize = -1L;
	private Long myChunkOffset = 0L;
	private int myMaxKeyLength = 0;
	private long myBaseOffset;
	String myFilename;
	Integer matches = 0;
	File myFile;
	private boolean myLiveUpdate = false;
	private byte[] myChunk;
	
	IStringInterpreter myStringMethods;

	public ChunkProcessor(File file, long offset, long chunk_size,
			HashMap<Long, StringInfo> binaryKInfo, IStringInterpreter stringMethods, boolean liveUpdate) throws IOException{
		myChunkSize = chunk_size;
		myFile = file;
		myFilename = myFile.getAbsolutePath();
		myKeyInfo = binaryKInfo;
		myBaseOffset = offset;
		myStringMethods = stringMethods;
		myLiveUpdate = liveUpdate;
		myChunk = performRead();

	}
	
	public ChunkProcessor(File file, long offset, byte [] data,
			HashMap<Long, StringInfo> stringInfo, IStringInterpreter stringMethods, boolean liveUpdate){
		myChunkSize = (long) data.length;
		myFile = file;
		myFilename = myFile.getAbsolutePath();
		myKeyInfo = stringInfo;
		myBaseOffset = offset;
		myStringMethods = stringMethods;
		myLiveUpdate = liveUpdate;
		myChunk = data;

	}

	void foundOne() {
		synchronized (matches) {
			matches += 1;
		}
	}

	private byte[] performRead() throws IOException {
		// 1) update the chunk offset for reading data
		long sz = myChunkSize;
		fhandle.seek(myBaseOffset + myChunkOffset);
		byte[] res = new byte[(int) sz];
		long a_sz = fhandle.read(res);
		if (a_sz != sz) {
			System.err.println(String
					.format("Warning: attempted to read %d bytes but got %d", 
							sz, a_sz));
		}
		return res;
	}
		
	long calculateActualOffset() {
		return myChunkOffset + myBaseOffset;
	}

	long calculateActualOffset(long at) {
		return at + calculateActualOffset();
	}
	
	private int performComparisonsOnChunk() throws Exception {
		Long pos = 0L;
		while (pos  < myChunk.length){
			Long strEndPos = myStringMethods.getLength(myChunk, pos);
			
			if (pos == 0) {
				pos += 1;
			} else if (strEndPos < myStringMethods.getMinLen()) {
				pos += (strEndPos+1);
			} else {
				synchronized (myKeyInfo) {
					byte [] val = new byte[strEndPos.intValue()];
					System.arraycopy(myChunk, pos.intValue(), val, 0, strEndPos.intValue());
					StringInfo si = new StringInfo(myFilename, pos+calculateActualOffset(), val, myStringMethods);
					myKeyInfo.put(pos, si);
					System.out.println(String.format("Found string @ %08x %s sizeof(%d)", pos+strEndPos, si.toString(), strEndPos.intValue()));
				}
				pos += (strEndPos+1);
				matches++;
			}
		}
		return matches;
	}

	@Override
	public void run() {
		try {
			performComparisonsOnChunk();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void shutdown() {
		synchronized (keepRunning) {
			keepRunning = false;
		}
	}

}
