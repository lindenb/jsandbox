package sandbox;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LogInputStream extends FilterInputStream
	{
	private OutputStream out;
	public LogInputStream(InputStream in,OutputStream out)
		{
		super(in);
		this.out=out;
		}
	@Override
	public int read() throws IOException
		{
		int c=super.read();
		if(c!=-1 && out!=null) out.write(c);
		return c;
		}
	@Override
	public int read(byte[] b) throws IOException
		{
		return read(b,0,b.length);
		}
	@Override
	public int read(byte[] b, int off, int len) throws IOException
		{
		int n= super.read(b, off, len);
		if(this.out!=null && n>0) out.write(b, off, n);
		return n;
		}
	@Override
	public void close() throws IOException
		{
		super.close();
		if(this.out!=null) out.flush();
		}
	}
