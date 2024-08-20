package sandbox.io;

import sandbox.lang.AbstractCharSequence;

public class LineNumber extends AbstractCharSequence {
	private final String source;
	private final int lineNumber;
	private final String line;
	public LineNumber(String source,int lineNumber,final String line) {
		this.source = source;
		this.lineNumber = lineNumber;
		this.line = line;
		}
	public String getSource() {
		return this.source;
		}
	public String getLine() {
		return this.line;
		}
	public int getLineNumber() {
		return this.lineNumber;
		}
	@Override
	public char charAt(int index) {
		return this.getLine().charAt(index);
		}
	@Override
	public int length() {
		return this.getLine().length();
		}
	
	@Override
	public int hashCode()
		{
		return this.line.hashCode();
		}

	public String getLocation() {
		return "["+getSource()+":"+getLineNumber()+"]";
		}
	public String getLabel() {
		return getLocation()+getLine();
		}
	
	@Override
	public String toString() {
		return this.line;
		}
	}
