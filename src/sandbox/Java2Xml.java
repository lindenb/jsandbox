package sandbox;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/**
 * Java2Xml
 *
 */
public class Java2Xml extends AbstractApplication
	{
	private XMLStreamWriter out=null;
	private ClassLoader classLoader=null;
	
	
	/** empty private cstor */
	public Java2Xml()
		{
		classLoader = System.class.getClassLoader();
		}
	
	private void writeModififiers(int m)throws Exception
	{
		if(Modifier.isPublic(m)) out.writeAttribute("public","true");
		if(Modifier.isProtected(m)) out.writeAttribute("protected","true");
		if(Modifier.isPrivate(m)) out.writeAttribute("private","true");
		if(Modifier.isFinal(m)) out.writeAttribute("final","true");
		if(Modifier.isStatic(m)) out.writeAttribute("static","true");
		if(Modifier.isStrict(m)) out.writeAttribute("strict","true");
		if(Modifier.isNative(m)) out.writeAttribute("native","true");
	}
	
	private void writeType(final Type type) throws Exception
		{
		out.writeStartElement("type");
		out.writeAttribute("name",type.getTypeName());
		if(type instanceof java.lang.reflect.ParameterizedType )
			{
			final ParameterizedType p=ParameterizedType.class.cast(type);
			out.writeStartElement("parameterized");
			
			if(p.getOwnerType()!=null) {
			out.writeStartElement("owner-type");
			writeType(p.getOwnerType());
			out.writeEndElement();
			}
			
			if(p.getRawType()!=null) {
				out.writeStartElement("raw-type");
				writeType(p.getRawType());
				out.writeEndElement();
				}
			
			out.writeStartElement("actualtypes-arguments");
			for(final Type t2:p.getActualTypeArguments())
				{
				writeType(t2);
				}
			out.writeEndElement();
			out.writeEndElement();
			}
	if(type instanceof java.lang.reflect.WildcardType )
		{
		final WildcardType w = WildcardType.class.cast(type);
		out.writeStartElement("wildcard");
		out.writeStartElement("upper-bounds");
		for(final Type t2:w.getUpperBounds())
			{
			writeType(t2);
			}
		out.writeEndElement();
		out.writeStartElement("lower-bounds");
		for(final Type t2:w.getLowerBounds())
			{
			writeType(t2);
			}
		out.writeEndElement();
		out.writeEndElement();
		}
			
			
	if(type instanceof java.lang.reflect.GenericArrayType )
			{
			out.writeStartElement("array");
			out.writeEndElement();
			}
	if(type instanceof java.lang.Class )
			{
			final Class<?> clazz=java.lang.Class.class.cast(type);
			
			if(clazz.isArray())
				{
				out.writeStartElement("array-of");
				writeType(clazz.getComponentType());
				out.writeEndElement();
				}
			else if(clazz.isPrimitive())
				{
				out.writeEmptyElement("primitive");
				out.writeAttribute("class",clazz.getName());
				}
			else
				{
				out.writeStartElement("class");
				out.writeAttribute("class", java.lang.Class.class.cast(type).getName());
				out.writeEndElement();
				}
			}
		out.writeEndElement();
		}

	
	
	private void writeClass(final String className) throws Exception
			{
			final Class<?> clazz;
			try
				{
				clazz = this.classLoader.loadClass(className);
				}
			catch(Exception err)
				{	
				out.writeComment(err.getMessage());
				return;
				}
			out.writeStartElement(clazz.isInterface()?"interface":"class");
			out.writeAttribute("name", clazz.getName());
			writeModififiers(clazz.getModifiers());
			
			out.writeStartElement("beans");
			for(Method m1 : clazz.getMethods())
				{
				if(!Modifier.isPublic(m1.getModifiers())) continue;
				if(Modifier.isStatic(m1.getModifiers())) continue;
				if(m1.getReturnType()!=Void.TYPE) continue;
				String name= m1.getName();
				if(name.length()<4 || !name.startsWith("set")) continue;
				if(m1.getParameterCount()!=1) continue;
				Type arg = m1.getParameterTypes()[0];
				String name2;
				if( arg == Boolean.TYPE)
					{
					name2="is"+name.substring(3);
					}
				else
					{
					name2="get"+name.substring(3);
					}
				try {
				Method m2 = clazz.getMethod(name2,new Class[0]);
				if(m2.getReturnType()!=arg) continue;
				out.writeStartElement("bean");
				out.writeAttribute("name",name.substring(3));
				out.writeAttribute("setter",m1.getName());
				out.writeAttribute("getter",m2.getName());
				writeType(arg);
				out.writeEndElement();
				} catch(Exception err) { continue;}
				}
			out.writeEndElement();
			
			for(Method method : clazz.getMethods())
				{
				out.writeStartElement("method");
				out.writeAttribute("name",method.getName());
				writeModififiers(method.getModifiers());
				out.writeStartElement("return");
				
				if(method.getReturnType()==Void.TYPE)
					{
					out.writeAttribute("is-void","true");
					}
				else
					{
									
					writeType(method.getGenericReturnType());
					}
				out.writeEndElement();
				
				out.writeStartElement("parameters");
				for(Parameter parameter:method.getParameters())
					{
					out.writeStartElement("parameter");
					out.writeAttribute("name", parameter.getName());
					writeType(parameter.getParameterizedType());
					out.writeEndElement();
					}
				out.writeEndElement();

				out.writeEndElement();
				}
			
			
			out.writeEndElement();
			}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("j").longOpt("jar").desc("jar file or dir containing jars").hasArgs().build());
		super.fillOptions(options);
		}
	
	private void addJarFile(final File jarFile,final Collection<URL> set)
		throws Exception
		{
		if(jarFile ==null || !jarFile.exists())
			{
			LOG.warning("The jar file "+jarFile.toString()+" doesn't exists. DId you put '--' at the end ?");
			return;
			}
	
		if(jarFile.isDirectory())
			{
			for(File fc: jarFile.listFiles(new FileFilter()
				{
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || (f.isFile() && f.getName().endsWith(".jar"));
					}
				}))
				{
				LOG.info("Adding file "+fc);
				this.addJarFile(fc,set);
				}
			}
		else if(jarFile.isFile() && jarFile.getName().endsWith(".jar"))
			{
			set.add(jarFile.toURI().toURL());
			}
		}
	
	@Override
	protected int execute(final CommandLine cmd) {
		try {
			List<URL> urls = new ArrayList<>();
			if(cmd.hasOption("j")) {
				for(String j:cmd.getOptionValues("j"))
					{
					for(String jarName:j.split("[:]"))
						{
						jarName=jarName.trim();
						if(jarName.isEmpty()) continue;
						addJarFile(new File(jarName),urls);
						}
					}				
				}
			
			this.classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),ClassLoader.getSystemClassLoader());
			
			final List<String> args = cmd.getArgList();
		
		    
		    final HashSet<String> setOfClasses=new HashSet<String>();
		    for( String className:args)
		    	{
		    	if(className.contains("/"))
		    		{	
		    		if(className.endsWith(".java"))
		    			{	
		    			className=className.substring(0,className.length()-5);
		    			}
		    		className=className.replace('/', '.');
		    		}
		    	setOfClasses.add(className);
		    	}
		   final XMLOutputFactory xof=XMLOutputFactory.newFactory();
		   this.out = xof.createXMLStreamWriter(System.out, "UTF-8");
		   this.out.writeStartDocument("UTF-8", "1.0");
		   this.out.writeStartElement("java2xml");
		   for(String className: setOfClasses)
			   {
			   this.writeClass(className);
			   }
		   this.out.writeEndElement();
		   this.out.writeEndDocument();
		   this.out.flush();
		   this.out.close();
		   return 0;
		} catch(Exception err) {
			err.printStackTrace();
			LOG.severe(err.getMessage());
			return -1;
		}
	}

	public static void main(String[] args)
		{
		new Java2Xml().instanceMainWithExit(args);
		}
	
}
