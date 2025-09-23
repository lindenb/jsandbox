package sandbox.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import sandbox.lang.Result;
import sandbox.lang.StringUtils;

public class IOUtils {

private IOUtils(){
}


/** convert string to URL */
public static Result<URL> toURL(final String s) {
	try {
		final URL u= URI.create(s).toURL();
		return Result.success(u);
		}
	catch(Throwable err) {
		return Result.fail(err);
		}
	}

/** return file suffix (INCLUDING the dot) or empty string if there is no dot */
public static String getFileSuffix(final Path path) {
	String fname = path.getFileName().toString();
	int idx = fname.lastIndexOf('.');
	if(idx<0) return "";
	return fname.substring(idx);
}

public static Path getDefaultTmpDirPath() {
	return Paths.get(System.getProperty("java.io.tmpdir"));
}

public static List<String> unroll(final List<String> args) throws IOException {
	if(args.isEmpty()) return Collections.emptyList();
	if(args.size()==1 && args.get(0).equals("-")) {
		return Collections.emptyList();
		}
	if(args.size()==1 && args.get(0).endsWith(".list")) {
		try(BufferedReader br =openBufferedReader(args.get(0))) {
			return br.lines().filter(S->!StringUtils.isBlank(S)).collect(Collectors.toList());
			}
		}
	return args;
	}

public static List<File> unrollFiles(final List<String> args) throws IOException {
	return unroll(args).stream().map(G->new File(G)).collect(Collectors.toList());
	}




public static List<Path> unrollPaths(final List<String> args) throws IOException {
	return unroll(args).stream().map(G->Paths.get(G)).collect(Collectors.toList());
	}


public static Path assertFileExists(final File f) {
	return assertFileExists(f==null?null:f.toPath());
	}

public static Path assertFileExists(final Path f) {
	if( f==null || !Files.exists(f) || Files.isDirectory(f))
		{
		throw new IllegalStateException("Not an existing file "+f);
		}
	return f;
	}


public static File assertDirectoryExist(final File f) {
	if( f==null || !f.exists() ||!f.isDirectory())
		{
		throw new IllegalStateException("Not an existing directory "+f);
		}
	return f;
	}

public static Path assertDirectoryExist(final Path f) {
	if(!(f!=null && Files.exists(f) && Files.isDirectory(f))) {
		throw new IllegalStateException("Not an existing directory "+f);
		}
	return f;
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



public static BufferedReader openBufferedReader(final InputStream is) throws IOException {
	return new BufferedReader(new InputStreamReader(is, "UTF-8"));
	}

public static BufferedReader openBufferedReader(final String path) throws IOException {
	return new BufferedReader(openReader(path));
	}

public static BufferedReader openBufferedReaderFromFile(final File path) throws IOException {
	return openBufferedReaderFromPath(path.toPath());
	}
public static BufferedReader openBufferedReaderFromPath(final Path path) throws IOException {
	return new BufferedReader(Files.newBufferedReader(path));
	}


public static String slurp(final File file) throws IOException {
	Reader r=null;
	try { r = openReader(file); return slurp(r);} 
	finally {close(r);}
	}


public static long consume(final InputStream is) throws IOException {
	try(NullOutputStream out=new NullOutputStream()) {
		copyTo(is,out);
		return out.length();
		}
	}


public static byte[] slurpBytes(final InputStream is) throws IOException {
	try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
		copyTo(is,out);
		return out.toByteArray();
		}
	}

public static String slurp(final String fileOrUrl) throws IOException {
	try(Reader r= openReader(fileOrUrl)) {
		return slurp(r);
		}
	}

public static String readFileContent(final File path) throws IOException {
	if(!path.exists()) throw new FileNotFoundException("not existing file "+path);
	if(!path.canRead()) throw new IOException("canRead==false : file "+path);
	try(Reader r= openReader(path)) {
		return slurp(r);
		}
	}

public static String slurp(final InputStream in) throws IOException {
	return slurp(new InputStreamReader(in));
	}

public static String slurp(final Reader r) throws IOException {
	try(final StringWriter sw= new StringWriter()) {
		copyTo(r, sw);
		return sw.toString();
		}
	}


public static Reader openReader(final String path) throws IOException {
	Objects.requireNonNull(path, "path is null");
	return new InputStreamReader(openStream(path));
	}

public static Reader openReader(final File path) throws IOException {
	return new InputStreamReader(openStream(path));
	}
public static InputStream openPathAsInputStream(final Path path) throws IOException {
	return mayGzipInputStream(Files.newInputStream(path));
	}

public static Reader openPathAsReader(final Path path) throws IOException {
	return new InputStreamReader(openPathAsInputStream(path));
	}



public static InputStream openStream(final String path) throws IOException {
	Objects.requireNonNull(path, "path is null");
	if(isURL(path)) {
		final InputStream in = URI.create(path).toURL().openStream();
		return mayGzipInputStream(in);
	}
	else {
		return openPathAsInputStream(Paths.get(path));
	}
	
}

public static InputStream openStream(final File path) throws IOException {
	return openPathAsInputStream(path.toPath());
	}

/**
 * return stdout if argument is null
 * return gzip compressed file if argument ends with gz
 * @param pathOrNull
 * @return
 * @throws IOException
 */
public static OutputStream openFileAsOutputStream(final File pathOrNull) throws IOException {
	return openPathAsOutputStream(pathOrNull==null?null:pathOrNull.toPath());
	}

public static OutputStream openPathAsOutputStream(final Path pathOrNull) throws IOException {
	if(pathOrNull==null || pathOrNull.toString().equals("-")) return System.out;
	OutputStream os = Files.newOutputStream(pathOrNull);
	if(pathOrNull.getFileName().toString().endsWith(".gz")) {
		return new GZIPOutputStream(os) {
			@Override
			public void close() throws IOException {
				finish();
				super.close();
				}
			};
		}
	return os;
	}

public static Writer openPathAsWriter(final Path pathOrNull) throws IOException {
	if(pathOrNull==null) return new PrintWriter(System.out);
	if(pathOrNull.getFileName().toString().endsWith(".gz")) {
		 return new PrintWriter(openPathAsOutputStream(pathOrNull));
		}
	return Files.newBufferedWriter(pathOrNull);
	}

public static PrintStream openPathAsPrintStream(final Path pathOrNull) throws IOException {
	if(pathOrNull==null) return System.out;
	if(pathOrNull.getFileName().toString().endsWith(".gz")) {
		 return new PrintStream(openPathAsOutputStream(pathOrNull));
		}
	return new PrintStream(Files.newOutputStream(pathOrNull));
	}


public static PrintWriter openPathAsPrintWriter(final Path pathOrNull) throws IOException {
	if(pathOrNull==null) return new PrintWriter(System.out);
	if(pathOrNull.getFileName().toString().endsWith(".gz")) {
		 return new PrintWriter(openPathAsOutputStream(pathOrNull));
		}
	return new PrintWriter(Files.newBufferedWriter(pathOrNull));
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

public static void copyTo(final InputStream in,final File out) throws IOException
	{
	copyTo(in,out.toPath());
	}

public static void copyTo(final Path fin,final OutputStream out) throws IOException
{
try(InputStream in=Files.newInputStream(fin)) {
	copyTo(in, out);
	}
}

public static void copyTo(final File fin,final OutputStream out) throws IOException {
	copyTo(fin.toPath(),out);
	}

public static void copyTo(final InputStream in,final OutputStream out) throws IOException
	{
	byte array[]=new byte[1024];
	int n;
	while((n=in.read(array, 0, array.length))!=-1)
		out.write(array, 0, n);
	}

/** copy in into Path */
public static void copyTo(final InputStream in,final Path out) throws IOException
	{
	try(OutputStream os = Files.newOutputStream(out)) {
		copyTo(in,os);
		}
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
	return "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0";
	}
}
