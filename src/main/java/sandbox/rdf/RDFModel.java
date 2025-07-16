package sandbox.rdf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

public class RDFModel implements Iterable<Statement> {
	private final Set<Statement> statements;
	public RDFModel(final RDFModel cp) {
		this(cp.statements);
		}
	
	public RDFModel(final Set<Statement> set) {
		this.statements = new HashSet<>(set);
		}
	public RDFModel() {
		this.statements = new HashSet<>();
		}
	
	public RDFModel add(final Statement stmt) {
		statements.add(stmt);
		return this;
		}
	
	@Override
	public int hashCode() {
		return statements.hashCode();
		}
	
	@Override
	public boolean equals(final Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof RDFModel)) return false;
		final RDFModel m = RDFModel.class.cast(obj);
		return this.statements.equals(m.statements);
		}
	
	
	public int size() {
		return this.statements.size();
		}
	
	@Override
	public Iterator<Statement> iterator() {
		return statements.iterator();
		}
	public RDFModel add(Resource subject,Resource property,RDFNode object ) {
		return add(new Statement(subject, property, object));
		}
	public RDFModel add(Resource subject,Resource property,byte object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,short object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,int object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,long object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,BigInteger object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,String object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,float object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,double object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,BigDecimal object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel add(Resource subject,Resource property,boolean object ) {
		return add(subject, property, new Literal(object));
		}
	public RDFModel remove(Statement stmt) {
		this.statements.remove(stmt);
		return this;
		}
	public RDFModel remove(Resource subject,Resource property,RDFNode object ) {
		return remove(new Statement(subject, property, object));
		}
	public Stream<Statement> stream() {
		return this.statements.stream();
		}

	public Stream<Statement> matching(Resource subject,Resource property,RDFNode object ) {
		return this.stream().filter(S->S.match(subject, property, object));
		}
	
	@Override
	protected RDFModel clone() {
		return new RDFModel(this);
		}
}
