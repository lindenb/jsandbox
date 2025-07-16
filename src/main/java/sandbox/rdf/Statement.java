package sandbox.rdf;

import java.util.Objects;

public class Statement implements Comparable<Statement> {
	private final Resource subject;
	private final Resource property;
	private final RDFNode object;
		
	public Statement(Resource subject,Resource property, RDFNode object) {
		this.subject = Objects.requireNonNull(subject);
		this.property = Objects.requireNonNull( property);
		this.object = Objects.requireNonNull(object);
		}
	public boolean match(Resource subject,Resource property, RDFNode object) {
		return (subject==null || subject.equals(this.subject)) &&
				(property==null || property.equals(this.property)) &&
				(object==null || object.equals(this.object))
				;
		}
	
	public Resource getSubject() {
		return subject;
		}
	
	public Resource getProperty() {
		return property;
		}
	public RDFNode getObject() {
		return object;
		}
	public boolean isLiteral() {
		return this.getObject().isLiteral();
	}
	public boolean isResource() {
		return this.getObject().isResource();
		}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.subject,this.property,this.object);
		}
	@Override
	public boolean equals(Object obj) {
		 if(obj==this) return true;
		 if(obj == null || !(obj instanceof Statement)) return false;
		 final Statement o = Statement.class.cast(obj);
		 return this.getSubject().equals(o.getSubject()) &&
				 this.getProperty().equals(o.getObject()) &&
				 this.getObject().equals(o.getObject())
				 ;
		}
	
	@Override
	public int compareTo(final Statement o) {
		int i= getSubject().compareTo(o.getSubject());
		if(i!=0) return i;
		i= getProperty().compareTo(o.getProperty());
		if(i!=0) return i;
		i= getObject().compareTo(o.getObject());
		return i;
		}
	
	@Override
	public String toString() {
		return String.join(" ", getSubject().toString(),getProperty().toString(),getObject().toString(),";");
		}
}
