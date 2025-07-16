package sandbox.rdf;

public interface RDFNode extends Comparable<RDFNode> {
public boolean isLiteral();
public boolean isResource();
public default Literal asLiteral() { return Literal.class.cast(this);}
public default Resource asResource() { return Resource.class.cast(this);}
}
