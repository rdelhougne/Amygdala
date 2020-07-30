package org.fuzzingtool.instrumentation;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.truffleinterop.InteropList;
import org.fuzzingtool.core.components.Amygdala;
import org.fuzzingtool.core.components.BranchingNodeAttribute;
import org.fuzzingtool.core.components.VariableContext;
import org.fuzzingtool.core.components.VariableIdentifier;
import org.fuzzingtool.core.symbolic.*;
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

class FuzzingNodeWrapperFactory implements ExecutionEventNodeFactory {
	private final TruffleInstrument.Env env;
	private final Amygdala amygdala;

	private static final boolean ENABLE_EVENT_LOGGING = true;

	private static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

	// Capture group 2 is variable name
    private final Pattern assignment_pattern = Pattern.compile("(var\\s+|let\\s+|const\\s+)?\\s*([A-Za-z_]\\w*)\\s*=.*");
    private final Pattern increment_pattern = Pattern.compile("[A-Za-z_]\\w*\\+\\+");
    private final Pattern decrement_pattern = Pattern.compile("[A-Za-z_]\\w*--");
    private final Pattern method_pattern = Pattern.compile(".*\\.([a-zA-Z_]\\w*)\\(.*\\)");
    private final Pattern branch_pattern = Pattern.compile("(if|for|while)\\s*\\((.*)\\)\\s*[{\\n]");

	FuzzingNodeWrapperFactory(final TruffleInstrument.Env env, Amygdala amy) {
		this.env = env;
		this.amygdala = amy;
	}

	public ExecutionEventNode create(final EventContext ec) {
		if (ec.getInstrumentedNode().getClass().getSimpleName().equals("MaterializedFunctionBodyNode")) {
			StringBuilder save_path = new StringBuilder();
			save_path.append(Paths.get(".").toAbsolutePath().normalize().toString()).append("/");
			save_path.append("function_");
			save_path.append(ec.getInstrumentedSourceSection().getStartLine()).append("-");
			save_path.append(ec.getInstrumentedSourceSection().getEndLine()).append("_");
			RootNode rn = ec.getInstrumentedNode().getRootNode();
			if (rn != null) {
				save_path.append(JSNodeUtil.resolveName(rn).replace(":", ""));
			} else {
				save_path.append("(unknown)");
			}
			File save_file = new File(save_path.toString() + ".svg");
			if (!save_file.exists()) {
				ASTVisualizer av = new ASTVisualizer(ec.getInstrumentedNode(), amygdala.logger);
				av.save_image(save_path.toString());
			}
		}

		final boolean create_nodeIsInputNode;
		final VariableIdentifier create_inputVariableIdentifier;
		if (ec.getInstrumentedNode() instanceof JSConstantNode) {
			Pair<Boolean, VariableIdentifier> create_inputNodeConfiguration =
					amygdala.getInputNodeConfiguration(ec.getInstrumentedSourceSection().getStartLine());
			create_nodeIsInputNode = create_inputNodeConfiguration.getLeft();
			create_inputVariableIdentifier = create_inputNodeConfiguration.getRight();
		} else {
			create_nodeIsInputNode = false;
			create_inputVariableIdentifier = null;
		}

		if (!amygdala.node_type_instrumented.containsKey(ec.getInstrumentedNode().getClass().getSimpleName())) {
			amygdala.node_type_instrumented.put(ec.getInstrumentedNode().getClass().getSimpleName(), new BitSet(3));
		}

		return new ExecutionEventNode() {
			private final EventContext event_context = ec;
			private final SourceSection my_sourcesection = ec.getInstrumentedSourceSection();
			private final Node my_node = ec.getInstrumentedNode();
			private final String node_type = my_node.getClass().getSimpleName();
			private final int node_hash = my_node.hashCode();

			// Input node config
			private final boolean isInputNode = create_nodeIsInputNode;
			private final VariableIdentifier inputVariableIdentifier = create_inputVariableIdentifier;

			// Various save spots
			// used by PropertyNode and WritePropertyNode to save the context of the operation
			private Object context_object = null;
			// used by ReadElementNode and WriteElementNode to determine the array index
			private Object element_access = null;
			// used by Call1..NNodes to construct the arguments array
			private final ArrayList<SymbolicNode> arguments_array = new ArrayList<>();

			protected String getSignatureString() {
				String node_type_padded = String.format("%1$-" + 36 + "s", node_type);
				String hash_padded = String.format("%1$-" + 12 + "s", node_hash);
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					int start_line = my_sourcesection.getStartLine();
					String line_padded;
					if (start_line >= 0) {
						line_padded = String.format("%1$" + 3 + "s", start_line);
					} else {
						line_padded = "  ~";
					}
					String characters = my_sourcesection.getCharacters().toString().replace("\n", "");
					String characters_cropped = characters.substring(0, Math.min(characters.length(), 16));
					return "[" + node_type_padded + " " + hash_padded + " " + line_padded + ":" + characters_cropped + "]";
				} else {
					return "[" + node_type_padded + " " + hash_padded + "     (NO SOURCE)]";
				}
			}

			protected Integer getSourceRelativeIdentifier() {
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					String identifier = my_sourcesection.getSource().getURI().toString()
							+ ":" + my_sourcesection.getCharIndex()
							+ ":" + my_sourcesection.getCharEndIndex();
					return identifier.hashCode();
				} else {
					return 0;
				}
			}

			protected String getLocalScopesString(VirtualFrame vFrame) {
				StringBuilder builder = new StringBuilder();
				Iterable<Scope> a = env.findLocalScopes(my_node, vFrame);
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

			public String objectToString(Object obj) {
				String info = obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
				if (obj instanceof Integer || obj instanceof Double || obj instanceof Boolean || obj instanceof String) {
					info = info + " (" + obj.toString() + ")";
				}
				return info;
			}

			/**
			 * Returns the hash-code of the current receiver object instance (aka. "this" in JavaScript)
			 *
			 * @param vFrame The current frame
			 * @return The corresponding hash-code, or 0 if an error occurs
			 */
			protected Integer getThisObjectHash(VirtualFrame vFrame) {
				Iterable<Scope> localScopes = env.findLocalScopes(my_node, vFrame);
				if (localScopes != null && localScopes.iterator().hasNext()) {
					Scope innermost_scope = localScopes.iterator().next();
					try {
						return System.identityHashCode(innermost_scope.getReceiver());
					} catch (java.lang.Exception ex) {
						amygdala.logger.critical("ExecutionEventNode.getThisObjectHash(): Node " + getSignatureString() + " has no Receiver-Object.");
						return 0;
					}
				}
				amygdala.logger.critical("ExecutionEventNode.getThisObjectHash(): Node " + getSignatureString() + " has no local scopes.");
				return 0;
			}

			@Override
			protected void onEnter(VirtualFrame vFrame) {
				if (ENABLE_EVENT_LOGGING) {
					amygdala.logger.log(getSignatureString() + " \033[32m→\033[0m");
				}

				boolean was_instrumented_on_enter = true;
				switch (node_type) {
					case "Call0Node":
					case "Call1Node":
					case "CallNNode":
						onEnterBehaviorCallNode(vFrame);
						break;
					case "Invoke0Node":
					case "Invoke1Node":
					case "InvokeNNode":
						onEnterBehaviorInvokeNode(vFrame);
						break;
					case "JSNewNodeGen":
						onEnterBehaviorJSNewNodeGen(vFrame);
						break;
					case "MaterializedFunctionBodyNode":
						onEnterBehaviorFunctionBodyNode(vFrame);
						break;
					default:
						was_instrumented_on_enter = false;
				}
				
				if (was_instrumented_on_enter) {
					amygdala.node_type_instrumented.get(node_type).set(0);
				}
			}

			@Override
			protected void onInputValue(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                        Object inputValue) {
				if (ENABLE_EVENT_LOGGING) {
					amygdala.logger.log(getSignatureString() + " \033[34m•\033[0m");
				}

				boolean was_instrumented_on_input_value = true;
				switch (node_type) {
					case "IfNode":
						onInputValueBehaviorIfNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "WhileNode":
						onInputValueBehaviorWhileNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "GlobalPropertyNode":
					case "PropertyNode":
					case "WritePropertyNode":
						onInputValueBehaviorPropertyNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "ReadElementNode":
					case "WriteElementNode":
						onInputValueBehaviorReadWriteElementNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "Call0Node":
					case "Call1Node":
					case "CallNNode":
						onInputValueBehaviorCallNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "Invoke0Node":
					case "Invoke1Node":
					case "InvokeNNode":
						onInputValueBehaviorInvokeNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "JSNewNodeGen":
						onInputValueBehaviorJSNewNodeGen(vFrame, inputContext, inputIndex, inputValue);
						break;
					case "FrameReturnNode":
						// FrameReturnNode has no onReturnValue event, f*cking whyyyy
						behaviorFrameReturnTerminalPositionReturnNode();
						break;
					default:
						was_instrumented_on_input_value = false;
				}
				
				if (was_instrumented_on_input_value) {
					amygdala.node_type_instrumented.get(node_type).set(1);
				}
			}

			@Override
			public void onReturnValue(VirtualFrame vFrame, Object result) {
				if (ENABLE_EVENT_LOGGING) {
					amygdala.logger.log(getSignatureString() + " \033[31m↵\033[0m");
				}
				
				boolean was_instrumented_on_return_value = true;
				switch (node_type) {
					// ===== JavaScript Read/Write =====
					case "GlobalPropertyNode":
						onReturnBehaviorGlobalPropertyNode(vFrame, result);
						break;
					case "GlobalObjectNode":
						onReturnBehaviorGlobalObjectNode(vFrame, result);
						break;
					case "PropertyNode":
						onReturnBehaviorPropertyNode(vFrame, result);
						break;
					case "WritePropertyNode":
						onReturnBehaviorWritePropertyNode(vFrame, result);
						break;
					case "JSReadCurrentFrameSlotNodeGen":
						onReturnBehaviorJSReadCurrentFrameSlotNodeGen(vFrame, result);
						break;
					case "JSWriteCurrentFrameSlotNodeGen":
						onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(vFrame, result);
						break;
					case "JSReadScopeFrameSlotNodeGen":
					case "JSReadScopeFrameSlotWithTDZNodeGen":
						onReturnBehaviorJSReadScopeFrameSlotNodeGen(vFrame, result);
						break;
					case "JSWriteScopeFrameSlotNodeGen":
						onReturnBehaviorJSWriteScopeFrameSlotNodeGen(vFrame, result);
						break;


					// ===== JavaScript Function Handling =====
					case "Call0Node":
					case "Call1Node":
					case "CallNNode":
						onReturnBehaviorCallNode(vFrame, result);
						break;
					case "Invoke0Node":
					case "Invoke1Node":
					case "InvokeNNode":
						onReturnBehaviorInvokeNode(vFrame, result);
						break;
					case "JSNewNodeGen":
						onReturnBehaviorJSNewNodeGen(vFrame, result);
						break;
					case "AccessIndexedArgumentNode":
						onReturnBehaviorAccessIndexedArgumentNode(vFrame, result);
						break;
					case "TerminalPositionReturnNode":
						behaviorFrameReturnTerminalPositionReturnNode();
						break;

					// ===== JavaScript Arithmetic Nodes =====
					case "JSAddNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.ADDITION);
						break;
					case "JSSubtractNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.SUBTRACTION);
						break;
					case "JSMultiplyNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.MULTIPLICATION);
						break;
					case "JSDivideNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.DIVISION);
						break;
					case "JSUnaryMinusNodeGen":
						onReturnBehaviorUnaryOperation(vFrame, result, Operation.UNARY_MINUS);
						break;
					case "JSUnaryPlusNodeGen":
						onReturnBehaviorUnaryOperation(vFrame, result, Operation.UNARY_PLUS);
						break;
					case "JSModuloNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.MODULO);
						break;
					case "JSAddSubNumericUnitNodeGen":
						onReturnBehaviorJSAddSubNumericUnitNodeGen(vFrame, result);
						break;


					// ===== JavaScript Constant Nodes =====
					case "JSConstantBooleanNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.BOOLEAN);
						break;
					case "JSConstantIntegerNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.NUMBER_INTEGER);
						break;
					case "JSConstantDoubleNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.NUMBER_REAL);
						break;
					case "JSConstantStringNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.STRING);
						break;
					case "JSConstantNullNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.NULL);
						break;
					case "JSConstantUndefinedNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.UNDEFINED);
						break;


						// ===== JavaScript Object/Function Creation Nodes
					case "DefaultFunctionExpressionNode":
					case "AutonomousFunctionExpressionNode":
						onReturnBehaviorConstant(vFrame, result, ExpressionType.OBJECT);
						break;
					case "ObjectLiteralNode":
						onReturnBehaviorObjectLiteralNode(vFrame, result);
						break;

						// ===== JavaScript Arrays =====
					case "DefaultArrayLiteralNode":
						onReturnBehaviorDefaultArrayLiteralNode(vFrame, result);
						break;
					case "ConstantEmptyArrayLiteralNode":
					case "ConstantArrayLiteralNode":
						onReturnBehaviorConstantArrayLiteralNode(vFrame, result);
						break;
					case "ReadElementNode":
						onReturnBehaviorReadElementNode(vFrame, result);
						break;
					case "WriteElementNode":
						onReturnBehaviorWriteElementNode(vFrame, result);
						break;


					// ===== JavaScript Logic Nodes =====
					case "JSLessThanNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.LESS_THAN);
						break;
					case "JSLessOrEqualNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.LESS_EQUAL);
						break;
					case "JSGreaterThanNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.GREATER_THAN);
						break;
					case "JSGreaterOrEqualNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.GREATER_EQUAL);
						break;
					case "JSEqualNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.EQUAL);
						break;
					case "JSIdenticalNodeGen":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.STRICT_EQUAL);
						break;
					case "JSAndNode":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.AND);
						break;
					case "JSOrNode":
						onReturnBehaviorBinaryOperation(vFrame, result, Operation.OR);
						break;
					case "JSNotNodeGen":
						onReturnBehaviorUnaryOperation(vFrame, result, Operation.NOT);
						break;

					// ===== JavaScript Miscellaneous =====
					case "DualNode":
						onReturnBehaviorDualNode(vFrame, result);
						break;
					case "JSGlobalPrintNodeGen":
						onReturnBehaviorJSGlobalPrintNodeGen(vFrame, result);
						break;
					case "ExprBlockNode":
						onReturnBehaviorExprBlockNode(vFrame, result);
						break;
					case "JSInputGeneratingNodeWrapper":
					case "JSTaggedExecutionNode":
						onReturnBehaviorPassthrough(vFrame, result);
						break;
					case "JSStringIndexOfNodeGen":
					case "JSStringConcatNodeGen":
					case "JSStringSubstrNodeGen":
						// Do nothing. These nodes should not have any behavior or are instrumented otherwise.
						break;
					default:
						was_instrumented_on_return_value = false;
				}

				if (was_instrumented_on_return_value) {
					amygdala.node_type_instrumented.get(node_type).set(2);
				}
			}

			@Override
			protected Object onUnwind(VirtualFrame frame, Object info) {
				return info;
			}

			public ArrayList<Pair<Integer, String>> getChildHashes() {
				return getChildHashes(my_node);
			}

			public ArrayList<Pair<Integer, String>> getChildHashes(Node basenode) {
				ArrayList<Pair<Integer, String>> children = new ArrayList<>();
				for (Node n: basenode.getChildren()) {
					try {
						InstrumentableNode.WrapperNode wn = (InstrumentableNode.WrapperNode) n;
						Node realnode = wn.getDelegateNode();
						children.add(Pair.create(realnode.hashCode(), realnode.getClass().getSimpleName()));
					} catch (ClassCastException ex) {
						children.addAll(getChildHashes(n));
					}
				}
				return children;
			}

			public void onEnterBehaviorCallNode(VirtualFrame vFrame) {
				this.arguments_array.clear();
				// if CallNode has no arguments (Call0Node)
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onEnterBehaviorInvokeNode(VirtualFrame vFrame) {
				this.arguments_array.clear();
				// if InvokeNode has no arguments (Invoke0Node)
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onEnterBehaviorJSNewNodeGen(VirtualFrame vFrame) {
				this.arguments_array.clear();
				// if JSNewNodeGen has no arguments
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onEnterBehaviorFunctionBodyNode(VirtualFrame vFrame) {
				RootNode rn = my_node.getRootNode();
				String function_name = "";
				if (rn != null) {
					function_name = JSNodeUtil.resolveName(rn);
				}
				// If the function is the main function (":program"), reset tracer.
				if (function_name.equals(":program")) {
					amygdala.tracer.reset(LanguageSemantic.JAVASCRIPT, getThisObjectHash(vFrame));
				}

				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes.hasNext()) {
					Scope innermost_scope = local_scopes.next();
					Object root_instance = innermost_scope.getRootInstance();
					if (root_instance != null) {
						amygdala.tracer.initializeFunctionScope(System.identityHashCode(root_instance));
					} else {
						amygdala.logger.critical("onEnterBehaviorFunctionBodyNode(): Cannot get root instance.");
					}
				} else {
					amygdala.logger.critical("onEnterBehaviorFunctionBodyNode(): Cannot find any local scopes.");
				}
				amygdala.tracer.resetFunctionReturnValue();
			}

			public void onInputValueBehaviorIfNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                                   Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 2;
				if (inputIndex == 0) {
					Boolean taken = (Boolean) inputValue;
					amygdala.branching_event(getSourceRelativeIdentifier(), BranchingNodeAttribute.BRANCH, children.get(0).getLeft(),
											 taken,
											 extractPredicate());
				}
			}

			public void onInputValueBehaviorWhileNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                                      Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				if (inputIndex == 0) {
					amygdala.branching_event(getSourceRelativeIdentifier(), BranchingNodeAttribute.LOOP, children.get(0).getLeft(),
											 (Boolean) inputValue, extractPredicate());
				}
			}

			public void onInputValueBehaviorPropertyNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
														 Object inputValue) {
				// Save the the object that is written to/read from
				if (inputIndex == 0) {
					context_object = inputValue;
				}
			}

			public void onInputValueBehaviorReadWriteElementNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
														 Object inputValue) {
				// Save the the object that is written to/read from
				if (inputIndex == 0) {
					context_object = inputValue;
				}
				// Save the array index
				if (inputIndex == 1) {
					element_access = inputValue;
				}
			}

			public void onInputValueBehaviorCallNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
			Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				// 0 is ? node, 1 is the function object to call
				// TODO a bit hacky
				if (inputIndex >= 2) {
					this.arguments_array.add(amygdala.tracer.getIntermediate(children.get(inputIndex).getLeft()));
				}
				// Every call to onInput (or onEnter if Call0Node!)
				// inside a call node could
				// be the last input before the function gets called
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onInputValueBehaviorInvokeNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
													 Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				// Save the the object which method is called
				if (inputIndex == 0) {
					context_object = inputValue;
				}
				// 0 is object node, 1 is the function object to call (?)
				// TODO a bit hacky
				if (inputIndex >= 2) {
					this.arguments_array.add(amygdala.tracer.getIntermediate(children.get(inputIndex).getLeft()));
				}
				// Every call to onInput (or onEnter if Invoke0Node!)
				// inside a call node could
				// be the last input before the function gets called
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onInputValueBehaviorJSNewNodeGen(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
													   Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				// 0 is object to create, others are arguments (?)
				// TODO a bit hacky
				if (inputIndex >= 1) {
					this.arguments_array.add(amygdala.tracer.getIntermediate(children.get(inputIndex).getLeft()));
				}
				// Every call to onInput
				// inside a call node could
				// be the last input before the function gets called
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			private String extractPredicate() {
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					Matcher branch_matcher = branch_pattern.matcher(my_sourcesection.getCharacters().toString());
					if (branch_matcher.lookingAt()) {
						String kind = branch_matcher.group(1);
						String predicate = branch_matcher.group(2);
						if (predicate.length() > 16) {
							predicate = predicate.substring(0, 12) + "...";
						}
						return kind.toUpperCase() + " " + predicate;
					} else {
						return "(NO SOURCE)";
					}
				} else {
					return "(NO SOURCE)";
				}
			}

			// Default Behavior is to just pass through any symbolic flow
			public void onReturnBehaviorPassthrough(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> child = getChildHashes();
				// Kann kein Verhalten aus mehrern Kindern ableiten
				if (child.size() == 1) {
					amygdala.tracer.passThroughIntermediate(node_hash, child.get(0).getLeft());
				}
			}

			// ===== JavaScript Read/Write =====

			public void onReturnBehaviorGlobalPropertyNode(VirtualFrame vFrame, Object result) {
				GlobalPropertyNode gpnode = (GlobalPropertyNode) my_node;
				amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object), gpnode.getPropertyKey(), node_hash);
			}

			public void onReturnBehaviorGlobalObjectNode(VirtualFrame vFrame, Object result) {
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
			}

			public void onReturnBehaviorPropertyNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> child = getChildHashes();
				PropertyNode pnode = (PropertyNode) my_node;
				String property_name = pnode.getPropertyKey().toString();
				if (JSGuards.isString(context_object) && property_name.equals("length")) {
					onReturnBehaviorUnaryOperation(vFrame, result, Operation.STR_LENGTH);
				} else {
					amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object), property_name, node_hash);
				}
			}

			public void onReturnBehaviorWritePropertyNode(VirtualFrame vFrame, Object result) {
				WritePropertyNode wpnode = (WritePropertyNode) my_node;
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				// TODO toString()?
				amygdala.tracer.intermediateToProperty(System.identityHashCode(context_object), wpnode.getKey().toString(), children.get(1).getLeft());
				amygdala.tracer.passThroughIntermediate(node_hash, children.get(1).getLeft());
			}

			public void onReturnBehaviorJSReadCurrentFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes.hasNext()) {
					Scope innermost_scope = local_scopes.next();
					Object root_instance = innermost_scope.getRootInstance();
					if (root_instance != null) {
						ArrayList<Integer> scope_hashes = new ArrayList<>();
						scope_hashes.add(System.identityHashCode(root_instance));
						amygdala.tracer.frameSlotToIntermediate(scope_hashes, JSFrameUtil.getPublicName(jsrfsn.getFrameSlot()), node_hash);
					} else {
						amygdala.logger.critical("onReturnBehaviorJSReadCurrentFrameSlotNodeGen(): Cannot get root instance.");
					}
				} else {
					amygdala.logger.critical("onReturnBehaviorJSReadCurrentFrameSlotNodeGen(): Cannot find any local scopes.");
				}
			}

			public void onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				JSWriteFrameSlotNode jswfsn = (JSWriteFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes.hasNext()) {
					Scope innermost_scope = local_scopes.next();
					Object root_instance = innermost_scope.getRootInstance();
					if (root_instance != null) {
						ArrayList<Integer> scope_hashes = new ArrayList<>();
						scope_hashes.add(System.identityHashCode(root_instance));
						amygdala.tracer.intermediateToFrameSlot(scope_hashes, JSFrameUtil.getPublicName(jswfsn.getFrameSlot()), children.get(0).getLeft());
					} else {
						amygdala.logger.critical("onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(): Cannot get root instance.");
					}
				} else {
					amygdala.logger.critical("onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(): Cannot find any local scopes.");
				}
				amygdala.tracer.passThroughIntermediate(node_hash, children.get(0).getLeft());
			}

			public void onReturnBehaviorJSReadScopeFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes.hasNext()) {
					ArrayList<Integer> scope_hashes = new ArrayList<>();
					while (local_scopes.hasNext()) {
						Scope curr_scope = local_scopes.next();
						Object root_instance = curr_scope.getRootInstance();
						if (root_instance != null) {
							scope_hashes.add(System.identityHashCode(root_instance));
						} else {
							amygdala.logger.critical("onReturnBehaviorJSScopeFrameSlotNodeGen(): Cannot get root instance.");
						}
					}
					amygdala.tracer.frameSlotToIntermediate(scope_hashes, JSFrameUtil.getPublicName(jsrfsn.getFrameSlot()), node_hash);
				} else {
					amygdala.logger.critical("onReturnBehaviorJSReadScopeFrameSlotNodeGen(): Cannot find any local scopes.");
				}
			}

			public void onReturnBehaviorJSWriteScopeFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				JSWriteFrameSlotNode jswfsn = (JSWriteFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes.hasNext()) {
					ArrayList<Integer> scope_hashes = new ArrayList<>();
					while (local_scopes.hasNext()) {
						Scope curr_scope = local_scopes.next();
						Object root_instance = curr_scope.getRootInstance();
						if (root_instance != null) {
							scope_hashes.add(System.identityHashCode(root_instance));
						} else {
							amygdala.logger.critical("onReturnBehaviorJSScopeFrameSlotNodeGen(): Cannot get root instance.");
						}
					}
					amygdala.tracer.intermediateToFrameSlot(scope_hashes, JSFrameUtil.getPublicName(jswfsn.getFrameSlot()), children.get(0).getLeft());
				} else {
					amygdala.logger.critical("onReturnBehaviorJSReadScopeFrameSlotNodeGen(): Cannot find any local scopes.");
				}
				amygdala.tracer.passThroughIntermediate(node_hash, children.get(0).getLeft());
			}

			// ===== JavaScript Function Handling =====

			public void onReturnBehaviorCallNode(VirtualFrame vFrame, Object result) {
				amygdala.tracer.functionReturnValueToIntermediate(node_hash);
				amygdala.tracer.resetFunctionReturnValue();
			}

			public void onReturnBehaviorInvokeNode(VirtualFrame vFrame, Object result) {
				if (JSGuards.isString(context_object)) {
					ArrayList<Pair<Integer, String>> children = getChildHashes();
					Matcher method_matcher = method_pattern.matcher(my_sourcesection.getCharacters().toString());
					if (method_matcher.matches()) {
						String method_name = method_matcher.group(1);
						switch (method_name) {
							case "concat":
								amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arguments_array, Operation.STR_CONCAT);
								break;
							case "charAt":
								amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arguments_array, Operation.STR_CHAR_AT);
								break;
							case "substr":
								amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arguments_array, Operation.STR_SUBSTR);
								break;
							case "includes":
								amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arguments_array, Operation.STR_INCLUDES);
								break;
							case "indexOf":
								amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arguments_array, Operation.STR_INDEXOF);
								break;
							default:
								amygdala.logger.critical("onReturnBehaviorInvokeNode(): String method '" + method_name + "' not implemented");
						}
					} else {
						amygdala.logger.critical("onReturnBehaviorInvokeNode(): Trying to compute string operation, but cannot extract name.");
					}
				} else {
					amygdala.tracer.functionReturnValueToIntermediate(node_hash);
					amygdala.tracer.resetFunctionReturnValue();
				}
			}

			public void onReturnBehaviorJSNewNodeGen(VirtualFrame vFrame, Object result) {
				//TODO always an object, never a basic type?
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
				amygdala.tracer.resetFunctionReturnValue();
			}

			public void onReturnBehaviorAccessIndexedArgumentNode(VirtualFrame vFrame, Object result) {
				AccessIndexedArgumentNode aian = (AccessIndexedArgumentNode) my_node;
				amygdala.tracer.argumentToIntermediate(aian.getIndex(), node_hash);
			}

			public void behaviorFrameReturnTerminalPositionReturnNode() {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				amygdala.tracer.intermediateToFunctionReturnValue(children.get(0).getLeft());
			}

			// ===== JavaScript General Nodes =====

			public void onReturnBehaviorBinaryOperation(VirtualFrame vFrame, Object result, Operation op) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 2;
				try {
					amygdala.tracer.addOperation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft(),
												 children.get(1).getLeft());
				} catch (SymbolicException.WrongParameterSize wrongParameterSize) {
					amygdala.logger.critical("onReturnBehaviorBinaryOperation(): WrongParameterSize: " + wrongParameterSize.getMessage() + ".");
				}
			}

			// TODO
			public void onReturnBehaviorJSAddSubNumericUnitNodeGen(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 1;
				String before = String.valueOf(my_sourcesection.getSource().getCharacters().charAt(my_sourcesection.getCharIndex() - 1));
				String after = String.valueOf(my_sourcesection.getSource().getCharacters().charAt(my_sourcesection.getCharEndIndex()));
				if (before.equals("+") || after.equals("+")) {
					SymbolicNode pre_add = amygdala.tracer.getIntermediate(children.get(0).getLeft());
					SymbolicNode add_result = null;
					try {
						add_result = new Addition(LanguageSemantic.JAVASCRIPT, pre_add, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
					} catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize ex) {
						ex.printStackTrace();
					}
					amygdala.tracer.setIntermediate(node_hash, add_result);
				} else if (before.equals("-") || after.equals("-")) {
					SymbolicNode pre_sub = amygdala.tracer.getIntermediate(children.get(0).getLeft());
					SymbolicNode sub_result = null;
					try {
						sub_result = new Subtraction(LanguageSemantic.JAVASCRIPT, pre_sub, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
					} catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize ex) {
						ex.printStackTrace();
					}
					amygdala.tracer.setIntermediate(node_hash, sub_result);
				} else {
					amygdala.logger.critical("onReturnBehaviorJSAddSubNumericUnitNodeGen(): Cannot determine operation from source code.");
				}
			}

			public void onReturnBehaviorUnaryOperation(VirtualFrame vFrame, Object result, Operation op) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 1;
				try {
					amygdala.tracer.addOperation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft());
				} catch (SymbolicException.WrongParameterSize wrongParameterSize) {
					amygdala.logger.critical("onReturnBehaviorUnaryOperation(): WrongParameterSize: " + wrongParameterSize.getMessage() + ".");
				}
			}

			public void onReturnBehaviorConstant(VirtualFrame vFrame, Object result, ExpressionType type) {
				if (this.isInputNode) {
					Object next_input = amygdala.getNextInputValue(this.inputVariableIdentifier);
					amygdala.tracer.addVariable(node_hash, LanguageSemantic.JAVASCRIPT, this.inputVariableIdentifier);
					throw this.event_context.createUnwind(next_input);
				} else {
					amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, type, result);
				}
			}

			public void onReturnBehaviorObjectLiteralNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				DynamicObject dobj;
				try {
					dobj = (DynamicObject) result;
				} catch (ClassCastException cce) {
					amygdala.logger.critical("onReturnBehaviorObjectLiteralNode(): Cannot cast result to DynamicObject.");
					return;
				}
				Shape obj_shape = dobj.getShape();
				List<Object> keys = obj_shape.getKeyList();
				if (keys.size() != children.size()) {
					amygdala.logger.critical("onReturnBehaviorObjectLiteralNode(): Resulting object has not the same number of keys as the child nodes.");
					return;
				}
				VariableContext obj_ctx = new VariableContext();
				for (int ch_index = 0; ch_index < keys.size(); ch_index++) {
					//TODO toString()?
					obj_ctx.set(keys.get(ch_index).toString(), amygdala.tracer.getIntermediate(children.get(ch_index).getLeft()));
				}
				amygdala.tracer.setSymbolicContext(System.identityHashCode(result), obj_ctx);
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
			}

			public void onReturnBehaviorDefaultArrayLiteralNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				VariableContext new_array = new VariableContext();
				for (int i = 0; i < children.size(); i++) {
					Pair<Integer, String> child = children.get(i);
					new_array.set(i, amygdala.tracer.getIntermediate(child.getLeft()));
				}
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
				amygdala.tracer.setSymbolicContext(System.identityHashCode(result), new_array);
			}

			public void onReturnBehaviorConstantArrayLiteralNode(VirtualFrame vFrame, Object result) {
				VariableContext array_ctx = null;
				try {
					array_ctx = arrayToSymbolic((DynamicObject) result);
				} catch (ClassCastException cce) {
					amygdala.logger.critical("onReturnBehaviorConstantArrayLiteralNode(): Cannot cast result to DynamicObject.");
				}
				if (array_ctx != null) {
					amygdala.tracer.setSymbolicContext(System.identityHashCode(result), array_ctx);
				}
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
			}

			public void onReturnBehaviorReadElementNode(VirtualFrame vFrame, Object result) {
				if (JSGuards.isString(context_object)) {
					ArrayList<Pair<Integer, String>> children = getChildHashes();
					ArrayList<SymbolicNode> arg = new ArrayList<>();
					arg.add(amygdala.tracer.getIntermediate(children.get(1).getLeft()));
					amygdala.tracer.addStringOperation(node_hash, LanguageSemantic.JAVASCRIPT, children.get(0).getLeft(), arg, Operation.STR_CHAR_AT);
				} else {
					amygdala.tracer.propertyToIntermediate(System.identityHashCode(context_object), element_access, node_hash);
				}
			}

			public void onReturnBehaviorWriteElementNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				amygdala.tracer.intermediateToProperty(System.identityHashCode(context_object), element_access, children.get(2).getLeft());
			}

			//TODO extremely hacky
			public void onReturnBehaviorDualNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				Matcher inc_matcher = increment_pattern.matcher(my_sourcesection.getCharacters().toString());
				Matcher dec_matcher = decrement_pattern.matcher(my_sourcesection.getCharacters().toString());
				// if DualNode is part of an increment/decrement operation
				if (inc_matcher.matches()) {
					assert children.size() == 1;
					SymbolicNode pre = amygdala.tracer.getIntermediate(children.get(0).getLeft());
					SymbolicNode revert_increment = null;
					try {
						revert_increment = new Subtraction(LanguageSemantic.JAVASCRIPT, pre, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
					} catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize ex) {
						ex.printStackTrace();
					}
					amygdala.tracer.setIntermediate(node_hash, revert_increment);
				} else if (dec_matcher.matches()) {
					assert children.size() == 1;
					SymbolicNode pre = amygdala.tracer.getIntermediate(children.get(0).getLeft());
					SymbolicNode revert_decrement = null;
					try {
						revert_decrement = new Addition(LanguageSemantic.JAVASCRIPT, pre, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER, 1));
					} catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize ex) {
						ex.printStackTrace();
					}
					amygdala.tracer.setIntermediate(node_hash, revert_decrement);
				} else {
					// nothing to do for now
				}
			}

			public void onReturnBehaviorJSGlobalPrintNodeGen(VirtualFrame vFrame, Object result) {
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null);
			}

			public void onReturnBehaviorExprBlockNode(VirtualFrame vFrame, Object result) {
				// TODO
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.NULL, null);
			}

			public VariableContext arrayToSymbolic(DynamicObject dyn_obj) {
				if (JSRuntime.isArray(dyn_obj)) {
					VariableContext array_ctx = new VariableContext();
					long size = 0;
					try {
						size = INTEROP.getArraySize(dyn_obj);
					} catch (UnsupportedMessageException e) {
						amygdala.logger.critical("arrayToSymbolic(): Object is an array, but we cannot get the size.");
						return null;
					}
					for (int i = 0; i < size; i++) {
						Object array_elem;
						try {
							array_elem = INTEROP.readArrayElement(dyn_obj, i);
						} catch (UnsupportedMessageException | InvalidArrayIndexException e) {
							amygdala.logger.critical("arrayToSymbolic(): Object is an array, but we cannot read the element with index" + i + ".");
							return null;
						}
						//amygdala.logger.alert(array_elem.toString());
						try {
							if (JSGuards.isBoolean(array_elem)) {
								//amygdala.logger.log("is Boolean");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BOOLEAN, array_elem));
							} else if (JSGuards.isString(array_elem)) {
								//amygdala.logger.log("is String");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.STRING, array_elem));
							} else if (JSGuards.isBigInt(array_elem)) {
								//amygdala.logger.log("is BigInt");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.BIGINT, array_elem));
							} else if (JSGuards.isNumberInteger(array_elem)) {
								//amygdala.logger.log("is Integer");
								array_ctx.set(i,
										new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_INTEGER,
															 array_elem));
							} else if (JSGuards.isNumberDouble(array_elem) && JSRuntime.isNaN(array_elem)) {
								//amygdala.logger.log("is NaN");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_NAN, null));
							} else if (JSGuards.isNumberDouble(array_elem) && JSRuntime.isPositiveInfinity((double) array_elem)) {
								//amygdala.logger.log("is +Infinity");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_NAN, null));
							} else if (JSGuards.isNumberDouble(array_elem)) {
								//amygdala.logger.log("is Double");
								array_ctx.set(i,
										new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NUMBER_REAL, array_elem));
							} else if (JSGuards.isUndefined(array_elem)) {
								//amygdala.logger.log("is Undefined");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.UNDEFINED, null));
							} else if (JSGuards.isJSNull(array_elem)) {
								//amygdala.logger.log("is Null");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.NULL, null));
							} else {
								//amygdala.logger.log("is Other");
								array_ctx.set(i, new SymbolicConstant(LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null));
							}
						} catch (SymbolicException.IncompatibleType ite) {
							amygdala.logger.warning("arrayToSymbolic(): Cannot create symbolic representation of " + array_elem.toString() + ".");
						}
					}
					return array_ctx;
				} else {
					amygdala.logger.critical("arrayToSymbolic(): Object \"" + dyn_obj.toString() + "\" is not a JavaScript array.");
					return null;
				}
			}
		};
	}
}