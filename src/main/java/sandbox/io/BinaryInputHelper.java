package sandbox.io;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Objects;


public class BinaryInputHelper {
	private final ByteBuffer byteBuffer;
	private final InputStream inputStream;
	private long count=0L;
	private String encoding ="UTF-8";
	public BinaryInputHelper(InputStream in) {
		this.inputStream = Objects.requireNonNull(in);
		byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
	
	public InputStream getInputStream() {
		return this.inputStream;
		}
	
	public long getCount() {
		return count;
		}
	
	public long readLong() throws IOException {
        readByteBuffer(8);
        byteBuffer.flip();
        return byteBuffer.getLong();
		}
	
    public int readInt()  throws IOException {
        readByteBuffer(4);
        byteBuffer.flip();
        return byteBuffer.getInt();
    }

    public BitSet readBitSet(int nBytes) throws IOException {
    	return BitSet.valueOf(this.readNBytes(nBytes));
    	}
    
    public byte[] readNBytes(int nBytes) throws IOException {
    	byte[] array= this.inputStream.readNBytes(nBytes);
    	count+=nBytes;
    	return array;
    	}
    
    public short readShort() throws IOException {
        readByteBuffer(2);
        byteBuffer.flip();
        return byteBuffer.getShort();
    	}
    private void  readByteBuffer(final int numBytes) throws IOException {
        assert(numBytes <= byteBuffer.capacity());
        readBytes(byteBuffer.array(), 0, numBytes);
        byteBuffer.limit(byteBuffer.capacity());
        byteBuffer.position(numBytes);
    	}
    

    public double readDouble() throws IOException {
        readByteBuffer(8);
        byteBuffer.flip();
        return byteBuffer.getDouble();
    }


    public float readFloat() throws IOException  {
        readByteBuffer(4);
        byteBuffer.flip();
        return byteBuffer.getFloat();
    }


    public boolean readBoolean() throws IOException  {
        return (((int)readByte()) == 1);
    }

    /**
     * Reads an 8-bit unsigned byte from the input stream.
     * This method assumes little-endianness.
     */
    public short readUByte() throws IOException  {
        readByteBuffer(1);
        byteBuffer.put((byte)0);
        byteBuffer.flip();
        return byteBuffer.getShort();
    }

    /**
     * Reads a 16-bit unsigned short from the input stream.
     * This method assumes little-endianness.
     */
    public int readUShort() throws IOException  {
        readByteBuffer(2);
        byteBuffer.putShort((short)0);
        byteBuffer.flip();
        return byteBuffer.getInt();
    }

    public void skipNBytes(long count)  throws IOException  {
    	getInputStream().skipNBytes(count);
    }
    
    /**
     * Reads a 32-bit unsigned int from the input stream.
     * This method assumes little-endianness.
     */
    public long readUInt() throws IOException  {
        readByteBuffer(4);
        byteBuffer.putInt(0);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }


    public byte readByte() throws IOException {
        final int ret = this.inputStream.read();
        if (ret == -1) {
            throw new EOFException("Premature EOF");
        	}
        this.count++;
        return (byte)ret;
    	}
    public String readNullTerminatedString() throws IOException {
	    final StringBuilder sb = new StringBuilder();
	    for(;;) {
	    	int c = this.readByte();
	    	if(c=='\0') return sb.toString();
	    	sb.append((char)c);
	        }
    	}
    

    /** read the length of a string as unsigned short and read the string */
    public String readStringUShort() throws IOException {
	   	return readString(readUShort());
	   	}
    
    /** read the length of a string as unsigned short and read the string */
    public String readStringUInt() throws IOException {
	   	return readString(uint2int(readUInt()));
	   	}
    
    private int uint2int(long n) {
    	if(n<0 || n>Integer.MAX_VALUE) throw new IllegalArgumentException("int "+n);
    	return (int)n;
    }
    public String readString(int nBytes) throws IOException {
	   	return new String(readNBytes(nBytes),encoding);
	   	}
    
    private  void readBytes(final byte[] buffer, final int offset, final int length) throws IOException {
        int totalNumRead = 0;
        do {
            final int numRead = readBytesOrFewer(buffer, offset + totalNumRead, length - totalNumRead);
            if (numRead < 0) {
                String msg = String.format("Premature EOF. Expected %d but only received %d", length, totalNumRead);
                throw new IOException(msg);
            } else {
                totalNumRead += numRead;
            }
        	} while (totalNumRead < length);
    	}
    private int readBytesOrFewer(final byte[] buffer, final int offset, final int length) throws IOException {
        if(length==0) return 0;
        int n= this.inputStream.read(buffer, offset, length);
        this.count+=n;
        return n;   
    }
}
