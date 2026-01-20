package sandbox.swing.xml;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import sandbox.Logger;
import sandbox.awt.Colors;
import sandbox.xml.XmlUtils;

public class SwingXmlContext extends BaseSwingXmlContext {
	private static final Logger LOG = Logger.builder(SwingXmlContext.class).build();
	private Optional<Object> firstInstance =Optional.empty();
	private SwingXmlContext() {
		}
	


	public Optional<Object> getInstance() {
		return this.firstInstance;
	}
	
	
	public static SwingXmlContext of(Element root) {
		final SwingXmlContext ctx=new SwingXmlContext();
		ctx.registerNodeHandlers();
		try {
			ctx.firstInstance = ctx.createInstanceFromElement(root);
			return ctx;
			}
		catch(Exception err) {
			throw new RuntimeException(err);
			}
		}
	@Override
	protected void registerNodeHandlers() {
		super.registerNodeHandlers();
		registerNodeHandler(new JSplitPaneNodeHandler2());
		registerNodeHandler(new JScrollPaneNodeHandler2());
		registerNodeHandler(new DimensionNodeHandler2());
		registerNodeHandler(new PointNodeHandler2());
		registerNodeHandler(new ColorNodeHandler2());
		registerNodeHandler(new FontNodeHandler2());
		}
	

	private class FontNodeHandler2 extends FontNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			Optional<String> content= AbstractSwingXmlContext.getNodeTextContent(root);
			if(content.isPresent()) {
				Font f=Font.decode(content.get());
				if(f!=null) return Optional.of(f);
				}

			
			String name=Font.DIALOG;
			int face=Font.PLAIN;
			int size=10;
			Attr att=root.getAttributeNode("family");
			if(att==null) att=root.getAttributeNode("name");
			if(att!=null) name=att.getValue();
			att=root.getAttributeNode("face");
			if(att!=null) face= parseInt(att.getValue()).orElse(face);
			att=root.getAttributeNode("size");
			if(att!=null) size= parseInt(att.getValue()).orElse(size);

			
			
			return Optional.of(new java.awt.Font(name,face,size));
			}
		}
	
	private class ColorNodeHandler2 extends ColorNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			Optional<String> content= AbstractSwingXmlContext.getNodeTextContent(root);
			if(content.isPresent()) {
				Optional<Color> c=Colors.getInstance().parse(content.get());
				if(c.isPresent()) return Optional.of(c.get());
				}
			int r=0,g=0,b=0;
			Attr att=root.getAttributeNode("r");
			if(att!=null) r = parseInt(att.getValue()).orElse(0);
			att=root.getAttributeNode("g");
			if(att!=null) g = parseInt(att.getValue()).orElse(0);
			att=root.getAttributeNode("b");
			if(att!=null) b = parseInt(att.getValue()).orElse(0);
			
			return Optional.of(new java.awt.Color(r,g,b));
			}
		}
	
	private class DimensionNodeHandler2 extends DimensionNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			Attr att=root.getAttributeNode("width");
			if(att==null) return Optional.empty();
			OptionalInt ow= parseInt(att.getValue());
			if(ow.isEmpty()) return Optional.empty();
			att=root.getAttributeNode("height");
			if(att==null) return Optional.empty();
			OptionalInt oh= parseInt(att.getValue());
			if(oh.isEmpty()) return Optional.empty();
			
			return Optional.of(new java.awt.Dimension(ow.getAsInt(),oh.getAsInt()));
			}
		}
	
	private class PointNodeHandler2 extends PointNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			Attr att=root.getAttributeNode("x");
			if(att==null) return Optional.empty();
			OptionalInt ox= parseInt(att.getValue());
			if(ox.isEmpty()) return Optional.empty();
			att=root.getAttributeNode("y");
			if(att==null) return Optional.empty();
			OptionalInt oy= parseInt(att.getValue());
			if(oy.isEmpty()) return Optional.empty();
			
			return Optional.of(new java.awt.Point(ox.getAsInt(),oy.getAsInt()));
			}
		}
	
	private class JSplitPaneNodeHandler2 extends JSplitPaneNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			try {
				List<Element> L=XmlUtils.elements(root);
				if(L.size()!=2) {
					return Optional.empty();
					}
				Component[] components=new Component[2];
				for(int i=0;i< 2;i++) {
					NodeHandler  h = findHandlerByElement(L.get(i)).orElse(null);
					if(h==null) {
						LOG.info("cannot find handler for "+XmlUtils.getNodePath(L.get(i)));
						return Optional.empty();
						}
					Object o = h.makeInstance(L.get(i)).orElse(null);
					if(o==null) {
						LOG.info("cannot make instance for for "+XmlUtils.getNodePath(L.get(i)));
						return Optional.empty();
						}
					if(!(o instanceof Component)) {
						LOG.info("not a component "+XmlUtils.getNodePath(L.get(i)));
						return Optional.empty();
						}
					components[i]=Component.class.cast(o);
					}
				int orient = parseInt(root.getAttribute("orientation")).orElse(JSplitPane.HORIZONTAL_SPLIT);
				
				JSplitPane split=new JSplitPane(orient, components[0], components[1]);
				return Optional.of(split);
				}
			catch(Exception err) {
				LOG.error(err);
				return Optional.empty();
				}
			}
		}
	private class JScrollPaneNodeHandler2 extends JScrollPaneNodeHandler {
		@Override
		public Optional<Object> createInstance(Element root) {
			try {
				List<Element> L= XmlUtils.elements(root);
				if(L.size()!=1) {
					return Optional.empty();
					}
				Object o = createInstanceFromElement(L.get(0)).orElse(null);
				if(o==null) {
					LOG.info("cannot make instance for for "+XmlUtils.getNodePath(L.get(0)));
					return Optional.empty();
					}
				if(!(o instanceof Component)) {
					LOG.info("not a component "+XmlUtils.getNodePath(L.get(0)));
					return Optional.empty();
					}
					
				final JScrollPane split=new JScrollPane(Component.class.cast(o));
				return Optional.of(split);
				}
			catch(Exception err) {
				LOG.error(err);
				return Optional.empty();
				}
			}
		}
	}
