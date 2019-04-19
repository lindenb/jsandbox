package sandbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


/** seen https://blog.nobel-joergensen.com/2008/07/16/using-eclipse-compiler-to-create-dynamic-java-objects-2/ */
public abstract class AbstractJavaCompilable {
	
	public AbstractJavaCompilable()
		{
		
		}
	
	/** custom class loader */
	private static class SpecialClassLoader extends ClassLoader
		{   
	    private Map<String,MemoryByteCode> class2code = new HashMap<String, MemoryByteCode>();
	 
	    @Override
	    protected Class<?> findClass(final String name) throws ClassNotFoundException {       
	        MemoryByteCode mbc = class2code.get(name);       
	        if (mbc==null){           
	            mbc = class2code.get(name.replace(".","/"));           
	            if (mbc==null){               
	                return super.findClass(name);           
	            }       
	        }       
	        return super.defineClass(name, mbc.getBytes(), 0, mbc.getBytes().length);   
	    }
	 
	    public void addClass(String name, MemoryByteCode mbc) {       
	    	class2code.put(name, mbc);   
	    }
	}
	
	/** custom SimpleJavaFileObject storing code in memory */ 
	private static class MemoryByteCode extends SimpleJavaFileObject {   
	    private ByteArrayOutputStream baos=null;   
	    public MemoryByteCode(final String name) {       
	        super(URI.create("byte:///" + name + ".class"), Kind.CLASS);   
	    }   
	    @Override
	    public CharSequence getCharContent(boolean ignoreEncodingErrors) {       
	        throw new IllegalStateException();   
	    }   
	    
	    @Override
	    public OutputStream openOutputStream() {       
	        baos = new ByteArrayOutputStream();       
	        return baos;   
	    }   
	    @Override
	    public InputStream openInputStream() {       
	        throw new IllegalStateException();   
	    }   
	    
	    byte[] getBytes() {       
	        return baos.toByteArray();   
	    }
	}
	 
	
	
	private static class MemorySource extends SimpleJavaFileObject {   
	    private final String src;   
	    public MemorySource(final String name, final String src) {       
	        super(URI.create("file:///" + name + ".java"), Kind.SOURCE);       
	        this.src = src;   
	    }   
	    @Override
	    public CharSequence getCharContent(boolean ignoreEncodingErrors) {       
	        return src;   
	    }   
	    @Override
	    public OutputStream openOutputStream() {       
	        throw new IllegalStateException();   
	    }   
	    @Override
	    public InputStream openInputStream() {       
	        return new ByteArrayInputStream(src.getBytes());   
	    }
	}
	
	private static class SpecialJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {   
	    private final SpecialClassLoader xcl;   
	    public SpecialJavaFileManager(final StandardJavaFileManager sjfm,final SpecialClassLoader xcl) {       
	        super(sjfm);       
	        this.xcl = xcl;   
	    }   
	    @Override
	    public JavaFileObject getJavaFileForOutput(final Location location,final String name, JavaFileObject.Kind kind, FileObject sibling) throws IOException {       
	        final MemoryByteCode mbc = new MemoryByteCode(name);       
	        xcl.addClass(name, mbc);       
	        return mbc;   
	    }
	    @Override
	    public ClassLoader getClassLoader(final Location location) {       
	        return xcl;   
	    }
	}
	 
	
	protected Class<?> compileClass(final String className,final String javaCode)
		{
		 try{           
	            final JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
	 
	            StandardJavaFileManager sjfm = javac.getStandardFileManager(null, null, null);
	            SpecialClassLoader cl = new SpecialClassLoader();           
	            SpecialJavaFileManager fileManager = new SpecialJavaFileManager(sjfm, cl);           
	            List<String> options = Collections.emptyList();
	 
	            List<JavaFileObject> compilationUnits = Arrays.asList(
	            		new MemorySource(className, javaCode)
	            		);           
	            DiagnosticListener<? super JavaFileObject> dianosticListener = null;           
	            Iterable<String> classes = null;           
	            Writer out = new PrintWriter(System.err);           
	            JavaCompiler.CompilationTask compile = javac.getTask(
	            		out,
	            		(JavaFileManager)fileManager,
	            		dianosticListener, options, 
	            		classes, compilationUnits
	            		);           
	            boolean res = compile.call();           
	            if (res){               
	                return cl.findClass(className);           
	            }       
	        } catch (Exception e){           
	            e.printStackTrace();       
	        }       
	        return null;   
	    }		
	
	
	
	protected void run() throws Exception
		{
		System.err.println(System.getProperty("java.class.path").replace(":", "\n"));
		StringWriter code=new StringWriter();
		PrintWriter pw=new PrintWriter(code);
		
		pw.println("public class Test2 extends "+this.getClass().getName()+"{");
		pw.println("protected void run() throws Exception { System.err.println(\"OK!!\");}");
		pw.println("public static void main(String[] args)throws Exception { new Test2().run();}");
		pw.println("}");
		pw.flush();
		pw.close();
		Class<?> compiledClass = compileClass("Test2",code.toString() );
		 
	        if (compiledClass==null){           
	            return;       
	        }       
	        try{           
	            Method m = compiledClass.getMethod("main",String[].class);           
	            m.invoke(null, new Object[]{null});       
	        }
	        catch (Exception e) {           
	            e.printStackTrace();       
	        }   

		}
}
