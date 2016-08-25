package sandbox;

public interface Named {
	public String getName();
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
	
	public default String getDescription() {
		return getLabel();
	}
}
