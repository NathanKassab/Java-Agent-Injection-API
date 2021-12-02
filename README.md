
# Java Agent Injection API
Our lib makes it easy to inject into a running java process and rewrite classes, we handle the dirty work for you! Our lib avoids the use of java reflection, instead we use asm to integrate your method calls directly into the program. This allows for blazing fast agent that adds no extra delay to your program. Our simple api allows you to use the lib with only a few lines of code, all you have to do is type a few annotations and pass a few args into our init method. We designed it so our transformers will do all the work while you get to focus and your agent

# Taking your first steps
Writing an agent is as easy as counting to three! Just follow the instructions below to get started

1. Create a new maven project with an ide of your choice
2. Create a new class named Agent and create an agentmain method
```
public static void agentmain(String agentArgs, Instrumentation inst) {

}
```

3. Create a META-INF folder in your resources, next create a new file name MANIFEST.MF. Fill the manifest file with the following text, make sure to change the Agent-Class to your agent class
<details>
<summary>Manifest contents</summary>
    
```
Manifest-Version: 1.0
Agent-Class: me.example.Agent <<< replace this with your own package and class
Can-Redefine-Classes: true
Can-Retransform-Classes: true
Can-Set-Native-Method-Prefix: true
Can-Set-Native-Prefix: true
```

</details>

5. Add the following to your pom.xml file
<details>
<summary>pom.xml</summary>
    
```
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
    
<dependencies>
	<dependency>
		<groupId>com.github.NathanKassab</groupId>
		<artifactId>Java-Agent-Injection-API</artifactId>
		<version>0.0.3-alpha</version>
	</dependency>
</dependencies>
    
<build>
	<plugins>
		<plugin>
			<artifactId>maven-compiler-plugin</artifactId>
			<configuration>
				<source>1.8</source>
				<target>1.8</target>
			</configuration>
		</plugin>
		<plugin>
			<artifactId>maven-assembly-plugin</artifactId>
			<configuration>
				<archive>
					<manifestFile>src/main/resources/META-INF/MANIFEST.MF</manifestFile>
				</archive>
				<descriptorRefs>
					<descriptorRef>jar-with-dependencies</descriptorRef>
				</descriptorRefs>
			</configuration>
		</plugin>
	</plugins>
</build>
```

</details>

6. Fill your agentmain with the following method call. inst is the instrumentation, Agent.class is a list of classes that contains methods with the inject annotation. The boolean is debug mode, it can assist you by exporting the transformed classes to a file. These classes can be loaded in a [bytecode viewer](https://github.com/Konloch/bytecode-viewer) or [recaf](https://github.com/Col-E/Recaf), if the transformer is creating issues then you might be requested to provide these files
```
InjectionApi.init(inst, false, Agent.class);
```

7. You should now have everything you need to inject methods, for this example we will be using the test jar included in the base dir of the repo. You can download that [here](https://github.com/NathanKassab/Java-Agent-Injection-API/blob/main/victim.jar). Our goal will be to inject into the "funny" method and do two tasks, change the "ping" string and print the old value to console. To do so we will add the following code to our agent class, the comments should explain what everything does
<details>
<summary>Method</summary>

```
// Tells the transformer to accept that we will be changing these variables, local variables accept a name while 
// static and instance variables will require a comma delimited list of the values
//
// static vars require "owner, name, desc" while instance vars require "instanceName, owner, name, desc"
@SetVars(staticNamesCsv = "pro/faithful/victim/Main, ping, Ljava/lang/String;")

// This annotation contains the class, method name and method desc that we want to inject into.
// The pointer is where the method call will be injected, we can move this to get the desired position in the "victim" method
// We can also move that starting line for the pointer by defining the injectLocation, options for this are start or end. Default for that is start
// We can move the pointer a specific amount of lines up or down by setting lineOffset. Default for that is 0
@Inject(className = "pro/faithful/victim/Main", methodName = "funny", methodDesc = "()V")

// The method returns a map because we are setting vars, the transformer will set the var by getting the var name from the map.
public static Map<String, Object> transformFunny1(
		// Gets a static var from the class that we inject into, args self explanatory
		@GetStaticVar(name = "ping", owner = "pro/faithful/victim/Main", desc = "Ljava/lang/String;") String ping){
	
	// Print the value that we got from the class to console
	System.out.println(ping);
	
	// Return a hashmap with the values for the vars put it @SetVars
	return new HashMap() {{
		put("ping", "Pong! " + new Random().nextInt(100) + " ");
	}};
}
```

</details>

8. Remember how I had you add all that build junk in your pom.xml earlier? That was for a reason, the java agent need to be built before attached to a process. You can build the jar by setting the goals "clean compile assembly:single"

10. You're done, all that's left to do now is inject the agent into the test jar and monitor the results
    
[Example of finished product](https://imgur.com/EWiVuqc)
