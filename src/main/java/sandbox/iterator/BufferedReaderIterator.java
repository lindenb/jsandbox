package sandbox.iterator;

import java.io.BufferedReader;
import java.io.IOException;


public class BufferedReaderIterator extends AbstractIterator<String>
	implements PeekIterator<String>, CloseableIterator<String>
	{
	private String peeked =null;
	private final BufferedReader br;
	private final boolean close_at_end;
	public BufferedReaderIterator(final BufferedReader br,boolean close_at_end) {
		this.br= br;
		this.close_at_end = close_at_end;
		}
	public BufferedReaderIterator(final BufferedReader br) {
		this(br,true);
		}
	@Override
	protected String advance()
		{
		if(peeked!=null) {
			final String tmp=peeked;
			peeked = null;
			return tmp;
			}
		try {
			return this.br.readLine();
			}
		catch(IOException err) {
			return null;
			}
		}
	@Override
	public String peek()
		{
		if(peeked!=null) return peeked;
		try {
			this.peeked =  this.br.readLine();
			return this.peeked;
			}
		catch(IOException err) {
			return null;
			}
		}
	
	@Override
	public void close()
		{
		if(this.close_at_end) {
			try { br.close();}
			catch(IOException err ) {}
			}
		}
	}
