package sandbox.c;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class C {
public static final int EOF=-1;
public static final int errno =-1;
public interface FILE {
	
	}
public interface Bytes {
	byte get(int i);
	byte set(int i,byte b);
	Bytes slice(int from);
	}


private static class ByteArray implements Bytes {
	final byte[] array;
	ByteArray(byte[] array) {
		this.array = array;
		}
	ByteArray(int n) {
		this(new byte[n]);
		}
	@Override
	public byte get(int i) {
		return array[i];
		}
	@Override
	public byte set(int i,byte b) {
		array[i]=b;
		return b;
		}
	@Override
	public Bytes slice(int from) {
		if(from==0) return this;
		return new SubBytes(this,from);
		}
	}

private static class SubBytes implements Bytes {
	int start;
	Bytes delegate;
	SubBytes(Bytes delegate,int start) {
		this.delegate= delegate;
		this.start = start;
		}
	@Override
	public byte get(int i) {
		return this.delegate.get(i+start);
		}
	@Override
	public byte set(int i, byte b) {
		return this.delegate.set(i+start,b);
		}
	@Override
	public Bytes slice(int from) {
		if(from==0) return this;
		return delegate.slice(from+this.start);
		}
	}

private static class FILEImpl implements FILE {
	boolean closed=false;
	boolean is_write=false;
	boolean is_read=false;
	RandomAccessFile random;
	InputStream in=null;
	OutputStream out=null;
	Bytes filename;
	Bytes mode;
	}

public static String toString(Bytes b) {
	return null;
}

public static Bytes of(String s) {
	return null;
}

public static FILE fopen(Bytes f,Bytes mode) {
	try {
		final FILEImpl fp = new FILEImpl();
		fp.filename = strdup(f);
		fp.mode = strdup(mode);
		if(strchr(f,'r')!=null && strchr(mode,'w')!=null) {
			fp.is_write=true;
			fp.is_read=true;
			try {
				fp.random = new RandomAccessFile(toString(f),toString(mode));
				}
			catch(IOException err) {
				return null;
				}
			return fp;
			}
		else if(strchr(mode,'r')!=null) {
			fp.is_read=true;
			try {
				fp.in = new FileInputStream(toString(f));
				}
			catch(IOException err) {
				return null;
				}
			return fp;
			}
		else if(strchr(mode,'w')!=null || strchr(mode,'a')!=null) {
			boolean append=strchr(mode,'a')!=null;
			fp.is_write=true;
			try {
				fp.out = new FileOutputStream(toString(f),append);
				}
			catch(IOException err) {
				return null;
				}
			return fp;
			}
		else
			{
			return null;
			}
		}
	catch(Throwable err) {
		return null;
		}
	}

public static void free(Bytes ptr) {
		
	}

public static Bytes strchr(Bytes ptr,int c) {
	int i=0;
	for(;;) {
		byte b=ptr.get(i);
		if(b==c) return ptr.slice(i);
		if(b==0) return null;
		}
	}

public static int strlen(Bytes ptr) {
	int i=0;
	while(ptr.get(i)!=0) {
		i++;
		}
	return i;
	}

public static Bytes strdup(Bytes ptr) {
	return strndup(ptr,strlen(ptr));
	}

public static Bytes strndup(Bytes ptr,int n) {
	Bytes cp=malloc(n);
	strncpy(cp,ptr,n);
	return cp;
	}

public static Bytes strcpy(Bytes dest,Bytes src) {
	return strncpy(dest,src,strlen(src));
	}

public static Bytes strncpy(Bytes dest,Bytes src,int len) {
	return dest;
	}

public static int strcmp(Bytes a,Bytes b) {
	 int i=0;
	 while(a.get(i)!=0 && (a.get(i)==b.get(i)))
	    {
	       i++;
	    }
	return Byte.compare(a.get(i), b.get(i));
	}

public static int strncmp(Bytes a,Bytes b,int n) {
	 int i=0;
	 while(i< n && a.get(i)!=0 && (a.get(i)==b.get(i)))
	    {
	       i++;
	    }
	return i==n?0:Byte.compare(a.get(i), b.get(i));
	}


public static Bytes memcpy(Bytes dest,Bytes src,int n) {
	System.arraycopy(src, 0, dest, 0, n);
	return dest;
	}


public static Bytes realloc(Bytes a,int n) {
	if(a==null) return malloc(n);
	byte[] a2= new byte[n];
	ByteArray ba=ByteArray.class.cast(a);
	
	return ba;
	}
public static Bytes malloc(int n) {
	return new ByteArray(n);
	}


public static int fread(FILE fp,Bytes buff, int len) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	return -1;
	}

public static int fwrite(FILE fp,Bytes buff, int len) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	if(!f.is_write) return -1;
	return -1;
	}


public static int fgetc(FILE fp) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	return 0;
	}
public static int ferror(FILE fp) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	return 0;
	}

public static int feof(FILE fp) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	return 0;
	}

public static int fflush(FILE fp) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	return 0;
	}

public static int fclose(FILE fp) {
	FILEImpl f = FILEImpl.class.cast(fp);
	if(f.closed) return -1;
	f.closed=true;
	return 0;
	}
public static void exit(int flag) {
	System.exit(flag);;
	}

}
