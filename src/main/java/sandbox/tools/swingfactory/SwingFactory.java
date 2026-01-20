package sandbox.tools.swingfactory;

import java.awt.Dimension;
import java.awt.Point;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.IndentWriter;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class SwingFactory extends Launcher {
@Parameter(names= { "-o"},description=OUTPUT_OR_STANDOUT)
private Path output=null;
@Parameter(names= { "--package","-p"},description="package")
private String packageName="generated";
@Parameter(names= { "--name"},description="class Name")
private String className="SwingFactories";
	
public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return SwingFactory.class.getSimpleName().toLowerCase();
			}
		};
	}

private String argName(Class<?> c) {
	String s=c.getSimpleName().toLowerCase();
	if(s.equals("string")) return "s";
	if(s.equals("boolean")) return "b";
	if(s.equals("int")) return "i";
	if(s.equals("double")) return "f";
	return s;
	
}

private void run(IndentWriter out,Class<?> clazz) throws Exception {
	String className= clazz.getName();
	final String simpleName=clazz.getSimpleName();
	final String factoryName= simpleName+"Factory";
	out.push();
	out.println("public static class "+factoryName+" implements java.util.function.Supplier<"+className+"> {");
	out.push();
	out.println("private final "+className+" object;");
	out.println("private "+factoryName+"(final "+className+" object) { this.object = object;}");	
	
	for(Method m:clazz.getMethods()) {
		if(Modifier.isStatic(m.getModifiers())) continue;
		if(!Modifier.isPublic(m.getModifiers())) continue;
		if(!m.getReturnType().equals(Void.TYPE)) continue;
		String fname=m.getName();
		if(!fname.startsWith("set")) continue;
		fname=fname.substring(3);
		fname=fname.substring(0,1).toLowerCase()+fname.substring(1);
		if(m.getParameterCount()!=1) continue;
		final java.lang.reflect.Parameter param= m.getParameters()[0];
		
		out.println("public "+factoryName+" "+fname+"(final "+param.getType().getTypeName()+" "+argName(param.getType())+") { this.object."+m.getName()+"("+argName(param.getType())+"); return this;}");
		if(param.getType().equals(Dimension.class)) {
			out.println("public "+factoryName+" "+fname+"(int w,int h) { return this."+fname+"(new java.awt.Dimension(w,h));}");
		}
		if(param.getType().equals(Point.class)) {
			out.println("public "+factoryName+" "+fname+"(int x,int y) { return this."+fname+"(new java.awt.Point(x,y));}");
		}
		
	}
	
	out.println("@Override public "+className+" "+"get() { return this.object;}");

	out.pop();
	out.println("}");
	
	
	for(Constructor<?> ctor:clazz.getConstructors()) {
		if(!Modifier.isPublic(ctor.getModifiers())) continue;
		out.print("public static "+factoryName+" make"+simpleName+"(");
		out.print(Arrays.stream(ctor.getParameters()).map(C->"final "+C.getParameterizedType().getTypeName()+" "+C.getName()).collect(Collectors.joining(", ")));
		out.println(") {");
		out.push();
		out.print("return new "+factoryName+"(new "+className+"(");
		out.print(Arrays.stream(ctor.getParameters()).map(C->C.getName()).collect(Collectors.joining(", ")));
		out.println("));");
		out.pop();
		out.println("}");
		}

	
	out.pop();
	}
@Override
public int doWork(List<String> args) {
	try {
		List<Class<?>> classes=new ArrayList<>();
		for(String s:args.stream().flatMap(S->Arrays.stream(S.split("[ ,\t]"))).
			filter(S->!StringUtils.isBlank(S)).
			collect(Collectors.toSet())) {
			Class<?> c= Class.forName(s);
			classes.add(c);
			}
		try(PrintWriter out1 = super.openPathAsPrintWriter(output)) {
			try(IndentWriter out=new IndentWriter(out1, "    ")) {
				out.println("package "+this.packageName+";");
				out.print("public class "+this.className+"{");
				for(Class<?> c: classes) {
					run(out,c);
					}
				out.print("}");
				out.flush();
				}
			}
		return 0;
		}
	catch(Throwable err) {
		err.printStackTrace();
		return -1;
		}
	}

public static void main(String[] args) {
	new SwingFactory().instanceMainWithExit(args);
	}
}
