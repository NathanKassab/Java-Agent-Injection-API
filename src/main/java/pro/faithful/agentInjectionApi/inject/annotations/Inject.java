package pro.faithful.agentInjectionApi.inject.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pro.faithful.agentInjectionApi.inject.InjectLocation;

@Retention(RUNTIME)
@Target(METHOD)
public @interface Inject {
	
	String className();
	String methodName();
	String methodDesc();
	InjectLocation injectLocation() default InjectLocation.START;
	int lineOffset() default 0;
	
}
