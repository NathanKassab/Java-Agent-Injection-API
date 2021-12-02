package pro.faithful.agentInjectionApi;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import pro.faithful.agentInjectionApi.inject.InjectLocation;
import pro.faithful.agentInjectionApi.inject.InjectionApi;
import pro.faithful.agentInjectionApi.inject.annotations.GetLocalVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetNonStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.Inject;
import pro.faithful.agentInjectionApi.inject.annotations.SetVars;

public class Agent {
	
	public static void agentmain(String agentArgs, Instrumentation inst) {
		InjectionApi.init(inst, true, Agent.class);
	}
	
	@SetVars(staticNamesCsv = "pro/faithful/victim/Main, ping, java/lang/String", useExplicitPrefixes = true)
	@Inject(className = "pro/faithful/victim/Main", methodName = "funny", methodDesc = "()V")
	public static Map<String, Object> transformFunny1(@GetStaticVar(name = "ping", owner = "pro/faithful/victim/Main", desc = "Ljava/lang/String;") String ping){
		System.out.println(ping);
		return new HashMap() {{
			put("static.ping", "Pong! " + new Random().nextInt(100) + " ");
		}};
	}
	
	public static String ping = "Ping!";
	public static void main(String[] args) {
		Map obj = transformFunny1(ping);
		ping = (String)obj.get("static.ping");
	}
	
}
