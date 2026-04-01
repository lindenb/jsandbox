package sandbox.bean;

public interface BeanSet {
	public Object getBeanById(final String id);
	public default <T> T getBeanById(final String id,final Class<T> clazz) {
		return clazz.cast(this.getBeanById(id));
	}
}
