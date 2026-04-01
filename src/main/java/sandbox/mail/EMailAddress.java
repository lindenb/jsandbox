package sandbox.mail;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import sandbox.lang.StringUtils;

public class EMailAddress {
	private final String email;
	private Set<String> names = new HashSet<String>();
	public EMailAddress(final String email) {
		this(email,"");
		}
	public EMailAddress(final String email,final String name) {
		this.email = email;
		if(!StringUtils.isBlank(name)) this.names.add(name);
		}
	public String getEmail() {
		return email;
		}
	public URI asURI() {
		return URI.create("mailto:"+getEmail());
		}
	@Override
	public int hashCode() {
		return email.toLowerCase().hashCode();
		}
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof EMailAddress)) return false;
		return this.getEmail().equalsIgnoreCase(EMailAddress.class.cast(obj).getEmail());
		}
	@Override
	public String toString() {
		return getEmail();
		}
	/** parse email
	 *  type is "name" <name@host> | name <name@host> |  <name@host> |  name@host
	 * @param s string to parse
	 * @return
	 */
	public static Optional<EMailAddress> parse(String s) {
		String name="";
		String em = null;
		int i = s.lastIndexOf('<');
		if(i!=-1) {
			int j=s.indexOf(i+1, '>');
			if(j>i) {
				String s2 = s.substring(i+1,j).trim().toLowerCase();
				if(s2.contains("@")) {
					em = s2;
					s=s.substring(0,i)+" "+s.substring(j+1);
					s=s.trim();
					}
				}
			}
		if(em!=null) {
			i = s.indexOf('"');
			if(i!=-1) {
				int j=s.indexOf(i+1, '"');
				if(j>i) {
					String s2 = s.substring(i+1,j).trim().toLowerCase();
					
					s=s.substring(0,i)+" "+s.substring(j+1);
					s=s.trim();
					}
				}
			}
		return Optional.empty();
		}
}
