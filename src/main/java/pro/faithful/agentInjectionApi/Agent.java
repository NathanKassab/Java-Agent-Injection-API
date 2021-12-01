package pro.faithful.agentInjectionApi;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import pro.faithful.agentInjectionApi.inject.InjectLocation;
import pro.faithful.agentInjectionApi.inject.InjectionApi;
import pro.faithful.agentInjectionApi.inject.annotations.GetLocalVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetNonStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.Inject;
import pro.faithful.agentInjectionApi.inject.annotations.SetVars;

public class Agent {
	
	public static void agentmain(String agentArgs, Instrumentation inst) {
		InjectionApi.init(inst, Agent.class);
	}
	
	@SetVars(localNames = "ping", instanceNamesCsv = "this, pro/faithful/victim/Main, ping, Ljava/lang/String;", staticNamesCsv = "pro/faithful/victim/Main, lastPing, J", useExplicitPrefixes = true)
	@Inject(className = "pro/faithful/victim/Main", methodName = "funny", methodDesc = "()V", lineOffset = 1, injectLocation = InjectLocation.START)
	public static Map<String, Object> transformFunny(@GetLocalVar(name = "ping") String ping1,
			@GetStaticVar(name = "lastPing", owner = "pro/faithful/victim/Main", desc = "J") final long ping2,
			@GetNonStaticVar(name = "ping", owner = "pro/faithful/victim/Main", desc = "Ljava/lang/String;", instanceName = "this") String ping3) {
		System.out.println(ping1 + " " + ping2 + " " + ping3);
		if (new Random().nextBoolean()) {
			try {
				System.out.println("Sleeping for 2000 ms...");
				Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new HashMap() {{
			put("local.ping", "Pong!");
			put("static.lastPing", ping2);
			put("instance.ping", "Pong!");
		}};
	}
	
	public static void main(String[] args) throws Exception {
		
	}
	
}
