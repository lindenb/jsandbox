package sandbox.tools.jaxb2java;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.apache.jena.atlas.io.IndentedWriter;

import sandbox.Launcher;
import sandbox.io.IndentWriter;
import sandbox.io.PrefixSuffixWriter;
import sandbox.ncbi.gb.GBFeaturePartial5;
import sandbox.ncbi.gb.GBSet;
import sandbox.ncbi.gbc.INSDSeq;

public class JaxbToJava extends Launcher{
	private IndentWriter w;
	final Set<Class<?>> seen= new HashSet<>();
	final Set<Class<?>> toBeDone= new HashSet<>();
	
	private abstract class AbstractField<T extends  java.lang.annotation.Annotation> {
		protected final Field field;
		AbstractField(final  Field field) {
			this.field=field;
			}
		public String isA(String varName) {
			return varName+".getLocalPart().equals(\""+getLocalName()+"\")";
			}
		public String getSetter() {
			return setter(getLocalName());
			}
		public String getGetter() {
			return getter(getLocalName());
			}
		public abstract Class<?> getRange();
		public abstract String getNamespaceURI();
		public abstract String getLocalName();
		public abstract T getAnnotation();
		public boolean isAttribute() { return false;}
		public boolean isElement() { return false;}
		public boolean isValue() { return false;}
		public String decodeString(String varName) {
			if(getRange().equals(String.class)) {
				return varName;
				}
			else if(getRange().equals(Integer.class)) {
				return "Integer.parseInt("+varName+")";
				}
			else if(getRange().equals(Double.class)) {
				return "Double.parseDouble("+varName+")";
				}
			throw new IllegalArgumentException(""+getRange());
			}
		}
	
	private class FieldAsAttribute extends AbstractField<XmlAttribute> {
		FieldAsAttribute(final  Field field) {
			super(field);
			}
		public Class<?> getRange() {
			return field.getType();
			}
		

		@Override
		public String getLocalName() {
			return getAnnotation().name();
			}
		@Override
		public String getNamespaceURI() {
			return getAnnotation().namespace();
			}
		@Override
		public XmlAttribute getAnnotation() {
			return this.field.getAnnotation(XmlAttribute.class);
			}
		@Override
		public boolean isAttribute() {
			return true;
		}
		
		public void read(IndentWriter w) {
			w.println("if("+isA("qName")+") {");
			w.push();
			w.println("final String attValue = att.getValue();");
			w.println("instance."+getSetter()+"("+decodeString("attValue")+");");
			w.pop();
			w.print("}");
			}
		public void write(IndentWriter w) {
			w.println("attValue = root."+getGetter()+"();");
			w.println("if(attValue!=null) w.writeAttribute(\""+getAnnotation().name()+"\",String.valueOf(attValue));");
			}
	}
	private class FieldAsElement extends AbstractField<XmlElement> {
		FieldAsElement(final  Field field) {
			super(field);
			}
		public boolean isList() {
			Class<?> fieldType0 = field.getType();
			return fieldType0.isAssignableFrom(List.class);
			}
		@Override
		public Class<?> getRange() {
			Class<?> fieldType0 = field.getType();
			if(fieldType0.isAssignableFrom(List.class)) {
				ParameterizedType pt = (ParameterizedType) field.getGenericType();
				return (Class<?>)(pt.getActualTypeArguments()[0]);
				}
			else
				{
				return fieldType0;
				}
			}
		@Override
		public String getNamespaceURI() {
			return getAnnotation().namespace();
			}
		@Override
		public String getLocalName() {
			return getAnnotation().name();
			}
		@Override
		public XmlElement getAnnotation() {
			return this.field.getAnnotation(XmlElement.class);
			}
		@Override
		public boolean isElement() {
			return true;
			}
		void read(IndentWriter w) {
			w.println("if("+isA("E.getName()")+") {");
			w.push();
			w.print("final "+getRange().getName()+" varx = ");
			if(getRange().equals(String.class)) {
				w.println("r.getElementText();");
			} else {
				w.println("parse"+getRange().getSimpleName()+"(r,se);");
			}
			
			if(isList()) {
				w.println("instance."+getGetter()+"().add(varx);");
				}
			else
				{
				w.println("instance."+getSetter()+"(varx);");
				}
			w.pop();
			w.println("}");
			}
		void write(IndentWriter w) {
			if(isList()) {
				w.println("for("+getRange().getName()+" item: root."+getGetter()+"()) {");
				w.push();
				if(getRange().equals(String.class)) {
					w.println("w.writeStartElement(\""+getLocalName() +"\");");
					w.println("w.writeCharacters(item);");
					w.println("w.writeEndElement();");
					}
				else
					{
					w.println("write"+javaName(getRange().getSimpleName())+"(w,item);");
					}
				w.pop();
				w.println("}");
				}
			else if(getRange().equals(String.class))
				{
				w.println("w.writeStartElement(\""+getLocalName() +"\");");
				w.println("w.writeCharacters(root."+getGetter()+"());");
				w.println("w.writeEndElement();");
				}
			else
				{
				w.println("write"+javaName(getAnnotation().name())+"(w,root."+getGetter()+"());");
				}
			}
	}
	
	private class FieldAsValue extends AbstractField<XmlValue> {
		FieldAsValue(final  Field field) {
			super(field);
			}
		@Override
		public String getLocalName() {
			return "";
			}
		@Override
		public String getNamespaceURI() {
			return "";
			}
		@Override
		public XmlValue getAnnotation() {
			return this.field.getAnnotation(XmlValue.class);
			}
		@Override
		public String getGetter() {
			return "get"+field.getName();
			}
		@Override
		public String getSetter() {
			return "set"+field.getName();
			}
		@Override
		public boolean isValue() {
			return true;
			}
		@Override
		public Class<?> getRange() {
			return this.field.getType();
			}
		void read(IndentWriter w) {
			w.println("instance."+getSetter()+"(evt.asCharacters().getData());");
			}
		void write(IndentWriter w) {
			w.println("w.writeCharacters(root."+getGetter()+"());");
			}
	}
	
	private String javaName(final String s0) {
		String s="";
		for(int i=0;i< s0.length();i++) {
			if(i==0) {
				s+=s0.substring(0, 1).toUpperCase();
				}
			else if(i+1 < s0.length() && (s0.charAt(i)=='-' || s0.charAt(i)=='_')) {
				i++;
				s+=s0.substring(i,i+1).toUpperCase();
				}
			else
				{
				s+= s0.charAt(i);
				}
			}
		return s;
	}
	
	private String setter(final String s) {
		return "set"+javaName(s);
	}
	
	private String getter(final String s) {
		return "get"+javaName(s);
	}

	
	private List<AbstractField<? extends Annotation>> getFields(final Class<?> clazz) throws Exception {
		 List<AbstractField<? extends Annotation>> L = new ArrayList<>();
		 for(Field field: clazz.getDeclaredFields()) {
				field.setAccessible(true);
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
				final XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
				if(xmlAttribute!=null) {
					L.add(new FieldAsAttribute(field));
					continue;
					}
				XmlElement xmlElement = field.getAnnotation(XmlElement.class);
				if(xmlElement!=null){
					L.add(new FieldAsElement(field));
					continue;
					}
				XmlValue xmlValue = field.getAnnotation(XmlValue.class);
				if(xmlValue!=null){
					L.add(new FieldAsValue(field));
					continue;
					}
				}
		return L;
		}
	
	private void doXmlRoot(final Class<?> clazz) throws Exception {
		if(seen.contains(clazz)) return;
		seen.add(clazz);
		XmlRootElement xmlRootElement =clazz.getAnnotation(XmlRootElement.class);
		if(xmlRootElement==null) throw new IllegalArgumentException("no XMLroot for "+clazz);
		final List<AbstractField<? extends Annotation>> fields = getFields(clazz);
		w.push();
		w.println("public "+clazz.getName()+" parse"+clazz.getSimpleName()+"(final XMLEventReader r,final StartElement se) throws XMLStreamException {");
		w.push();
		w.println("final QName qName = se.getName();");
		w.println("if(!isA(qName,\""+ xmlRootElement.namespace()+"\",\""+xmlRootElement.name()+""+"\")) throw new XMLStreamException(\"\");");
		w.println("final "+clazz.getName()+" instance = new "+clazz.getName()+"();");

		
		w.println("for(Iterator<Attribute> iter= se.getAttributes(); iter.hasNext();) {");
		w.push();
		w.println("final Attribute att = iter.next();");
		String elsestr="";
		for(FieldAsAttribute field: fields.stream().filter(F->F.isAttribute()).map(F->FieldAsAttribute.class.cast(F)).collect(Collectors.toList())) {
			w.print(elsestr);
			field.read(w);
			elsestr="else ";
			}
		w.println(elsestr+"{");
		w.push();
		w.println("System.err.println(\"undefined attribute :\"+att);");
		w.pop();
		w.println("}");
		w.pop();
		w.println("}");

		
		
		
		w.println("while (r.hasNext()) {");
		w.println("final XMLEvent evt= r.nextEvent();");
		w.println("if(evt.isStartElement()) {");
		w.push();
		w.println("final StartElement E = evt.asStartElement();");
	
		
		elsestr="";
		for(FieldAsElement field: fields.stream().filter(F->F.isElement()).map(F->FieldAsElement.class.cast(F)).collect(Collectors.toList())) {
			field.read(w);
			elsestr="else ";
			if(toBeDone.add(field.getRange()));
			}
		
		
		
		w.println(elsestr+" {");
		w.println("unknownElement(se);");
		w.println("}");
		
		
		w.pop();
		w.println("	}");
		w.println("else if(evt.isEndElement()) {");
		w.println("    return instance;");
		w.println("}");
		w.println("else if(evt.isCharacters()) {");
		FieldAsValue fieldAsValue= fields.stream().filter(F->F.isValue()).map(F->FieldAsValue.class.cast(F)).findFirst().orElse(null);
		if(fieldAsValue!=null){
			w.push();
			fieldAsValue.read(w);
			w.pop();
			}
		else
			{
			w.push();
			w.println("final String textContent = evt.asCharacters().getData().trim();");
			w.println("if(!textContent.isEmpty()) throw new XMLStreamException(\"unecpedted content\", evt.getLocation());");
			w.pop();
			}
		w.println("	}");
		
		
	
		w.println("} // end of loop");
		w.println("throw new XMLStreamException(\"boum\");");
		w.pop();
		w.println("}");
		w.pop();
		
		
		w.push();
		w.println("public void write"+clazz.getSimpleName()+"(final XMLStreamWriter w,final "+clazz.getName()+" root) throws XMLStreamException {");
		w.push();
		w.println("w.writeStartElement(\""+xmlRootElement.name()+"\");");
		if(fields.stream().anyMatch(F->F.isAttribute())) {
			w.println("Object attValue=null;");
			}
		for(FieldAsAttribute field: fields.stream().filter(F->F.isAttribute()).map(F->FieldAsAttribute.class.cast(F)).collect(Collectors.toList())) {
			field.write(w);
			}

		
		for(FieldAsElement field: fields.stream().filter(F->F.isElement()).map(F->FieldAsElement.class.cast(F)).collect(Collectors.toList())) {
			field.write(w);
			}
		
		if(fieldAsValue!=null) {
			fieldAsValue.write(w);
			}
		
		w.println("w.writeEndElement();");
		w.pop();
		w.println("}");
		w.pop();
		}
	
	@Override
	public int doWork(List<String> args) {
		try  {
			seen.add(String.class);
			seen.add(Integer.class);
			seen.add(List.class);
			seen.add(Long.class);
			seen.add(Boolean.class);
			this.w=new IndentWriter(this.openPathAsPrintWriter(Paths.get("/home/lindenb/src/jsandbox/src/sandbox/tools/jaxb2java/BlastFactory.java")));
			
			this.w.println("package sandbox.tools.jaxb2java;");
			this.w.println("import java.util.Iterator;");
			this.w.println("import java.util.List;");
			this.w.println("import javax.xml.namespace.QName;");
			this.w.println("import javax.xml.stream.XMLStreamException;");
			this.w.println("import javax.xml.stream.XMLEventReader;");
			this.w.println("import javax.xml.stream.XMLStreamWriter;");
			this.w.println("import javax.xml.stream.events.StartElement;");
			this.w.println("import javax.xml.stream.events.EndElement;");
			this.w.println("import javax.xml.stream.events.XMLEvent;");
			this.w.println("import javax.xml.stream.events.Attribute;");
			
			
		
			this.w.println("public class BlastFactory {");
			this.w.push();
			this.w.println("private void unknownElement(final StartElement se) throws XMLStreamException {");
			this.w.println("}");

			this.w.println("private boolean isA(final QName qName,final String ns,final String localName) {");
			this.w.println("if(!qName.getLocalPart().equals(localName)) return false;");
			this.w.println("if(!ns.equals(\"##default\") && !ns.equals(qName.getNamespaceURI())) return false;");
			this.w.println("return true;");
			this.w.println("}");
			
			
			this.w.pop();
			
			toBeDone.add(GBSet.class);
			toBeDone.add(INSDSeq.class);
			
			for(final String arg:args) {
				Class<?> clazz= Class.forName(arg);
				doXmlRoot(clazz);
				}
			while(!toBeDone.isEmpty()) {
				Class<?> clazz = toBeDone.iterator().next();
				toBeDone.remove(clazz);
				doXmlRoot(clazz);
				}
			this.w.println("}");
			this.w.flush();
			System.err.println("done");
			return 0;
			}
		catch(Throwable err ) {
			err.printStackTrace();
			return -1;
			}
		}
	public static void main(String[] args) {
		new JaxbToJava().instanceMainWithExit(new String[] {"sandbox.ncbi.blast.BlastOutput"});

	}

}
