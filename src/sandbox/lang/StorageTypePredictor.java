package sandbox.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

public class StorageTypePredictor implements Consumer<String> {
private static final String XSD=XMLConstants.W3C_XML_SCHEMA_NS_URI;
private static interface Type extends Predicate<String>{
	public Class<?> getJavaType();
	public QName getXsdType();
	public Function<String,?> getMapper();
	@Override
	default boolean test(String t) {
		try {
			getMapper().apply(t);
			return true;
			}
		catch(Throwable err) 
			{
			return false;
			}
		}
	}



private final List<Type> types = new ArrayList<>(Arrays.asList(
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Boolean.class;
				}
			@Override public QName getXsdType() { return new QName(XSD,"bool","xsd");}

			@Override
			public Function<String, ?> getMapper() {
				return S-> Boolean.parseBoolean(S);
				}
			
			@Override
			public boolean test(String t) {
				if(t.equals("true") || t.equals("false")) return true;
				return false;
				}
			},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Short.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S-> Short.parseShort(S);
				}
			
			@Override public QName getXsdType() { return new QName(XSD,"int","xsd");}
			},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Integer.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S-> Integer.parseInt(S);
				}
			@Override public QName getXsdType() { return new QName(XSD,"int","xsd");}
			},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Long.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S-> Long.parseLong(S);
				}
			@Override public QName getXsdType() { return new QName(XSD,"long","xsd");}
			},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return BigInteger.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S->new BigInteger(S);
				}
			@Override public QName getXsdType() { return new QName(XSD,"integer","xsd");}
			},
		
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Float.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S->Float.parseFloat(S);
				}
			@Override public QName getXsdType() { return new QName(XSD,"float","xsd");}
			},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return Double.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S->Double.parseDouble(S);
				}
			
			@Override public QName getXsdType() { return new QName(XSD,"double","xsd");}
			},
		new Type( ) {
				public java.lang.Class<?> getJavaType() {
					return BigDecimal.class;
					}
				@Override
				public Function<String, ?> getMapper() {
					return S->new BigDecimal(S);
					}
				@Override public QName getXsdType() { return new QName(XSD,"decimal","xsd");}
				},
		new Type( ) {
				public java.lang.Class<?> getJavaType() {
					return Character.class;
					}
				@Override
				public Function<String, ?> getMapper() {
					return S->new Character(S.charAt(0));
					}
				@Override
				public boolean test(String t) {
					return t.length()==1;
					}
				@Override public QName getXsdType() { return new QName(XSD,"string","xsd");}
				},
		new Type( ) {
			public java.lang.Class<?> getJavaType() {
				return String.class;
				}
			@Override
			public Function<String, ?> getMapper() {
				return S->S;
				}
			@Override
			public boolean test(String t) {
				return true;
				}
			@Override public QName getXsdType() { return new QName(XSD,"string","xsd");}
			}
		));

public StorageTypePredictor() {
}

public QName getXsdType() {
	return this.types.get(0).getXsdType();
	}

@Override
public void accept(String v) {
	while(types.size()>1) {
		if(types.get(0).test(v)) break;
		types.remove(0);
		}
	}
}
