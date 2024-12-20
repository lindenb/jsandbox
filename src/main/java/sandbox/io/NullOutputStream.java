package sandbox.io;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream
	{
	private long length=0;
	public NullOutputStream() {
		}
	@Override
	public void write(byte[] b) throws IOException
		{
		this.length+=b.length;
		}
	@Override
	public void write(byte[] b, int off, int len) throws IOException
		{
		this.length+=len;
		}
	@Override
	public void write(int b) throws IOException
		{
		this.length++;
		}
	public long length() {
		return this.length;
	}
	@Override
	public void close()
		{
		}
	}
