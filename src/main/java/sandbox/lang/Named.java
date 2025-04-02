package sandbox.lang;

public interface Named {
	/** get Name for this object */
	public String getName();
	/** get Nice name for this object */
	public default String getLabel() { 
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
		return sb.toString();
		}
	/** get Descrition for this project , default is getName() */
	public default String getDescription() {
		return getLabel();
	}
}
