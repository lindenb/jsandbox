package sandbox.io;

import java.io.DataInputStream;
import java.io.IOException;

@FunctionalInterface
public interface EntryReader<T> {
	public T read(final DataInputStream dis) throws IOException;

}
