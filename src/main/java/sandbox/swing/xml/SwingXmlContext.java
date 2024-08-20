package sandbox.swing.xml;

import java.awt.Component;
import java.util.List;
import java.util.Optional;

import javax.swing.JSplitPane;

import org.w3c.dom.Element;

import sandbox.Logger;
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
			Optional<NodeHandler> h = ctx.findHandlerByElement(root);
			if(h.isPresent()) {
				ctx.firstInstance = h.get().makeInstance(root);
				if(!ctx.firstInstance.isPresent()) {
					LOG.warning(h.get().getName() + " cannot create instance for "+XmlUtils.getNodePath(root));
					}
				}
			else
				{
				LOG.warning("No handler was found for "+XmlUtils.getNodePath(root));
				}
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
	
	}
