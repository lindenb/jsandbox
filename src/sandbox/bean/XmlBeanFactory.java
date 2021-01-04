package sandbox.bean;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlBeanFactory {
public XmlBeanFactory() {
	
	}
public BeanSet parse(final Path path) throws IOException {
	try {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse(path.toFile());
		return new BeanSetImpl(dom);
		}
	catch (Exception err) {
		throw new IOException(err);
		}
	}

private class BeanSetImpl implements BeanSet {
		private final Map<String,Object> id2object = new HashMap<>();
		private final  Document document;
		BeanSetImpl(final Document document) {
			this.document = document;
			final Element root = this.document.getDocumentElement();
			if(root==null || root.getNodeName().equals("beans")) throw new IllegalArgumentException("root is not <beans>");
			}
		public Object getBeanById(final String id) {
			if(id2object.containsKey(id)) return id2object.get(id);
			return null;
			}
		public <T> T getBeanById(final String id,final Class<T> clazz) {
			return clazz.cast(this.getBeanById(id));
			}
		private void visitBeans(Element root) {
			for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
				if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
				Element E = Element.class.cast(n);
				visitBeans(E);
				}
			}
		
		private Object makeBean(Element root) {
			try {
				String className = root.getAttribute("class");
				Class<?> clazz  = Class.forName(className);
				Element constructorE= null;
				Map<String, Object> properties = new LinkedHashMap<>();
				for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
					if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
					Element E = Element.class.cast(n);
					if(E.getNodeName().equals("constructor")) {
						constructorE  =E;
						}
					else if(E.hasAttribute("property")) {
						String attName = E.getAttribute("property");
						properties.put(attName, makeBean(E));
						}
					}
				final Object obj;
				if(constructorE!=null) {
					for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
						if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
						
						}
					obj= clazz.getDeclaredConstructor().newInstance();
					}
				else
					{
					obj= clazz.getDeclaredConstructor().newInstance();
					}
				for(String prop:properties.keySet()) {
					final Object v = properties.get(prop);
					final Method setter = clazz.getMethod(prop, null);
					setter.setAccessible(true);
					setter.invoke(obj, v);
				}
				
				
				return obj;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		
		private Object nil() {
			return null;
		}
		
		private Object primitive(final Element root) {
			final String name = root.getNodeName();
			final String content = root.getTextContent();
			if(name.equals("string")) {
				return content;
				}
			else if(name.equals("double")) {
				return Double.parseDouble(content);
				}
			else if(name.equals("float")) {
				return Float.parseFloat(content);
				}
			else if(name.equals("int")) {
				return Integer.parseInt(content);
				}
			else if(name.equals("long")) {
				return Long.parseLong(content);
				}
			else if(name.equals("short")) {
				return Short.parseShort(content);
				}
			else if(name.equals("byte")) {
				return Byte.parseByte(content);
				}
			else if(name.equals("boolean")) {
				return Boolean.parseBoolean(content);
				}
			else if(name.equals("bigInteger")) {
				return new BigInteger(content);
				}
			else if(name.equals("bigDecimal")) {
				return new BigDecimal(content);
				}
			else
				{
				throw new IllegalArgumentException(name);
				}
			}
		}

}
