package sandbox.tools.scriptreplay;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.List;
import java.util.OptionalInt;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;
// https://en.wikipedia.org/wiki/ANSI_escape_code#CSIsection
public class ScriptReplay extends Launcher {
	private final byte ESC=27;
	private String readLine(InputStream in) throws IOException {
		StringBuilder sb= null;
		int c;
		while((c=in.read())!=-1) {
			if(sb==null) sb=new StringBuilder();
			if(c=='\n') break;
			sb.append((char)c);
			}
		return sb==null?null:sb.toString();
		}
	
	private OptionalInt readInt(PushbackInputStream in ) throws IOException {
		int c=in.read();
		if(c==-1) throw new EOFException("cannot read one byte");
		if(!Character.isDigit(c)) {
			in.unread(c);
			return OptionalInt.empty();
			}
		int n= (byte)c-(byte)'0';
		for(;;) {
			c=in.read();
			if(c==-1) break;
			if(!Character.isDigit(c)) {
				in.unread(c);
				break;
				}
			n=n*10+((byte)c-(byte)'0');
			}
		return OptionalInt.of(n);
		}
	
	private boolean yynext(PushbackInputStream in ) throws IOException {
		int c = in.read();
		if(c==-1) return false;
		//in.unread(c);
		if(c==ESC) {
			c = in.read();
			if(c==-1) return false;
			if(c=='[') {
				OptionalInt v = readInt(in);
				c = in.read();
				if(c==-1) return false;
				
				}
			}
		else if(c<32) {
			System.err.println("bin "+c);
			}
		else
			{
			System.err.println("ascii "+(char)c);
			}
		return true;
		}
	
	private void process(InputStream in0) throws IOException {
		try(PushbackInputStream in= new PushbackInputStream(in0,1000)) {
			// Script started on 2026-09-18 13:47:59+02:00 [TERM="xterm-256color" TTY="/dev/pts/1" COLUMNS="140" LINES="36"]
			String line= readLine(in);
			if(line==null) throw new IOException("cannot read first line of input stream");
			while(yynext(in)) {
				
				}
			}
		}
	
	@Override
	public int doWork(List<String> args) {
		try {
			final String input = oneFileOrNull(args);
			if(StringUtils.isBlank(input)) {
				process(System.in);
				}
			else
				{
				try(InputStream in=IOUtils.openStream(input)) {
					process(in);
					}
				}
			return 0;
			}
		catch(Throwable err ) {
			err.printStackTrace();
			return -1;
			}
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "scriptreplay";
    			}
    		};
    	}
	
	public static void main(final String[] args) {
		new ScriptReplay().instanceMainWithExit(args);
	}
}
