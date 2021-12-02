package pro.faithful.agentInjectionApi.inject.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import pro.faithful.agentInjectionApi.inject.annotations.GetLocalVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetNonStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.GetStaticVar;
import pro.faithful.agentInjectionApi.inject.annotations.SetVars;

public class AsmUtils {
	
	public static InsnList getVoidInvoke(int lineNum, Class<?> owner, String method, String methodDesc, MethodNode victim, Object[] args) {
		InsnList insnList = new InsnList();
		// Invoke injection method
		LabelNode line1 = new LabelNode(new Label());
		insnList.add(line1);
		insnList.add(new LineNumberNode(lineNum, line1)); lineNum++;
		getArgs(insnList, victim, args);
		insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner.getName().replace(".", "/"), method, methodDesc));
		
		return insnList;
	}
	
	public static InsnList getObjInvoke(int lineNum, Class<?> owner, String method, String methodDesc, MethodNode victim, Object[] args, SetVars setVars) {
		InsnList insnList = new InsnList();
		
		int mapping = getUnusedVarInt(victim);
		createThisIfNotExist(insnList, victim, owner);
		
		// Invoke injection method
		LabelNode line1 = new LabelNode(new Label());
		insnList.add(line1);
		insnList.add(new LineNumberNode(lineNum, line1)); lineNum++;
		getArgs(insnList, victim, args);
		insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner.getName().replace(".", "/"), method, methodDesc));
		insnList.add(new VarInsnNode(Opcodes.ASTORE, mapping));
		
		// Set each return value
		// locals
		for (String returnVar : setVars.localNames()) {
			if (returnVar.isEmpty()) {
				continue;
			}
			LocalVariableNode returnVarInt = getVarFromName(victim, returnVar);
			LabelNode lineI = new LabelNode(new Label());
			insnList.add(lineI);
			insnList.add(new LineNumberNode(lineNum, lineI)); lineNum++;
			insnList.add(new VarInsnNode(Opcodes.ALOAD, mapping));
			insnList.add(new LdcInsnNode((setVars.useExplicitPrefixes() ? "local." : "") + returnVar));
			insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			if (isPrimitive(returnVarInt.desc)) {
				castPrimitive(returnVarInt.desc, insnList);
			}else {
				if (returnVarInt.desc.startsWith("L") || returnVarInt.desc.endsWith(";")) {
					System.err.println("Error: Desc starts with L or ends with ;. If the agent fails to load it may be because of this");
				}
				insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, returnVarInt.desc));
			}
			insnList.add(new VarInsnNode(Opcodes.ASTORE, returnVarInt.index));
		}
		// instances
		for (String instanceCsv : setVars.instanceNamesCsv()) {
			if (instanceCsv.isEmpty()) {
				continue;
			}
			List<String> items = Arrays.asList(instanceCsv.split("\\s*,\\s*"));
			if (items.size() != 4) {
				System.err.println("Incorrect csv format for\"" + instanceCsv + "\"");
				continue;
			}
			LabelNode lineI = new LabelNode(new Label());
			insnList.add(lineI);
			insnList.add(new LineNumberNode(lineNum, lineI)); lineNum++;
			insnList.add(new VarInsnNode(Opcodes.ALOAD, getVarFromName(victim, items.get(0)).index));
			insnList.add(new VarInsnNode(Opcodes.ALOAD, mapping));
			insnList.add(new LdcInsnNode((setVars.useExplicitPrefixes() ? "instance." : "") + items.get(2)));
			insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			if (isPrimitive(items.get(3))) {
				castPrimitive(items.get(3), insnList);
			}else {
				if (items.get(3).startsWith("L") || items.get(3).endsWith(";")) {
					System.err.println("Error: Desc starts with L or ends with ;. If the agent fails to load it may be because of this");
				}
				insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, items.get(3)));
			}
			insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, items.get(1), items.get(2), items.get(3)));
		}
		// statics
		for (String staticCsv : setVars.staticNamesCsv()) {
			if (staticCsv.isEmpty()) {
				continue;
			}
			System.out.println(staticCsv);
			List<String> items = Arrays.asList(staticCsv.split("\\s*,\\s*"));
			if (items.size() != 3) {
				System.err.println("Incorrect csv format for\"" + staticCsv + "\"");
				continue;
			}
			LabelNode lineI = new LabelNode(new Label());
			insnList.add(lineI);
			insnList.add(new LineNumberNode(lineNum, lineI)); lineNum++;
			insnList.add(new VarInsnNode(Opcodes.ALOAD, mapping));
			insnList.add(new LdcInsnNode((setVars.useExplicitPrefixes() ? "static." : "") + items.get(1)));
			insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			if (isPrimitive(items.get(2))) {
				castPrimitive(items.get(2), insnList);
			}else {
				if (items.get(2).startsWith("L") || items.get(2).endsWith(";")) {
					System.err.println("Error: Desc starts with L or ends with ;. If the agent fails to load it may be because of this");
				}
				insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, items.get(2)));
			}
			insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC, items.get(0), items.get(1), items.get(2)));
		}
		
		return insnList;
	}
	
	public static void getArgs(InsnList insnList, MethodNode victim, Object[] args) {
		for (Object arg : args) {
			if (arg instanceof GetLocalVar) {
				insnList.add(new VarInsnNode(Opcodes.ALOAD, getVarFromName(victim, ((GetLocalVar)arg).name()).index));
			}
			else if (arg instanceof GetStaticVar) {
				GetStaticVar getStaticVar = (GetStaticVar)arg;
				insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, getStaticVar.owner(), getStaticVar.name(), getStaticVar.desc()));
			}
			else if (arg instanceof GetNonStaticVar) {
				GetNonStaticVar getNonStaticVar = (GetNonStaticVar)arg;
				insnList.add(new VarInsnNode(Opcodes.ALOAD, getVarFromName(victim, getNonStaticVar.instanceName()).index));
				insnList.add(new FieldInsnNode(Opcodes.GETFIELD, getNonStaticVar.owner(), getNonStaticVar.name(), getNonStaticVar.desc()));
			}
		}
	}
	
	public static LabelNode getFirstLabel(InsnList insnList) {
		AbstractInsnNode node = insnList.getFirst();
		while (!(node instanceof LabelNode)) {
			node = node.getNext();
		}
		return (LabelNode) node;
	}
	
	public static LabelNode getLastLabel(InsnList insnList) {
		AbstractInsnNode node = insnList.getLast();
		while (!(node instanceof LabelNode)) {
			node = node.getPrevious();
		}
		return (LabelNode) node;
	}
	
	public static void createThisIfNotExist(InsnList insnList, MethodNode victim, Class<?> clazz) {
		if (victim.access > Opcodes.ACC_STATIC) {
			return;
		}
		if (getVarFromName(victim, "this") != null)
			return;
		victim.localVariables.add(new LocalVariableNode("this", Type.getDescriptor(clazz), null, getFirstLabel(insnList), getLastLabel(insnList), getUnusedVarInt(victim)));
	}
	
	public static int getUnusedVarInt(MethodNode methodNode) {
		int returnObj = 0;
		while (methodContainsVar(methodNode, returnObj)) {
			returnObj++;
		}
		return returnObj;
	}
	
	public static int getFirstLineNum(InsnList insnList) {
		for (int i = 0; i < insnList.size(); i++) {
			AbstractInsnNode node = insnList.get(i);
			if (node instanceof LineNumberNode)
				return ((LineNumberNode)node).line;
		}
		return -1;
	}
	
	public static int getInsnLineCount(InsnList insnList) {
		int num = 0;
		for (int i = 0; i < insnList.size(); i++) {
			AbstractInsnNode node = insnList.get(i);
			if (node instanceof LineNumberNode)
				num++;
		}
		return num;
	}
	
	// Only works if the method was written by a human, some obfuscators or optimizers may break this
	public static LocalVariableNode getVarFromName(MethodNode node, String name) {
		if (node.localVariables == null)
			return null;
		for (LocalVariableNode localVariableNode : node.localVariables) {
			if (localVariableNode.name.equals(name))
				return localVariableNode;
		}
		return null;
	}
	
	public static boolean methodContainsVar(MethodNode node, int var) {
		if (node.localVariables == null)
			return false;
		for (LocalVariableNode localVariableNode : node.localVariables) {
			if (localVariableNode.index == var)
				return true;
		}
		for (AbstractInsnNode insn : node.instructions) {
			if (insn instanceof VarInsnNode) {
				VarInsnNode varInsnNode = (VarInsnNode)insn;
				if (varInsnNode.var == var)
					return true;
			}
		}
		return false;
	}
	
	public static void castPrimitive(String desc, InsnList insnList) {
		
		String checkCast = "";
		switch (desc) {
		case "J":
			checkCast = "java/lang/Long";
			break;
		case "F":
			checkCast = "java/lang/Float";
			break;
		case "D":
			checkCast = "java/lang/Double";
			break;
		case "Z":
			checkCast = "java/lang/Boolean";
			break;
		case "C":
			checkCast = "java/lang/Character";
			break;
		case "I":
			checkCast = "java/lang/Integer";
			break;
		case "S":
			checkCast = "java/lang/Short";
			break;
		case "B":
			checkCast = "java/lang/Byte";
			break;
		default:
			break;
		}
		
		insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, checkCast));
		
		String owner = checkCast, name = "";
		
		switch (desc) {
		case "J":
			name = "longValue";
			break;
		case "F":
			name = "floatValue";
			break;
		case "D":
			name = "doubleValue";
			break;
		case "Z":
			name = "booleanValue";
			break;
		case "C":
			name = "charValue";
			break;
		case "I":
			name = "intValue";
			break;
		case "S":
			name = "shortValue";
			break;
		case "B":
			name = "byteValue";
			break;
		default:
			break;
		}
		
		insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, "()" + desc));
		
	}
	
	public static boolean isPrimitive(String desc) {
		List<String> prims = Arrays.asList("J", "F", "D", "Z", "C", "I", "S", "B");
		return prims.contains(desc);
	}
	
	public static String getPrimitiveName(String desc) {
		String name = "";
		
		switch (desc) {
		case "J":
			name = "long";
			break;
		case "F":
			name = "float";
			break;
		case "D":
			name = "double";
			break;
		case "Z":
			name = "boolean";
			break;
		case "C":
			name = "char";
			break;
		case "I":
			name = "int";
			break;
		case "S":
			name = "short";
			break;
		case "B":
			name = "byte";
			break;
		default:
			break;
		}
		return name;
	}
	
}
