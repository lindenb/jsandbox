package sandbox.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
/**
 * DefaultNamespaceContext
 *
 */
public class DefaultNamespaceContext implements NamespaceContext {
	private final Map<String,String> prefix2ns = new HashMap<>();
	public DefaultNamespaceContext() {
		put(XMLConstants.DEFAULT_NS_PREFIX,  XMLConstants.NULL_NS_URI);
		put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
		}
	public DefaultNamespaceContext(final String...prefixNs) {
		this();
		if(prefixNs.length%2!=0) throw new IllegalArgumentException("not even number");
		for(int i=0;i+1< prefixNs.length;i+=2) {
			put(prefixNs[i], prefixNs[i+1]);
			}
		}
	public DefaultNamespaceContext put(final String prefix,final String ns) {
		this.prefix2ns.put(prefix, ns);
		return this;
		}
	@Override
	public String getNamespaceURI(final String prefix) {
		if(prefix==null) throw new IllegalArgumentException("null prefix");
		return this.prefix2ns.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
		}

	@Override
	public String getPrefix(final String namespaceURI) {
		if(namespaceURI==null) throw new IllegalArgumentException("null namespaceURI");
		return this.prefix2ns.
				entrySet().
				stream().
				filter(KV->KV.getValue().equals(namespaceURI)).
				map(KV->KV.getKey()).
				findFirst().
				orElse(null);
		}

	public Set<String> getPrefixes() {
		return Collections.unmodifiableSet(this.prefix2ns.keySet());
	}

	
	@Override
	public Iterator<String> getPrefixes(final String namespaceURI) {
		return this.prefix2ns.
				entrySet().
				stream().
				filter(KV->KV.getValue().equals(namespaceURI)).
				map(KV->KV.getKey()).
				collect(Collectors.toSet()).
				iterator();
		}

}
