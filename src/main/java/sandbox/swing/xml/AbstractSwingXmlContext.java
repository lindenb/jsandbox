package sandbox.swing.xml;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.util.Pair;
import sandbox.xml.XmlUtils;

public abstract class AbstractSwingXmlContext {
	private static final Logger LOG = Logger.builder(AbstractSwingXmlContext.class).build();
	protected final Map<Class<?>,NodeHandler> class2handler = new HashMap<>();
	
	protected abstract class NodeHandler {
		
		public String getName() {
			return this.getType().getTypeName();
			}
		
		public boolean isHandlerForNode(Element root) {
			return getType().getSimpleName().equals(root.getNodeName()) ||
				getType().getName().equals(root.getNodeName());
			}
		public Optional<Object> createInstance(Element root) {
			/** try with string as argument */
			try {
				Optional<String> optStr= getNodeTextContent(root);
				if(optStr.isPresent()) {
					final Constructor<?> ctor=getType().getConstructor(String.class);
					if(Modifier.isPublic(ctor.getModifiers())) {
						final String arg = parseString(optStr.get()).get();
						final Object instance = ctor.newInstance(arg);
						return Optional.of(instance);
						}
					}
				}
			catch(Exception err) {
				LOG.warning(err.getMessage());
				}
			/** try with 0 as argument */
			try {
				Constructor<?> ctor=getType().getConstructor();
				if(Modifier.isPublic(ctor.getModifiers())) {
					Object instance = ctor.newInstance();
					return Optional.of(instance);
					}
				}
			catch(Exception err) {
				LOG.warning(err);
				}
			LOG.warning("no ctor found for "+getName());
			return Optional.empty();
			}
		
		protected Optional<Object> makeInstance(Element root) {
			LOG.info("create instance for "+XmlUtils.getNodePath(root));
			try {
				Optional<Object> opt=createInstance(root);
				if(!opt.isPresent()) {
					LOG.warning("make instance return null for "+getName());
					return Optional.empty();
					}
				fillSetters(opt.get(),root);
				if(opt.get() instanceof Container) {
					fillContainer(Container.class.cast(opt.get()),root);
					}
				
				return opt;
				}
			catch(Throwable err) {
				return Optional.empty();
				}
			}
		protected void fillContainer(Container container,Element root) {
			LOG.info("fillContainer");

			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(!XmlUtils.isElement.test(c)) continue;
				Element e = XmlUtils.toElement.apply(c);
				if(!e.getNodeName().equals("add")) {
					LOG.debug("not add "+e.getNodeName());
					continue;
					}
				addComponent(container,e);
				}
			}
		
		protected void  addComponent(Container container,Element root) {
				LOG.info("addComponent2");
				Optional<Element> dataE = getDataElement(root);
				if(!dataE.isPresent()) {
					LOG.warning("not a data node for "+XmlUtils.getNodePath(root));
					return;
					}
				Optional<NodeHandler> handler = findHandlerByElement(dataE.get());
				if(!handler.isPresent()) {
					LOG.warning("not instance found for "+XmlUtils.getNodePath(root));
					return;
					}
				Optional<Object> instance2 = handler.get().makeInstance(dataE.get());
				if(!instance2.isPresent()) {
					LOG.warning("cannot make instance");
					return;
					}
				if(!(instance2.get() instanceof Component)) {
					LOG.warning("not instance of Component");
					return;
					}
				
				String constaint = root.getAttribute("constraint");
				if(StringUtils.isBlank(constaint)) {
					container.add(Component.class.cast(instance2.get()));
					}
				else
					{
					container.add(Component.class.cast(instance2.get()),constaint);
					}
				}
		
		public abstract Class<?> getType();
		protected void fillSetters(Object o,Element root) {
			}
		
		private Optional<Element> findOneElement(Element root,final String name) {
			final List<Element> L = XmlUtils.stream(root).
					filter(XmlUtils.isElement).
					map(XmlUtils.toElement).
					filter(E->E.getNodeName().equals(name)).
					collect(Collectors.toList());
			if(L.isEmpty()) {
				return Optional.empty();
				}
			else
				{
				if(L.size()>1) LOG.warning("multiple element for "+XmlUtils.getNodePath(root)+"/"+name);
				return Optional.of(L.get(0));
				}
			}
		private Pair<Optional<String>, Optional<Element>> findAttributeOrElement(Element root,final String name) {
			Optional<String> att;
			if(root.hasAttribute(name)) {
				att= Optional.of(root.getAttribute(name));
				}
			else
				{
				att= Optional.empty();
				}
			final Optional<Element> optE = findOneElement(root,name);
			if(att.isPresent() && optE.isPresent()) {
				LOG.warning("att and element both defined for "+XmlUtils.getNodePath(root)+"/"+name);
				}
			return Pair.of(att, optE);
			}
		
		private Optional<String> findStringAttributeOrElement(Element root,final String name) {
			Pair<Optional<String>, Optional<Element>>  p = findAttributeOrElement(root,name);
			if(p.getKey().isPresent()) return p.getKey();
			if(p.getValue().isPresent() &&isNodeTextContent(p.getValue().get())) return Optional.of(p.getValue().get().getTextContent());
			return Optional.empty();
			}
		
		
		protected OptionalInt findByte(Element root,final String name) {
			Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseByte(opt.get()):OptionalInt.empty();
			}
		protected Optional<Character> findCharacter(Element root,final String name) {
			final Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseCharacter(opt.get()):Optional.empty();
			}
		protected OptionalInt findInt(Element root,final String name) {
			final Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseInt(opt.get()):OptionalInt.empty();
			}
		protected OptionalLong findLong(Element root,final String name) {
			final Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseLong(opt.get()):OptionalLong.empty();
			}
		protected OptionalDouble findDouble(Element root,final String name) {
			final Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseDouble(opt.get()):OptionalDouble.empty();
			}
		protected Optional<String> findString(Element root,final String name) {
			return findStringAttributeOrElement(root,name);
			}
		protected Optional<Boolean> findBoolean(Element root,final String name) {
			final Optional<String> opt=findStringAttributeOrElement(root,name);
			return opt.isPresent()?parseBoolean(opt.get()):Optional.empty();
			}
		protected Optional<Object> findObject(Element root,final String name,Class<?> clazz) {
			//LOG.info("find Object");
			Optional<Element> e1=findOneElement(root,name);
			if(!e1.isPresent()) {
				//LOG.info("cannot find <"+name+"/> for "+XmlUtils.getNodePath(root));
				return Optional.empty();
				}
			
			final Optional<Element> e2=getDataElement(e1.get());
			if(!e2.isPresent()) {
				LOG.debug("cannot find data Element under "+XmlUtils.getNodePath(e1.get()));
				return Optional.empty();
				}
			final NodeHandler handler=findHandlerByElement(e2.get()).orElse(null);
			if(handler==null) {
				LOG.info("Cannot find handler for "+XmlUtils.getNodePath(e2.get()));
				return Optional.empty();
				}
			LOG.info("now create instance for "+handler.getName()+" "+e2.get().getNodeName());
			final Optional<Object> o = handler.makeInstance(e2.get());
			if(o.isPresent()) {
				if(!clazz.isInstance(o.get())) {
					LOG.error(o.get().getClass().getTypeName()+"is not an instance of "+clazz);
					return Optional.empty();
					}
				}
			return o;
			}

		
		private Optional<Object> getPublicStaticFinalField(final String s,Class<?> type) {
			try {
				Field f= Arrays.stream(getType().getFields()).
					filter(F->F.getName().equals(s)).
					filter(F->isPublisStaticFinalField(F)).
					filter(F->F.getType().equals(Boolean.TYPE)).
					findFirst().
					orElse(null);
				return f==null?Optional.empty():Optional.of(f.get(null));
				}
			catch(Throwable err) {
				return Optional.empty();
				}
			}
		
		protected Optional<Character> parseCharacter(String s) {
			try {
				if(s.length()!=1) return Optional.empty();
				return Optional.of(s.charAt(0)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Character.TYPE);
				if(opt.isPresent()) return Optional.of((char)opt.get());
				return Optional.empty();
				}
			}
		
		protected OptionalInt parseByte(String s) {
			try {
				return OptionalInt.of(Byte.parseByte(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Byte.TYPE);
				if(opt.isPresent()) return OptionalInt.of((byte)opt.get());
				return OptionalInt.empty();
				}
			}
		
		protected OptionalInt parseShort(String s) {
			try {
				return OptionalInt.of(Short.parseShort(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Short.TYPE);
				if(opt.isPresent()) return OptionalInt.of((short)opt.get());
				return OptionalInt.empty();
				}
			}

		
		protected OptionalInt parseInt(String s) {
			try {
				return OptionalInt.of(Integer.parseInt(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Integer.TYPE);
				if(opt.isPresent()) return OptionalInt.of((int)opt.get());
				return OptionalInt.empty();
				}
			}

		protected OptionalLong parseLong(String s) {
			try {
				return OptionalLong.of(Long.parseLong(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Long.TYPE);
				if(opt.isPresent()) return OptionalLong.of((long)opt.get());
				return OptionalLong.empty();
				}
			}
		protected OptionalDouble parseDouble(String s) {
			try {
				return OptionalDouble.of(Double.parseDouble(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Double.TYPE);
				if(opt.isPresent()) return OptionalDouble.of((double)opt.get());
				return OptionalDouble.empty();
				}
			}
		protected OptionalDouble parseFloat(String s) {
			try {
				return OptionalDouble.of(Float.parseFloat(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Float.TYPE);
				if(opt.isPresent()) return OptionalDouble.of((float)opt.get());
				return OptionalDouble.empty();
				}
			}
		protected Optional<Boolean> parseBoolean(String s) {
			try {
				return Optional.of(Boolean.parseBoolean(s)) ;
				}
			catch(Exception err) {
				final Optional<Object> opt=getPublicStaticFinalField(s,Boolean.TYPE);
				if(opt.isPresent()) return Optional.of((boolean)opt.get());
				return Optional.empty();
				}
			}
		protected Optional<String> parseString(String s) {
			return Optional.of(s) ;
			}
		
		}
		
	
	
	
	public Logger getLogger() {
		return LOG;
		}
	
	
	private static boolean isPublisStaticFinalField(Field f) {
		final int m=f.getModifiers();
		if(!Modifier.isPublic(m)) return false;
		if(!Modifier.isStatic(m)) return false;
		if(!Modifier.isFinal(m)) return false;
		return true;
	}
	
	
	/** return the one and only one child element, check no blank text */
	protected Optional<Element> getDataElement(Element root) {
		if(!root.hasChildNodes()) return Optional.empty();
		if(XmlUtils.stream(root).
				filter(XmlUtils.isTextOrCData).
				map(N->CharacterData.class.cast(N).getData()).
				anyMatch(S->!StringUtils.isBlank(S))) {
				LOG.debug("not a data element because text");
				return Optional.empty();
				}
		final List<Element> L  = XmlUtils.elements(root);
		if(L.size()==1) {
			LOG.info("ok got it "+XmlUtils.getNodePath(L.get(0)));
			return Optional.of(L.get(0));
			}
		LOG.info("no or multuple element under "+XmlUtils.getNodePath(root));
		return Optional.empty();
		}

	
	/** return true if there are text nodes and if there is ONLY text node */
	protected static boolean isNodeTextContent(Element root) {
		if(!root.hasChildNodes()) return false;
		if(XmlUtils.stream(root).anyMatch(XmlUtils.isTextOrCData.negate())) return false;
		return true;
		}

	
	/** return text contnent if there are text nodes and if there is ONLY text node */
	protected static Optional<String> getNodeTextContent(Element root) {
		if(!isNodeTextContent(root)) return Optional.empty();
		return Optional.of(root.getTextContent());
		}
	
	protected Optional<NodeHandler> findHandlerByElement(Element root) {
		List<NodeHandler> L=this.class2handler.values().stream().filter(H->H.isHandlerForNode(root)).collect(Collectors.toList());
		if(L.isEmpty()) return Optional.empty();
		if(L.size()>1) LOG.warning("multiple handlers for "+root.getNodeName()+":"+L.toString());
		return Optional.of(L.get(0));
		}
	
	protected void registerNodeHandler(NodeHandler h) {
		if(class2handler.containsKey(h.getType())) {
			LOG.warning("replace existing handler for "+h.getType().getTypeName());
			}
		this.class2handler.put(h.getType(), h);
		}
	
	protected void registerNodeHandlers() {
		}
}
