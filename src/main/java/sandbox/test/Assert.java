package sandbox.test;

import sandbox.lang.StringUtils;

public class Assert {

public static void assertTrue(boolean b) {
	assertTrue(b,null);
	}
public static void assertTrue(boolean b,final String msg) {
	if(!b) error(StringUtils.orElse(msg,"Assertion TRUE Failed"));
	}

public static void assertFalse(boolean b) {
	assertFalse(b,null);
	}

public static void assertFalse(boolean b,final String msg) {
	if(b) error(StringUtils.orElse(msg,"Assertion FALSE Failed"));
	}
public static void assertEquals(int a,int b) {
	if(a!=b) error("Assertion equals failed :"+a+" "+b);
	}
public static void assertEquals(Object a,Object b) {
	if(a==null && b==null) return;
	if(a==b) return;
	if(a==null /* b is not null */ || !a.equals(b)) error("Assertion equals failed :"+a+" "+b);
	}


private static void error(final String s) {
	System.err.println(s);
	throw new IllegalStateException(StringUtils.orElse(s,"Error"));
	}

}
