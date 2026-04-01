package sandbox.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadCountInputStream extends FilterInputStream {
	private long count=0L;
	public ReadCountInputStream(InputStream in) {
		super(in);
	}
	/** number of bytes read so far */
	public long getCount() {
		return this.count;
		}
	@Override
	public int read() throws IOException {
		int c = super.read();
		if(c!=-1) count++;
		return c;
		}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int n= super.read(b, off, len);
		if(n!=-1) {
			count+=n;
			}
		return n;
		}
	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b,0,b.length);
		}

}
