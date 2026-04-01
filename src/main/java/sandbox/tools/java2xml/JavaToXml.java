package sandbox.tools.java2xml;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.lang.StringUtils;
import sandbox.lang.reflect.Primitive;
import sandbox.tools.central.ProgramDescriptor;

public class JavaToXml extends Launcher {

@Parameter(names= { "-o"},description=OUTPUT_OR_STANDOUT)
private Path output=null;
@Parameter(names= { "--package","-p"},description="package")
private String packageName="generated";
@Parameter(names= { "--name"},description="class Name")
private String className="MyConstructor";
@Parameter(names= { "--super"},description="use super classes")
private boolean use_super_class=false;


private String unJava(String s) {
	if(s.length()>1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) return s; /** setURL */
	if(Character.isLowerCase(s.charAt(0))) return s;
	return s.substring(0,1).toLowerCase()+(s.length()>1?s.substring(1):"");
}

private String getScope(int modifier) {
	if(Modifier.isPrivate(modifier)) return "private";
	if(Modifier.isPublic(modifier)) return "public";
	if(Modifier.isProtected(modifier)) return "protected";
	return "package";
	}

private String argName(Class<?> clazz,Set<String> seen) {
	int i=1;
	String suffix="";
	for(;;) {
		String s ;
		final Optional<Primitive> primitive = Primitive.findPrimitiveByTypeOrBoxed(clazz);
		if(clazz.isArray()) {
			s="array";
			}
		else if(primitive.isPresent()) {
			s = primitive.get().getBoxedType().getSimpleName().toLowerCase()+"_value";
			}
		else
			{
			s=unJava(clazz.getSimpleName());
			}
	
		s+=suffix;
		if(seen.contains(s)) {
			i++;
			suffix=String.valueOf(i);
			continue;
			}
		seen.add(s);
		return s;
		}
	}
private void exceptions(XMLStreamWriter w,java.lang.Class<?>[] classes) throws Exception {
	if(classes.length==0) return;
	w.writeStartElement("exceptions");
	for(java.lang.Class<?> c:classes) {
		w.writeEmptyElement("exception");
		w.writeAttribute("name", c.getTypeName());
		}
	w.writeEndElement();
}


private void parameters(XMLStreamWriter w,java.lang.reflect.Parameter[] params) throws Exception {
	Set<String> seen=new HashSet<>();
	w.writeStartElement("parameters");
	w.writeAttribute("arity", String.valueOf(params.length));
	int index=0;
	for(java.lang.reflect.Parameter param:params) {
		w.writeStartElement("parameter");
		w.writeAttribute("index", String.valueOf(index));
		w.writeAttribute("name", argName(param.getType(),seen));
		w.writeAttribute("type", param.getType().getTypeName());
		
		final Optional<Primitive> primitive = Primitive.findPrimitiveByType(param.getType());
		if(primitive.isPresent()) {
			w.writeAttribute("class", primitive.get().getTypeName());
			}
		
		w.writeEndElement();
		index++;
		}
	w.writeEndElement();
}

private void run(XMLStreamWriter w,final Class<?> clazz) throws Exception {
	String className= clazz.getName();
	final String simpleName=clazz.getSimpleName();
	
	w.writeStartElement("class");
	w.writeAttribute("name", className);
	w.writeAttribute("package", clazz.getPackageName());
	w.writeAttribute("simpleName", simpleName);
	w.writeAttribute("scope", getScope(clazz.getModifiers()));
	w.writeAttribute("final", String.valueOf(Modifier.isFinal(clazz.getModifiers())));
	w.writeAttribute("abstract", String.valueOf(Modifier.isAbstract(clazz.getModifiers())));
	if(!clazz.getSuperclass().equals(Object.class)) w.writeAttribute("extends", clazz.getSuperclass().getTypeName());
	
	w.writeStartElement("constructors");
	for(Constructor<?> ctor:clazz.getConstructors()) {
		w.writeStartElement("constructor");
		w.writeAttribute("scope", getScope(ctor.getModifiers()));
		if(Modifier.isPublic(ctor.getModifiers()) && ctor.getParameterCount()==0) {
			w.writeAttribute("default", "true");
			}

		parameters(w,ctor.getParameters());
		exceptions(w,ctor.getExceptionTypes());
		w.writeEndElement();
		}
	w.writeEndElement();
	
	w.writeStartElement("super-class");
	Class<?> parent=clazz.getSuperclass();
	while(!parent.equals(Object.class)) {
		w.writeEmptyElement("extends");
		w.writeAttribute("class", parent.getTypeName());
		parent=parent.getSuperclass();
		}
	w.writeEndElement();
	
	w.writeStartElement("interfaces");
	for(Class<?> itf: clazz.getInterfaces()) {
		w.writeEmptyElement("implements");
		w.writeAttribute("class", itf.getTypeName());
		if(itf.getDeclaringClass()!=null) {
			w.writeAttribute("declared-class",itf.getDeclaringClass().getTypeName());
			w.writeAttribute("declared",String.valueOf(itf.getDeclaringClass().equals(clazz)));
			}
		}
	w.writeEndElement();

	
	w.writeStartElement("fields");
	for(Field field:clazz.getFields()) {
		w.writeStartElement("field");
		w.writeAttribute("name", field.getName());
		w.writeAttribute("type", field.getType().getTypeName());

		w.writeAttribute("scope",getScope(field.getModifiers()));
		w.writeAttribute("final", String.valueOf(Modifier.isFinal(field.getModifiers())));
		w.writeAttribute("static", String.valueOf(Modifier.isStatic(field.getModifiers())));

		
		
		w.writeAttribute("declared-class",field.getDeclaringClass().getTypeName());
		w.writeAttribute("declared",String.valueOf(field.getDeclaringClass().equals(clazz)));

		w.writeEndElement();
		}
	w.writeEndElement();
	
	
	w.writeStartElement("methods");

	for(Method m:clazz.getMethods()) {
		w.writeStartElement("method");
		w.writeAttribute("name", m.getName());
		w.writeAttribute("return", m.getReturnType().getTypeName());
		w.writeAttribute("static", String.valueOf(Modifier.isStatic(m.getModifiers())));
		w.writeAttribute("scope",getScope(m.getModifiers()));
		w.writeAttribute("native", String.valueOf(Modifier.isNative(m.getModifiers())));
		w.writeAttribute("synchronized", String.valueOf(Modifier.isSynchronized(m.getModifiers())));
		w.writeAttribute("final", String.valueOf(Modifier.isFinal(m.getModifiers())));
		w.writeAttribute("declared-class",m.getDeclaringClass().getTypeName());
		w.writeAttribute("declared",String.valueOf(m.getDeclaringClass().equals(clazz)));

		
		if(!Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
			if(m.getReturnType().equals(Void.TYPE) &&
				m.getParameterCount()==1 &&
				m.getName().startsWith("set") &&
				m.getName().length()>3 &&
				Character.isUpperCase(m.getName().charAt(3))) {
				w.writeAttribute("setter","true");
				w.writeAttribute("setter-name",unJava(m.getName().substring(3)));
				}
			if(m.getParameterCount()==0 && !m.getReturnType().equals(Void.TYPE))
				{
				boolean is_boolean= m.getReturnType().equals(Boolean.TYPE) || m.getReturnType().equals(Boolean.class);
				String name2="";
				if(is_boolean && m.getName().startsWith("is") && m.getName().length()>2) {
					name2=m.getName().substring(2);
					}
				else if(!is_boolean && m.getName().startsWith("get") && m.getName().length()>3) {
					name2=m.getName().substring(3);
					}
				if(!name2.isEmpty() && Character.isUpperCase(name2.charAt(0)))
					{
					w.writeAttribute("getter","true");
					w.writeAttribute("getter-name",unJava(name2));
					}
				}
			}
		exceptions(w,m.getExceptionTypes());
		parameters(w,m.getParameters());
		w.writeEndElement();
		}
	w.writeEndElement();
	
	w.writeEndElement();
	}

private Set<Class<?>> superOf(Class<?> c) {
	final Set<Class<?>> set = new HashSet<>();
	while(!c.equals(Object.class)) {
		set.add(c);
		c=c.getSuperclass();
		}
	return set;
}

@Override
public int doWork(List<String> args) {
	try {
		final Set<Class<?>> classes=new HashSet<>();
		for(String s:args.stream().flatMap(S->Arrays.stream(S.split("[ ,\t]"))).
			filter(S->!StringUtils.isBlank(S)).
			collect(Collectors.toSet())) {
			Class<?> c= Class.forName(s);
			if(Modifier.isInterface(c.getModifiers())) continue;
			if(Modifier.isAbstract(c.getModifiers())) continue;
			if(!Modifier.isPublic(c.getModifiers())) continue;
			classes.add(c);
			if(use_super_class) {
				classes.addAll(superOf(c));
				}
			}
		
		
		XMLOutputFactory xof=XMLOutputFactory.newDefaultFactory();
		final Set<String> packages = classes.stream().map(C->C.getPackageName()).collect(Collectors.toSet());
		
		try(OutputStream out1 = super.openPathAsOuputStream(output)) {
			XMLStreamWriter w=xof.createXMLStreamWriter(out1,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("packages");
			for(final String packname:packages) {
				w.writeStartElement("package");
				w.writeAttribute("name", packname);
				w.writeStartElement("classes");
				for(Class<?> c:classes) {
					if(!c.getPackageName().equals(packname)) continue;
					run(w,c);
					}
				w.writeEndElement();
				w.writeEndElement();
			}
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			}
		return 0;
		}
	catch(Throwable err) {
		err.printStackTrace();
		return -1;
		}
	}
public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "java2xml";
			}
		};
	}
public static void main(String[] args) {
	new JavaToXml().instanceMainWithExit(args);
	}

}
