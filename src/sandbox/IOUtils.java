package sandbox;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class IOUtils {

private IOUtils(){
}

public static boolean isURL(final String s) 
	{
	try  {new java.net.URL(s); return true;}
	catch(java.net.MalformedURLException e) { return false;}
	}

public static Collection<String> expandList(final String[] files) throws IOException
	{
	return expandList(Arrays.asList(files));
	}

public static Collection<String> expandList(Collection<String> files) throws IOException {
	final LinkedHashSet<String> L = new  LinkedHashSet<>();
	for(final String f: files)
		{
		if(isURL(f) || !f.endsWith(".list")) {
			L.add(f);
		}
		else
			{
			L.addAll(java.nio.file.Files.
						lines(Paths.get(f)).
						filter(F-> !F.startsWith("#") || F.trim().isEmpty()).
						collect(Collectors.toList())
						);
			}
		}	
	return L;
}


public static BufferedReader openBufferedReader(final String path) throws IOException {
	return new BufferedReader(openReader(path));
	}

public static BufferedReader openBufferedReaderFromFile(final File path) throws IOException {
	return new BufferedReader(new FileReader(path));
	}

public static String slurp(final String fileOrUrl) throws IOException {
	Reader r=null;
	try { r = openReader(fileOrUrl); return readReaderContent(r);} 
	finally {close(r);}
	}

public static String readFileContent(final File path) throws IOException {
	if(!path.exists()) throw new FileNotFoundException("not existing file "+path);
	if(!path.canRead()) throw new IOException("canRead==false : file "+path);
	Reader r=null;
	try { r = openReader(path); return readReaderContent(r);} 
	finally {close(r);}
	}

public static String readStreamContent(final InputStream in) throws IOException {
	return readReaderContent(new InputStreamReader(in));
	}

public static String readReaderContent(final Reader r) throws IOException {
	final StringWriter sw= new StringWriter();
	copyTo(r, sw);
	return sw.toString();
	}


public static Reader openReader(final String path) throws IOException {
	return new InputStreamReader(openStream(path));
	}

public static Reader openReader(final File path) throws IOException {
	return new InputStreamReader(openStream(path));
	}


public static InputStream openStream(final String path) throws IOException {
	InputStream in = null;
	if(isURL(path)) {
		in = new java.net.URL(path).openStream();
	}
	else {
		in = Files.newInputStream(Paths.get(path));
	}
	return mayGzipInputStream(in);
}

public static InputStream openStream(final File path) throws IOException {
	return mayGzipInputStream( new FileInputStream(path));
	}


public static InputStream mayGzipInputStream(InputStream in) throws IOException {
	/* http://stackoverflow.com/questions/4818468 */
	java.io.PushbackInputStream pb = new java.io.PushbackInputStream( in, 2 ); //we need a pushbackstream to look ahead
 	byte [] signature = new byte[2];
	pb.read( signature ); //read the signature
	pb.unread( signature ); //push back the signature to the stream
 	if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) //check if matches standard gzip magic number
   		return new java.util.zip.GZIPInputStream( pb );
 	else 
   		return pb;

}

public static void copyTo(final Reader in,final Writer out) throws IOException
{
char array[]=new char[1024];
int n;
while((n=in.read(array, 0, array.length))!=-1)
	out.write(array, 0, n);
}


public static void copyTo(final InputStream in,final OutputStream out) throws IOException
	{
	byte array[]=new byte[1024];
	int n;
	while((n=in.read(array, 0, array.length))!=-1)
		out.write(array, 0, n);
	}

public static void flush(final Object...array)
	{
	if(array==null ) return;
	for(final Object o:array)
		{
		if(o==null) continue;
		if(o instanceof Flushable) {
			try {
				Flushable.class.cast(o).flush();		
			} catch (Exception e) {
				}
			continue;
			}
		
		try {
			final Method m = o.getClass().getMethod("flush");
			if(Modifier.isStatic(m.getModifiers())) return;
			if(Modifier.isPublic(m.getModifiers())) return;
			m.invoke(o);
			} 
		catch (Exception e) {
			}
		}
	}
	
	
public static void close(final Object...array)
	{
	if(array==null ) return;
	for(final Object o:array)
		{
		if(o==null) continue;
		if(o instanceof Closeable) {
			try {
				Closeable.class.cast(o).close();		
			} catch (Exception e) {
				}
			continue;
			}
		
		try {
			final Method m = o.getClass().getMethod("close");
			if(Modifier.isStatic(m.getModifiers())) return;
			if(Modifier.isPublic(m.getModifiers())) return;
			m.invoke(o);
			} 
		catch (Exception e) {
			}
		}
	}

public static String getUserAgent() {
	return "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:60.0) Gecko/20100101 Firefox/60.0";
	}
}
