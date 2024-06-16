package sandbox.io;

import java.io.IOException;
import java.io.Writer;

public class PrefixSuffixWriter extends Writer {
	private String prefix;
	private final Writer delegate;
	private boolean at_start=false;
	public PrefixSuffixWriter(final String prefix,Writer delegate,final String suffix) {
		this.prefix = prefix;
		this.delegate = delegate;
		}
	public PrefixSuffixWriter(final String prefix,Writer delegate) {
		this(prefix,delegate,"");
		}
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for(int i=0;i< len;i++) {
			if(at_start) delegate.append(prefix);
			char c =  cbuf[off+i];
			delegate.append(c);
			at_start=(c=='\n');
			}
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
