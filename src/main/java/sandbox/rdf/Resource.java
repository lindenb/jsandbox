package sandbox.rdf;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

public class Resource implements RDFNode {
	private String uri;
	private static long ID_GENERATOR = System.currentTimeMillis();

	
	public Resource(final String uri) {
		this.uri = Objects.requireNonNull(uri);
		}
	public Resource(final URI uri) {
		this(uri.toString());
		}
	public Resource(final URL url) {
		this(url.toString());
		}
	public Resource(final String ns,final String localName) {
		this(ns+localName);
		}
	public Resource() {
		this("blank:",String.valueOf(++ID_GENERATOR));
		}
	public String getURI() {
		return uri;
		}
	private String[] split() {
		int i= uri.lastIndexOf('#');
		if(i==-1) {
			i= uri.lastIndexOf('/');
			if(i==-1) {
				i= uri.lastIndexOf(':');
				if(i==-1) {
					throw new IllegalArgumentException(this.uri);
					}
				}
			}
		return new String[] {uri.substring(0,i+1),uri.substring(i+1)};
		}
	public String getNamespaceURI() {
		return split()[0];
		}
	public String getLocalName() {
		return split()[1];
		}
	@Override
	public int hashCode() {
		return  uri.hashCode();
		}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof Resource)) return false;
		return getURI().equals(Resource.class.cast(obj).getURI());
		}
	
	@Override
	public final boolean isResource() {
		return true;
		}
	@Override
	public final boolean isLiteral() {
		return false;
		}
	@Override
	public int compareTo(final RDFNode o) {
		if(o.isLiteral()) return -1;
		return this.getURI().compareTo(o.asResource().getURI());
		}
	@Override
	public String toString() {
		return "<"+this.uri+">";
		}
}
