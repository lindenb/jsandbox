package sandbox.emf;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

/*import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
*/
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.xml.XmlUtils;

public class EMFModel {
	public static final int UNLIMITED_OCCURENCE = -1;
	private Map<String, EPackage> name2package = new LinkedHashMap<>();
	public EPackage getEPackageByName(final String name) {
		EPackage c = this.name2package.get(name);
		if(c==null) {
			c= new EPackageImpl();
			EPackageImpl.class.cast(c).setName(name);
			this.name2package.put(name,c);
			}
		return c;
		}
	
	
	public interface EObject {
		public EMFModel getEMFModel();
		}
	
	public interface ENamed extends  EObject {
		public String getName();
		public default QName getQlName() {
			return new QName(getName());
			}
		public default String getJavaName() {
			final String s = getName();
			return s.substring(0, 1).toUpperCase() + (s.length()==1?"":s.substring(1));
			}
		}
	
	private abstract class EObjectImpl implements EObject {
		@Override
		public EMFModel getEMFModel() {
			return EMFModel.this;
			}
		}
	
	private abstract class ENamedImpl extends EObjectImpl implements ENamed {
		private String name;
		@Override
		public String getName() {
			return name;
			}
		public void setName(String name) {
			this.name = name;
			}
		}
	
	public interface EPackage extends ENamed {
		public EClass getEClassByName(final String name);
		public Collection<EClass> getEClasses();
		}
	
	
	
	private class EPackageImpl extends ENamedImpl implements EPackage {
		private Map<String, EClass> name2class = new LinkedHashMap<>();
		@Override
		public EClass getEClassByName(final String name) {
			EClass c = this.name2class.get(name);
			if(c==null) {
				c= new EClassImpl();
				EClassImpl.class.cast(c).setName(name);
				EClassImpl.class.cast(c).owner = this;
				this.name2class.put(name,c);
				}
			return c;
			}
		@Override
		public Collection<EClass> getEClasses() {
			return name2class.values();
			}
		
		
		}
	
	
	public interface EType {
		
	}
	private class ETypeImpl implements EType {
		
	}
	
	
	public interface EClass extends EType,ENamed {
		
		public EPackage getPackage();
		
		public List<EStructuralFeature> getEStructuralFeatures();
		
		public default List<EAttribute> getAttributes() {
			return getEStructuralFeatures().
					stream().
					filter(SF->SF instanceof EAttribute).
					map(SF->EAttribute.class.cast(SF)).
					collect(Collectors.toList())
					;
		}
		public default Optional<EAttribute> getAttributeByName(final String name) {
			return getAttributes().
					stream().
					filter(E->E.getName().equals(name)).
					findFirst();
		}
		public default List<EReference> getEReferences() {
			return getEStructuralFeatures().
					stream().
					filter(it->it.isEReference()).
					map(it->it.asEReference()).
					collect(Collectors.toList());
		}
		public default Optional<EReference> getEReferenceByName(final String name) {
			return getEReferences().
					stream().
					filter(E->E.getName().equals(name)).
					findFirst();
		}
		
		public default void writeJavaCode(PrintWriter w) {
			w.println("public class "+getJavaName()+" {");
			for(EStructuralFeature f:getEStructuralFeatures()) {
				f.writeJavaCode(w);
				}
			w.println("}");
			}
		}
	
	
	private class EClassImpl extends ENamedImpl implements EClass {
		EPackageImpl owner;
		final List<EStructuralFeature> features = new ArrayList<>();
		
		@Override
		public EPackage getPackage() {
			return owner;
			}

		@Override
		public List<EStructuralFeature> getEStructuralFeatures() {
			return features;
			}
		
		public EAttribute getAttribute(final String name, EType eType) {
			Optional<EAttribute> o = this.features.stream().
					filter(it->it.isEAttribute() && it.getName().equals(name)).
					map(it->it.asEAttribute()).
					findFirst();
			if(o.isEmpty()) {
				EAttributeImpl a  = new EAttributeImpl();
				a.setName(name);
				a.eClass = this;
				this.features.add(a);
				return a;
				}
			else
				{
				return o.get();
				}
			}
		
		public EAttribute getIntAttribute(final String name) {
			return getAttribute(name,EINT);
			}
		}
	
	
	public interface EPrimitive extends EType{
		}
	private static class EPrimitiveImpl implements EPrimitive {
		final Class<?> clazz;
		EPrimitiveImpl(Class<?> clazz) {
			this.clazz=clazz;
			}
		}
	
	
	public interface EStructuralFeature extends ENamed {
		public EClass getEClass();
		public boolean isEAttribute();
		public boolean isEReference();
		public void setLowerBound(int minOccurence);
		public void setUpperBound(int maxOccurence);

		public default EAttribute asEAttribute() {
			return EAttribute.class.cast(this);
			}
		public default EReference asEReference() {
			return EReference.class.cast(this);
			}
		void writeJavaCode(PrintWriter w);
		}
	private abstract class EStructuralFeatureImpl extends ENamedImpl implements EStructuralFeature {
		EClassImpl eClass;
		int minOccurence = 0;
		int maxOccurence = UNLIMITED_OCCURENCE;
		@Override
		public EClass getEClass() {
			return eClass;
			}
		@Override
		public void setLowerBound(int minOccurence) {
			this.minOccurence = minOccurence;
		}
		@Override
		public void setUpperBound(int maxOccurence) {
			this.maxOccurence = maxOccurence;
			}
		}

	
	
	public interface EAttribute extends EStructuralFeature {
		default String getGetterName() {
			return "get"+getJavaName();
			}
		default String getSetterName() {
			return "set"+getJavaName();
			}
		}
	
	private class EAttributeImpl extends EStructuralFeatureImpl implements EAttribute {
		@Override
		public final boolean isEAttribute() {
			return true;
			}
		@Override
		public final boolean isEReference() {
			return false;
			}
		@Override
		public void writeJavaCode(PrintWriter w) {
			String type="";
			String scope = "private";
			String def = "null";
			w.println(scope+" "+type+" "+getName()+"="+def+";");
			
			w.println("public "+type+" "+getGetterName()+"() {");
			w.print("return this."+getName()+";");
			w.println("}");

			w.println("public void "+getSetterName()+"("+type+" "+getName()+") {");
			w.print("this."+getName() + "=" + getName() + ";");
			w.println("}");

			}
		
		}

	
	public interface EReference extends EStructuralFeature{
		}
	
	private class EReferenceImpl extends EStructuralFeatureImpl implements EReference {
		@Override
		public final boolean isEAttribute() {
			return false;
			}
		@Override
		public final boolean isEReference() {
			return true;
			}
		}
	
	public static final EPrimitive EINT= new EPrimitiveImpl(Integer.class) {
		
		};
	
	public static final EPrimitive EBOOLEAN= new EPrimitiveImpl(Boolean.class) {
			
		};

	
	private static class Att2Type {
		final EReference ref;
		final String qName;
		Att2Type(EReference ref,String qName) {
				this.ref = ref;
				this.qName = qName;
		}
	}
public static class ECoreFactory {
	
	}

public static EMFModel parse(final Document dom) {
	final Map<String,EClass> qName2eclass = new HashMap<>();
	final Map<String,EReference> qName2ereference = new HashMap<>();
	final List<EPackage> packages = new ArrayList<>();
	final List<Att2Type> att2types = new ArrayList<>();
	final List<Att2Type> opposites = new ArrayList<>();
	final EMFModel model = new EMFModel();
	final ECoreFactory ecoreFactory  = new ECoreFactory();
	for(final Element packE: XmlUtils.elements(dom.getDocumentElement()).stream().filter(E->E.getLocalName().equals("package")).collect(Collectors.toList()) )
		{
		final EPackage ecorePackage = ecoreFactory.createEPackage();
		final String prefix =  att(packE,"prefix");
		final String uri =  att(packE,"uri");
		ecorePackage.setName(att(packE,"name"));
		ecorePackage.setNsPrefix(prefix);
		ecorePackage.setNsURI(uri);
		
		for(final Element classE: XmlUtils.elements(packE).stream().filter(E->E.getLocalName().equals("class")).collect(Collectors.toList()) )
			{
			final EClass eClass = ecoreFactory.createEClass();
			final String className = att(classE,"name"); 
			eClass.setName(className);
			qName2eclass.put(prefix+":"+className,eClass);
			
			
			
			for(final Element structFeatE: XmlUtils.elements(classE).stream().filter(
					E->E.getLocalName().equals("attribute") || E.getLocalName().equals("reference")).collect(Collectors.toList()) )
				{
				final EStructuralFeature eStructuralFeature;
				if( structFeatE.getLocalName().equals("attribute"))
					{
					final Element attE = structFeatE;
					final EAttribute eAtt = ecoreFactory.createEAttribute();
					eStructuralFeature = eAtt;
					eAtt.setName(att(attE,"name"));
					final String type = att(attE,"type");
						switch(type) {
						case "string": eAtt.setEType(ecorePackage.getEString()); break;
						case "int": eAtt.setEType(ecorePackage.getEInt()); break;
						case "short": eAtt.setEType(ecorePackage.getEShort()); break;
						case "long": eAtt.setEType(ecorePackage.getELong()); break;
						case "float": eAtt.setEType(ecorePackage.getEFloat()); break;
						case "double": eAtt.setEType(ecorePackage.getEDouble()); break;
						default: throw new IllegalArgumentException("Bad attribute type :" + type);
						}
					}
				else //reference 
					{
					final Element refE = structFeatE;
					final EReference eRef = ecoreFactory.createEReference();
					eStructuralFeature = eRef;
					final String refName = att(refE,"name"); 
					eRef.setName(refName);
					
					
					Boolean b = boolAtt(refE,"containment");
					if(b!=null) eRef.setContainment(b);
					b = boolAtt(refE,"resolve-proxies");
					if(b!=null) eRef.setResolveProxies(b);
					
					Attr att = refE.getAttributeNode("opposite");
					if(att!=null) {
						opposites.add(new Att2Type(eRef, att.getValue()));
					}
					
					final String ref = att(refE,"type");
					att2types.add(new Att2Type(eRef, ref));
					qName2ereference.put(prefix+":"+className+":"+refName,eRef);
					}
				
				Boolean b = boolAtt(structFeatE,"uniq");
				if(b!=null) eStructuralFeature.setUnique(b);
				b = boolAtt(structFeatE,"ordered");
				if(b!=null) eStructuralFeature.setOrdered(b);
				b = boolAtt(structFeatE,"changeable");
				if(b!=null) eStructuralFeature.setChangeable(b);
				b = boolAtt(structFeatE,"transient");
				if(b!=null) eStructuralFeature.setTransient(b);
				b = boolAtt(structFeatE,"unsettable");
				if(b!=null) eStructuralFeature.setUnsettable(b);
				b = boolAtt(structFeatE,"volatile");
				if(b!=null) eStructuralFeature.setVolatile(b);
				b = boolAtt(structFeatE,"derived");
				if(b!=null) eStructuralFeature.setDerived(b);
				
				
				Attr att = structFeatE.getAttributeNode("lower-bound");
				if(att!=null) eStructuralFeature.setLowerBound(Integer.parseInt(att.getValue()));
				att = structFeatE.getAttributeNode("upper-bound");
				if(att!=null) eStructuralFeature.setUpperBound(Integer.parseInt(att.getValue()));
				att = structFeatE.getAttributeNode("default");
				if(att!=null) eStructuralFeature.setDefaultValueLiteral(att.getValue());
				
				
				
				eClass.getEStructuralFeatures().add(eStructuralFeature);
				}
			
			pack.getEClassifiers().add(eClass);
			}
		
		packages.add(pack);
		}
	
	for(final Att2Type att2type: att2types) {
		final EClass eClass= qName2eclass.get(att2type.qName);
		att2type.ref.setEType(eClass);
	}
	for(final Att2Type att2type: opposites) {
		final EReference eRef= qName2ereference.get(att2type.qName);
		att2type.ref.setEOpposite(eRef);
	}

	return model;
}

private static String att(Node n,String name) {
	Attr att = (Attr)n.getAttributes().getNamedItem(name);
	return att.getValue();
}

private static Boolean boolAtt(Node n,String name)
{
return null;	
}
private static Integer intAtt(Node n,String name)
{
return null;	
}
}
