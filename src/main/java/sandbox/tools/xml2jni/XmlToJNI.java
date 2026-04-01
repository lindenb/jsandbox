package sandbox.tools.xml2jni;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;


import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.ArchiveFactory;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class XmlToJNI extends Launcher {
	
	@Parameter(names= {"-o","--output"},description=ArchiveFactory.OPT_DESC,required = true)
	private Path archiveFile = null;
	@Parameter(names= {"-p","--package"},description="Default package name")
	private String defaultPackageName = "generated";

	private ArchiveFactory archive;
	private final Map<String,Element> id2element = new HashMap<>();
	private final Set<JavaClass> generated = new LinkedHashSet<>();

	private class JavaClass {
		private String packageName;
		private String javaName;
		JavaClass(final String packageName,String javaClass) {
			this.packageName = packageName;
			this.javaName = javaClass;
			}
		@Override
		public int hashCode() {
			return Objects.hash(packageName,javaName);
			}
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof JavaClass)) return false;
			JavaClass o= JavaClass.class.cast(obj);
			return o.packageName.equals(this.packageName) && o.javaName.equals(this.javaName);
			}
		
		private String getBaseCPath() {
			return "src/main/c/" + packageName.replace("/", ".")+"_"+javaName;
			}
		
		public String getCPath() {
			return getBaseCPath()+".c";
			}
		public String getHPath() {
			return getBaseCPath()+".h";
			}
		public String getOPath() {
			return getBaseCPath()+".o";
			}
		
		public String getJavaPath() {
			return "src/main/java/" + packageName.replace("/", ".")+"/"+javaName+".java";
			}
		private PrintWriter openJavaSourceForWriting() throws IOException {
			return archive.openWriter(getJavaPath());
			}
		private PrintWriter openCSourceForWriting() throws IOException {
			return archive.openWriter(getCPath());
			}
		}
	
	private static class Type {
		
		}
	private static class VoidType extends Type {
		
		}
	
	
	private static class Argument {
		String name;
		Type type;
		Argument(final String name, final Type type) {
			this.name = name;
			this.type = type;
			}
	}
	
	private static class Function {
		final String name;
		Type returnType = new VoidType();
		List<Argument> arguments = new ArrayList<>();
		Function(final String name) {
			this.name = name;
			}
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
		return new ProgramDescriptor() {
			@Override
			public String getName() {
				return "xml2jni";
				}
			};
		}
	private void printCopyright(PrintWriter pw) {
		
		}
	

	
	
	
	private String getpackageName(final Element root) {
		return this.defaultPackageName;
		}
	
	private Type parseType(final Element root) {
		if(root.getNodeName().equals("type")) {
			
			}
		else if(root.hasAttribute("type")) {
			
			}
		else
			{
			
			}
		return new VoidType();
		}
	private Function processFunction(final JavaClass owner,final Element root) throws IOException {
		
		Function fun= new Function( StringUtils.assertNoBlank(root.getAttribute("name")));
		for(Node c=root.getFirstChild();c!=null; c=c.getNextSibling()) {
			if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E=Element.class.cast(c);
			if(E.getNodeName().equals("return")) {
				fun.returnType = parseType(E);
				}
			else if(E.getNodeName().equals("param")) {
				Type t=parseType(E);
				Argument a = new Argument(E.getAttribute("name"),t);
				fun.arguments.add(a);
				}
			}
		return fun;
		}
	
	private void processClass(final Element root) throws IOException {
		final String className =  StringUtils.assertNoBlank(root.getAttribute("name"));
		final String packageName = getpackageName(root);
		final JavaClass clazz = new JavaClass(packageName,className);
		this.generated.add(clazz);
		final List<Function> functions = new ArrayList<>();
		for(Node c=root.getFirstChild();c!=null; c=c.getNextSibling()) {
			if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E=Element.class.cast(c);
			if(E.getNodeName().equals("function")) {
				functions.add(processFunction(clazz,E));
				}
			}
		}
	
	private void processEnum(final Element root) throws IOException {
		final Set<String> item_names= new LinkedHashSet<>();
		for(Node c=root.getFirstChild();c!=null; c=c.getNextSibling()) {
			if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E=Element.class.cast(c);
			if(E.getNodeName().equals("item")) {
				String attName =StringUtils.assertNoBlank(E.getAttribute("name"));
				item_names.add(attName);
				}
			}
		
		
		final String className =  StringUtils.assertNoBlank(root.getAttribute("name"));
		final String packageName = getpackageName(root);
		final JavaClass clazz = new JavaClass(packageName,className);
		this.generated.add(clazz);
		
		

		
		try(PrintWriter w = clazz.openJavaSourceForWriting()) {
			printCopyright(w);
			w.println("package "+packageName+";");
			w.println("public enum "+className+" {");
			
			w.println(item_names.stream()
				.map(S->S+"("+className+".C_"+S+"())")
				.collect(Collectors.joining(",\n")) +";"
				);
			
			
			
			w.println("private final int value;");
			w.println(className+"(final int v) {");
			w.println("    this.value = v;");
			w.println("}");
			w.println("public int intValue() {");
			w.println("    return this.value;");
			w.println("}");
			
			for(String item:item_names) {
				w.println("static native int C_"+item+"();");
				}
			
			w.println("}");
			}
		try(PrintWriter w = clazz.openCSourceForWriting()) {
			printCopyright(w);
			w.println("#include \"config.h\"");
			w.println("#include \""+packageName+"_"+className+".h\"");
			for(String item:item_names) {
				w.println(" jint Java_"+packageName+"_"+className+"_C_1"+item+"(JNIEnv* _env, jclass _ckass) {");
				w.println("  return "+item+";");
				w.println("  }");
				w.println();
				}
			}
		
		}

	private void collectIds(Node n) {
		if(n==null) return;
		if(n.getNodeType()==Node.ELEMENT_NODE) {
			final Element E = Element.class.cast(n);
			if(E.hasAttribute("id")) {
				String id = E.getAttribute("id");
				if(this.id2element.containsKey(id)) throw new IllegalArgumentException("duplicate id "+id);
				id2element.put(id,E);
			}
		}
		for(Node c=n.getFirstChild();c!=null; c=c.getNextSibling()) {
			collectIds(c);
			}
		}
	
	private void makeMakefile() throws IOException {
		try(PrintWriter pw=archive.openWriter("Makefile")) {
			pw.println("SHELL=/bin/bash");
			pw.println(".PHONY=compile_java compile_c");
			pw.println("CC?=gcc");
			pw.println("CFLAGS=-O3 -Wall  -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE");
			pw.println("ARCH=$(shell arch)");
			
			pw.println("ifeq (${JAVA_HOME},)");
			pw.println("$(error $${JAVA_HOME} is not defined)");
			pw.println("endif");
			pw.println("JAVAC=${JAVA_HOME}/bin/javac");
			pw.println("## find path where to find include files");
			pw.println("JDK_JNI_INCLUDES?=$(addprefix -I,$(sort $(dir $(shell find ${JAVA_HOME}/include -type f -name \"*.h\"))))");
			
			pw.println("ifeq (${JDK_JNI_INCLUDES},)");
			pw.println("$(error Cannot find C header files under $${JAVA_HOME})");
			pw.println("endif");

			pw.println("## see https://github.com/lindenb/jbwa/pull/5");
			pw.println("ifeq (${OSNAME},Darwin)");
			pw.println("native.extension=jnilib");
			pw.println("else");
			pw.println("native.extension=so");
			pw.println("endif");
			
			
			pw.println("compile_c : compile_java "+generated.stream().map(C->C.getOPath()).collect(Collectors.joining(" ")));
			pw.println("\techo $^");

			
			for(JavaClass c: this.generated) {
				pw.println(c.getOPath()+" : "+c.getCPath()+" "+c.getHPath());
				pw.println("\t$(CC) -c $(CFLAGS) -o $@ -fPIC  $(JDK_JNI_INCLUDES)  $<");
				
				pw.println(c.getHPath() +" : compile_java");
				}
			
			
			
			pw.println("\ttouch -c $@");
			pw.println("compile_java : "+ generated.stream().map(C->C.getJavaPath()).collect(Collectors.joining(" ")));
			pw.println("\tmkdir -p TMP");
			pw.println("\t$(JAVAC) -d TMP -h src/main/c -sourcepath src/main/java $^");
			pw.println("clean:");
			pw.println("\trm -rf TMP");
			}
		}
	
	@Override
	public int doWork(final List<String> args) {
		try {	
			final DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			factory.setExpandEntityReferences(true);
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);
			final DocumentBuilder builder=factory.newDocumentBuilder();
			final Document dom;
			final String input = oneFileOrNull(args);
			if(input==null)
				{
				dom=builder.parse(System.in);
				}
			else
				{
				dom=builder.parse(new File(input));
				}
			collectIds(dom);
			final Element root= dom.getDocumentElement();
			try(ArchiveFactory a= ArchiveFactory.open(this.archiveFile)) {
				this.archive = a;
				for(Node c=root.getFirstChild();c!=null; c=c.getNextSibling()) {
					if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
					Element E=Element.class.cast(c);
					System.err.println("("+E.getNodeName()+")");
					if(E.getNodeName().equals("enum")) {
						processEnum(E);
						}
					else if(E.getNodeName().equals("class")) {
						processClass(E);
						}
					}
				makeMakefile();
				}
			
			return 0;
			}
		catch(Throwable error)
			{
			error.printStackTrace();
			return -1;
			}
		}

	public static void main(String[] args)
		{
		new XmlToJNI().instanceMainWithExit(args);
		}
	}
