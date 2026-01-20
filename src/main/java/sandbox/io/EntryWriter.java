package sandbox.io;

import java.io.DataOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface EntryWriter<T> {
	public void write(final DataOutputStream dos,final T rec) throws IOException;

}
