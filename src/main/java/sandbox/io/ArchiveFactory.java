package sandbox.io;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class ArchiveFactory implements Closeable {
	public static final String OPT_DESC = "Existing directory or zip file";
	public abstract OutputStream openOuputStream(final String filename) throws IOException;
	
	/** copy whole file into the archive */
	public void copyTo(final Path externalFile,final String filename) throws IOException {
		try ( OutputStream os = openOuputStream(filename))
			{
			IOUtils.copyTo(externalFile, os);
			os.flush();
			}
		}
	
	/** open a writer to this archive */
	public PrintWriter openWriter(final String filename) throws IOException
		{
		return new PrintWriter(openOuputStream(filename), true);
		}

	/** set compression level for zip archives */
	public abstract void setCompressionLevel(int level);
	 
	public abstract boolean isZipArchive();

	
	/** open a new ArchiveFactory, if filename ends with '.zip' it will be a zip instance
	 * otherwise it will be a FileInstance */
	public static ArchiveFactory open(final Path f)  throws IOException
		{
		if( f == null ) throw new IllegalArgumentException("Cannot open(null)");
		final String fn = f.getFileName().toString().toLowerCase();
		if(fn.endsWith(".zip"))
			{
			return new ZipInstance(f);
			}
		else
			{
			return new FileInstance(f);
			}
		}

	
	/** open a new ArchiveFactory, if filename ends with '.zip' it will be a zip instance
	 * otherwise it will be a FileInstance */
	public static ArchiveFactory open(final File f)  throws IOException
		{
		return open(f==null?null:f.toPath());
		}
	
	
	static abstract class AbstractOutputStream extends OutputStream
		{
		protected OutputStream out;
		
		@Override
		public void write(final int b) throws IOException {
			if(out!=null) out.write(b);
			}
		
		@Override
		public void write(final byte[] b) throws IOException {
			if(out!=null)  out.write(b);
			}
		
		@Override
		public void write(final byte[] b, final int off, final int len) throws IOException {
			if(out!=null) out.write(b, off, len);
			}
		
		@Override
		public void flush() throws IOException
			{
			if(out!=null) out.flush();
			}
		}
	
	static class ZipInstance
		extends ArchiveFactory
		{
		OutputStream fout;
		ZipOutputStream zout;
		
		ZipInstance(final Path f) throws IOException
			{
			fout= Files.newOutputStream(f);
			zout=new ZipOutputStream(fout);
			}
		
		@Override
		public final boolean isZipArchive() { return true;}

		@Override
		public void setCompressionLevel(int level) {
			this.zout.setLevel(Math.max(Deflater.NO_COMPRESSION, Math.min(Deflater.BEST_COMPRESSION, level)));
			}
		
		
		@Override
		public OutputStream openOuputStream(final String filename) throws IOException
			{
			final ZipOS os =new ZipOS(filename);
			return os;
			}
		
		@Override
		public void close() throws IOException
			{
			if(this.zout!=null)
				{
				this.zout.finish();
				this.zout.flush();
				this.fout.flush();
				this.zout.close();
				this.fout.close();
				this.zout=null;
				this.fout=null;
				}
			}
		
		private class ZipOS extends AbstractOutputStream
			{
			ZipEntry ze;
			File tmp;
			
			ZipOS(String filename) throws IOException
				{
				while(filename.startsWith("/")) filename=filename.substring(1);
				this.ze=new ZipEntry(filename);
				
				this.tmp=File.createTempFile("tmp", ".zipentry");
				this.tmp.deleteOnExit();
				super.out=new FileOutputStream(this.tmp);
				}
			

			
			@Override
			public void close() throws IOException
				{
				if(out!=null)
					{
					out.flush();
					out.close();
					
					if(ZipInstance.this.zout!=null)
						{
						zout.putNextEntry(this.ze);
						IOUtils.copyTo(this.tmp,ZipInstance.this.zout );
						ZipInstance.this.zout.flush();
						ZipInstance.this.zout.closeEntry();
						}
					ze=null;
					out=null;
					tmp.delete();
					tmp=null;
					}
				}
			}
		
		}
	
	
	static class FileInstance extends ArchiveFactory
		{
		private final Path baseDir;
		
		FileInstance(final Path baseDir) throws IOException
			{
			this.baseDir=baseDir;
			if(Files.exists(baseDir) && !Files.isDirectory(baseDir))
				{
				throw new IOException("Not a directory:"+baseDir);
				}				
			}
		
		@Override
		public void setCompressionLevel(int level) {
			// do nothing
			}
		@Override
		public boolean isZipArchive() {
			return false;
			}
		
		@Override
		public OutputStream openOuputStream(String filename) throws IOException
			{
			while(filename.startsWith(File.separator)) filename=filename.substring(1);
			final Path f= this.baseDir.resolve(filename);
			if(f.getParent()!=null)
				{
				Files.createDirectories(f.getParent());
				}
			return Files.newOutputStream(f);
			}
		@Override
		public void close() throws IOException
			{
			}
		}

}
