package org.fuzzingtool.core.components;

import org.apache.commons.text.RandomStringGenerator;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.Operation;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.fuzzingtool.core.symbolic.basic.SymbolicConstant;
import org.fuzzingtool.core.symbolic.basic.SymbolicVariable;
import org.fuzzingtool.core.symbolic.arithmetic.*;
import org.fuzzingtool.core.symbolic.conversion.StringToInt;
import org.fuzzingtool.core.symbolic.logical.*;
import org.fuzzingtool.core.symbolic.string.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Tracer {
	public final Logger logger;
	// Saves intermediate results of all nodes
	private final Map<Integer, SymbolicNode> intermediate_results = new HashMap<>();

	// Saves a symbolic representation of the entire program
	private final Map<Integer, VariableContext> symbolic_program = new HashMap<>();

	// These two are for crossing function boundaries
	private ArrayList<SymbolicNode> arguments_array = new ArrayList<>();
	private SymbolicNode function_return_value;

	// Caching
	public final Map<Integer, Integer> cached_scopes = new HashMap<>();

	private Integer js_global_object_id = 0;

	private final HashSet<String> used_gids = new HashSet<>();
	private final RandomStringGenerator gid_generator;

	// Short-circuit evaluation
	public final HashSet<Integer> logic_node_full_expression = new HashSet<>();
	private Boolean no_side_effects_allowed = false;

	public Tracer(Logger l) {
		this.logger = l;

		RandomStringGenerator.Builder rand_builder = new RandomStringGenerator.Builder();
		rand_builder.selectFrom('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0');
		this.gid_generator = rand_builder.build();

		resetFunctionReturnValue();
	}

	public void allowSideffects() {
		this.no_side_effects_allowed = false;
	}

	public void forbidSideffects() {
		this.no_side_effects_allowed = true;
	}

	public boolean noSideeffectsAllowed() {
		return this.no_side_effects_allowed;
	}

	public void setArgumentsArray(ArrayList<SymbolicNode> arguments) {
		this.arguments_array = arguments;
	}

	public void argumentToIntermediate(Integer argument_index, Integer node_id_intermediate) {
		assert argument_index >= 0;
		if (argument_index >= this.arguments_array.size()) {
			logger.warning("Tracer::argumentToIntermediate(): Argument index " + argument_index + " out of range");
		}
		this.intermediate_results.put(node_id_intermediate, this.arguments_array.get(argument_index));
	}

	// TODO handle "arguments" array
	public void initializeFunctionScope(Integer context_key) {
		VariableContext new_scope = new VariableContext();
		new_scope.set("this", new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null));
		new_scope.set("arguments", new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null));
		symbolic_program.put(context_key, new_scope);
	}

	/**
	 * Initialize a new variable context if it does not exist.
	 *
	 * @param context_key Key for new variable context
	 */
	public void initializeIfAbsent(Integer context_key) {
		symbolic_program.putIfAbsent(context_key, new VariableContext());
	}

	public void resetFunctionReturnValue() {
		function_return_value = new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
	}

	public void intermediateToFunctionReturnValue(Integer intermediate_key) {
		if (intermediate_results.containsKey(intermediate_key)) {
			this.function_return_value = intermediate_results.get(intermediate_key);
		} else {
			logger.critical("Tracer::setFunctionReturnValueFromIntermediate(): Trying to set return value to result from " + intermediate_key + " but it does not exist");
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
			logger.critical("Tracer.getSymbolicContext(): Context with key " + context_key + " does not exist");
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
			try {
				if (!var_ctx.hasProperty(key)) {
					logger.warning("Tracer::propertyToIntermediate(): Context " + context + " has no property '" + key + "', returning JS.undefined");
				}
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
				logger.critical("Tracer::intermediateToProperty(): Context " + context + " does not exist");
			}
		} else {
			logger.critical("Tracer::intermediateToProperty(): No intermediate result for " + node_id_intermediate);
		}
	}

	/**
	 * Read a symbolic variable from a function scope into an intermediate result.
	 * This function is used by JSReadCurrentFrameSlotNodeGen and JSReadScopeFrameSlotNodeGen.
	 *
	 * @param function_scope Hash-Code of the function object
	 * @param key The name of the variable
	 * @param node_id_intermediate The ID of the new intermediate result
	 * @return A boolean, indicating if the read was successful
	 */
	public boolean frameSlotToIntermediate(int function_scope, Object key, Integer node_id_intermediate) {
		if (symbolic_program.containsKey(function_scope)) {
			VariableContext var_ctx = symbolic_program.get(function_scope);
			try {
				if (var_ctx.hasProperty(key)) {
					intermediate_results.put(node_id_intermediate, var_ctx.get(key));
					return true;
				}
			} catch (IllegalArgumentException iae) {
				logger.critical(iae.getMessage());
			}
			logger.critical("Tracer::frameSlotToIntermediate(): Function scope does not contain variable '" + key + "'");
		} else {
			logger.critical("Tracer::frameSlotToIntermediate(): No function scope " + function_scope + " found");
		}
		return false;
	}

	/**
	 * Writes an intermediate result to a frame slot (e.g. a variable).
	 * Used by JSWriteCurrentFrameSlotNodeGen and JSWriteScopeFrameSlotNodeGen.
	 *
	 * @param function_scope Hash-Code of the function object
	 * @param key The name of the variable
	 * @param node_id_intermediate The key to the intermediate result.
	 */
	public void intermediateToFrameSlot(int function_scope, Object key, Integer node_id_intermediate) {
		if (intermediate_results.containsKey(node_id_intermediate)) {
			if (symbolic_program.containsKey(function_scope)) {
				VariableContext var_ctx = symbolic_program.get(function_scope);
				try {
					var_ctx.set(key, intermediate_results.get(node_id_intermediate));
				} catch (IllegalArgumentException iae) {
					logger.critical(iae.getMessage());
				}
			} else {
				logger.critical("Tracer::intermediateToFrameSlot(): Function scope " + function_scope + " does not exist");
			}
		} else {
			logger.critical("Tracer::intermediateToFrameSlot(): No intermediate result for " + node_id_intermediate);
		}
	}

	public boolean containsVariable(int context, Object key) {
		if (symbolic_program.containsKey(context)) {
			VariableContext var_ctx = symbolic_program.get(context);
			try {
				if (var_ctx.hasProperty(key)) {
					return true;
				}
			} catch (IllegalArgumentException iae) {
				logger.critical(iae.getMessage());
			}
		} else {
			logger.critical("Tracer::containsVariable(): No context " + context);
		}
		return false;
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
			logger.critical("Tracer::getIntermediate(): Cannot get intermediate results for hash " + node_id);
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.INTERNAL_ERROR, null);
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
		arguments_array.clear();
		resetFunctionReturnValue();
		cached_scopes.clear();
		logic_node_full_expression.clear();
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
	 */
	public void addOperation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source_a,
							 Integer node_source_b) {
		// Handle short-circuit evaluation
		if (op == Operation.AND || op == Operation.OR) {
			if (!intermediate_results.containsKey(node_source_a) && !intermediate_results.containsKey(node_source_b)) {
				logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
										" but intermediate result from " + node_source_a + " and " + node_source_b +
										" does not exist");
				return;
			}
			if (!intermediate_results.containsKey(node_source_a)) {
				logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
										" but intermediate result from " + node_source_a +
										" does not exist");
				return;
			}
			if (!intermediate_results.containsKey(node_source_b)) {
				logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() +
										" but intermediate result from " + node_source_b +
										" does not exist");
				return;
			}
			SymbolicNode a = intermediate_results.getOrDefault(node_source_a, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.INTERNAL_ERROR, false));
			SymbolicNode b = intermediate_results.getOrDefault(node_source_b, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.INTERNAL_ERROR, false));
			if (op == Operation.AND) {
				intermediate_results.put(node_target, new And(s, a, b));
			} else {
				intermediate_results.put(node_target, new Or(s, a, b));
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
	 */
	public void addOperation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source) {
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
		intermediate_results.put(node_target, new SymbolicConstant(s, t, v));
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
	 * Add a new operation for Strings
	 *
	 * @param node_target The node-hash of the intermediate result
	 * @param s           Semantic of the language
	 */
	public void addStringOperation(Integer node_target, LanguageSemantic s, Integer operand_intermediate_id,
								   ArrayList<SymbolicNode> arguments, Operation op) {
		if (intermediate_results.containsKey(operand_intermediate_id)) {
			SymbolicNode operand = intermediate_results.get(operand_intermediate_id);
			switch (op) {
				case STR_CONCAT:
					for (SymbolicNode arg: arguments) {
						operand = new Addition(s, operand, arg);
					}
					intermediate_results.put(node_target, operand);
					break;
				case STR_CHAR_AT:
					assert arguments.size() == 1;
					intermediate_results.put(node_target, new StringCharAt(LanguageSemantic.JAVASCRIPT, operand,
																		   arguments.get(0)));
					break;
				case STR_SUBSTR:
					assert arguments.size() == 1 || arguments.size() == 2;
					if (arguments.size() == 1) {
						intermediate_results.put(node_target, new StringSubstr(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0)));
					}
					if (arguments.size() == 2) {
						intermediate_results.put(node_target, new StringSubstr(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0), arguments.get(1)));
					}
					break;
				case STR_INCLUDES:
					assert arguments.size() == 1 || arguments.size() == 2;
					if (arguments.size() == 1) {
						intermediate_results.put(node_target, new StringIncludes(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0)));
					}
					if (arguments.size() == 2) {
						intermediate_results.put(node_target, new StringIncludes(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0), arguments.get(1)));
					}
					break;
				case STR_INDEXOF:
					assert arguments.size() == 1 || arguments.size() == 2;
					if (arguments.size() == 1) {
						intermediate_results.put(node_target, new StringIndexOf(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0)));
					}
					if (arguments.size() == 2) {
						intermediate_results.put(node_target, new StringIndexOf(LanguageSemantic.JAVASCRIPT, operand, arguments.get(0), arguments.get(1)));
					}
					break;
				default:
					logger.critical("Tracer::addStringOperation(): Cannot process operation " + op.name());
			}
		} else {
			logger.critical("Tracer::addStringOperation(): Trying to add operation " + op.name() + " but operand " +
									operand_intermediate_id + " does not exist");
		}
	}

	/**
	 * Add a new operation for Arrays
	 *
	 * @param node_target The node-hash of the intermediate result
	 * @param s           Semantic of the language
	 */
	public void addArrayOperation(Integer node_target, LanguageSemantic s, Integer array_context,
								   ArrayList<SymbolicNode> arguments, Operation op, long arr_length) {
		VariableContext symbolic_array = getSymbolicContext(array_context);
		switch (op) {
			case ARR_LENGTH:
				assert arguments.size() == 1;
				setIntermediate(node_target, new SymbolicConstant(s, ExpressionType.NUMBER_INTEGER, arr_length));
				break;
			case ARR_PUSH:
				assert arguments.size() == 1;
				// size - 1: element is already added, and index...
				symbolic_array.set(arr_length - 1, arguments.get(0));
				break;
			case ARR_JOIN:
				assert arguments.size() == 0 || arguments.size() == 1;

				SymbolicNode spacer = null;
				if (arguments.size() == 0) {
					spacer = new SymbolicConstant(s, ExpressionType.STRING, ",");
				}
				if (arguments.size() == 1) {
					spacer = arguments.get(0);
				}

				// forces string concatenation
				SymbolicNode join_expression;
				join_expression = new SymbolicConstant(s, ExpressionType.STRING, "");
				for (long i = 0; i < arr_length; i++) {
					if (i == arr_length - 1) {
						join_expression = new Addition(s, join_expression,
													   symbolic_array.get(i));
					} else {
						SymbolicNode first_part = new Addition(s, join_expression, symbolic_array.get(i));
						join_expression = new Addition(s, first_part, spacer);
					}
				}
				setIntermediate(node_target, join_expression);
				break;
			default:
				logger.critical("Tracer::addArrayOperation(): Cannot process operation " + op.name());
		}
	}

	/**
	 * This function provides functionality for operands that are called internally and
	 * behave like functions e.g. Math.sqrt(x). It reads all input from {@link #arguments_array}
	 * and writes the result to {@link #function_return_value}.
	 *
	 * @param s Semantic of the language
	 * @param op Operation
	 */
	public void performSingularMethodInvocation(LanguageSemantic s, Operation op) {
		switch (op) {
			case SQRT:
				if (arguments_array.size() == 1) {
					function_return_value = new SquareRoot(s, arguments_array.get(0));
				} else {
					logger.critical("Arguments for Operation SQRT have the wrong size");
					function_return_value = new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.INTERNAL_ERROR, null);
				}
				break;
			case STR_TO_INT:
				if (arguments_array.size() == 1) {
					function_return_value = new StringToInt(s, arguments_array.get(0), new SymbolicConstant(s, ExpressionType.NUMBER_INTEGER, 10));
				} else if (arguments_array.size() == 2) {
					function_return_value = new StringToInt(s, arguments_array.get(0), arguments_array.get(1));
				} else {
					logger.critical("Arguments for Operation STR_TO_INT have the wrong size");
					function_return_value = new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.INTERNAL_ERROR, null);
				}
				break;
			default:
				logger.critical("Operation '" + op.name() + "' is not an internal method call");
		}
	}

	public Integer getJSGlobalObjectId() {
		return this.js_global_object_id;
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
			this.js_global_object_id = global_object_id;
			VariableContext global_object_context = new VariableContext();
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
			symbolic_program.put(global_object_id, global_object_context);
		} else {
			logger.warning("Tracer::initializeProgramContext(): Cannot initialize context with semantic '" +
                                   sem.toString() + "'");
		}
	}

	/**
	 * Create a new Exception for non-allowed side effects that cannot be caught.
	 *
	 * @param message Message of the exception
	 * @return Exception of type "EscalatedException"
	 */
	public static Tracer.SideEffectException createException(String message) {
		return new Tracer.SideEffectException(message);
	}

	/**
	 * An exception for side effects in a manually executed partial 'and' or 'or' statement.
	 * This is used for handling short-circuit evaluation.
	 */
	public static class SideEffectException extends RuntimeException {
		public SideEffectException() {
			super();
		}

		public SideEffectException(String message) {
			super(message);
		}

		public SideEffectException(String message, Throwable cause) {
			super(message, cause);
		}

		public SideEffectException(Throwable cause) {
			super(cause);
		}

		protected SideEffectException(String message, Throwable cause,
									 boolean enableSuppression,
									 boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
	}
}
