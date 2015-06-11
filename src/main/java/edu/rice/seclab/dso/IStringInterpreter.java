package edu.rice.seclab.dso;

public interface IStringInterpreter {
	String getEncoding();
	void setMaxLen(long v);
	void setMinLen(long v);
	long getMaxLen();
	long getMinLen();
	boolean isValidChar(byte[] myBytes);
	boolean isValidChar(byte[] myBytes, long pos);
	boolean isWhiteSpace(byte[] mybytes);
	boolean isWhiteSpace(byte[] mybytes, long pos);
	Long createHash(byte[] myBytes);
	String printableString(byte[] myBytes);
	String noWhiteSpaceString(byte[] myBytes);
	String noWhiteSpaceString(byte[] myBytes, String replace);
	String noNewLineString(byte[] myBytes);
	String noNewLineString(byte[] myBytes, String replace);
	byte[] hashAs(byte[] myBytes, String hashAlgo);
	String defaultHash(byte[] value);
	Integer maxSize();
	Integer minSize();
	Long getLength(byte[] myBytes);
	Long getLength(byte[] myChunk, Long pos);
	public Long getLength(byte[] myBytes, Long pos, Long end);

	public Long getStringLength(byte[] myBytes);
	public Long getStringLength(byte[] myBytes, Long pos);
	public Long getStringLength(byte[] myBytes, Long pos, Long end);
	public void setLiveUpdate(boolean x);
	boolean doliveUpdate();
}
