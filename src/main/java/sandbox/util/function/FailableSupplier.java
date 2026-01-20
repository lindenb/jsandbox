package sandbox.util.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface FailableSupplier<T,ERROR extends Throwable >{
public T get() throws ERROR;
public default Supplier<T> toSupplier() {
	return FailableSupplier.supplier(this);
	}
public static <T,ERROR extends Throwable> Supplier<T> supplier(final FailableSupplier<T,ERROR> fun) {
	return new Supplier<T>() {
		@Override
			public T get() {
			try {
				return fun.get();
				}
			catch(Throwable err) {
				throw new RuntimeException(err);
				}
			}
		};
	}
}
