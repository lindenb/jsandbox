package sandbox.annotation;


import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ ElementType.TYPE })
public @interface IncludeUrl
	{
	String url() default "";
	String directory() default "" ;
	}
