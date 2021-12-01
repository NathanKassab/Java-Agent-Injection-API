package pro.faithful.agentInjectionApi.inject;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pro.faithful.agentInjectionApi.inject.annotations.Inject;
import pro.faithful.agentInjectionApi.inject.utils.TransformerHelper;

public class InjectionApi {
	
	public static HashMap<Inject, Method> transformMethods = new HashMap<>();
	public static HashMap<Inject, Class<?>> injectClasses = new HashMap<>();
	public static boolean debugMode = false;
	
	public static void init(Instrumentation instrumentation, Class<?>... transformerClasses) {
		init(instrumentation, false, transformerClasses);
	}
	
	public static void init(Instrumentation instrumentation, boolean debug, Class<?>... transformerClasses) {
		
		// For debug class output
		debugMode = debug;
		
		// Search for methods that need injecting
		System.out.println("Searching for transformers...");
		for (Class<?> clazz : transformerClasses) {
			for (Method method : clazz.getMethods()) {
				if (method.isAnnotationPresent(Inject.class)) {
					transformMethods.put(method.getAnnotation(Inject.class), method);
					injectClasses.put(method.getAnnotation(Inject.class), clazz);
				}
			}
		}
		System.out.println("Found " + transformerClasses.length + " transformers");
		
		// Add transformer
		System.out.println("Adding the transformer...");
		TransformerHelper transformerHelper = new TransformerHelper();
		instrumentation.addTransformer(transformerHelper, true);
		
		// Get all classes that need transforming
		List<Class<?>> transformMe = new ArrayList<>();
		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			for (Inject inject : transformMethods.keySet()) {
				if (inject.className().replace("/", ".").equals(clazz.getName())) {
					transformMe.add(clazz);
				}
			}
		}
		System.out.println("Transformer added");
		
		if (transformMe.size() == 0) {
			System.out.println("Found no classes to transform");
			return;
		}
		
		// Transform classes
		System.out.println("Transforming classes...");
		try {
			instrumentation.retransformClasses(transformMe.toArray(new Class[transformMe.size()]));
		} catch (UnmodifiableClassException e1) {
			System.err.println("Unable to modify class");
			e1.printStackTrace(System.err);
		}
		
		System.out.println("Finished running transformer");
		
		// These are not needed after the classes are transformed
		instrumentation.removeTransformer(transformerHelper);
		transformMethods.clear();
		injectClasses.clear();
		
	}
	
}
