package sandbox;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.util.iterator.ExtendedIterator;

public class JenaUtils {

public static <T> Stream<T> stream(final ExtendedIterator<T> iter) {
	final Spliterator<T> stmt =  Spliterators.spliteratorUnknownSize(
			iter,  Spliterator.ORDERED);
	return StreamSupport.stream(stmt,false).onClose(()->{iter.close();});
	}

}
