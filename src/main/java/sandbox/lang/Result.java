package sandbox.lang;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import sandbox.util.function.FailableFunction;
import sandbox.util.function.FailableSupplier;

public interface Result<T> extends Supplier<T> {
	public boolean isSuccess();
	public T orElse(T default_value);
	public T orElse(Function<Throwable,T> map);
	public Throwable getError();
	public Optional<T> toOptional();
	public default boolean isError() {
		return !isSuccess();
		}
	public default <R> R map(Function<T,R> fun) { return fun.apply(get());}
	public T orElseThrow() throws Throwable; 
	
	
	static class Success<T> implements Result<T> {
		private final T value;
		Success(final T value) {
			this.value = Objects.requireNonNull(value);
			}
		public final boolean isSuccess() {
  	  		return true;
  	  		}
		@Override
  	  	public T get() {return this.value;}
  	  	@Override
		public T orElse(T default_value) { return this.get();}
  	  	@Override
  	  	public T orElse(Function<Throwable,T> map) { return this.get();}
  	  	@Override
  	  	public Throwable getError() {
      			throw new NoSuchElementException("Attempted to retrieve error on non-erroneous result");
    			}
  	  	@Override
		public Optional<T> toOptional() {return Optional.of(get());}
		@Override
  	  	public T orElseThrow() throws Throwable { return get();}
		}
	
	static class Failure<T> implements Result<T> {
		private final Throwable error;
		Failure(final Throwable error) {
			this.error = Objects.requireNonNull(error);
			}
		@Override
		public final boolean isSuccess() {
  	  		return false;
  	  		}
  	  	@Override
  	  	public T get() {
  	  		throw new NoSuchElementException("Attempted to retrieve value on erroneous result");
  	  		}
  	  	@Override
  	  	public T orElse(T default_value) { return default_value;}
  	  	@Override
  	  	public T orElse(Function<Throwable,T> map) { return map.apply(getError());}
  	  	@Override
  	  	public Throwable getError() {return error;}
  	  	@Override
  	  	public Optional<T> toOptional() {return Optional.empty();}
  	  	@Override
  	  	public T orElseThrow() throws Throwable  { throw getError();}
		}
		
	  public static <T> Result<T> success(final T value) {
    		return new Success<>(Objects.requireNonNull(value));
  		}
	  public static <T> Result<T> fail(final Throwable error) {
    		return new Failure<>(Objects.requireNonNull(error));
  		}
	  public static <T,ERR extends Throwable> Result<T> of(final FailableSupplier<T,ERR> fun) {
  		try {
  			return success(fun.get());
  			}
  		catch(Throwable err) {
  			return fail(err);
  			}
		}
	  public static <ARG,T,ERR extends Throwable> Result<T> of(final FailableFunction<ARG,T,ERR> fun,ARG arg) {
	  		try {
	  			return success(fun.apply(arg));
	  			}
	  		catch(Throwable err) {
	  			return fail(err);
	  			}
			}
	}
