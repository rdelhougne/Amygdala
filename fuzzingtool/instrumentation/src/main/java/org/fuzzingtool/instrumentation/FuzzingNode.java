package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.truffleinterop.InteropList;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.BranchingNodeAttribute;
import org.fuzzingtool.core.components.CustomError;
import org.fuzzingtool.core.components.TimeProbe;
import org.fuzzingtool.core.components.VariableContext;
import org.fuzzingtool.core.components.VariableIdentifier;
import org.fuzzingtool.core.symbolic.ExpressionType;
import org.fuzzingtool.core.symbolic.LanguageSemantic;
import org.fuzzingtool.core.symbolic.Operation;
import org.fuzzingtool.core.symbolic.SymbolicNode;
import org.fuzzingtool.core.symbolic.arithmetic.Addition;
import org.fuzzingtool.core.symbolic.arithmetic.Subtraction;
import org.fuzzingtool.core.symbolic.basic.SymbolicConstant;
import org.fuzzingtool.core.visualization.ASTVisualizer;
import org.graalvm.collections.Pair;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuzzingNode extends ExecutionEventNode {
	private final TruffleInstrument.Env environment;
	private final Amygdala amygdala;
	private static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

	// Capture group 2 is variable name
	private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("(var\\s+|let\\s+|const\\s+)?\\s*([A-Za-z_]\\w*)\\s*=.*");
	private static final Pattern INCREMENT_PATTERN = Pattern.compile("[A-Za-z_]\\w*\\+\\+");
	private static final Pattern DECREMENT_PATTERN = Pattern.compile("[A-Za-z_]\\w*--");
	private static final Pattern METHOD_PATTERN = Pattern.compile(".*\\.([a-zA-Z_]\\w*)\\(.*\\)");
	private static final Pattern BRANCH_PATTERN = Pattern.compile("(if|for|while)\\s*\\((.*)\\)\\s*[{\\n]");

	private final EventContext event_context;
	private final SourceSection source_section;
	private final Node instrumented_node;
	private final String instrumented_node_type;
	private final int instrumented_node_hash;
	private final int source_relative_identifier;

	// Coverage
	private final boolean is_root_tag;
	private final boolean is_statement_tag;
	private boolean covered = false;

	// Input node config
	private final boolean is_input_node;
	private final VariableIdentifier input_variable_identifier;

	// Hash-Codes of all instrumentable direct children, in-order
	ArrayList<Pair<Integer, String>> child_hashes;

	// Various save spots
	// used by PropertyNode and WritePropertyNode to save the context of the operation
	private Object context_object = null;
	// used by ReadElementNode and WriteElementNode to determine the array index
	private Object element_access = null;
	// used by Call1..NNodes to construct the arguments array
	private final ArrayList<SymbolicNode> arguments_array = new ArrayList<>();
	// used by JSEqualNodeGen to compare the types if the custom error class "equal_is_strict_equal" is enabled
	private String type_of_first_equal_input = "";
	// used by all nodes to cache a custom error exception because it cannot be thrown at onInputValue
	private CustomError.EscalatedException cached_exception = null;
	// text representation of the branch predicate in an IfNode or WhileNode
	String branch_predicate;
	// attribute name, used for properties and frame slots
	String attribute_name;
	// behavior for DualNode
	boolean dual_node_is_increment;
	boolean dual_node_is_decrement;
	// cached symbolic array for ConstantArrayLiteralNode
	VariableContext cached_constant_array = null;
	// function argument index for AccessIndexedArgumentNode
	Integer argument_index;

	public FuzzingNode(TruffleInstrument.Env env, Amygdala amy, EventContext ec) {
		this.amygdala = amy;
		this.environment = env;
		this.event_context = ec;

		this.source_section = ec.getInstrumentedSourceSection();
		this.instrumented_node = ec.getInstrumentedNode();
		this.instrumented_node_type = instrumented_node.getClass().getSimpleName();
		this.instrumented_node_hash = instrumented_node.hashCode();
		this.source_relative_identifier = getSourceRelativeIdentifier(source_section, instrumented_node);

		this.child_hashes = getChildHashes(instrumented_node);

		if (amygdala.isFunctionVisEnabled() && instrumented_node_type.equals("MaterializedFunctionBodyNode")) {
			StringBuilder save_name = new StringBuilder();
			save_name.append("function_");
			save_name.append(source_section.getStartLine()).append("-");
			save_name.append(source_section.getEndLine()).append("_");
			RootNode rn = instrumented_node.getRootNode();
			if (rn != null) {
				save_name.append(JSNodeUtil.resolveName(rn).replace(":", ""));
			} else {
				save_name.append("(unknown)");
			}
			save_name.append(".svg");
			File save_path = new File(Paths.get(amygdala.getResultsPath(), "ast", save_name.toString()).toString());
			if (!save_path.exists()) {
				ASTVisualizer av = new ASTVisualizer(instrumented_node, amygdala.logger);
				av.saveImage(save_path);
			}
		}

		if (instrumented_node instanceof JSConstantNode) {
			Pair<Boolean, VariableIdentifier> input_node_configuration = amygdala.getInputNodeConfiguration(source_section.getStartLine());
			is_input_node = input_node_configuration.getLeft();
			input_variable_identifier = input_node_configuration.getRight();
		} else {
			is_input_node = false;
			input_variable_identifier = null;
		}

		this.is_root_tag = ec.hasTag(StandardTags.RootTag.class);
		this.is_statement_tag = ec.hasTag(StandardTags.StatementTag.class);

		if (!amygdala.node_type_instrumented.containsKey(instrumented_node_type)) {
			amygdala.node_type_instrumented.put(instrumented_node_type, new BitSet(7));
		}

		if (instrumented_node instanceof WhileNode || instrumented_node instanceof IfNode) {
			this.branch_predicate = extractPredicate();
		}

		if (instrumented_node instanceof DualNode) {
			Matcher inc_matcher = INCREMENT_PATTERN.matcher(source_section.getCharacters().toString());
			Matcher dec_matcher = DECREMENT_PATTERN.matcher(source_section.getCharacters().toString());
			this.dual_node_is_increment = inc_matcher.matches();
			this.dual_node_is_decrement = dec_matcher.matches();
		}

		if (instrumented_node instanceof AccessIndexedArgumentNode) {
			AccessIndexedArgumentNode aian = (AccessIndexedArgumentNode) instrumented_node;
			this.argument_index = aian.getIndex();
		}

		this.attribute_name = getAttributeName();
	}

	private String getSignatureString() {
		String node_type_padded = String.format("%1$-" + 36 + "s", instrumented_node_type);
		String hash_padded = String.format("%1$-" + 12 + "s", instrumented_node_hash);
		if (source_section != null && source_section.isAvailable()) {
			int start_line = source_section.getStartLine();
			String line_padded;
			if (start_line >= 0) {
				line_padded = String.format("%1$" + 3 + "s", start_line);
			} else {
				line_padded = "  ~";
			}
			String characters = source_section.getCharacters().toString().replace("\n", "");
			String characters_cropped = Logger.capBack(characters, 16);
			return "[" + node_type_padded + " " + hash_padded + " " + line_padded + ":" + characters_cropped + "]";
		} else {
			return "[" + node_type_padded + " " + hash_padded + "     (NO SOURCE)]";
		}
	}

	public static Integer getSourceRelativeIdentifier(SourceSection source_section, Node node) {
		if (node != null && source_section != null && source_section.isAvailable()) {
			String identifier = source_section.getSource().getURI().toString()
					+ ":" + source_section.getCharIndex()
					+ ":" + source_section.getCharEndIndex()
					+ ":" + node.getClass().getSimpleName();
			return identifier.hashCode();
		} else {
			return 0;
		}
	}

	private String getLocalScopesString(VirtualFrame frame) {
		StringBuilder builder = new StringBuilder();
		Iterable<Scope> a = environment.findLocalScopes(instrumented_node, frame);
		if (a != null) {
			builder.append("=== LOCAL SCOPES ===\n\n");
			for (Scope s: a) {
				builder.append("Scope Object: ").append(s.toString()).append("\n");
				try {
					builder.append("getName: ").append(s.getName()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getReceiverName: ").append(s.getReceiverName()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getReceiver: ").append(s.getReceiver().toString()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getVariables: ").append(s.getVariables().toString()).append("\n");
					if (INTEROP.hasMembers(s.getVariables())) {
						builder.append("Members:\n");
						InteropList members = (InteropList) INTEROP.getMembers(s.getVariables());
						for (int i = 0; i < INTEROP.getArraySize(members); i++) {
							builder.append(INTEROP.readArrayElement(members, i).toString()).append("\n");
						}
					} else {
						builder.append("Has no Members\n");
					}
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getNode: ").append(s.getNode().toString()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getArguments: ").append(s.getArguments().toString()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				try {
					builder.append("getRootInstance: ").append(s.getRootInstance().toString()).append("\n");
				} catch (java.lang.Exception ex) {
					builder.append("NOT GOOD\n");
				}
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	private static String objectToString(Object obj) {
		String info = obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
		if (obj instanceof Integer || obj instanceof Double || obj instanceof Boolean || obj instanceof String) {
			info = info + " (" + obj.toString() + ")";
		}
		return info;
	}

	/**
	 * Returns the hash-code of the current receiver object instance (aka. "this" in JavaScript)
	 *
	 * @param frame The current frame
	 * @return The corresponding hash-code, or 0 if an error occurs
	 */
	private Integer getThisObjectHash(VirtualFrame frame) {
		Iterable<Scope> localScopes = environment.findLocalScopes(instrumented_node, frame);
		if (localScopes != null && localScopes.iterator().hasNext()) {
			Scope innermost_scope = localScopes.iterator().next();
			try {
				return System.identityHashCode(innermost_scope.getReceiver());
			} catch (java.lang.Exception ex) {
				amygdala.logger.critical("ExecutionEventNode.getThisObjectHash(): Node " + getSignatureString() + " has no Receiver-Object");
				return 0;
			}
		}
		amygdala.logger.critical("ExecutionEventNode.getThisObjectHash(): Node " + getSignatureString() + " has no local scopes");
		return 0;
	}

	@Override
	public void onEnter(VirtualFrame frame) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[32m→\033[0m");
		}

		boolean was_instrumented_on_enter = true;
		switch (instrumented_node_type) {
			case "Call0Node":
			case "Call1Node":
			case "CallNNode":
				onEnterBehaviorCallNode(frame);
				break;
			case "Invoke0Node":
			case "Invoke1Node":
			case "InvokeNNode":
				onEnterBehaviorInvokeNode(frame);
				break;
			case "JSNewNodeGen":
				onEnterBehaviorJSNewNodeGen(frame);
				break;
			case "MaterializedFunctionBodyNode":
				onEnterBehaviorFunctionBodyNode(frame);
				break;
			case "JSStringIndexOfNodeGen":
			case "JSStringConcatNodeGen":
			case "JSStringSubstrNodeGen":
			case "DeclareProviderNode":
			case "BreakNode":
			case "JSArrayJoinNodeGen":
			case "EchoTargetValueNode": //TODO verify...
			case "MaterializedTargetablePropertyNode": //TODO verify...
			case "ForInIteratorPrototypeNextNodeGen":
				// Do nothing. These nodes should not have any behavior or are instrumented otherwise.
				break;
			default:
				was_instrumented_on_enter = false;
		}

		if (!covered) {
			if (is_statement_tag) {
				amygdala.coverage.addStatementCovered(source_relative_identifier);
			}
			if (is_root_tag) {
				amygdala.coverage.addRootCovered(source_relative_identifier);
			}
			covered = true;
		}

		// node was executed
		amygdala.node_type_instrumented.get(instrumented_node_type).set(0);
		if (was_instrumented_on_enter) {
			amygdala.node_type_instrumented.get(instrumented_node_type).set(1);
		}

		this.cached_exception = null;
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
	}

	@Override
	public void onInputValue(VirtualFrame frame, EventContext input_context, int input_index,
								Object input_value) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[34m•\033[0m");
		}

		boolean was_instrumented_on_input_value = true;
		switch (instrumented_node_type) {
			case "IfNode":
				onInputValueBehaviorIfNode(frame, input_context, input_index, input_value);
				break;
			case "WhileNode":
				onInputValueBehaviorWhileNode(frame, input_context, input_index, input_value);
				break;
			case "GlobalPropertyNode":
			case "PropertyNode":
			case "WritePropertyNode":
				onInputValueBehaviorPropertyNode(frame, input_context, input_index, input_value);
				break;
			case "ReadElementNode":
			case "WriteElementNode":
			case "CompoundWriteElementNode":
				onInputValueBehaviorReadWriteElementNode(frame, input_context, input_index, input_value);
				break;
			case "Call0Node":
			case "Call1Node":
			case "CallNNode":
				onInputValueBehaviorCallNode(frame, input_context, input_index, input_value);
				break;
			case "Invoke0Node":
			case "Invoke1Node":
			case "InvokeNNode":
				onInputValueBehaviorInvokeNode(frame, input_context, input_index, input_value);
				break;
			case "JSNewNodeGen":
				onInputValueBehaviorJSNewNodeGen(frame, input_context, input_index, input_value);
				break;
			case "JSEqualNodeGen":
				onInputValueBehaviorJSEqualNodeGen(frame, input_context, input_index, input_value);
				break;
			default:
				was_instrumented_on_input_value = false;
		}

		if (was_instrumented_on_input_value) {
			amygdala.node_type_instrumented.get(instrumented_node_type).set(2);
		}

		if (amygdala.custom_error.someEnabled() && source_section != null) {
			try {
				amygdala.custom_error.inspectInputValue(instrumented_node_type, input_value, input_index, source_section.getStartLine());
			} catch (CustomError.EscalatedException ee) {
				//throw event_context.createError(ee); // does not work...
				this.cached_exception = ee;
			}
		}
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
	}

	@Override
	public void onReturnValue(VirtualFrame frame, Object result) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[31m↵\033[0m");
		}

		boolean was_instrumented_on_return_value = true;
		switch (instrumented_node_type) {
			// ===== JavaScript Read/Write =====
			case "GlobalPropertyNode":
				onReturnBehaviorGlobalPropertyNode(frame, result);
				break;
			case "GlobalObjectNode":
				onReturnBehaviorGlobalObjectNode(frame, result);
				break;
			case "PropertyNode":
				onReturnBehaviorPropertyNode(frame, result);
				break;
			case "WritePropertyNode":
				onReturnBehaviorWritePropertyNode(frame, result);
				break;
			case "JSReadCurrentFrameSlotNodeGen":
				onReturnBehaviorJSReadCurrentFrameSlotNodeGen(frame, result);
				break;
			case "JSWriteCurrentFrameSlotNodeGen":
				onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(frame, result);
				break;
			case "JSReadScopeFrameSlotNodeGen":
			case "JSReadScopeFrameSlotWithTDZNodeGen":
				onReturnBehaviorJSReadScopeFrameSlotNodeGen(frame, result);
				break;
			case "JSWriteScopeFrameSlotNodeGen":
				onReturnBehaviorJSWriteScopeFrameSlotNodeGen(frame, result);
				break;


			// ===== JavaScript Function Handling =====
			case "Call0Node":
			case "Call1Node":
			case "CallNNode":
				onReturnBehaviorCallNode(frame, result);
				break;
			case "Invoke0Node":
			case "Invoke1Node":
			case "InvokeNNode":
				onReturnBehaviorInvokeNode(frame, result);
				break;
			case "JSNewNodeGen":
				onReturnBehaviorJSNewNodeGen(frame, result);
				break;
			case "AccessIndexedArgumentNode":
				onReturnBehaviorAccessIndexedArgumentNode(frame, result);
				break;
			case "TerminalPositionReturnNode":
				behaviorFrameReturnTerminalPositionReturnNode();
				break;

			// ===== JavaScript Arithmetic Nodes =====
			case "JSAddNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.ADDITION);
				break;
			case "JSSubtractNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.SUBTRACTION);
				break;
			case "JSMultiplyNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.MULTIPLICATION);
				break;
			case "JSDivideNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.DIVISION);
				break;
			case "JSUnaryMinusNodeGen":
				onReturnBehaviorUnaryOperation(frame, result, Operation.UNARY_MINUS);
				break;
			case "JSUnaryPlusNodeGen":
				onReturnBehaviorUnaryOperation(frame, result, Operation.UNARY_PLUS);
				break;
			case "JSModuloNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.MODULO);
				break;
			case "JSAddSubNumericUnitNodeGen":
				onReturnBehaviorJSAddSubNumericUnitNodeGen(frame, result);
				break;
			case "SqrtNodeGen":
				onReturnBehaviorInternalInvokedFunction(frame, result, Operation.SQRT);
				break;


			// ===== JavaScript Constant Nodes =====
			case "JSConstantBooleanNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.BOOLEAN);
				break;
			case "JSConstantIntegerNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.NUMBER_INTEGER);
				break;
			case "JSConstantDoubleNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.NUMBER_REAL);
				break;
			case "JSConstantStringNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.STRING);
				break;
			case "JSConstantNullNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.NULL);
				break;
			case "JSConstantUndefinedNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.UNDEFINED);
				break;


			// ===== JavaScript Object/Function Creation Nodes
			case "DefaultFunctionExpressionNode":
			case "AutonomousFunctionExpressionNode":
				onReturnBehaviorConstant(frame, result, ExpressionType.OBJECT);
				break;
			case "ObjectLiteralNode":
				onReturnBehaviorObjectLiteralNode(frame, result);
				break;

			// ===== JavaScript Arrays =====
			case "DefaultArrayLiteralNode":
				onReturnBehaviorDefaultArrayLiteralNode(frame, result);
				break;
			case "ConstantEmptyArrayLiteralNode":
			case "ConstantArrayLiteralNode":
				onReturnBehaviorConstantArrayLiteralNode(frame, result);
				break;
			case "ConstructArrayNodeGen":
				onReturnBehaviorConstructArrayNodeGen(frame, result);
				break;
			case "ReadElementNode":
				onReturnBehaviorReadElementNode(frame, result);
				break;
			case "WriteElementNode":
			case "CompoundWriteElementNode":
				onReturnBehaviorWriteElementNode(frame, result);
				break;

			// ===== JavaScript Logic Nodes =====
			case "JSLessThanNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.LESS_THAN);
				break;
			case "JSLessOrEqualNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.LESS_EQUAL);
				break;
			case "JSGreaterThanNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.GREATER_THAN);
				break;
			case "JSGreaterOrEqualNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.GREATER_EQUAL);
				break;
			case "JSEqualNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.EQUAL);
				break;
			case "JSIdenticalNodeGen":
				onReturnBehaviorBinaryOperation(frame, result, Operation.STRICT_EQUAL);
				break;
			case "JSAndNode":
				onReturnBehaviorBinaryOperation(frame, result, Operation.AND);
				break;
			case "JSOrNode":
				onReturnBehaviorBinaryOperation(frame, result, Operation.OR);
				break;
			case "JSNotNodeGen":
				onReturnBehaviorUnaryOperation(frame, result, Operation.NOT);
				break;

			// ===== JavaScript Error Handling =====
			// TODO

			// ===== JavaScript Miscellaneous =====
			case "DualNode":
				onReturnBehaviorDualNode(frame, result);
				break;
			case "JSGlobalPrintNodeGen":
				onReturnBehaviorJSGlobalPrintNodeGen(frame, result);
				break;
			case "ExprBlockNode":
				onReturnBehaviorExprBlockNode(frame, result);
				break;
			case "VoidBlockNode":
				onReturnBehaviorVoidBlockNode(frame, result);
				break;
			case "DiscardResultNode":
				onReturnBehaviorDiscardResultNode(frame, result);
				break;
			case "JSInputGeneratingNodeWrapper":
			case "JSTaggedExecutionNode":
			case "LocalVarPostfixIncMaterializedNode": // "Dec" node does not exist
			case "LocalVarPrefixIncMaterializedNode": // TODO wie DualNode?
				onReturnBehaviorPassthrough(frame, result);
				break;
			default:
				was_instrumented_on_return_value = false;
		}

		if (was_instrumented_on_return_value) {
			amygdala.node_type_instrumented.get(instrumented_node_type).set(3);
		}

		if (amygdala.custom_error.someEnabled() && source_section != null) {
			if (this.cached_exception != null) {
				throw event_context.createError(this.cached_exception);
			}
			try {
				amygdala.custom_error.inspectReturnValue(instrumented_node_type, result, source_section.getStartLine());
			} catch (CustomError.EscalatedException ee) {
				throw event_context.createError(ee);
			}
		}
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
	}

	@Override
	protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[33m↯\033[0m");
		}

		// Exception should only be escalated if it is not already an escalated exception
		// and is not a ControlFlowException, these exceptions can occur in normal program executions
		if (!(exception instanceof CustomError.EscalatedException) && !(exception instanceof ControlFlowException)) {
			if (amygdala.custom_error.escalateExceptionsEnabled()) {
				amygdala.logger.info("Escalating exception with message: '" + exception.getMessage() + "' (escalate_exceptions)");
				throw event_context.createError(CustomError.createException(exception.getMessage()));
			}
		}

		boolean was_instrumented_on_return_exceptional = true;
		switch (instrumented_node_type) {
			case "FrameReturnNode":
				// FrameReturnNode has no onReturnValue event, instead it throws a ControlFlowException
				behaviorFrameReturnTerminalPositionReturnNode();
				break;
			default:
				was_instrumented_on_return_exceptional = false;
		}

		if (was_instrumented_on_return_exceptional) {
			amygdala.node_type_instrumented.get(instrumented_node_type).set(4);
		}
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
	}

	@Override
	public Object onUnwind(VirtualFrame frame, Object info) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[35m↺\033[0m");
		}

		amygdala.node_type_instrumented.get(instrumented_node_type).set(5);
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
		return info;
	}

	@Override
	protected void onDispose(VirtualFrame frame) {
		amygdala.probe.switchState(TimeProbe.ProgramState.INSTRUMENTATION);
		if (amygdala.isEventLoggingEnabled()) {
			amygdala.logger.event(getSignatureString() + " \033[36m×\033[0m");
		}

		amygdala.node_type_instrumented.get(instrumented_node_type).set(6);
		amygdala.probe.switchState(TimeProbe.ProgramState.EXECUTION);
	}

	private ArrayList<Pair<Integer, String>> getChildHashes(Node base_node) {
		ArrayList<Pair<Integer, String>> children = new ArrayList<>();
		for (Node n: base_node.getChildren()) {
			try {
				InstrumentableNode.WrapperNode wn = (InstrumentableNode.WrapperNode) n;
				Node real_node = wn.getDelegateNode();
				children.add(Pair.create(real_node.hashCode(), real_node.getClass().getSimpleName()));
			} catch (ClassCastException ex) {
				children.addAll(getChildHashes(n));
			}
		}
		return children;
	}

	private void onEnterBehaviorCallNode(VirtualFrame frame) {
		this.arguments_array.clear();
		// if CallNode has no arguments (Call0Node)
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onEnterBehaviorInvokeNode(VirtualFrame frame) {
		this.arguments_array.clear();
		// if InvokeNode has no arguments (Invoke0Node)
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onEnterBehaviorJSNewNodeGen(VirtualFrame frame) {
		this.arguments_array.clear();
		// if JSNewNodeGen has no arguments
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onEnterBehaviorFunctionBodyNode(VirtualFrame frame) {
		RootNode rn = instrumented_node.getRootNode();
		String function_name = "";
		if (rn != null) {
			function_name = JSNodeUtil.resolveName(rn);
		}
		// If the function is the main function (":program"), reset tracer.
		if (function_name.equals(":program")) {
			amygdala.tracer.reset(LanguageSemantic.JAVASCRIPT, getThisObjectHash(frame));
		}

		Iterator<Scope> local_scopes = environment.findLocalScopes(instrumented_node, frame).iterator();
		if (local_scopes.hasNext()) {
			Scope innermost_scope = local_scopes.next();
			Object root_instance = innermost_scope.getRootInstance();
			if (root_instance != null) {
				amygdala.tracer.initializeFunctionScope(System.identityHashCode(root_instance));
			} else {
				amygdala.logger.critical("onEnterBehaviorFunctionBodyNode(): Cannot get root instance");
			}
			amygdala.tracer.initializeIfAbsent(getThisObjectHash(frame));
		} else {
			amygdala.logger.critical("onEnterBehaviorFunctionBodyNode(): Cannot find any local scopes");
		}
		amygdala.tracer.resetFunctionReturnValue();
	}

	private void onInputValueBehaviorIfNode(VirtualFrame frame, EventContext input_context, int input_index,
										   Object input_value) {
		if (input_index == 0) {
			Boolean taken = JSRuntime.toBoolean(input_value);
			amygdala.branchingEvent(source_relative_identifier, BranchingNodeAttribute.BRANCH, child_hashes.get(0).getLeft(),
									taken, branch_predicate);
			amygdala.coverage.addBranchTaken(source_relative_identifier, taken);
		}
	}

	private void onInputValueBehaviorWhileNode(VirtualFrame frame, EventContext input_context, int input_index,
											  Object input_value) {
		// for-in-loop has only one child
		if (input_index == 0 && child_hashes.size() == 2) {
			Boolean taken = JSRuntime.toBoolean(input_value);
			amygdala.branchingEvent(source_relative_identifier, BranchingNodeAttribute.LOOP, child_hashes.get(0).getLeft(),
									taken, branch_predicate);
			amygdala.coverage.addBranchTaken(source_relative_identifier, taken);
		}
	}

	private void onInputValueBehaviorPropertyNode(VirtualFrame frame, EventContext input_context, int input_index,
												 Object input_value) {
		// Save the the object that is written to/read from
		if (input_index == 0) {
			context_object = input_value;
		}
	}

	private void onInputValueBehaviorReadWriteElementNode(VirtualFrame frame, EventContext input_context, int input_index,
														 Object input_value) {
		// Save the the object that is written to/read from
		if (input_index == 0) {
			context_object = input_value;
		}
		// Save the array index
		if (input_index == 1) {
			element_access = input_value;
		}
	}

	private void onInputValueBehaviorCallNode(VirtualFrame frame, EventContext input_context, int input_index,
											 Object input_value) {
		// 0 is ? node, 1 is the function object to call
		// TODO a bit hacky
		if (input_index >= 2) {
			this.arguments_array.add(amygdala.tracer.getIntermediate(child_hashes.get(input_index).getLeft()));
		}
		// Every call to onInput (or onEnter if Call0Node!)
		// inside a call node could
		// be the last input before the function gets called
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onInputValueBehaviorInvokeNode(VirtualFrame frame, EventContext input_context, int input_index,
											   Object input_value) {
		// Save the the object which method is called
		if (input_index == 0) {
			context_object = input_value;
		}
		// 0 is object node, 1 is the function object to call (?)
		// TODO a bit hacky
		if (input_index >= 2) {
			this.arguments_array.add(amygdala.tracer.getIntermediate(child_hashes.get(input_index).getLeft()));
		}
		// Every call to onInput (or onEnter if Invoke0Node!)
		// inside a call node could
		// be the last input before the function gets called
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onInputValueBehaviorJSNewNodeGen(VirtualFrame frame, EventContext input_context, int input_index,
												 Object input_value) {
		// 0 is object to create, others are arguments (?)
		// TODO a bit hacky
		if (input_index >= 1) {
			this.arguments_array.add(amygdala.tracer.getIntermediate(child_hashes.get(input_index).getLeft()));
		}
		// Every call to onInput
		// inside a call node could
		// be the last input before the function gets called
		amygdala.tracer.setArgumentsArray(this.arguments_array);
	}

	private void onInputValueBehaviorJSEqualNodeGen(VirtualFrame frame, EventContext input_context, int input_index,
												  Object input_value) {
		if (amygdala.custom_error.equalIsStrictEqualEnabled()) {
			if (input_index == 0) {
				this.type_of_first_equal_input = JSRuntime.typeof(input_value);
			}
			if (input_index == 1) {
				String type_of_second_equal_input = JSRuntime.typeof(input_value);
				if (!type_of_first_equal_input.equals(type_of_second_equal_input)) {
					this.cached_exception = CustomError.createException("Detected different types '" + type_of_first_equal_input + "' and '" + type_of_second_equal_input + "' for equality operation (equal_is_strict_equal). [" + instrumented_node_type + ", line " + source_section.getStartLine() + "]");
				}
			}
		}
	}

	private String extractPredicate() {
		if (source_section != null && source_section.isAvailable()) {
			Matcher branch_matcher = BRANCH_PATTERN.matcher(source_section.getCharacters().toString());
			if (branch_matcher.lookingAt()) {
				String kind = branch_matcher.group(1);
				String predicate = Logger.capBack(branch_matcher.group(2), 16);
				return kind.toUpperCase() + " " + predicate;
			} else {
				return "(NO SOURCE)";
			}
		} else {
			return "(NO SOURCE)";
		}
	}

	// Default Behavior is to just pass through any symbolic flow
	private void onReturnBehaviorPassthrough(VirtualFrame frame, Object result) {
		// Cannot determine behavior from several children
		if (child_hashes.size() == 1) {
			amygdala.tracer.passThroughIntermediate(instrumented_node_hash, child_hashes.get(0).getLeft());
		}
	}

	// ===== JavaScript Read/Write =====

	private String getAttributeName() {
		if (instrumented_node instanceof GlobalPropertyNode) {
			GlobalPropertyNode gpnode = (GlobalPropertyNode) instrumented_node;
			return gpnode.getPropertyKey();
		}
		if (instrumented_node instanceof PropertyNode) {
			PropertyNode pnode = (PropertyNode) instrumented_node;
			return pnode.getPropertyKey().toString();
		}
		if (instrumented_node instanceof WritePropertyNode) {
			WritePropertyNode wpnode = (WritePropertyNode) instrumented_node;
			return wpnode.getKey().toString();
		}
		if (instrumented_node instanceof JSReadFrameSlotNode) {
			JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) instrumented_node;
			return JSFrameUtil.getPublicName(jsrfsn.getFrameSlot());
		}
		if (instrumented_node instanceof JSWriteFrameSlotNode) {
			JSWriteFrameSlotNode jswfsn = (JSWriteFrameSlotNode) instrumented_node;
			return JSFrameUtil.getPublicName(jswfsn.getFrameSlot());
		}
		return null;
	}

	private void onReturnBehaviorGlobalPropertyNode(VirtualFrame frame, Object result) {
		if (context_object != null) {
			amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object),
												   attribute_name,
												   instrumented_node_hash);
		} else {
			amygdala.tracer.propertyToIntermediate(amygdala.tracer.getJSGlobalObjectId(),
												   attribute_name,
												   instrumented_node_hash);
		}
		enforceExistingProperties(context_object, attribute_name);
	}

	private void onReturnBehaviorGlobalObjectNode(VirtualFrame frame, Object result) {
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
	}

	private void onReturnBehaviorPropertyNode(VirtualFrame frame, Object result) {
		if (JSGuards.isString(context_object) && attribute_name.equals("length")) {
			onReturnBehaviorUnaryOperation(frame, result, Operation.STR_LENGTH);
		} else if (JSRuntime.isArray(context_object) && attribute_name.equals("length")) {
			amygdala.tracer.addArrayOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
											  System.identityHashCode(context_object), new ArrayList<>(),
											  Operation.ARR_LENGTH, getJSArraySize((DynamicObject) context_object));
		} else if (source_section.getCharacters().toString().equals("Math.PI")) {
			amygdala.tracer.setIntermediate(instrumented_node_hash,
											new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_REAL, Math.PI));
		} else {
			amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object),
												   attribute_name,
												   instrumented_node_hash);
		}
		enforceExistingProperties(context_object, attribute_name);
	}

	private void enforceExistingProperties(Object js_object, Object key) {
		if (amygdala.custom_error.enforceExistingPropertiesEnabled() && JSRuntime.isObject(js_object)) {
			DynamicObject js_dynamic_object = (DynamicObject) js_object;
			if (!js_dynamic_object.containsKey(key)) {
				amygdala.logger.info("Detected non-existing property '" + key.toString() + "' (enforce_existing_properties)");
				int line_num = -1;
				if (source_section != null && source_section.isAvailable()) {
					line_num = source_section.getStartLine();
				}
				throw event_context.createError(CustomError.createException("Detected non-existing property '" + key.toString() + "' (enforce_existing_properties). [" + instrumented_node_type + ", line " + line_num + "]"));
			}
		}
	}

	private void onReturnBehaviorWritePropertyNode(VirtualFrame frame, Object result) {
		amygdala.tracer.intermediateToProperty(System.identityHashCode(context_object),
											   attribute_name,
											   child_hashes.get(1).getLeft());
		amygdala.tracer.passThroughIntermediate(instrumented_node_hash, child_hashes.get(1).getLeft());
	}

	private int getScopeHashCurrent(VirtualFrame frame) {
		if (amygdala.tracer.cached_scopes.containsKey(instrumented_node_hash)) {
			return amygdala.tracer.cached_scopes.get(instrumented_node_hash);
		} else {
			Iterator<Scope> local_scopes = environment.findLocalScopes(instrumented_node, frame).iterator();
			if (local_scopes.hasNext()) {
				Scope innermost_scope = local_scopes.next();
				Object root_instance = innermost_scope.getRootInstance();
				if (root_instance != null) {
					int scope_hash = System.identityHashCode(root_instance);
					amygdala.tracer.cached_scopes.put(instrumented_node_hash, scope_hash);
					return scope_hash;
				} else {
					amygdala.logger.critical("getScopeHashCurrent(): Cannot get root instance, returning -1");
				}
			} else {
				amygdala.logger.critical("getScopeHashCurrent(): Cannot find any local scopes, returning -1");
			}
		}
		return -1;
	}

	private int getScopeHashScoped(VirtualFrame frame, String variable_name) {
		if (amygdala.tracer.cached_scopes.containsKey(instrumented_node_hash)) {
			return amygdala.tracer.cached_scopes.get(instrumented_node_hash);
		} else {
			Iterator<Scope> local_scopes = environment.findLocalScopes(instrumented_node, frame).iterator();
			if (local_scopes.hasNext()) {
				while (local_scopes.hasNext()) {
					Scope curr_scope = local_scopes.next();
					Object root_instance = curr_scope.getRootInstance();
					if (root_instance != null) {
						int scope_hash = System.identityHashCode(root_instance);
						if (amygdala.tracer.containsVariable(scope_hash, variable_name)) {
							amygdala.tracer.cached_scopes.put(instrumented_node_hash, scope_hash);
							return scope_hash;
						}
					} else {
						amygdala.logger.critical("getScopeHashScoped(): Cannot get root instance");
					}
				}
				amygdala.logger.critical("getScopeHashScoped(): No scope with variable '" + variable_name + "' found, returning -1");
			} else {
				amygdala.logger.critical("getScopeHashScoped(): Cannot find any local scopes, returning -1");
			}
		}
		return -1;
	}

	private void onReturnBehaviorJSReadCurrentFrameSlotNodeGen(VirtualFrame frame, Object result) {
		boolean read_successful = amygdala.tracer.frameSlotToIntermediate(getScopeHashCurrent(frame),
																		  attribute_name,
																		  instrumented_node_hash);
		if (!read_successful && Amygdala.EXPERIMENTAL_FRAMESLOT_FILL_IN_NONEXISTENT) {
			amygdala.tracer.setIntermediate(instrumented_node_hash, jsObjectToSymbolic(result));
			amygdala.logger.warning("onReturnBehaviorJSReadCurrentFrameSlotNodeGen(): Experimental option frameslot_fill_in_nonexistent is enabled, filling in value '" + result.toString() + "'");
		}
	}

	private void onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(VirtualFrame frame, Object result) {
		amygdala.tracer.intermediateToFrameSlot(getScopeHashCurrent(frame),
												attribute_name,
												child_hashes.get(0).getLeft());
		amygdala.tracer.passThroughIntermediate(instrumented_node_hash, child_hashes.get(0).getLeft());
	}

	private void onReturnBehaviorJSReadScopeFrameSlotNodeGen(VirtualFrame frame, Object result) {
		boolean read_successful = amygdala.tracer.frameSlotToIntermediate(getScopeHashScoped(frame, attribute_name),
																		  attribute_name,
																		  instrumented_node_hash);
		if (!read_successful && Amygdala.EXPERIMENTAL_FRAMESLOT_FILL_IN_NONEXISTENT) {
			amygdala.tracer.setIntermediate(instrumented_node_hash, jsObjectToSymbolic(result));
			amygdala.logger.warning("onReturnBehaviorJSReadScopeFrameSlotNodeGen(): Experimental option frameslot_fill_in_nonexistent is enabled, filling in value '" + result.toString() + "'");
		}
	}

	private void onReturnBehaviorJSWriteScopeFrameSlotNodeGen(VirtualFrame frame, Object result) {
		amygdala.tracer.intermediateToFrameSlot(getScopeHashScoped(frame, attribute_name),
												attribute_name,
												child_hashes.get(0).getLeft());
		amygdala.tracer.passThroughIntermediate(instrumented_node_hash, child_hashes.get(0).getLeft());
	}

	// ===== JavaScript Function Handling =====

	private void onReturnBehaviorCallNode(VirtualFrame frame, Object result) {
		amygdala.tracer.functionReturnValueToIntermediate(instrumented_node_hash);
		amygdala.tracer.resetFunctionReturnValue();
	}

	private void onReturnBehaviorInvokeNode(VirtualFrame frame, Object result) {
		if (JSGuards.isString(context_object)) {
			Matcher method_matcher = METHOD_PATTERN.matcher(source_section.getCharacters().toString());
			if (method_matcher.matches()) {
				String method_name = method_matcher.group(1);
				switch (method_name) {
					case "concat":
						amygdala.tracer.addStringOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														   child_hashes.get(0).getLeft(), arguments_array, Operation.STR_CONCAT);
						break;
					case "charAt":
						amygdala.tracer.addStringOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														   child_hashes.get(0).getLeft(), arguments_array, Operation.STR_CHAR_AT);
						break;
					case "substr":
						amygdala.tracer.addStringOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														   child_hashes.get(0).getLeft(), arguments_array, Operation.STR_SUBSTR);
						break;
					case "includes":
						amygdala.tracer.addStringOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														   child_hashes.get(0).getLeft(), arguments_array, Operation.STR_INCLUDES);
						break;
					case "indexOf":
						amygdala.tracer.addStringOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														   child_hashes.get(0).getLeft(), arguments_array, Operation.STR_INDEXOF);
						break;
					default:
						amygdala.logger.critical(
								"onReturnBehaviorInvokeNode(): String method '" + method_name + "' not implemented");
				}
			} else {
				amygdala.logger.critical(
						"onReturnBehaviorInvokeNode(): Trying to compute string operation, but cannot extract name");
			}
		} else if (JSRuntime.isArray(context_object)) {
			Matcher method_matcher = METHOD_PATTERN.matcher(source_section.getCharacters().toString());
			if (method_matcher.matches()) {
				String method_name = method_matcher.group(1);
				switch (method_name) {
					case "push":
						amygdala.tracer.addArrayOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														  System.identityHashCode(context_object), arguments_array, Operation.ARR_PUSH,
														  getJSArraySize((DynamicObject) context_object));
						break;
					case "join":
						amygdala.tracer.addArrayOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT,
														  System.identityHashCode(context_object), arguments_array, Operation.ARR_JOIN,
														  getJSArraySize((DynamicObject) context_object));
						break;
					default:
						amygdala.logger.critical(
								"onReturnBehaviorInvokeNode(): Array method '" + method_name + "' not implemented");
				}
			} else {
				amygdala.logger.critical(
						"onReturnBehaviorInvokeNode(): Trying to compute array operation, but cannot extract name");
			}
		} else {
			amygdala.tracer.functionReturnValueToIntermediate(instrumented_node_hash);
			amygdala.tracer.resetFunctionReturnValue();
		}
	}

	private long getJSArraySize(DynamicObject arr_obj) {
		if (JSRuntime.isArray(arr_obj)) {
			try {
				return INTEROP.getArraySize(arr_obj);
			} catch (UnsupportedMessageException e) {
				amygdala.logger.critical("getJSArraySize(): Object is an array, but we cannot get the size");
				return 0;
			}
		} else {
			amygdala.logger.critical("getJSArraySize(): Object is not an array");
			return 0;
		}
	}

	private void onReturnBehaviorJSNewNodeGen(VirtualFrame frame, Object result) {
		//TODO always an object, never a basic type?
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
		amygdala.tracer.resetFunctionReturnValue();
	}

	private void onReturnBehaviorAccessIndexedArgumentNode(VirtualFrame frame, Object result) {
		amygdala.tracer.argumentToIntermediate(argument_index, instrumented_node_hash);
	}

	private void behaviorFrameReturnTerminalPositionReturnNode() {
		amygdala.tracer.intermediateToFunctionReturnValue(child_hashes.get(0).getLeft());
	}

	// ===== JavaScript General Nodes =====

	private void onReturnBehaviorBinaryOperation(VirtualFrame frame, Object result, Operation op) {
		amygdala.tracer.addOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, op,
									 child_hashes.get(0).getLeft(), child_hashes.get(1).getLeft());
	}

	private void onReturnBehaviorInternalInvokedFunction(VirtualFrame frame, Object result, Operation op) {
		// These nodes behave like methods
		amygdala.tracer.performSingularMethodInvocation(LanguageSemantic.JAVASCRIPT, op);
	}

	// TODO
	private void onReturnBehaviorJSAddSubNumericUnitNodeGen(VirtualFrame frame, Object result) {
		String before = String.valueOf(
				source_section.getSource().getCharacters().charAt(source_section.getCharIndex() - 1));
		String after = String.valueOf(source_section.getSource().getCharacters().charAt(source_section.getCharEndIndex()));
		if (before.equals("+") || after.equals("+")) {
			SymbolicNode pre_add = amygdala.tracer.getIntermediate(child_hashes.get(0).getLeft());
			SymbolicNode add_result = new Addition(LanguageSemantic.JAVASCRIPT, pre_add, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
			amygdala.tracer.setIntermediate(instrumented_node_hash, add_result);
		} else if (before.equals("-") || after.equals("-")) {
			SymbolicNode pre_sub = amygdala.tracer.getIntermediate(child_hashes.get(0).getLeft());
			SymbolicNode sub_result = new Subtraction(LanguageSemantic.JAVASCRIPT, pre_sub, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
			amygdala.tracer.setIntermediate(instrumented_node_hash, sub_result);
		} else {
			amygdala.logger.critical("onReturnBehaviorJSAddSubNumericUnitNodeGen(): Cannot determine operation from source code");
		}
	}

	private void onReturnBehaviorUnaryOperation(VirtualFrame frame, Object result, Operation op) {
		amygdala.tracer.addOperation(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, op, child_hashes.get(0).getLeft());
	}

	private void onReturnBehaviorConstant(VirtualFrame frame, Object result, ExpressionType type) {
		if (this.is_input_node) {
			Object next_input = amygdala.getNextInputValue(this.input_variable_identifier);
			amygdala.tracer.addVariable(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, this.input_variable_identifier);
			amygdala.node_type_instrumented.get(instrumented_node_type).set(3);
			throw this.event_context.createUnwind(next_input);
		} else {
			amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, type, result);
		}
	}

	private void onReturnBehaviorObjectLiteralNode(VirtualFrame frame, Object result) {
		DynamicObject dobj;
		try {
			dobj = (DynamicObject) result;
		} catch (ClassCastException cce) {
			amygdala.logger.critical("onReturnBehaviorObjectLiteralNode(): Cannot cast result to DynamicObject");
			return;
		}
		Shape obj_shape = dobj.getShape();
		List<Object> keys = obj_shape.getKeyList();
		if (keys.size() != child_hashes.size()) {
			amygdala.logger.critical("onReturnBehaviorObjectLiteralNode(): Resulting object has not the same number of keys as the child nodes");
			return;
		}
		VariableContext obj_ctx = new VariableContext();
		for (int ch_index = 0; ch_index < keys.size(); ch_index++) {
			//TODO toString()?
			obj_ctx.set(keys.get(ch_index).toString(), amygdala.tracer.getIntermediate(child_hashes.get(ch_index).getLeft()));
		}
		amygdala.tracer.setSymbolicContext(System.identityHashCode(result), obj_ctx);
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
	}

	private void onReturnBehaviorDefaultArrayLiteralNode(VirtualFrame frame, Object result) {
		VariableContext new_array = new VariableContext();
		for (int i = 0; i < child_hashes.size(); i++) {
			Pair<Integer, String> child = child_hashes.get(i);
			new_array.set(i, amygdala.tracer.getIntermediate(child.getLeft()));
		}
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
		amygdala.tracer.setSymbolicContext(System.identityHashCode(result), new_array);
	}

	private void onReturnBehaviorConstantArrayLiteralNode(VirtualFrame frame, Object result) {
		if (this.cached_constant_array == null) {
			try {
				this.cached_constant_array = arrayToSymbolic((DynamicObject) result);
			} catch (ClassCastException cce) {
				amygdala.logger.critical("onReturnBehaviorConstantArrayLiteralNode(): Cannot cast result to DynamicObject");
			}
		}
		amygdala.tracer.setSymbolicContext(System.identityHashCode(result), this.cached_constant_array.copy());
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
	}

	private void onReturnBehaviorConstructArrayNodeGen(VirtualFrame frame, Object result) {
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
		amygdala.tracer.initializeIfAbsent(System.identityHashCode(result));
	}

	private void onReturnBehaviorReadElementNode(VirtualFrame frame, Object result) {
		if (JSGuards.isString(context_object)) {
			ArrayList<SymbolicNode> arg = new ArrayList<>();
			arg.add(amygdala.tracer.getIntermediate(child_hashes.get(1).getLeft()));
			amygdala.tracer.addStringOperation(instrumented_node_hash,
											   LanguageSemantic.JAVASCRIPT,
											   child_hashes.get(0).getLeft(),
											   arg,
											   Operation.STR_CHAR_AT);
		} else {
			amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object),
												   element_access,
												   instrumented_node_hash);
		}
		enforceExistingProperties(context_object, element_access);
	}

	private void onReturnBehaviorWriteElementNode(VirtualFrame frame, Object result) {
		amygdala.tracer.intermediateToProperty(System.identityHashCode(context_object),
											   element_access,
											   child_hashes.get(2).getLeft());
		amygdala.tracer.passThroughIntermediate(instrumented_node_hash, child_hashes.get(2).getLeft());
	}

	//TODO extremely hacky
	private void onReturnBehaviorDualNode(VirtualFrame frame, Object result) {
		// if DualNode is part of an increment/decrement operation
		if (dual_node_is_increment) {
			SymbolicNode pre = amygdala.tracer.getIntermediate(child_hashes.get(0).getLeft());
			SymbolicNode revert_increment = new Subtraction(LanguageSemantic.JAVASCRIPT, pre, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
			amygdala.tracer.setIntermediate(instrumented_node_hash, revert_increment);
		} else if (dual_node_is_decrement) {
			SymbolicNode pre = amygdala.tracer.getIntermediate(child_hashes.get(0).getLeft());
			SymbolicNode revert_decrement = new Addition(LanguageSemantic.JAVASCRIPT, pre, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
			amygdala.tracer.setIntermediate(instrumented_node_hash, revert_decrement);
		}
	}

	private void onReturnBehaviorJSGlobalPrintNodeGen(VirtualFrame frame, Object result) {
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
	}

	private void onReturnBehaviorExprBlockNode(VirtualFrame frame, Object result) {
		// TODO
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.NULL, null);
	}

	private void onReturnBehaviorVoidBlockNode(VirtualFrame frame, Object result) {
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
	}

	private void onReturnBehaviorDiscardResultNode(VirtualFrame frame, Object result) {
		amygdala.tracer.addConstant(instrumented_node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
	}

	private VariableContext arrayToSymbolic(DynamicObject dyn_obj) {
		if (JSRuntime.isArray(dyn_obj)) {
			VariableContext array_ctx = new VariableContext();
			long size = getJSArraySize(dyn_obj);
			for (int i = 0; i < size; i++) {
				Object array_elem;
				try {
					array_elem = INTEROP.readArrayElement(dyn_obj, i);
				} catch (UnsupportedMessageException | InvalidArrayIndexException e) {
					amygdala.logger.critical("arrayToSymbolic(): Object is an array, but we cannot read the element with index" + i);
					return new VariableContext();
				}
				array_ctx.set(i, jsObjectToSymbolic(array_elem));
			}
			return array_ctx;
		} else {
			amygdala.logger.critical("arrayToSymbolic(): Object '" + dyn_obj.toString() + "' is not a JavaScript array.");
			return new VariableContext();
		}
	}

	private SymbolicNode jsObjectToSymbolic(Object js_obj) {
		if (JSGuards.isBoolean(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, js_obj);
		} else if (JSGuards.isString(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.STRING, js_obj);
		} else if (JSGuards.isBigInt(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BIGINT, js_obj);
		} else if (JSGuards.isNumberInteger(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, js_obj);
		} else if (JSGuards.isNumberDouble(js_obj) && JSRuntime.isNaN(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_NAN, null);
		} else if (JSGuards.isNumberDouble(js_obj) && JSRuntime.isPositiveInfinity((double) js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_POS_INFINITY, null);
		} else if (JSGuards.isNumberDouble(js_obj) && JSRuntime.isPositiveInfinity(-(double) js_obj)) {
				return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_NEG_INFINITY, null);
		} else if (JSGuards.isNumberDouble(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_REAL, js_obj);
		} else if (JSGuards.isUndefined(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
		} else if (JSGuards.isJSNull(js_obj)) {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NULL, null);
		} else {
			return new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
		}
	}
}
