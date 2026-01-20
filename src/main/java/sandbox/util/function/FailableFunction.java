package sandbox.util.function;

import java.util.function.Function;

@FunctionalInterface
public interface FailableFunction<T,RET,ERROR extends Throwable >{
public RET apply(T  arg) throws ERROR;
public default Function<T,RET> toFunction() {
	return FailableFunction.function(this);
	}
public static <T,RET,ERROR extends Throwable> Function<T,RET> function(final FailableFunction<T,RET,ERROR> fun) {
	return new Function<T,RET>() {
		@Override
			public RET apply(T arg) {
			try {
				return fun.apply(arg);
				}
			catch(Throwable err) {
				throw new RuntimeException(err);
				}
			}
		};
	}
}
