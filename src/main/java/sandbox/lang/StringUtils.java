package sandbox.lang;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Properties;

public class StringUtils {

public static String subStringBefore(String s, String delim) {
	int i=s.indexOf(delim);
	if(i<0) return s;
	return s.substring(0,i);
	}


public static String subStringAfter(String s, String delim) {
	int i=s.indexOf(delim);
	if(i<0) return "";
	return s.substring(i+delim.length());
	}

	
public static String repeat( int count,char c) {
	final StringBuilder sb = new StringBuilder(count);
	while(count>0) {
		sb.append(c);
		count--;
		}
	return sb.toString();
	}
	
public static String ltrim(final String s) {
	int x=0;
	while(x<s.length() && Character.isSpaceChar(s.charAt(x))) {
		x++;
		}
	return x==0?s:s.substring(x);
	}
private static String hash(final String s,final String method) {
	 MessageDigest md;
	 try {
		 md = MessageDigest.getInstance(method);
	 } catch (final Exception err) {
		throw new RuntimeException(err);
	 	}
	md.update(s.getBytes());
	return new BigInteger(1,md.digest()).toString(16);
}

public static String md5(final String s) {
	return hash(s,"MD5"); 
	}
public static String sha1(final String s) {
	return hash(s,"SHA1"); 
	}


public static String ifBlank(final String s,final String def) {
	return isBlank(s)?def:s;
}

public static boolean isBlank(final String s) {
	if(s==null) return true;
	for(int i=0;i< s.length();i++) {
		if(!Character.isWhitespace(s.charAt(i))) return false;
		}
	return true;
	}
/**   strpbrk - search a string for any of a set of characters 
 * @return the index or -1 */
public static int strpbrk(final String s,final String accept) {
	for(int i=0;i< s.length();i++) {
		if(accept.indexOf(s.charAt(i))!=-1) return i;
		}
	return -1;
	}

/** escape C string */
public static String escapeC(final CharSequence s) {
	final StringBuilder sb = new StringBuilder(s.length());
	for(int i=0;i< s.length();i++)
		{
		final char c = s.charAt(i);
		switch(c) {
			case '\n' : sb.append("\\n");break;
			case '\r' : sb.append("\\r");break;
			case '\t' : sb.append("\\t");break;
			case '\\' : sb.append("\\\\");break;
			case '\'' : sb.append("\\\'");break;
			case '\"' : sb.append("\\\"");break;
			default:sb.append(c);break;
			}
		}
	return sb.toString();
	}

/** parse an Optional integer */
public static OptionalInt parseInteger(final String s) {
	if(isBlank(s)) return OptionalInt.empty();
	try {
		return OptionalInt.of(Integer.parseInt(s.trim()));
		}
	catch(final NumberFormatException err) {
		return OptionalInt.empty();
		}
	}

public static boolean isInteger(final String s) {
	return parseInteger(s).isPresent();
	}


/** parse an Optional double */
public static OptionalDouble parseDouble(final String s) {
	if(isBlank(s)) return OptionalDouble.empty();
	try {
		return OptionalDouble.of(Double.parseDouble(s.trim()));
		}
	catch(final NumberFormatException err) {
		return OptionalDouble.empty();
		}
	}

public static boolean isDouble(final String s) {
	return parseDouble(s).isPresent();
	}

public static String orElse(final String s,final String def) {
	return isBlank(s)?def:s;
	}

public static String normalizeSpaces(final String s) {
	return s.replaceAll("\\s+"," ").trim();
	}

/** convert first char to UpperCase */
public static String getJavaName(final String s) {
	if(s.length()<2) return s.toUpperCase();
	return s.substring(0, 1).toUpperCase()+s.substring(1);
}

//https://stackoverflow.com/questions/13700333/convert-escaped-unicode-character-back-to-actual-character
public static  String unescapeUnicode(final String s) {
	try
		{
		final Properties p = new Properties();
		p.load(new StringReader("key="+s));
		return p.getProperty("key");
		}
	catch (final IOException e)
		{
		return s;
		}
	}
public static String unescape(String s) {
	s=unescapeUnicode(s);
	StringBuilder sb=new StringBuilder(s.length());
	int i=0;
	while(i<s.length()) {
		if(s.charAt(i)=='\\' && i+1 < s.length()) {
			switch(s.charAt(i+1)) {
				case 'n': sb.append("\n");break;
				case 't': sb.append("\t");break;
				case 'r': sb.append("\r");break;
				case '\"': sb.append("\"");break;
				case '\'': sb.append("\'");break;
				case '\\': sb.append("\\");break;
				default:break;
				}
			i++;
			}
		else
			{
			sb.append(s.charAt(i));
			}	
		i++;
		}
	return sb.toString();
	}
}
