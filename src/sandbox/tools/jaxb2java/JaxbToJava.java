package sandbox.tools.jaxb2java;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.StringUtils;
import sandbox.io.IndentWriter;

public class JaxbToJava extends Launcher{
	@Parameter(names={"-o"},description=OUTPUT_OR_STANDOUT)
	private Path outputPath = null;
	@Parameter(names={"--package"},description="package name")
	private String packageName = "";

	private Class<?> rootClass;

	private IndentWriter w;
	private final Set<Class<?>> seen= new HashSet<>();
	private final Set<Class<?>> toBeDone= new HashSet<>();
	
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
		public boolean isList() {
			Class<?> fieldType0 = field.getType();
			return fieldType0.isAssignableFrom(List.class);
			}
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
			else if(getRange().equals(Integer.class) || getRange().equals(Integer.TYPE)) {
				return "Integer.parseInt("+varName+")";
				}
			else if(getRange().equals(Double.class) || getRange().equals(Double.TYPE)) {
				return "Double.parseDouble("+varName+")";
				}
			else if(getRange().equals(Float.class) || getRange().equals(Float.TYPE)) {
				return "Float.parseFloat("+varName+")";
				}
			else if(getRange().equals(Boolean.class) || getRange().equals(Boolean.TYPE)) {
				return "Boolean.parseBoolean("+varName+")";
				}
			else if(getRange().equals(XMLGregorianCalendar.class)) {
				return "javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar("+varName+")";
				}
			throw new IllegalArgumentException(""+getRange());
			}
		public String encodeString(String varName) {
			return "String.valueOf("+varName+")";
			}
		}
	
	private class FieldAsAttribute extends AbstractField<XmlAttribute> {
		FieldAsAttribute(final  Field field) {
			super(field);
			}
		
		@Override
		public String getLocalName() {
			String s= getAnnotation().name();
			if(s.equals("##default")) return field.getName();
			return s;
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
			w.println("if(attValue!=null) w.writeAttribute(\""+getAnnotation().name()+"\","+encodeString("attValue")+");");
			}
	}
	private class FieldAsElement extends AbstractField<XmlElement> {
		FieldAsElement(final  Field field) {
			super(field);
			}
	
		@Override
		public String getNamespaceURI() {
			return getAnnotation().namespace();
			}
		@Override
		public String getLocalName() {
			String s= getAnnotation().name();
			if(s.equals("##default")) return field.getName();
			return s;
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
				w.println("if(root."+getGetter()+"()!=null) {");
				w.push();
				w.println("w.writeStartElement(\""+getLocalName() +"\");");
				w.println("w.writeCharacters(root."+getGetter()+"());");
				w.println("w.writeEndElement();");
				w.pop();
				w.println("}");
				}
			else
				{
				w.println("if(root."+getGetter()+"()!=null) {");
				w.push();
				w.println("write"+javaName(getAnnotation().name())+"(w,root."+getGetter()+"());");
				w.pop();
				w.println("}");
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
		final XmlRootElement optXmlRootElement =clazz.getAnnotation(XmlRootElement.class);
		final String xmlRootElementName = (optXmlRootElement==null?clazz.getSimpleName():optXmlRootElement.name());
		final List<AbstractField<? extends Annotation>> fields = getFields(clazz);
		if(fields.isEmpty()) return;
		if(clazz.equals(this.rootClass)) {
			w.println("	   public "+ clazz.getName()+" parse"+clazz.getSimpleName()+"(final XMLEventReader r) throws XMLStreamException {");
			w.println("		   while(r.hasNext()) {");
			w.println("			   final XMLEvent evt= r.nextEvent();");
			w.println("		         if(evt.isStartElement()) {");
			w.println("		        	 return parse"+ clazz.getSimpleName()+"(r,evt.asStartElement());");
			w.println("		         	}");
			w.println("		         else if(evt.isStartDocument() || evt.isEndDocument()) {");
			w.println("		        	continue; ");
			w.println("		         	}");
			w.println("		         else if(evt.isCharacters() && evt.asCharacters().getData().trim().isEmpty()) {");
			w.println("		        	 continue;");
			w.println("		         }");
			w.println("		         else  {");
			w.println("		        	 throw new XMLStreamException(\"unexpected element\",evt.getLocation());");
			w.println("		         }");
			w.println("		   }");
			w.println("		   return null;");
			w.println("	   }");
			}

		
		
		w.push();
		w.println("public "+clazz.getName()+" parse"+clazz.getSimpleName()+"(final XMLEventReader r,final StartElement se) throws XMLStreamException {");
		w.push();
		w.println("final QName qName = se.getName();");
		w.println("if(!qName.getLocalPart().equals(\""+xmlRootElementName+"\")) throw new XMLStreamException(\"unecpetded element\",se.getLocation());");
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
		w.push();
		w.println("final XMLEvent evt= r.nextEvent();");
		w.println("if(evt.isStartElement()) {");
		w.push();
		w.println("final StartElement E = evt.asStartElement();");
	
		
		elsestr="";
		for(FieldAsElement field: fields.stream().filter(F->F.isElement()).map(F->FieldAsElement.class.cast(F)).collect(Collectors.toList())) {
			field.read(w);
			elsestr="else ";
			toBeDone.add(field.getRange());
			}
		
		
		
		w.println(elsestr+" {");
		w.push();
		w.println("throw new XMLStreamException(\"unexpected element\",E.getLocation());");
		w.pop();
		w.println("}");
		
		
		w.pop();
		w.println("	}");
		w.println("else if(evt.isEndElement()) {");
		w.push();
		w.println("if(!evt.asEndElement().getName().getLocalPart().equals(\""+xmlRootElementName+"\")) throw new XMLStreamException(\"unecpetded element\",evt.getLocation());");
		w.println("return instance;");
		w.pop();
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
		w.println("}");
		w.println("else  {");
		w.println("throw new XMLStreamException(\"unecpedted event. \", evt.getLocation());");
		w.println("}");
		
		w.pop();
		w.println("} // end of loop");
		w.println("throw new XMLStreamException(\"boum\");");
		w.pop();
		w.println("}");
		w.pop();
		
		
		w.push();
		w.println("public void write"+clazz.getSimpleName()+"(final XMLStreamWriter w,final "+clazz.getName()+" root) throws XMLStreamException {");
		w.push();
		w.println("w.writeStartElement(\""+ xmlRootElementName +"\");");
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
			this.rootClass = Class.forName(oneAndOnlyOneFile(args));
			toBeDone.add(this.rootClass);
			seen.add(String.class);
			seen.add(Integer.class);
			seen.add(List.class);
			seen.add(Long.class);
			seen.add(Boolean.class);
			this.w=new IndentWriter(this.openPathAsPrintWriter(this.outputPath));
			
			if(!StringUtils.isBlank(packageName)) this.w.println("package "+packageName+";");
			this.w.println("import java.util.Iterator;");
			this.w.println("import javax.xml.namespace.QName;");
			this.w.println("import javax.xml.stream.XMLStreamException;");
			this.w.println("import javax.xml.stream.XMLEventReader;");
			this.w.println("import javax.xml.stream.XMLStreamWriter;");
			this.w.println("import javax.xml.stream.events.StartElement;");
			this.w.println("import javax.xml.stream.events.XMLEvent;");
			this.w.println("import javax.xml.stream.events.Attribute;");
			
			
		
			this.w.println("public class "+this.rootClass.getSimpleName()+"Factory {");
			this.w.push();
			
			while(!toBeDone.isEmpty()) {
				Class<?> remain = toBeDone.iterator().next();
				toBeDone.remove(remain);
				if(seen.contains(remain)) continue;
				doXmlRoot(remain);
			}
			
			
			this.w.pop();
			this.w.println("}");
			this.w.flush();
			return 0;
			}
		catch(Throwable err ) {
			err.printStackTrace();
			return -1;
			}
		}
	public static void main(String[] args) {
		new JaxbToJava().instanceMainWithExit(args);

	}

}
