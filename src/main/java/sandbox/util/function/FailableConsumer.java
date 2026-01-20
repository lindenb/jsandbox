package sandbox.util.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface FailableConsumer<T,ERROR extends Throwable >{
public void accept(T arg) throws ERROR;
public default Consumer<T> toConsumer() {
	return FailableConsumer.consumer(this);
	}
public static <T,ERROR extends Throwable> Consumer<T> consumer(final FailableConsumer<T,ERROR> fun) {
	return new Consumer<T>() {
		@Override
			public void accept(T arg) {
			try {
				fun.accept(arg);
				}
			catch(Throwable err) {
				throw new RuntimeException(err);
				}
			}
		};
	}
}
