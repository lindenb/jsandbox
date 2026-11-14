package sandbox.tools.reversefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.ArrayUtils;
import sandbox.tools.central.ProgramDescriptor;

public class ReverseFile extends Launcher {
	private final static Logger LOG=Logger.builder(ReverseFile.class).build();
	@Parameter(names={"-o","-out","--out"},description="output file",required = true)
	private File fileOut=null;
	@Parameter(names={"-n","--buffer-size"},description="buffer size")
	private int buffer_size=100_000;

	
	@Override
	public int doWork(final List<String> args) {
		try {
			if(buffer_size<=0) {
				LOG.error("buffer_size<=0");
				return -1;
				}
			final File input = new File(super.oneAndOnlyOneFile(args));
			IOUtils.assertFileExists(input);
			if(input.equals(this.fileOut)) {
				LOG.error("input==output");
				return -1;
				}
			final long len = input.length();
			final byte[] buffer=new byte[buffer_size];
			try(RandomAccessFile fp = new RandomAccessFile(input, "r")) {
				long remain = len;
				try(FileOutputStream out=new FileOutputStream(this.fileOut)) {
					while(remain>0) {
						final int n_fetch = (int)Math.min(remain,(long)buffer.length);
						long pos = remain-n_fetch;
						fp.seek(pos);
						int n_read = fp.read(buffer, 0, n_fetch);
						if(n_read<0) {
							LOG.error("I/0 error cannot read "+n_fetch+" got "+n_read);
							return -1;
							}
						
						ArrayUtils.reverse(buffer,0,n_read);
						
						out.write(buffer, 0, n_read);
						remain -=n_read;
						}
					out.flush();
					}
				}
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		return 0;
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "reversefile";
    			}
    		};
    	}
	
	public static void main(String[] args) {
		new ReverseFile().instanceMainWithExit(args);
	}

}
