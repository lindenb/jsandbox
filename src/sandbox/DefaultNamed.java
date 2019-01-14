package sandbox;

public class DefaultNamed implements Named {
	private final String name;
	private String label = null ;
	private String description = null;
	public DefaultNamed(final String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
		}
	
	@Override
	public String getDescription() {
		return this.description==null?getLabel():this.description;
		}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getLabel() {
		if(this.label!=null) return this.label;
		final StringBuilder sb=new StringBuilder(getName().replace('_', ' '));
		int i=0;
		while(i+1<sb.length())
			{
			if(		Character.isLowerCase(sb.charAt(i)) &&
					Character.isUpperCase(sb.charAt(i+1))
					)
				{
				sb.insert(i+1, ' ');
				i++;
				}
			i++;
			}
		this.label =  sb.toString();
		return this.label;
		}
	
	public void setLabel(String label) {
		this.label = label;
		}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj == null || !(obj instanceof Named)) return false;
		return Named.class.cast(obj).getName().equals(this.getName());
		}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
		}
	
	@Override
	public String toString() {
		return getName();
		}
	}
