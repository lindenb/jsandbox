package sandbox.tools.swap2bits;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;

public class SwapTwoBits extends Launcher {
	protected static final Logger LOG=Logger.builder(SwapTwoBits.class).build();
	private static final String EXT="bits";
	
	private static void reverse(final byte[] array) {
	      if (array == null) {
	          return;
	      }
	      int i = 0;
	      int j = array.length - 1;
	      byte tmp;
	      while (j > i) {
	          tmp = array[j];
	          array[j] = array[i];
	          array[i] = tmp;
	          j--;
	          i++;
	      }
	  }
	
	@Override
	public int doWork(List<String> args) {
		for(String arg:args) {
			try {
			final File file1 = new File(arg);
			if(!file1.isFile()) {
				LOG.info("not a file :"+file1);
				continue;
				}
			if(file1.getName().equals(EXT)) {
				LOG.info("short name :"+file1);
				continue;
				}
			final long n= file1.length();
			if(n<2) {
				LOG.info("short file :"+file1);
				continue;
				}
			final int nswap;
			final String fname= file1.getName();
			int dot = fname.lastIndexOf('.');
			final File file2;
			if(dot>0 && fname.endsWith("bits") && StringUtils.isInteger(fname.substring(dot+1,fname.length()-4))) {
				nswap = Integer.parseInt(fname.substring(dot+1,fname.length()-4));
				if(nswap > n) {
					LOG.info("swap > size of "+file1);
					continue;
					}
				if(nswap<=0) {
					LOG.info("swap > size of "+file1);
					continue;
					}
				file2 = new File(file1.getParentFile(),fname.substring(0,dot));
				}
			else
				{
				nswap=(int)Math.min(1000,n);
				file2 = new File(arg+"."+nswap+"bits");
				}
			if(file2.exists()) {
				LOG.info("already exists:"+file2);
				continue;
				}
			
			try(RandomAccessFile io = new RandomAccessFile(file1,"rw")) {
				final byte[] array =new byte[nswap];
				if(io.read(array)!=nswap) {
					LOG.error("Cannot read "+nswap+" bytes");
					continue;
					}
				io.seek(0L);
				reverse(array);
				io.write(array);
				}
			file1.renameTo(file2);
			LOG.info(file1.toString()+" -> " + file2);
			} catch(final IOException err ) {
				LOG.error(err);
				}
			}
		return 0;
		}
	public static void main(String[] args) {
		new SwapTwoBits().instanceMainWithExit(args);
	}

}
