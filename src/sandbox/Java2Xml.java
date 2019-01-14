package sandbox;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;



/**

## Example

```
find /home/lindenb/package/gephi-0.9.2/gephi/modules/ /home/lindenb/package/gephi-0.9.2/platform/core /home/lindenb/package/gephi-0.9.2/platform/lib/ -type f -name "*.jar" > jeter.list
 java -jar dist/java2xml.jar -j jeter.list  org.gephi.layout.plugin.forceAtlas2.ForceAtlas2 | xmllint --format -
 ```

 */
public class Java2Xml extends Launcher
	{
	private static final Logger LOG = Logger.builder(Launcher.class).build();
	@com.beust.jcommander.Parameter(
			names= {"-j","--jar"},
			description="one *.jar file or a *.list file with one jar per line."
			)
	private File userJarFile = null;
	

	
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
			out.writeAttribute("simple-name", clazz.getSimpleName());
			out.writeAttribute("canonical-name", clazz.getCanonicalName());
			if(clazz.getPackage()!=null)
				{
				Package p =clazz.getPackage();
				out.writeAttribute("package", p.getName());
				}
			
			writeModififiers(clazz.getModifiers());
			if(clazz.getSuperclass()!=null)
				{
				out.writeAttribute("super", clazz.getSuperclass().getName());
				}
			out.writeStartElement("interfaces");
			for(final Type i:clazz.getInterfaces())
				{
				out.writeStartElement("interface");
				writeType(i);
				out.writeEndElement();
				}
			out.writeEndElement();

			
			out.writeStartElement("beans");
			for(Method m1 : clazz.getMethods())
				{
				final String name= m1.getName();
				if(!Modifier.isPublic(m1.getModifiers())) {
					continue;
				}
				if(Modifier.isStatic(m1.getModifiers())) {
					continue;
				}
				if(m1.getReturnType()!=Void.TYPE) {
					continue;
				}
				
				
				if(!(name.length()>3 && name.startsWith("set"))) {
					continue;
				}
				if(m1.getParameterCount()!=1) {
					continue;
					}
				Type arg = m1.getParameterTypes()[0];

				String name2;
				if( arg == Boolean.TYPE || arg.getTypeName().equals("java.lang.Boolean"))
					{
					name2="is"+name.substring(3);
					}
				else
					{
					name2="get"+name.substring(3);
					}
				try {
				final Method m2 = clazz.getMethod(name2,new Class[0]);
				if(m2.getReturnType()!=arg) continue;
				out.writeStartElement("bean");
				out.writeAttribute("name",
						name.substring(3,4).toLowerCase()+
						name.substring(4)
						);
				out.writeAttribute("setter",m1.getName());
				out.writeAttribute("getter",m2.getName());
				writeType(arg);
				out.writeEndElement();
				} catch(Exception err) { continue;}
				}
			out.writeEndElement();
	
			out.writeStartElement("fields");
			for(final Field f:clazz.getFields())
				{
				out.writeStartElement("field");
				out.writeAttribute("name",f.getName());
				out.writeAttribute("declaring-class",f.getDeclaringClass().getName());
				writeModififiers(f.getModifiers());

				writeType(f.getType());
				
				out.writeEndElement();
				}
			out.writeEndElement();

			
			out.writeStartElement("constructors");
			for(final Constructor<?> c:clazz.getConstructors())
				{
				out.writeStartElement("constructor");
				out.writeAttribute("declaring-class",c.getDeclaringClass().getName());
				writeModififiers(c.getModifiers());

				out.writeStartElement("parameters");
				for(Parameter parameter:c.getParameters())
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


			
			out.writeStartElement("methods");
			for(Method method : clazz.getMethods())
				{
				out.writeStartElement("method");
				out.writeAttribute("name",method.getName());
				out.writeAttribute("declaring-class",method.getDeclaringClass().getName());
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

				out.writeStartElement("throws");
				for(final Type ex:method.getGenericExceptionTypes())
					{
					out.writeStartElement("throw");
					writeType(ex);
					out.writeEndElement();
					}
				out.writeEndElement();

				
				out.writeEndElement();
				}
			out.writeEndElement();
			
			
			out.writeEndElement();
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
	public int doWork(List<String> args) {
		
		try {
			List<URL> urls = new ArrayList<>();
			if(this.userJarFile!=null) {
				final Set<File> extJarFiles = new HashSet<>();
				if(userJarFile.getName().endsWith(".list"))
					{
					extJarFiles.addAll(Files.lines(this.userJarFile.toPath()).
							map(L->L.trim()).
							filter(L->L.endsWith(".jar")).
							map(L->new File(L)).
							collect(Collectors.toSet()))
							;
					}
				else if(this.userJarFile.getName().endsWith(".jar"))
					{
					extJarFiles.add(this.userJarFile);
					}
				else
					{
					LOG.error("bad jar file "+this.userJarFile);
					return -1;
					}
				for(final File jf: extJarFiles)
					{
					addJarFile(jf,urls);
					}				
				}
			
			this.classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),ClassLoader.getSystemClassLoader());
			
		
		    
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
		} catch(final Throwable err) {
			err.printStackTrace();
			LOG.error(err.getMessage());
			return -1;
		}
	}

	public static void main(final String[] args)
		{
		new Java2Xml().instanceMainWithExit(args);
		}
	
}
