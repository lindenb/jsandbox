package sandbox.tools.java2jni;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;


public class JavaToJNI extends Launcher
	{
	protected static final Logger LOG=Logger.builder(JavaToJNI.class).build();

	/** unique id generator */
	private static int ID_GENERATOR=0;
	/** final printer */
	@com.beust.jcommander.Parameter( names= {"-j","--jar"}, description="one *.jar file or a *.list file with one jar per line.")
	private List<File> userJarFiles = new ArrayList<>();
	
	
	public static class DataType {
		Class<?> clazz;
		DataType(Class<?> clazz) {
			this.clazz=clazz;
			}
		}
	public static class NamedParam {
		String name;
		DataType dataType;
		NamedParam(Parameter param) {
			this.name = param.getName();
			this.dataType = new DataType(param.getType());
			}
		}
	
	public static abstract class AbstractFunction {
		private final ClassInfo ci;
		private final List<NamedParam> parameters = new ArrayList<>();
		AbstractFunction(final ClassInfo ci) {
			this.ci = ci;
			}
		}
	
	public static class MyConstructor extends AbstractFunction {
		private final Constructor<?> ctor;
		MyConstructor(ClassInfo ci,Constructor<?> ctor) {
			super(ci);
			this.ctor =ctor;
			for(Parameter  param : ctor.getParameters()) {
				super.parameters.add(new NamedParam(param));
				}
			
			}
		public String getFunctionName() {
			return super.ci.getSimpleName()+"New";
			}
		}
	
	public static class MyMethod extends AbstractFunction {
		private final Method  method;
		private DataType retType;
		MyMethod(ClassInfo ci,Method method) {
			super(ci);
			this.method =method;
			for(Parameter  param : method.getParameters()) {
				super.parameters.add(new NamedParam(param));
				}
			retType = new DataType(method.getReturnType());
			}
		public DataType getReturnType() {
			return retType;
			}
		public String getFunctionName() {
			return super.ci.getSimpleName()+method.getName();
			}
		}
	
	public static class ClassInfo {
		private final Class<?> clazz;
		final List<MyConstructor> constructors = new ArrayList<>();
		final List<MyMethod> methods = new ArrayList<>();
		ClassInfo(Class<?> clazz) {
			this.clazz = clazz;
			}
		public List<MyConstructor> getConstructors() {
			return constructors;
			}
		public List<MyMethod> getMethods() {
			return methods;
			}
		public String getSimpleName() {
			return this.clazz.getSimpleName();
			}
		public String getQualifiedName() {
			return this.clazz.getCanonicalName();
			}
		}
	
	private Map<Class<?>,ClassInfo> visited = new HashMap<>();
	
	
	



	private void processClass(final URLClassLoader classLoader, String className) throws Exception {
		if(className.equals("byte")) return;
		if(className.equals("char")) return;
		if(className.equals("short")) return;
		if(className.equals("int")) return;
		if(className.equals("long")) return;
		if(className.equals("[B")) return;
		if(className.equals("[C")) return;
		if(className.equals("[I")) return;
		Class<?> clazz=classLoader.loadClass(className);

		if(visited.containsKey(clazz)) {
			return;
			}
		final ClassInfo ci = new ClassInfo(clazz);
		visited.put(clazz, ci);
		
		
		
		
		
		if(!Modifier.isAbstract(clazz.getModifiers())) {
			for(Constructor<?> ctor:clazz.getConstructors()) {
				if(!Modifier.isPublic(ctor.getModifiers())) continue;
				for(Parameter param:ctor.getParameters()) {
					processClass(classLoader,param.getType().getName());
					}
				ci.constructors.add(new MyConstructor(ci, ctor));
				}
			}
		
		for(Method method : clazz.getMethods()) {
			if(!Modifier.isPublic(method.getModifiers())) continue;
			if(Modifier.isAbstract(method.getModifiers())) continue;
			if(Modifier.isNative(method.getModifiers())) continue;
			for(Parameter param:method.getParameters()) {
				processClass(classLoader,param.getType().getName());
				}
			ci.methods.add(new MyMethod(ci, method));
			}
		
		}
	
	@Override
	public int doWork(final List<String> argv) {
		try {
			final ArrayList<URL> urls=new ArrayList<URL>();
			for(File f:this.userJarFiles)
			 	{
				urls.add(f.toURI().toURL()); 
			 	}
			   
			 
		    final URLClassLoader cl= new URLClassLoader(urls.toArray(new URL[urls.size()]),ClassLoader.getSystemClassLoader());
			
		    for(String className:  IOUtils.unroll(argv).stream().collect(Collectors.toSet())) {
				processClass(cl,className);
				}
			final Context context = new VelocityContext();
			context.put("classes", this.visited.values());
		    
            final VelocityEngine ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            ve.setProperty("file.resource.loader.path","/home/lindenb/src/jsandbox/src/main/java/sandbox/tools/java2jni");
            //ve.setProperty("file.resource.loader.path",file.getParent()==null?".":file.getParent().toString());
            ve.init();
            final Template template = ve.getTemplate("java2jni.c.vm");
           
            try(PrintWriter w =new PrintWriter(System.out)) {
            template.merge( context, w);
            w.flush();
            }
            
		

		    
		    return 0;
		} catch (final Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	 public static ProgramDescriptor getProgramDescriptor() {
	    	return new ProgramDescriptor() {
	    		@Override
	    		public String getName() {
	    			return "java2jni";
	    			}
	    		};
	    	}	
	
	
	public static void main(String[] args)
		{
		new JavaToJNI().instanceMainWithExit(args);
		}
	
}
