package sandbox.io;

import java.io.IOException;
import java.io.Writer;

public class NullWriter extends Writer
	{
	@Override
	public void write(char[] cbuf) throws IOException
		{
		}
	
	@Override
	public void write(int c) throws IOException
		{
		}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException
		{
		
		}
	@Override
	public void close()
		{
		
		}

	@Override
	public void flush()
		{
		}
	}
