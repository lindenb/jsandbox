/**
 * 
 */
package sandbox.lang;

import java.util.Objects;


/**
 * @author lindenb
 *
 */
public abstract class HashCodeEqualsToString {
	protected abstract Object getIdentityObject();
	@Override
	public int hashCode() {
		return  Objects.hashCode(getIdentityObject());
		}
	@Override
	public boolean equals(final Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof HashCodeEqualsToString)) return false;
		return Objects.equals(getIdentityObject(), HashCodeEqualsToString.class.cast(obj).getIdentityObject());
		}
	@Override
	public String toString() {
		return Objects.toString(getIdentityObject());
		}
	

}
