package sandbox.tools.hexdump;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;

import javax.script.ScriptEngine;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import sandbox.Launcher;
import sandbox.io.BinaryInputHelper;
import sandbox.io.IOUtils;
import sandbox.io.IndentWriter;
import sandbox.nashorn.NashornParameters;
import sandbox.nashorn.NashornUtils;
import sandbox.tools.central.ProgramDescriptor;


public class HexDump extends Launcher {
	
    @ParametersDelegate
    private NashornParameters nashorParams =new   NashornParameters();
    @Parameter(names="-o",description=OUTPUT_OR_STANDOUT)
    private Path output = null;

    
    public  class Context {
    	private IndentWriter w;
    	private final BinaryInputHelper codec;
    	Context(InputStream is,IndentWriter w) throws IOException {
    		this.codec=new BinaryInputHelper(is);
    		this.w = w;
    		}
    	private Object println(String str,Object v) {
    		
    		this.w.print("["+str+"]\t");
    		if(v instanceof String) {
    			String s = String.class.cast(v);
    			if(s.chars().anyMatch(C->!(C=='\n' || C=='\t' || C=='\r' || C>=32 && C<=127))) {
    				this.w.print("<<<STRING length="+s.length()+" CONTAINS BINARY>>");
    				}
    			else if(s.length()>1000) {
    				this.w.print(s.substring(0,1000)+"...");
    				}
    			else
    				{
    				this.w.print(s);
    				}
    			}
    		else
    			{
    			this.w.print(String.valueOf(v));
    			}
    		this.w.println();
    		return v;
    		}
    	public Object ftell() throws IOException {
    		if(codec.getInputStream() instanceof FileInputStream) {
    			long n= FileInputStream.class.cast(codec.getInputStream()).getChannel().position();
    			return println("FILE OFFSET",n);
    			}
    		else
    			{
    			return println("NOT A SEEKABLE STREAM ("+codec.getInputStream().getClass()+")",-1);
    			}
    	}
    	
    	public Object readInt() throws IOException {
    		return println("Int",codec.readInt());
    		}
    	public Object readBoolean() throws IOException {
    		return println("Boolean",codec.readBoolean());
    		}
    	public Object readUInt() throws IOException {
    		return println("readUInt",codec.readUInt());
    		}
    	
    	public Object readUShort() throws IOException {
    		return println("readUShort",codec.readUShort());
    		}
    	
    	public Object skipNBytes(long count) throws IOException {
    		codec.skipNBytes(count);
    		return println("skipNBytes",count);
    		}
    	
    	public void print(Object o) {
    		w.print(String.valueOf(o));
    		}
    	public void println(Object o) {
    		w.println(String.valueOf(o));
    		}
    	
    	public Object readStringUShort()  throws IOException {
    		return println("stringUShort",codec.readStringUShort());
    	}
    	public Object readStringUInt()  throws IOException {
    		return println("readStringUInt",codec.readStringUInt());
    	}
    	public Object readString(int nbytes)  throws IOException {
    		return println("readString("+nbytes+")",codec.readString(nbytes));
    	}
    	public Object readNBytes(int n)  throws IOException {
    		byte[] array = codec.readNBytes(n);
    		this.w.print("Read Array. Size="+n);
    		int i=0;
    		while(i< array.length) {
    			if(i==0 || i%30==0) {
    				w.print(String.format("\n[%05d] ",i));
    				}
    			else if(i%10==0) {
    				w.print(" ");
    				}
    			w.print(String.format("%03d(",array[i]));
    			if(array[i]=='\r') {
    				w.print("\\r");
    				}
    			else if(array[i]=='\t') {
    				w.print("\\t");
    				}
    			else if(array[i]=='\n') {
    				w.print("\\n");
    				}
    			else if(array[i]=='\b') {
    				w.print("\\b");
    				}
    			else if((array[i]>=32 && array[i]<127)) {
    				w.print((char)array[i]);
    				w.print(" ");
    				}
    			else {
    				w.print("  ");
    				}
    			w.print(") ");
    			i++;
    			}
    		this.w.println();
    		return array;
    		}
    	
    	public void push() {
    		this.w.push();
    		}
    	public void pop() {
    		this.w.pop();
    		}
    	
    	}
    
    private int run(InputStream in,IndentWriter pw) throws Exception{
        Context ctx =new Context(in, pw);
       try {
        	final ScriptEngine ee = NashornUtils.makeRequiredEngine();
			ee.put("context",ctx);
			try(Reader r=	this.nashorParams.getReader()) {
				ee.eval(r);
				}
			 ctx.w.println();
			pw.flush();
			return 0;
        	}
        catch(Throwable err) {
        	ctx.w.println("AN Error occured "+err.getMessage()+" "+err.getClass());
        	 ctx.w.println();
        	return -1;
        	}
      
    	}
	
	@Override
	public int doWork(final List<String> args) {
		int ret=0;
		try {
			   // Here we are generating Nashorn JavaScript Engine 
	        final List<Path> inputs = IOUtils.unrollPaths(args);
	        try(IndentWriter pw =new IndentWriter(super.openPathAsPrintWriter(this.output))) {

		        if(inputs.isEmpty()) {
		        	ret=run(System.in,pw);
		        	}
		        else
		        	{
		        	for(Path p:inputs) {
		        		pw.println("##"+p.toString());
		        		pw.push();
		        		try(InputStream fin=new FileInputStream(p.toFile())) {
		        			if(run(fin,pw)!=0) ret=-1;
		        			}
		        		pw.pop();
		        		}
		        	}
		        }	        
			return ret;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		
		}
	
	 public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "hexdump";
    			}
    		};
    	}
	
	public static void main(String[] args) {
		new HexDump().instanceMainWithExit(args);
	}
}
