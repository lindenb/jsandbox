package sandbox.cipher;

import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class CipherCodecFactory {
private char[] passPhrase;
private byte[] salt;
private int iteration_count=23;
private String instanceType="PBEWithMD5AndDES";

public CipherCodecFactory setIterationCount(int iteration_count) {
	this.iteration_count = iteration_count;
	return this;
	}

public CipherCodecFactory setPassPhrase(char[] passPhrase) {
	this.passPhrase = passPhrase;
	return this;
	}

public CipherCodecFactory setPassPhrase(String passPhrase) {
	return setPassPhrase(passPhrase.toCharArray());
	}
public CipherCodecFactory setSalt(byte[] salt) {
	this.salt=salt;
	return this;
	}


public CipherCodec make() {
	if(passPhrase==null || passPhrase.length==0) throw new IllegalArgumentException("empty password");
	if(salt==null || salt.length==0) throw new IllegalArgumentException("empty salt");
	try {
		// Create the key
	final KeySpec keySpec = new PBEKeySpec(passPhrase, salt, iteration_count);
	final SecretKey key = SecretKeyFactory.getInstance(this.instanceType).generateSecret(keySpec);



	// Prepare the parameter to the ciphers
	final AlgorithmParameterSpec paramSpec = new PBEParameterSpec(this.salt,this.iteration_count);
	final CypherCodecImpl cipher = new CypherCodecImpl();
	cipher.cipherEncoder = Cipher.getInstance(key.getAlgorithm());
	cipher.cipherEncoder.init(Cipher.ENCRYPT_MODE, key, paramSpec);

	cipher.cipherDecoder = Cipher.getInstance(key.getAlgorithm());
	cipher.cipherDecoder.init(Cipher.DECRYPT_MODE, key, paramSpec);
	
	return cipher;
	
	} catch(Throwable err) {
		throw new RuntimeException(err);
	}
}
	
	private static class CypherCodecImpl implements CipherCodec {
		private Cipher cipherEncoder;
		private Cipher cipherDecoder;
			
		@Override
		public byte[] decode(byte[] ut8) {
		    try {
				return cipherDecoder.doFinal(ut8);
			} catch (Throwable e) {
				throw new RuntimeException();
				}
			}
		
		
		
		public byte[] encode(byte[] utf8) {
				try {
					return cipherEncoder.doFinal(utf8);
				} catch (Throwable e) {
					throw new RuntimeException();
					}
				}
			public String encode(String str) {
				byte[] enc;
				try {
					enc = encode(str.getBytes("UTF8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException();
				}
			    return Base64.getEncoder().encodeToString(enc);
			}
	}
}
