package sandbox;

public class StringUtils {

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

}
