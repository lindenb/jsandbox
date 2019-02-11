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
}
