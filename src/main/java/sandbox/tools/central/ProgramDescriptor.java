package sandbox.tools.central;

public interface ProgramDescriptor {
	public String getName();
	public default String getDescription() {
		return getName();
		}
	public default String getUsage() {
		return "java -jar sandbox.jar "+getName()+" [options] (input|stdin)";
	}
	public default boolean isHidden() {
		return false;
		}

}
