package sandbox.emf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	
	public interface EObject {
		public EMFModel getEMFModel();
	}
	
	public interface ENamed extends  EObject {
		public String getName();
	}
	public interface EPackage extends ENamed{
		public List<EClass> getEClasses();
	}
	
	public interface EType {
		
	}
	
	public interface EClass extends EType {
		
		public EPackage getPackage();
		
		public Stream<EStructuralFeature> getStructuralFeatures();
		
		public default Stream<EAttribute> getAttributes() {
			return getStructuralFeatures().
					filter(SF->SF instanceof EAttribute).
					map(SF->EAttribute.class.cast(SF));
		}
		public default Optional<EAttribute> getAttributeByName(final String name) {
			return getAttributes().
					filter(E->E.getName().equals(name)).
					findFirst();
		}
		public default Stream<EReference> getEReferences() {
			return getStructuralFeatures().
					filter(SF->SF instanceof EReference).
					map(SF->EReference.class.cast(SF));
		}
		public default Optional<EReference> getEReferenceByName(final String name) {
			return getEReferences().
					filter(E->E.getName().equals(name)).
					findFirst();
		}
		
	}
	
	
	
	public interface EPrimitive extends EType{
		
	}
	
	public interface EStructuralFeature extends ENamed {
		public EClass getEClass();
		}
	
	public interface EAttribute extends EStructuralFeature {
		}
	
	public interface EReference extends EStructuralFeature{
		}
	
	public static final EPrimitive EINT= new EPrimitive() {
		
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
