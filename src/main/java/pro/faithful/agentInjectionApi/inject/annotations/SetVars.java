package pro.faithful.agentInjectionApi.inject.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import pro.faithful.agentInjectionApi.inject.InjectLocation;

@Retention(RUNTIME)
@Target(METHOD)
public @interface SetVars {
	
	// explicit prefix is "local."
	String[] localNames() default "";
	
	// explicit prefix is "static."
	// a comma delimited string in the format "owner, name, desc"
	String[] staticNamesCsv() default "";
	
	// explicit prefix is instance.
	// a comma delimited string in the format "instanceName, owner, name, desc"
	String[] instanceNamesCsv() default "";
	
	// whether the transformer should use explicit prefixes, these are used in the return map in case two vars share the same name
	boolean useExplicitPrefixes() default false;
	
}
