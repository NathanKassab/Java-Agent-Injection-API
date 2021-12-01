package pro.faithful.agentInjectionApi.inject.annotations;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface GetNonStaticVar {
	String instanceName();
	String owner();
	String name();
	String desc();
}
