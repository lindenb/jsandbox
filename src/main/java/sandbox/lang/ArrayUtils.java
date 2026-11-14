package sandbox.lang;

public class ArrayUtils {
	public static void reverse(final byte[] array) {
		reverse(array,0,array.length);
		}
	
	public static void reverse(final byte[] array,int off,int len) {
	      int i = off;
	      int j = off+len - 1;
	      byte tmp;
	      while (j > i) {
	          tmp = array[j];
	          array[j] = array[i];
	          array[i] = tmp;
	          j--;
	          i++;
	      }
	  }
}
