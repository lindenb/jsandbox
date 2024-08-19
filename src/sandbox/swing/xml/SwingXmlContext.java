package sandbox.swing.xml;

import java.util.Optional;

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
	}
