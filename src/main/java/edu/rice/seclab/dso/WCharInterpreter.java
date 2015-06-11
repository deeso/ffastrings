package edu.rice.seclab.dso;

import com.google.common.base.CharMatcher;

public class WCharInterpreter implements IStringInterpreter {

	static public final String ENCODING = "wchar";
	private static final Integer MAX_CHAR_SIZE = 1;
	private static final Integer MIN_CHAR_SIZE = 1;
	static int CHAR_SIZE = 2;
	static long min_len = 4;
	static long max_len = Integer.MAX_VALUE;

	public String getEncoding() {
		return ENCODING;
	}

	public Long getLength(byte[] myBytes) {
		return getLength(myBytes, 0L, (long)myBytes.length);
	}
	
	public Long getLength(byte[] myBytes, Long pos, Long end) {
		long cnt = 0;
		for (; pos < end; pos += CHAR_SIZE) {
			if (isValidChar(myBytes, pos)){
				cnt++;
			} else {
				break;
			}
		}
		return cnt*CHAR_SIZE;
	}

	public Long getLength(byte[] myBytes, Long pos) {
		return getLength(myBytes, pos, (long) myBytes.length);
	}

	public String printableString(byte[] myBytes) {
		return new String(myBytes);
	}

	public String noWhiteSpaceString(byte[] myBytes) {
		return noWhiteSpaceString(myBytes, "_") ;
	}
	

	public String noWhiteSpaceString(byte[] myBytes, String replace) {
		return printableString(myBytes).replaceAll("\\s+",replace) ;
	}

	public String noNewLineString(byte[] myBytes) {
		return noNewLineString(myBytes, "--linebreak--");
	}
	public String noNewLineString(byte[] myBytes, String replace) {
		return printableString(myBytes).replace("\n", replace);
	}

	public byte[] hashAs(byte[] myBytes, String hashAlgo) {
		// TODO Auto-generated method stub
		return null;
	}

	public String defaultHash(byte[] value) {
		// TODO Auto-generated method stub
		return null;
	}

	public Long createHash(byte[] myBytes) {
		// TODO Auto-generated method stub
		return 0L;
	}

	public Integer maxSize() {
		return MIN_CHAR_SIZE;
	}

	public Integer minSize() {
		return MAX_CHAR_SIZE;
	}

	public void setMaxLen(long v) {
		max_len = v;

	}

	public void setMinLen(long v) {
		min_len = v;
	}

	public long getMaxLen() {
		return max_len*CHAR_SIZE;
	}

	public long getMinLen() {
		return min_len*CHAR_SIZE;
	}

	public boolean isValidChar(byte[] myBytes) {
		return isValidChar(myBytes, 0);
	}
	
	public boolean preByteCheck(byte[] myBytes, int pos) {
		return myBytes[(int)pos] == 0 && pos+1 < myBytes.length;
	}
	
	public boolean isValidChar(byte[] myBytes, long pos) {
		
		return  preByteCheck(myBytes, (int) pos) &&
				CharMatcher.ASCII.matches((char) (myBytes[(int) pos+1] & 0xff));
	}

	public boolean isWhiteSpace(byte[] mybytes) {
		return isWhiteSpace(mybytes, 0);
	}

	public boolean isWhiteSpace(byte[] myBytes, long pos) {
		return  preByteCheck(myBytes, (int) pos) &&
				CharMatcher.WHITESPACE.matches((char) (myBytes[(int) pos] & 0xff));
	}

	public Long getStringLength(byte[] myBytes) {
		return getStringLength(myBytes, 0L);
	}

	public Long getStringLength(byte[] myBytes, Long pos) {
		return getStringLength(myBytes, pos, (long) myBytes.length);
	}

	public Long getStringLength(byte[] myBytes, Long pos, Long end) {
		Long cnt = getLength(myBytes, pos, end);
		return cnt/CHAR_SIZE;
	}
}
