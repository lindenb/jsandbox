package sandbox.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer implements Closeable {
	public static final int EOF = 1;
	private final Reader r;
	private final StringBuilder stack =new StringBuilder();
	public Lexer(final Reader r) {
		this.r=r;
	}
	
	/** @return true if no more input is available */
	public boolean isEof() throws IOException {
		return this.peek() == EOF ;
	}
	
	/** same as isEof */
	public final boolean eof() throws IOException {
		return isEof() ;
	}
	
	/** @return next byte in stream. -1 on EOF */
	public int peek() throws IOException
	{
		return peek(0);
	}
	
	/** @return byte in stream at position <code>pos</code>. -1 on EOF */
	public int peek(final int pos) throws IOException
		{
		while(this.stack.length()<=pos) {
			final int c= this.r.read();
			if(c==EOF) return EOF;
			this.stack.append((char)c);
			}
		return this.stack.charAt(pos);
		}
	
	/** test wether string s is in above , starting from pos*/
	public boolean downstream(final int pos,String s)  throws IOException {
		for(int i=0;i< s.length();i++) {
			int c = peek(pos+i);
			if(c==EOF ||c!=(int)s.charAt(i)) return false;
			}
		return true;
		}
	
	/** test wether string s is in above */
	public boolean downstream(final String s)  throws IOException {
			return downstream(0, s);
		}
	
	public int consume(int pos,int n) throws IOException {
		for(int i=0;i< n &&  pos+i < this.stack.length();++i) {
			this.stack.deleteCharAt(pos+i);
			n--;
			}
		while(n>0) {
			int c= this.r.read();
			if(c==EOF) return EOF;
			n--;
			}
		return peek(pos);
		}
	
	public int consume(int n)  throws IOException {
		return consume(0,n);
	}
	
	public int skipWithspaces()  throws IOException {
		int c;
		while((c=this.peek())!=EOF && Character.isWhitespace(c)) {
			consume(1);
		}
		return c;
	}
	
	@Override
	public void close() throws IOException {
		this.stack.setLength(0);
		r.close();
		}
	
	
	private static final Pattern BIGINTEGER = Pattern.compile("[+-]?[0-9]+");
	public java.math.BigInteger nextBigInteger(int pos) throws IOException {
		final String s = peek(pos,BIGINTEGER);
		if(s==null) return null;
		final java.math.BigInteger bi = new java.math.BigInteger(s);
		consume(pos,s.length());
		return bi;
	}
	
	public String quoted(int pos,char qc) throws IOException {
		  final StringBuilder sb=new StringBuilder();
		  if(peek(pos)!=(int)qc) return null;
		  int n=1;
		  int idx=pos+1;
		  for(;;) {
			  int c = peek(idx);
			  if(c==-1) return null;
			  if(c==(int)qc) {
				  consume(pos,n);
				  return sb.toString();
			  }
			 return null;
		  }
		  
		  
	}
	
    public String peek(final Pattern pattern) throws IOException {
    		return peek(0,pattern);
    }

    public String peekRegex(final String pattern) throws IOException {
		return peekRegex(0,pattern);
    	}
    

    
    public String peekRegex(int pos,final String pattern) throws IOException {
    	return peek(pos,Pattern.compile(pattern));
    }
    
    public boolean matchRegex(final Pattern pattern) throws IOException {
    	return peek(pattern)!=null;
    	}
    
    public String nextRegex(final Pattern pattern) throws IOException {
    	String s = peek(pattern);
    	if(s!=null) {
    		consume(s.length());
    		}
    	return s;
    	}
    
    public String peek(int pos,final Pattern pattern) throws IOException {
    	String prevMatch=null;
    	int c;
    	if((c=peek(pos))==-1) return null;
        final StringBuilder sb=new StringBuilder();
        sb.append((char)c);
        
        for(;;) {
        	final Matcher matcher1=pattern.matcher(sb);
        	/* Attempts to match the input sequence, starting at the beginning of the region, against the pattern.*/
        	if (matcher1.lookingAt())
        		{
        		if(matcher1.hitEnd()) {
        			prevMatch =  sb.toString();
        			}
        		++pos;
        		if((c=peek(pos))==-1) return prevMatch;
        		sb.append((char)c);
        		} 
        	else {
        		return prevMatch;
        	}
        }       
    }
   
  public static void main(String[] args) {
	try {
		Lexer in = new Lexer(new StringReader("GATAADYHAY azd azd"));
		System.out.println(in.peek(0, Pattern.compile("[ATGC]+")));
		in.close();
	} catch (Exception e) {
		e.printStackTrace();
	}
}  
    
}
