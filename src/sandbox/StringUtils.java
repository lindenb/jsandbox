package sandbox;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class StringUtils {

public static String md5(final String s) {
	 MessageDigest md;
	 try {
		 md = MessageDigest.getInstance("MD5");
	 } catch (final Exception err) {
		throw new RuntimeException(err);
	 	}
	md.update(s.getBytes());
	return new BigInteger(1,md.digest()).toString(16);
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
}
