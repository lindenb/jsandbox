package sandbox.cipher;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public interface CipherCodec {
	public byte[] encode(byte[] ut8);
	public byte[] decode(byte[] ut8);
	public default String encode(String str) {
		byte[] enc;
		try {
			enc = encode(str.getBytes("UTF8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
		}
	    return Base64.getEncoder().encodeToString(enc);
	}
	
	public default String decode(String str) {
		try {
		      // Decode base64 to get bytes
		    byte[] utf8 = Base64.getDecoder().decode(str);
		    byte[] dec = decode(utf8);
		    return new String(dec,"UTF8");
		} catch (Throwable e) {
			throw new RuntimeException();
			}
		}

}
