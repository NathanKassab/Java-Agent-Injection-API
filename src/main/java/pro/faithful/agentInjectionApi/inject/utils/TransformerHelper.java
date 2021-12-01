package pro.faithful.agentInjectionApi.inject.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import pro.faithful.agentInjectionApi.inject.InjectLocation;
import pro.faithful.agentInjectionApi.inject.InjectionApi;
import pro.faithful.agentInjectionApi.inject.annotations.GetLocalVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetNonStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.Inject;
import pro.faithful.agentInjectionApi.inject.annotations.SetVars;

public class TransformerHelper implements ClassFileTransformer {
	
	@Override
	public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return transformClass(classfileBuffer);
	}
	
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return transformClass(classfileBuffer);
	}
	
	public static byte[] transformClass(byte[] classBytes) {
		
		// Create class node
		ClassReader reader = new ClassReader(classBytes);
		ClassNode clazz = new ClassNode();
		reader.accept(clazz, ClassReader.EXPAND_FRAMES);
		
		// Modify methods
		for (MethodNode methodNode : clazz.methods) {
			
			// Check if we should inject into the method
			for (Inject inject : InjectionApi.transformMethods.keySet()) {
				
				if ((methodNode.name + methodNode.desc).equals(inject.methodName() + inject.methodDesc())) {
					
					// Found method
					System.out.println("Found method " + methodNode.name + methodNode.desc);
					
					// Get first or last line depending on where to inject
					AbstractInsnNode injectNode;
					if (inject.injectLocation() == InjectLocation.START) {
						injectNode = methodNode.instructions.getFirst();
					}else {
						injectNode = methodNode.instructions.getLast();
						while (!(injectNode instanceof LabelNode)) {
							injectNode = injectNode.getPrevious();
						}
					}
					
					// So we can offset the line that the method call is injected on
					int offset = inject.lineOffset() + (inject.injectLocation() == InjectLocation.START ? 0 : -1);
					while (offset != 0) {
						if (offset < 0) {
							injectNode = injectNode.getPrevious();
						}
						else if (offset > 0) {
							injectNode = injectNode.getNext();
						}
						
						// Shouldn't happen?
						else {
							break;
						}
						
						if (injectNode instanceof LabelNode) {
							if (offset < 0) {
								offset++;
							}
							else if (offset > 0) {
								offset--;
							}
						}
						
					}
					
					// Inject method call without reflection
					Method method = InjectionApi.transformMethods.get(inject);
					boolean voidMethod = Type.getMethodDescriptor(method).endsWith(")V") || method.getAnnotation(SetVars.class) == null;
					int startingLineNum = AsmUtils.getFirstLineNum(methodNode.instructions);
					InsnList insnList = new InsnList();
					for (int instructionNum = 0; instructionNum < methodNode.instructions.size(); instructionNum++) {
						AbstractInsnNode node = methodNode.instructions.get(instructionNum);
						
						// So exceptions display the correct line numbers
						if (node instanceof LineNumberNode) {
							((LineNumberNode)node).line = startingLineNum + instructionNum;
						}
						
						// Inject here
						if (node == injectNode) {
							Object[] args = new Object[InjectionApi.transformMethods.get(inject).getParameterCount()];
							int j = 0;
							for (Parameter parameter : InjectionApi.transformMethods.get(inject).getParameters()) {
								if (parameter.getAnnotation(GetLocalVar.class) != null) {
									args[j] = parameter.getAnnotation(GetLocalVar.class);
								}
								else if (parameter.getAnnotation(GetStaticVar.class) != null) {
									args[j] = parameter.getAnnotation(GetStaticVar.class);
								}
								else if (parameter.getAnnotation(GetNonStaticVar.class) != null) {
									args[j] = parameter.getAnnotation(GetNonStaticVar.class);
								}
								j++;
							}
							SetVars returnVars = null;
							if (InjectionApi.transformMethods.get(inject).getAnnotation(SetVars.class) != null) {
								returnVars = InjectionApi.transformMethods.get(inject).getAnnotation(SetVars.class);
							}
							InsnList injectList = voidMethod ? AsmUtils.getVoidInvoke(startingLineNum + instructionNum, InjectionApi.injectClasses.get(inject), method.getName(), Type.getMethodDescriptor(method), methodNode, args) : AsmUtils.getObjInvoke(startingLineNum + instructionNum, InjectionApi.injectClasses.get(inject), method.getName(), Type.getMethodDescriptor(method), methodNode, args, returnVars);
							startingLineNum += AsmUtils.getInsnLineCount(injectList);
							insnList.add(injectList);
						}
						
						insnList.add(node);
						
					}
					
					// Rewrite method
					methodNode.instructions = insnList;
					
				}
				
			}
			
		}
		
		// Write classnode to byte array
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		clazz.accept(classWriter);
		
		// For debugging the transformer
		try {
			FileOutputStream out = new FileOutputStream(new File(System.nanoTime() + ".class"));
			out.write(classWriter.toByteArray());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return classWriter.toByteArray();
		
	}
	
}
