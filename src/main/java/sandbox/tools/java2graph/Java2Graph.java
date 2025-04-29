package sandbox.tools.java2graph;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;



import sandbox.AbstractApplication;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;


/**
 * Java2UML
 * Reference: http://plindenbaum.blogspot.fr/2008/10/javadoc-is-not-enough-java2dia.html
 * prints a java hierarchy for javaclasses to the dot format
 * @author Pierre Lindenbaum
 *
 */
public class Java2Graph extends Launcher
	{
	protected static final Logger LOG=Logger.builder(Java2Graph.class).build();

	/** unique id generator */
	private static int ID_GENERATOR=0;
	/** final printer */
	@com.beust.jcommander.Parameter(
			names= {"-j","--jar"},
			description="one *.jar file or a *.list file with one jar per line."
			)
	private List<File> userJarFiles = null;

	

	/** all the files */
	private ArrayList<File> files=new ArrayList<File>();
	/** all the classes that may be observed */
	private HashSet<ClassWrapper> classes= new HashSet<ClassWrapper>();
	/** all the links between the classes */
	private HashSet<Link> links= new HashSet<Link>();
	/** ignore pattern */
	private ArrayList<Pattern> ignorePattern= new ArrayList<Pattern>();
	private Set<String> ignorePackagesStartingWith= new HashSet<String>();
	
	/** are we using any.any$any classes ? */ 
	@com.beust.jcommander.Parameter(names= {"-d"}, description="ignore declared-classes (classes with $ in the name)")
	private boolean usingDeclaredClasses=true;
	/** are we using interfaces ? */
	@com.beust.jcommander.Parameter(names= {"-i"}, description="ignore interfaces")
	private boolean usingInterfaces=true;
	/** use private inner classes */
	@com.beust.jcommander.Parameter(names= {"-p"}, description="use *private* inner classes")
	private boolean usePrivateDeclaredClasses=false;
	@com.beust.jcommander.Parameter(names= {"-m"}, description="ignore classes iMplementing interfaces")
	private boolean usingClassesImplementingInterfaces=false;
	@com.beust.jcommander.Parameter(names= {"-x"}, description="(int) max distance to classe(s) defined by user. Default: unlimited")
	/** distance max to class targeted by user -1= no restriction*/
	private int limitDistance=-1;
	@com.beust.jcommander.Parameter(names= {"-D"}, description="dot output")
	private boolean dot_flag=false;
	@com.beust.jcommander.Parameter(names= {"-G"}, description="GEXF output")
	private boolean gexf=false;
	@com.beust.jcommander.Parameter(names= {"-r"}, description="<regex> add a pattern of classes to be ignored.")
	private List<String> regexstr=new ArrayList<>();
	@com.beust.jcommander.Parameter(names= {"-r"}, description="<package name> ignore the package starting with this string. Can be used muliple times.")
	private String packageName="";
	@com.beust.jcommander.Parameter(names= {"-o"}, description="output file")
	private Path outputFile=null;
	
	
	private AbstractGraphPrinter graphPrinter=new DotGraphPrinter();
	
	private abstract class AbstractGraphPrinter
		{
		public abstract void print(PrintStream out) throws Exception;
		}
	
	private class GexfPrinter
		extends AbstractGraphPrinter
		{
		XMLStreamWriter w;
		
		
		private void gexfAttDecl(
				String key,
				String type
				)throws XMLStreamException
				{
				w.writeEmptyElement("attribute");
				w.writeAttribute("id", key);
				w.writeAttribute("title", key.replace('_', ' '));
				w.writeAttribute("type", type);
				}
		private void gexfAtt(String key,String value)
				throws XMLStreamException
				{
				if(value==null) return;
				w.writeStartElement("attvalue");
				w.writeAttribute("for", key);
				w.writeAttribute("value", value);
				w.writeEndElement();
				}
		
		@Override
		public void print(PrintStream out) throws Exception
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			this.w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
			
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("gexf");
			w.writeAttribute("xmlns", "http://www.gexf.net/1.2draft");
			w.writeAttribute("xmlns:viz","http://www.gexf.net/1.2draft/viz");
			w.writeAttribute("version", "1.2");
			
			
			/* meta */
			w.writeStartElement("meta");
				w.writeStartElement("creator");
				  w.writeCharacters(Java2Graph.class.getCanonicalName());
				w.writeEndElement();
				w.writeStartElement("description");
				  w.writeCharacters("java2 DOT Graph");
				w.writeEndElement();
			w.writeEndElement();
			
			/* graph */
			w.writeStartElement("graph");
			w.writeAttribute("mode", "static");
			w.writeAttribute("defaultedgetype", "directed");
			
			
			
			/* attributes */
			w.writeStartElement("attributes");
			w.writeAttribute("class","node");
			w.writeAttribute("mode","static");
			gexfAttDecl("simpleName","string");
			gexfAttDecl("canonicalName","string");
			gexfAttDecl("defaultName","string");
			gexfAttDecl("package","string");
			gexfAttDecl("classOrInterface","string");
			w.writeEndElement();//attributes
			
			/* nodes */
			w.writeStartElement("nodes");
			for(ClassWrapper c: Java2Graph.this.classes)
				{
				if(!c.seen) continue;
				if(Java2Graph.this.limitDistance>0 && c.distancdeToUserTarget>Java2Graph.this.limitDistance)
					{
					continue;
					}
				w.writeStartElement("node");
				w.writeAttribute("id", "N"+c.id);
				
				String className=String.valueOf(c.id);
				try 
					{
					className = c.clazz.getSimpleName()==null?
							c.clazz.toString():
								c.clazz.getSimpleName()
								;
					}
				catch(Throwable err) {
					className=String.valueOf(c.id);
				}
				
				w.writeAttribute("label", className );
				
				w.writeEmptyElement("viz:color");
				if(c.isInterface())
					{
					w.writeAttribute("r", "161");
					w.writeAttribute("g", "83");
					w.writeAttribute("b", "83");
					}
				else
					{
					w.writeAttribute("r", "83");
					w.writeAttribute("g", "101");
					w.writeAttribute("b", "161");
					}
				
				w.writeStartElement("attvalues");
				String cn;
				try 
					{
					cn = c.clazz.getSimpleName();
					}
				catch(Throwable err)
					{
					cn = c.toString();
					}
				
				
				gexfAtt("simpleName",cn);
				
				try 
					{
					cn = c.clazz.getCanonicalName();
					}
				catch(Throwable err)
					{
					cn = c.toString();
					}
				
				gexfAtt("canonicalName",cn);
				

				try 
					{
					cn = c.clazz.getName();
					}
				catch(Throwable err)
					{
					cn = c.toString();
					}
				
				gexfAtt("defaultName",cn);

				Package pack=c.clazz.getPackage();
				if(pack==null)
					{
					gexfAtt("package","(default)");
					}
				else
					{
					gexfAtt("package",pack.getName());
					}
				w.writeEndElement();//attvalues
				
				gexfAtt("classOrInterface",c.isInterface()?"interface":"class");
				
				w.writeEndElement();
				}
	
			w.writeEndElement();//nodes
			
			/* edges */
			int relid=0;
			w.writeStartElement("edges");
			for(Link L: Java2Graph.this.links)
				{
				if(!L.from.seen ||	 !L.to.seen ) continue;
				if(Java2Graph.this.limitDistance>0 &&
					(
					L.to.distancdeToUserTarget>Java2Graph.this.limitDistance ||
					L.from.distancdeToUserTarget>Java2Graph.this.limitDistance
					))
					{
					continue;
					}
				
				
				w.writeEmptyElement("edge");
				w.writeAttribute("id", "E"+(++relid));
				w.writeAttribute("type", "directed");
				w.writeAttribute("source","N"+L.from.id);
				w.writeAttribute("target","N"+L.to.id);
				w.writeAttribute("label",L.label.name());
				}
			w.writeEndElement();//edges

			w.writeEndElement();//graph
			
			w.writeEndElement();//gexf
			w.writeEndDocument();
			w.flush();
			
			
			}
		}
	
	private class DotGraphPrinter
		extends AbstractGraphPrinter
		{
		PrintStream out;
		
		public void print(PrintStream out) throws Exception
			{
			LOG.info("printing to dot");
			this.out=out;
			
			out.println("digraph G{");
			
			for(ClassWrapper c: Java2Graph.this.classes)
				{
				if(!c.seen) continue;
				if(Java2Graph.this.limitDistance>0 && c.distancdeToUserTarget>Java2Graph.this.limitDistance)
					{
					continue;
					}
				this.dot(c);
				}
			for(Link L: Java2Graph.this.links)
				{
				this.dot(L);
				}
			out.println("}");
			out.flush();
			}
		private void dot(Link L)
			{
			if(!L.from.seen ||	 !L.to.seen ) return;
			if(Java2Graph.this.limitDistance>0 &&
				(
				L.to.distancdeToUserTarget>Java2Graph.this.limitDistance ||
				L.from.distancdeToUserTarget>Java2Graph.this.limitDistance
				))
				{
				return;
				}
			
			out.print("id"+L.from.id+"->id"+L.to.id+"[");
			switch(L.label)
				{
				case IMPLEMENTS: out.print("color=red,fontcolor=red,arrowType=onormal,"); break;
				case DECLARES: out.print("color=green,fontcolor=green,"); break;
				case SUPER:out.print("color=black,fontcolor=black,arrowType=normal,"); break;
				case HAS_PRIMITIVE_PARAMETER: out.print("color=black,fontcolor=orange,arrowType=normal,"); break;
				default:System.err.println("???? dot type not handled "+L.label);break;
				}
			out.print("label=\""+L.label.name().toLowerCase()+"\"");
			out.println("]");
			}
		
		private void dot(ClassWrapper C)
			{
			out.print("id"+C.id+"[shape=rectangle,style=filled,");
			if(C.isInterface())
				{
				out.println("fillcolor=khaki,");
				}
			else
				{
				out.println("fillcolor=gray77,");
				}
			out.print("label=\""+C.clazz.getName()+"\"");
			out.println("]");
			}
		}

	
	
	
	private static enum Relation
		{
		SUPER,
		IMPLEMENTS,
		DECLARES,
		HAS_PRIMITIVE_PARAMETER
		};
		
		
	/** Wrapper around a java class */
	private class ClassWrapper
		{
		/** unique id */
		int id= (++ID_GENERATOR);
		/** the class observed */
		Class<?> clazz;
		/** did we already processed this class ? */
		private boolean seen=false;
		/** was selected by the user */
		//private boolean userTarget=false;
		/** distance to user Target */
		private int distancdeToUserTarget=Integer.MAX_VALUE;
		
		ClassWrapper(final Class<?> clazz)
			{
			this.clazz=clazz;
			}
		
		@Override
		public int hashCode()
			{
			return this.clazz.hashCode();
			}
		
		@Override
		public boolean equals(Object obj)
			{
			if(obj==this) return true;
			if(obj==null || getClass()!=obj.getClass()) return false;
			return ClassWrapper.class.cast(obj).clazz==this.clazz;
			}
		
		public boolean isInterface()
			{
			return this.clazz.isInterface();
			}
		
		public Set<ClassWrapper> getInterfaces() {
			
			try {
				return findByClasses(this.clazz.getInterfaces());
			} catch (Exception e) {
				return Collections.emptySet();
			}
		}
		
		@Override
		public String toString()
			{
			return this.clazz.getName();
			}
		} 
	
	/**
	 * Defines a Link between to classes
	 * @author lindenb
	 *
	 */
	private static class Link
		{
		private final ClassWrapper from;
		private final ClassWrapper to;
		private final Relation label;
		Link(final ClassWrapper from,final ClassWrapper to,final Relation label)
			{
			this.from=from;
			this.to=to;
			this.label=label;
			}
		
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || getClass()!=obj.getClass()) return false;
			return 	Link.class.cast(obj).from.equals(this.from) &&
					Link.class.cast(obj).to.equals(this.to)
					;
			}
		
		@Override
		public int hashCode() {
			return from.hashCode()*31+to.hashCode();
			}
		
		@Override
		public String toString() {
			return from.toString() +" -["+label+"]-> " +to.toString();
			}
		
		
		
		}
	
	
	
	/** add a file in the list of jar files */
	private void addFile(File jarFile) throws IOException
		{
		if(!jarFile.exists())
			{
			LOG.warning(jarFile.toString()+" doesn't exists");
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
				LOG.info("Adding file "+jarFile);
				this.addFile(fc);
				}
			return;
			}
		
		this.files.add(jarFile);
		}
	
	/** finds a class Wrapper by its name */
	private ClassWrapper findByName(final String s)
		{
		for(final ClassWrapper cw: this.classes)
			{
			if(cw.clazz.getName().equals(s)) return cw;
			}
		try {
			final Class<?> c=Class.forName(s);
			LOG.info("adding class "+c);
			final ClassWrapper cw= new ClassWrapper(c);
			this.classes.add(cw);
			return cw;
		} catch (final Exception e) {
			LOG.warning(s+" not found");
			return null;
			}
		
		
		}
	
	/** finds a class Wrapper by its delegated class */
	private Set<ClassWrapper> findByClasses(Class<?>[] array)
		{
		if(array==null || array.length==0) return Collections.emptySet();
		final Set<ClassWrapper> S=new HashSet<>();
		for(final Class<?> clazz: array)
			{
			ClassWrapper cw = this.findByClass(clazz);
			if(cw !=null) S.add(cw);
			}
		return S;
		}

	
	/** finds a class Wrapper by its delegated class */
	private ClassWrapper findByClass(Class<?> c)
		{
		if(c==null) return null;
		for(ClassWrapper cw: this.classes)
			{
			if(cw.clazz==c) return cw;
			}
		ClassWrapper cw= new ClassWrapper(c);
		this.classes.add(cw);
		return cw;
		}
	

	
	/** workhorse. recursive call for this class wrapper */
	private void recursive(ClassWrapper cw,int distance)
		{
		if(cw==null) return;
		
		for(Pattern pat:this.ignorePattern)
			{
			if(pat.matcher(cw.clazz.getName()).matches())
				{
				return;
				}
			}
		for(String s:this.ignorePackagesStartingWith)
			{
			if((cw.clazz.getName().startsWith(s)))
				{
				break;
				}
			}
		
		if(distance< cw.distancdeToUserTarget) cw.distancdeToUserTarget=distance;
		if(cw.seen) return;
		LOG.info("running for "+cw.clazz);
		HashSet<ClassWrapper> next= new HashSet<ClassWrapper>();
		//System.err.println("Found "+cw);
		cw.seen=true;
		Class<?> c= cw.clazz;
	
		Class<?> parent2= c.getSuperclass();
		if(parent2!=null && parent2!=Object.class)
			{
			ClassWrapper cw2= findByClass(parent2);
			if(cw2!=null)
				{
				this.links.add(new Link(cw,cw2,Relation.SUPER));
				next.add(cw2);
				}
			}
	
		if(usingInterfaces)
			{
			for(Class<?> i:c.getInterfaces())
				{
				ClassWrapper cw2= findByClass(i);
				if(cw2==null) continue;
					
				ClassWrapper cw3= findByClass(parent2);
				if(cw3==null) continue;
				for(Class<?> i2:cw3.clazz.getInterfaces())
					{
					if(i2==i)
						{
						i=null;
						break;
						}
					}
				if(i==null) continue;
				
				Link L=new Link(cw,cw2,
						(c.isInterface()?Relation.SUPER:Relation.IMPLEMENTS));
				this.links.add(L);
				next.add(cw2);
				}
			
			if(usingClassesImplementingInterfaces && c.isInterface())
				{
				for(ClassWrapper cw2:this.classes)
					{
					for(Class<?> i:cw2.clazz.getInterfaces())
						{
						if(i==c)
							{
							Link L=new Link(cw2,cw,Relation.IMPLEMENTS);
							this.links.add(L);
							next.add(cw2);
							}
						}
					}
				}
			
			}
		
		if(usingDeclaredClasses)
			{
			Class<?> subclasses[];
			
			try 
				{
				if(usePrivateDeclaredClasses)
					{
					subclasses=c.getDeclaredClasses();
					}
				else
					{
					subclasses=c.getClasses();
					}
				}
			catch(Throwable err) {
				subclasses=new Class[0];
				}
				
			for(Class<?> d:subclasses)
				{
				ClassWrapper cw2= findByClass(d);
				if(cw2!=null)
					{
					this.links.add(new Link(cw,cw2,Relation.DECLARES));
					next.add(cw2);
					}
				}
			}
		
		
		try {
		Type genericSuperClass = c.getGenericSuperclass();
		if(genericSuperClass!=null && genericSuperClass instanceof ParameterizedType) {
		ParameterizedType parameterizedType=(ParameterizedType)genericSuperClass;
		for(Type paramType:parameterizedType.getActualTypeArguments())
			{
			if(paramType instanceof java.lang.Class) {
				ClassWrapper cw2= findByClass((Class<?>)paramType);
				if(cw2!=null)
					{
					this.links.add(new Link(cw,cw2,Relation.HAS_PRIMITIVE_PARAMETER));
					next.add(cw2);
					}
			}
			else
			System.err.println(paramType+" "+paramType.getClass().toString());
			}
		} } catch(java.lang.NoClassDefFoundError err) {
			//ignore
		}
		
		for(final ClassWrapper child: this.classes)
			{
			Class<?> parent3= child.clazz.getSuperclass();
			if(parent3==null) continue;
			
			if(parent3!=c)
				{
				continue;
				}
			LOG.info("parent of "+child+" is "+parent3);
			ClassWrapper cw3= findByClass(parent3);
			if(cw3!=null)
				{
				Link L=new Link(child,cw,Relation.SUPER);
				this.links.add(L);
				next.add(child);
				}
			}
		for(ClassWrapper cw2:next)
			{
			recursive(cw2,distance+1);
			}
		}
	
	private void run(HashSet<String> setOfClasses) throws IOException
			{
			LOG.info("run for "+setOfClasses);
				
			ArrayList<URL> urls=new ArrayList<URL>();
			for(File f:this.files)
			 	{
				urls.add(f.toURI().toURL()); 
			 	}
			    
			 
		    URLClassLoader cl= new URLClassLoader(urls.toArray(new URL[urls.size()]),ClassLoader.getSystemClassLoader());
		    
		    //loop over each file
		    for(File f:this.files)
		    	{
		    	LOG.info("Scanning "+f);
		    	JarFile jf= new JarFile(f);
		    	Enumeration<JarEntry> e=jf.entries();
		    	//loop over each entry of this jar file
		    	while(e.hasMoreElements())
		    		{
		    		JarEntry je=e.nextElement();
		    		if(!je.getName().endsWith(".class")) continue;
		    		
		    		String className=je.getName();
		    		className=className.substring(0,className.length()-6);
		    		className=className.replace('/','.');
		    		int sub= className.indexOf('$');
		    		if(sub!=-1 && usingDeclaredClasses==false) continue;
		    		//ignore anonymous classes
		    		if(sub!=-1 && Character.isDigit(className.charAt(sub+1))) continue;
		    		
		    		for(Pattern pat:this.ignorePattern)
			    		{
			    		if(pat.matcher(className).matches())
			    			{
			    			je=null;
			    			break;
			    			}
			    		}
		    		for(String s:this.ignorePackagesStartingWith)
		    			{
		    			
		    			if(jf==null) break;
		    			if(className.startsWith(s)) 
		    				{
		    				LOG.info("Ignoring "+className);
			    			je=null;
			    			break;
		    				}
		    			}
		    		if(jf==null) continue;
		    		try
			    		{
			    		Class<?> c=cl.loadClass(className);
			    		//System.err.println(c.getName());
			    		classes.add(new ClassWrapper(c));
			    		}
		    		catch(IllegalAccessError err)
		    			{
		    			LOG.warning("#cannot access \""+className+"\" message:"+err.getMessage());
		    			}
		    		catch(NoClassDefFoundError err)
		    			{
		    			LOG.warning("#class def not found : \""+className+"\" message:"+err.getMessage());
		    			}
		    		catch(ClassNotFoundException err)
		    			{
		    			LOG.warning("#class not found : \""+className+"\" message:"+err.getMessage());
		    			}
		    		}
		    	jf.close();
		    	}
		    cl.close();
		    for(String x: setOfClasses)
			    {
			    ClassWrapper cw=findByName( x );
			    if(x==null)
			    	{
			    	System.err.println("Cannot find class "+x);
			    	continue;
			    	}
			    //cw.userTarget=true;
			    recursive(cw,0);
			    }
			}
	
	
	@Override
	public int doWork(List<String> args) {
		try {
			File output=null;

				if(this.gexf)
					{
					this.graphPrinter=new GexfPrinter();
					}
				
				if(this.dot_flag)
					{
					this.graphPrinter=new DotGraphPrinter();
					}
				
					
				
				for(final String s:this.regexstr)
					{
					this.ignorePattern.add(Pattern.compile(s));
					}
					
				
			
		    if(args.isEmpty())
		    	{
		    	LOG.error("classes missing");
		    	return -1;
		    	}
		    final HashSet<String> setOfClasses=new HashSet<String>();
		    for(String className:args)
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
		   this.run(setOfClasses);
		  LOG.info("COUNT(Classes) : "+this.classes.size());
		  LOG.info("COUNT(LINKS) : "+this.links.size());

			  
		   try(PrintStream out = IOUtils.openPathAsPrintStream(this.outputFile)) {
		    graphPrinter.print(out);
		    out.flush();
			}
		    return 0;
		} catch (final Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static void main(String[] args)
		{
		new Java2Graph().instanceMainWithExit(args);
		}
	
}
