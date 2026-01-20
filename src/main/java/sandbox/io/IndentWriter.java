package sandbox.io;

import java.io.IOException;
import java.io.Writer;

public class IndentWriter extends Writer {
	private String margin;
	private final Writer delegate;
	private boolean at_start=true;
	private int level=0;
	public IndentWriter(Writer delegate,final String margin) {
		this.margin = margin;
		this.delegate = delegate;
		}
	public IndentWriter(Writer delegate) {
		this(delegate,"   ");
		}
	public IndentWriter push() {
		level++;
		return this;
		}
	public IndentWriter pop() {
		level--;
		if(level<0)throw new IllegalArgumentException("something is wrong in indetation "+level);
		return this;
		}
	public int getIndentLevel() {
		return level;
		}
	public String getMarginUnit() {
		return this.margin;
		}
	/** get margin for current level */
	public String getMargin() {
		return getMarginforLevel(getIndentLevel());
		}
	/** get margin for current level */
	public String getMarginforLevel(int indentLevel) {
		final StringBuilder sb=new StringBuilder();
		for(int k=0;k< indentLevel;k++) {
			sb.append(getMarginUnit());
			}
		return sb.toString();
		}

	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for(int i=0;i< len;i++) {
			if(at_start) {
				delegate.append(getMargin());
				}
			char c =  cbuf[off+i];
			delegate.append(c);
			at_start=(c=='\n');
			}
		}
	public void print(final char c) {
		print(String.valueOf(c));
		}
	public void print(final String s) {
		try {
			write(s);
			}
		catch(IOException err) {
			throw new RuntimeIOException(err);
			}
		}

	public void println(final String s) {
		print(s);
		println();
		}
	public void println() {
		print("\n");
		}
	
	@Override
	public void flush() throws IOException {
		this.delegate.flush();
		
	}

	@Override
	public void close() throws IOException {
		this.delegate.close();
	}

}
