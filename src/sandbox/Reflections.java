package sandbox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Reflections
	{
	private static final Logger LOG = Logger.getLogger("sandbox.Reflections");
	
	public interface Wrapper<T>
		{
		public T unwrap();
		}
	public interface HasName
		{
		public String getName();
		}
	public interface HasModifiers
		{
		public int getModifiers();
		public default boolean isStatic() {
			return Modifier.isStatic(this.getModifiers());
			}
		public default boolean isPublic() {
			return Modifier.isPublic(this.getModifiers());
			}
		public default boolean isProtected() {
			return Modifier.isProtected(this.getModifiers());
			}
		}
	
	public interface ConstructorOrMethod
		extends HasModifiers
		{
		public ClassDef getDeclaringClass();
		
		}
	
	public static class ConstructorDef
		implements ConstructorOrMethod, Wrapper<Constructor<?>>,HasName
		{
		private final Constructor<?> ctor;
		ConstructorDef(final Constructor<?> ctor)
			{
			this.ctor=ctor;
			}
		@Override
		public Constructor<?> unwrap()
			{
			return ctor;
			}
		@Override
		public ClassDef getDeclaringClass()
			{
			return new ClassDef(unwrap().getDeclaringClass());
			}
		@Override
		public int getModifiers()
			{
			return unwrap().getModifiers();
			}
		@Override
		public String getName()
			{
			return unwrap().getName();
			}
		}
	
	public static class MethodDef
		implements ConstructorOrMethod,Wrapper<Method>,HasName
		{
		private final Method method;
		MethodDef(final Method method)
			{
			this.method=method;
			}
		@Override
		public Method unwrap()
			{
			return this.method;
			}
		@Override
		public ClassDef getDeclaringClass()
			{
			return new ClassDef(unwrap().getDeclaringClass());
			}
		@Override
		public int getModifiers()
			{
			return unwrap().getModifiers();
			}
		@Override
		public String getName()
			{
			return unwrap().getName();
			}
		
		public boolean isGetter() {
			return false;
			}
		public boolean isSetter()
			{
			return false;
			}
		}
	
	public static class GetterSetter
		implements HasName
		{
		private final MethodDef getter; 
		private final MethodDef setter;
		GetterSetter(MethodDef getter,MethodDef setter)
			{
			this.getter = getter;
			this.setter = setter;
			}
		public MethodDef getGetter()
			{
			return getter;
			}
		public MethodDef getSetter()
			{
			return setter;
			}
		public String getName()
			{
			return setter.getName().substring(3);
			}
		}
	
	public static class ClassDef
		implements HasModifiers,Wrapper<Class<?>>
		{
		private final Class<?> clazz;
		private ClassDef(final Class<?> clazz)
			{
			this.clazz=clazz;
			}
		@Override
		public Class<?> unwrap()
			{
			return this.clazz;
			}
		@Override
		public int getModifiers()
			{
			return this.unwrap().getModifiers();
			}
		
		public Optional<ClassDef> getSuperclass()
			{
			final Class<?> sup =unwrap().getSuperclass();
			return sup==null?
				Optional.empty():
				Optional.of(new ClassDef(sup));
			}
		
		public List<ConstructorDef> getConstructors() {
			return Arrays.asList(this.unwrap().getConstructors()).
				stream().
				map(C->new ConstructorDef(C)).
				collect(Collectors.toList())
				;
			}

		public List<MethodDef> getMethods() {
		return Arrays.asList(this.unwrap().getMethods()).
			stream().
			map(C->new MethodDef(C)).
			collect(Collectors.toList())
			;
		}
	
		
		@Override
		public String toString()
			{
			return this.unwrap().getName();
			}
		}
	
	
	
	
	public static Optional<ClassDef> forName(final String className)
		{
		try
			{
			final Class<?> clazz=Class.forName(className);
			return Optional.of(new ClassDef(clazz));
			}
		catch (final Exception e)
			{
			LOG.warning(e.getMessage());
			return Optional.empty();
			}
		}
	}
