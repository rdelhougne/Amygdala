package org.fuzzingtool.core.components;

import org.apache.commons.text.RandomStringGenerator;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.symbolic.*;
import org.fuzzingtool.core.symbolic.arithmetic.*;
import org.fuzzingtool.core.symbolic.basic.SymbolicConstant;
import org.fuzzingtool.core.symbolic.basic.SymbolicVariable;
import org.fuzzingtool.core.symbolic.logical.*;
import org.fuzzingtool.core.symbolic.string.StringLength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Tracer {
	public final Logger logger;
	// Saves intermediate results of all nodes
	private final HashMap<Integer, SymbolicNode> intermediate_results = new HashMap<>();

	// Saves a symbolic representation of the entire program
	private final HashMap<Integer, VariableContext> symbolic_program = new HashMap<>();

	// These two are for crossing function boundaries
	private ArrayList<SymbolicNode> arguments_array = new ArrayList<>();
	private SymbolicNode function_return_value;

	private final HashSet<String> used_gids = new HashSet<>();
	private final RandomStringGenerator gid_generator;

	public Tracer(Logger l) {
		this.logger = l;

		RandomStringGenerator.Builder rand_builder = new RandomStringGenerator.Builder();
		rand_builder.selectFrom('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0');
		this.gid_generator = rand_builder.build();

		resetFunctionReturnValue();
	}

	public void setArgumentsArray(ArrayList<SymbolicNode> arguments) {
		this.arguments_array = arguments;
	}

	public void argumentToIntermediate(Integer argument_index, Integer node_id_intermediate) {
		assert argument_index >= 0;
		if (argument_index >= this.arguments_array.size()) {
			logger.warning("Tracer::argumentToIntermediate(): Argument index " + argument_index + " out of range.");
		}
		this.intermediate_results.put(node_id_intermediate, this.arguments_array.get(argument_index));
	}

	// TODO handle "arguments" array
	public void initializeFunctionScope(Integer context_key) {
		VariableContext new_scope = new VariableContext();
		try {
			new_scope.set("this", new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null));
			new_scope.set("arguments", new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null));
		} catch (SymbolicException.IncompatibleType incompatibleType) {
			incompatibleType.printStackTrace();
		}
		symbolic_program.put(context_key, new_scope);
	}

	public void resetFunctionReturnValue() {
		try {
			function_return_value = new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
		} catch (SymbolicException.IncompatibleType incompatibleType) {
			incompatibleType.printStackTrace();
		}
	}

	public void intermediateToFunctionReturnValue(Integer intermediate_key) {
		if (intermediate_results.containsKey(intermediate_key)) {
			this.function_return_value = intermediate_results.get(intermediate_key);
		} else {
			logger.critical("Tracer.setFunctionReturnValueFromIntermediate(): Trying to set return value to result from " + intermediate_key + " but it does not exist.");
		}
	}

	public void functionReturnValueToIntermediate(Integer intermediate_key) {
		this.intermediate_results.put(intermediate_key, this.function_return_value);
	}

	public String getNewGID() {
		String new_gid = this.gid_generator.generate(8);
		while (used_gids.contains(new_gid)) {
			new_gid = this.gid_generator.generate(8);
		}
		this.used_gids.add(new_gid);
		return new_gid;
	}

	public void setSymbolicContext(Integer context_key, VariableContext context) {
		symbolic_program.put(context_key, context);
	}

	public VariableContext getSymbolicContext(Integer context_key) {
		if (symbolic_program.containsKey(context_key)) {
			return symbolic_program.get(context_key);
		} else {
			logger.critical("Tracer.getSymbolicContext(): Context with key " + context_key + " does not exist.");
			return null;
		}
	}

	/**
	 * Load a symbolic object property to an intermediate result, used by PropertyNode.
	 *
	 * @param context The object identifier, hashCode of input 0
	 * @param key The attribute key
	 * @param node_id_intermediate hashCode of the intermediate result of the PropertyNode
	 */
	public void propertyToIntermediate(Integer context, Object key, Integer node_id_intermediate) {
		if (symbolic_program.containsKey(context)) {
			VariableContext var_ctx = symbolic_program.get(context);
			if (!var_ctx.hasProperty(key)) {
				logger.warning("Tracer::propertyToIntermediate(): Context " + context + " has no property '" + key + "', returning JS.undefined.");
			}
			try {
				intermediate_results.put(node_id_intermediate, var_ctx.get(key));
			} catch (IllegalArgumentException iae) {
				logger.critical(iae.getMessage());
			}
		} else {
			logger.critical("Tracer::propertyToIntermediate(): No symbolic context " + context);
		}
	}

	/**
	 * Set an attribute of a symbolic object, used by WritePropertyNode.
	 *
	 * @param context The object identifier, hashCode of input 0
	 * @param key The attribute key
	 * @param node_id_intermediate hashCode of the intermediate result of the child node
	 */
	public void intermediateToProperty(Integer context, Object key, Integer node_id_intermediate) {
		if (intermediate_results.containsKey(node_id_intermediate)) {
			if (symbolic_program.containsKey(context)) {
				VariableContext var_ctx = symbolic_program.get(context);
				try {
					var_ctx.set(key, intermediate_results.get(node_id_intermediate));
				} catch (IllegalArgumentException iae) {
					logger.critical(iae.getMessage());
				}
			} else {
				logger.critical("Tracer::intermediateToProperty(): Context " + context + " does not exist.");
			}
		} else {
			logger.critical("Tracer::intermediateToProperty(): No intermediate result for " + node_id_intermediate);
		}
	}

	/**
	 * Read a symbolic variable from a list of function scopes into an intermediate result.
	 * This function is used by JSReadCurrentFrameSlotNodeGen and JSReadScopeFrameSlotNodeGen.
	 * Calling from JSReadCurrentFrameSlotNodeGen, frame_stack.length should always be 1.
	 *
	 * @param frame_stack A list of function scope identifiers, ranging from the innermost [0] to outermost [n] scopes.
	 * @param key The name of the variable
	 * @param node_id_intermediate The ID of the new intermediate result
	 */
	public void frameSlotToIntermediate(ArrayList<Integer> frame_stack, Object key, Integer node_id_intermediate) {
		for (Integer function_scope: frame_stack) {
			if (symbolic_program.containsKey(function_scope)) {
				VariableContext var_ctx = symbolic_program.get(function_scope);
				try {
					if (var_ctx.hasProperty(key)) {
						intermediate_results.put(node_id_intermediate, var_ctx.get(key));
						return;
					}
				} catch (IllegalArgumentException iae) {
					logger.critical(iae.getMessage());
				}
			} else {
				logger.critical("Tracer::frameSlotToIntermediate(): No frame slot " + function_scope);
			}
		}
		logger.critical("Tracer::frameSlotToIntermediate(): No function scope with variable '" + key + "' found.");
	}

	/**
	 * Writes an intermediate result to a frame slot (e.g. a variable).
	 * Used by JSWriteCurrentFrameSlotNodeGen and JSWriteScopeFrameSlotNodeGen.
	 * If called by JSWriteCurrentFrameSlotNodeGen, the number of function scopes has
	 * to be 1. The behavior for these two nodes differs significantly.
	 *
	 * @param frame_stack A list of function scopes, starting with the innermost
	 * @param key The name of the variable
	 * @param node_id_intermediate The key to the intermediate result.
	 */
	public void intermediateToFrameSlot(ArrayList<Integer> frame_stack, Object key, Integer node_id_intermediate) {
		if (intermediate_results.containsKey(node_id_intermediate)) {
			if (frame_stack.size() == 1) {
				// Behavior for JSWriteCurrentFrameSlotNodeGen (or block scopes)?
				if (symbolic_program.containsKey(frame_stack.get(0))) {
					VariableContext var_ctx = symbolic_program.get(frame_stack.get(0));
					try {
						var_ctx.set(key, intermediate_results.get(node_id_intermediate));
					} catch (IllegalArgumentException iae) {
						logger.critical(iae.getMessage());
					}
				} else {
					logger.critical("Tracer::intermediateToFrameSlot(): Context " + frame_stack.get(0) + " does not exist.");
				}
			} else if (frame_stack.size() > 1) {
				// Behavior for JSWriteScopeFrameSlotNodeGen
				for (Integer function_scope: frame_stack) {
					if (symbolic_program.containsKey(function_scope)) {
						VariableContext var_ctx = symbolic_program.get(function_scope);
						try {
							if (var_ctx.hasProperty(key)) {
								var_ctx.set(key, intermediate_results.get(node_id_intermediate));
								return;
							}
						} catch (IllegalArgumentException iae) {
							logger.critical(iae.getMessage());
						}
					} else {
						logger.warning("Tracer::intermediateToFrameSlot(): No frame slot " + function_scope);
					}
				}
				logger.critical("Tracer::intermediateToFrameSlot(): No function scope with variable '" + key + "' found.");
			} else {
				logger.critical("Tracer::intermediateToFrameSlot(): No function scopes provided.");
			}
		} else {
			logger.critical("Tracer::intermediateToFrameSlot(): No intermediate result for " + node_id_intermediate);
		}
	}

	/**
	 * Removes the intermediate result of the specified node.
	 *
	 * @param node_id The hash-code of the node
	 */
	public void removeIntermediate(Integer node_id) {
		intermediate_results.remove(node_id);
	}

	/**
	 * Returns an intermediate results for a given node-hash.
	 *
	 * @param node_id The hash-code of the node
	 */
	public SymbolicNode getIntermediate(Integer node_id) {
		if (intermediate_results.containsKey(node_id)) {
			assert intermediate_results.get(node_id) != null;
			return intermediate_results.get(node_id);
		} else {
			logger.warning("Tracer::get_intermediate(): Cannot get intermediate results for hash " + node_id);
			return null;
		}
	}

	/**
	 * Sets an intermediate result for a given node-hash.
	 *
	 * @param node_target The hash-code of the node
	 * @param expression The symbolic expression
	 */
	public void setIntermediate(Integer node_target, SymbolicNode expression) {
		intermediate_results.put(node_target, expression);
	}

	/**
	 * Pass through an intermediate result from old node to new node.
	 * The old result is not deleted.
	 *
	 * @param new_node_id        The hash-code of the new node
	 * @param old_intermediate_result The hash-code of the old node
	 */
	public void passThroughIntermediate(Integer new_node_id, Integer old_intermediate_result) {
		if (intermediate_results.containsKey(old_intermediate_result)) {
			intermediate_results.put(new_node_id, intermediate_results.get(old_intermediate_result));
		} else {
			logger.critical("Tracer::passThroughIntermediate(): No intermediate result for key " + old_intermediate_result);
		}
	}

	/**
	 * Deletes all intermediate results from intermediate_results and the symbolic frame.
	 * This should be used by terminate(), e.g. when the program restarts.
	 */
	public void clearAll() {
		intermediate_results.clear();
		symbolic_program.clear();
	}

	/**
	 * Resets all states and re-initializes the program context to the given semantic.
	 *
	 * @param ls The language semantic used to re-initialize the program context
	 */
	public void reset(LanguageSemantic ls, Integer global_object_id) {
		clearAll();
		initializeProgramContext(ls, global_object_id);
	}

	/**
	 * This function adds a new symbolic operation to the intermediate results of the given node-hash.
	 *
	 * @param node_target   Node-hash of the new intermediate result
	 * @param op            ExpressionType of Operation, see {@link Operation} for all available operation types
	 * @param node_source_a Left-hand operator
	 * @param node_source_b Right-hand operator
	 * @throws SymbolicException.WrongParameterSize
	 */
	public void addOperation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source_a,
							 Integer node_source_b) throws
			SymbolicException.WrongParameterSize {
		// handle early discard
		if (op == Operation.OR) {
			if (!intermediate_results.containsKey(node_source_a) && !intermediate_results.containsKey(node_source_b)) {
				logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
										" but intermediate result from " + node_source_a + " and " + node_source_b +
										" does not exist");
				return;
			}
			try {
				SymbolicNode a = intermediate_results.getOrDefault(node_source_a, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, false));
				SymbolicNode b = intermediate_results.getOrDefault(node_source_b, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, false));
				intermediate_results.put(node_target, new Or(s, a, b));
			} catch (SymbolicException.IncompatibleType incompatibleType) {
				incompatibleType.printStackTrace();
			}
		} else if (op == Operation.AND) {
			if (!intermediate_results.containsKey(node_source_a) && !intermediate_results.containsKey(node_source_b)) {
				logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
										" but intermediate result from " + node_source_a + " and " + node_source_b +
										" does not exist");
				return;
			}
			try {
				SymbolicNode a = intermediate_results.getOrDefault(node_source_a, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, true));
				SymbolicNode b = intermediate_results.getOrDefault(node_source_b, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, true));
				intermediate_results.put(node_target, new And(s, a, b));
			} catch (SymbolicException.IncompatibleType incompatibleType) {
				incompatibleType.printStackTrace();
			}
		} else {
			if (intermediate_results.containsKey(node_source_a) && intermediate_results.containsKey(node_source_b)) {
				SymbolicNode a = intermediate_results.get(node_source_a);
				SymbolicNode b = intermediate_results.get(node_source_b);
				switch (op) {
					case ADDITION:
						intermediate_results.put(node_target, new Addition(s, a, b));
						break;
					case SUBTRACTION:
						intermediate_results.put(node_target, new Subtraction(s, a, b));
						break;
					case MULTIPLICATION:
						intermediate_results.put(node_target, new Multiplication(s, a, b));
						break;
					case DIVISION:
						intermediate_results.put(node_target, new Division(s, a, b));
						break;
					case MODULO:
						intermediate_results.put(node_target, new Modulo(s, a, b));
						break;
					case EQUAL:
						intermediate_results.put(node_target, new Equal(s, a, b));
						break;
					case STRICT_EQUAL:
						intermediate_results.put(node_target, new StrictEqual(s, a, b));
						break;
					case GREATER_EQUAL:
						intermediate_results.put(node_target, new GreaterEqual(s, a, b));
						break;
					case GREATER_THAN:
						intermediate_results.put(node_target, new GreaterThan(s, a, b));
						break;
					case LESS_EQUAL:
						intermediate_results.put(node_target, new LessEqual(s, a, b));
						break;
					case LESS_THAN:
						intermediate_results.put(node_target, new LessThan(s, a, b));
						break;
					default:
						logger.critical("Tracer::add_operation(): Unknown operation " + op.toString());
				}
			} else {
				if (!intermediate_results.containsKey(node_source_a) && !intermediate_results.containsKey(node_source_b)) {
					logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
											" but intermediate result from " + node_source_a + " and " + node_source_b +
											" does not exist");
				} else if (!intermediate_results.containsKey(node_source_a)) {
					logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
											" but intermediate result from " + node_source_a + " does not exist");
				} else {
					logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
											" but intermediate result from " + node_source_b + " does not exist");
				}
			}
		}
	}

	/**
	 * This is an overloaded method of {@link #addOperation(Integer, LanguageSemantic, Operation, Integer, Integer)}
	 * for unary operations.
	 *
	 * @param node_target Node-hash of the new intermediate result
	 * @param op          ExpressionType of Operation, see {@link Operation} for all available operation types
	 * @param node_source child operator
	 * @throws SymbolicException.WrongParameterSize
	 */
	public void addOperation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source) throws
			SymbolicException.WrongParameterSize {
		if (intermediate_results.containsKey(node_source)) {
			SymbolicNode k = intermediate_results.get(node_source);
			switch (op) {
				case NOT:
					intermediate_results.put(node_target, new Not(s, k));
					break;
				case UNARY_MINUS:
					intermediate_results.put(node_target, new UnaryMinus(s, k));
					break;
				case UNARY_PLUS:
					intermediate_results.put(node_target, new UnaryPlus(s, k));
					break;
				case STR_LENGTH:
					intermediate_results.put(node_target, new StringLength(s, k));
					break;
				default:
					logger.critical("Tracer::add_operation(): Unknown operation " + op.toString());
			}
		} else {
			logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
                            " but intermediate result from " + node_source + " does not exist");
		}
	}

	/**
	 * Add a new symbolic constant to the intermediate results of node_target.
	 *
	 * @param node_target The node-hash of the intermediate result
	 * @param s           Semantic of the language
	 * @param t           ExpressionType of the new constant value, see {@link ExpressionType}
	 * @param v           Value of the constant, the value is automatically casted
	 */
	public void addConstant(Integer node_target, LanguageSemantic s, ExpressionType t, Object v) {
		try {
			intermediate_results.put(node_target, new SymbolicConstant(s, t, v));
		} catch (SymbolicException.IncompatibleType incompatibleType) {
			logger.critical(incompatibleType.getMessage());
		}
	}

	/**
	 * Add a new symbolic variable to the intermediate results of node_target.
	 *
	 * @param node_target The node-hash of the intermediate result
	 * @param s           Semantic of the language
	 * @param id          VariableIdentifier of the new variable, see {@link VariableIdentifier}
	 */
	public void addVariable(Integer node_target, LanguageSemantic s, VariableIdentifier id) {
		intermediate_results.put(node_target, new SymbolicVariable(s, id));
	}

	/**
	 * Initializes all objects and functions that are provided by the VM by default.
	 *
	 * @param sem The language semantic
	 * @param global_object_id The ID of the global variable context
	 */
	public void initializeProgramContext(LanguageSemantic sem, Integer global_object_id) {
		if (sem == LanguageSemantic.JAVASCRIPT) {
			// Initialize JavaScript global objects
			VariableContext global_object_context = new VariableContext();
			try {
				// TODO biggest todo ever
				// output from jsglobalsearch.js
				global_object_context.set("Object", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Function", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("String", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Date", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Number", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Boolean", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("RegExp", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Math", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("JSON", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("NaN", new SymbolicConstant(sem, ExpressionType.NUMBER_NAN, null));
				global_object_context.set("Infinity", new SymbolicConstant(sem, ExpressionType.NUMBER_POS_INFINITY, null));
				global_object_context.set("undefined", new SymbolicConstant(sem, ExpressionType.UNDEFINED, null));
				global_object_context.set("isNaN", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("isFinite", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("parseFloat", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("parseInt", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("encodeURI", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("encodeURIComponent", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("decodeURI", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("decodeURIComponent", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("eval", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("escape", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("unescape", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Error", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("EvalError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("RangeError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("ReferenceError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("SyntaxError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("TypeError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("URIError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("ArrayBuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Int8Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Uint8Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Uint8ClampedArray", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Int16Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Uint16Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Int32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Uint32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Float32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Float64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("BigInt64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("BigUint64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("DataView", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("BigInt", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Polyglot", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Map", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Set", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("WeakMap", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("WeakSet", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Symbol", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Reflect", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Proxy", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Promise", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("SharedArrayBuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Atomics", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("globalThis", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Graal", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("quit", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("readline", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("read", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("readbuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("load", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("loadWithNewGlobal", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("console", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("print", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("printErr", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("performance", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Java", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("Packages", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("java", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("javafx", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("javax", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("com", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("org", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("edu", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
				global_object_context.set("arguments", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
			} catch (SymbolicException.IncompatibleType incompatibleType) {
				logger.critical("Tracer::initializeProgramContext(): Cannot initialize JavaScript program context");
			}
			symbolic_program.put(global_object_id, global_object_context);
		} else {
			logger.warning("Tracer::initializeProgramContext(): Cannot initialize context with semantic '" +
                                   sem.toString() + "'");
		}
	}
}
