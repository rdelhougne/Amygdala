package org.fuzzingtool;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.truffleinterop.InteropList;
import org.fuzzingtool.components.Amygdala;
import org.fuzzingtool.components.BranchingNodeAttribute;
import org.fuzzingtool.components.VariableContext;
import org.fuzzingtool.components.VariableIdentifier;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.Operation;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.visualization.ASTVisualizer;
import org.graalvm.collections.Pair;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

class FuzzingNodeWrapperFactory implements ExecutionEventNodeFactory {
	private final TruffleInstrument.Env env;
	private final Amygdala amygdala;
	private int visualized_counter = 0;

	private static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

	// Capture group 2 is variable name
    private final Pattern assignment_pattern = Pattern.compile("(var\\s+|let\\s+|const\\s+)?\\s*([A-Za-z]\\w*)\\s*=.*");

	FuzzingNodeWrapperFactory(final TruffleInstrument.Env env, Amygdala amy) {
		this.env = env;
		this.amygdala = amy;
	}

	public ExecutionEventNode create(final EventContext ec) {
		if (ec.getInstrumentedNode().getClass().getSimpleName().equals("MaterializedFunctionBodyNode")) {
			ASTVisualizer av = new ASTVisualizer(ec.getInstrumentedNode(), amygdala.logger);
			av.save_image(Paths.get(".").toAbsolutePath().normalize().toString() +
                                  "/function_visualization_" + visualized_counter);
			visualized_counter += 1;
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

		final boolean create_nodeIsMainLoopInputNode;
		if (ec.getInstrumentedSourceSection() != null && ec.getInstrumentedSourceSection().isAvailable()) {
			String source_code_line =
					ec.getInstrumentedSourceSection().getSource().getCharacters(amygdala.main_loop_line_num).toString();
			create_nodeIsMainLoopInputNode = ec.getInstrumentedNode() instanceof JSConstantNode &&
					ec.getInstrumentedSourceSection().getStartLine() == amygdala.main_loop_line_num &&
					source_code_line.contains(amygdala.main_loop_identifier_string);
		} else {
			create_nodeIsMainLoopInputNode = false;
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
			private final boolean isMainLoopInputNode = create_nodeIsMainLoopInputNode;

			// Various save spots
			// used by PropertyNode and WritePropertyNode to save the hash of the object
			private int object_context_hash = 0;
			// used by ReadElementNode and WriteElementNode to determine the array index
			private int array_index = 0;
			// used by Call1..NNodes to construct the arguments array
			private VariableContext arguments_array = new VariableContext(VariableContext.ContextType.ARRAY);

			protected String getSignatureString() {
				String node_type_padded = String.format("%1$-" + 36 + "s", node_type);
				String hash_padded = String.format("%1$-" + 12 + "s", node_hash);
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					String line_padded = String.format("%1$" + 3 + "s", my_sourcesection.getStartLine());
					String characters = my_sourcesection.getCharacters().toString().replace("\n", "");
					String characters_cropped = characters.substring(0, Math.min(characters.length(), 16));
					return "[" + node_type_padded + " " + hash_padded + " " + line_padded + ":" + characters_cropped + "]";
				} else {
					return "[" + node_type_padded + " " + hash_padded + "     (NO SOURCE)]";
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
						return innermost_scope.getReceiver().hashCode();
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
				//amygdala.logger.log(getSignatureString() + " \033[32m→\033[0m");

				switch (node_type) {
					case "Call0Node":
					case "Call1Node":
					case "CallNNode":
						onEnterBehaviorCallNode(vFrame);
						break;
					case "MaterializedFunctionBodyNode":
						onEnterBehaviorFunctionBodyNode(vFrame);
					default:
						onEnterBehaviorDefault(vFrame);
				}
			}

			@Override
			protected void onInputValue(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                        Object inputValue) {
				//amygdala.logger.log(getSignatureString() + " \033[34m•\033[0m");

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
						onInputBehaviorCallNode(vFrame, inputContext, inputIndex, inputValue);
						break;
					default:
						onInputValueBehaviorDefault(vFrame, inputContext, inputIndex, inputValue);
				}
			}

			@Override
			public void onReturnValue(VirtualFrame vFrame, Object result) {
				//amygdala.logger.log(getSignatureString() + " \033[31m↵\033[0m");

				try {
					switch (node_type) {
						// ===== JavaScript Read/Write =====
						case "GlobalPropertyNode":
							onReturnBehaviorGlobalPropertyNode(vFrame, result);
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
						case "AccessIndexedArgumentNode":
							onReturnBehaviorAccessIndexedArgumentNode(vFrame, result);
							break;
						case "TerminalPositionReturnNode":
							onReturnBehaviorTerminalPositionReturnNode(vFrame, result);
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
						case "AutonomousFunctionExpressionNode":
							onReturnBehaviorConstant(vFrame, result, ExpressionType.OBJECT);
							break;

							// ===== JavaScript Arrays =====
						case "DefaultArrayLiteralNode":
							onReturnBehaviorDefaultArrayLiteralNode(vFrame, result);
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
						default:
							onReturnBehaviorDefault(vFrame, result);
					}
				} catch (SymbolicException.WrongParameterSize ex) {
					amygdala.logger.alert(ex.getMessage());
				}
			}

			@Override
			protected Object onUnwind(VirtualFrame frame, Object info) {
				return info;
			}

			// This method is only for testing for inconsistencies in the symbolic flow, should be removed later
			public void invalidate_interim(ArrayList<Pair<Integer, String>> old_results) {
				for (Pair<Integer, String> to_destroy: old_results) {
					amygdala.tracer.removeIntermediate(to_destroy.getLeft());
				}
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

			public void onEnterBehaviorDefault(VirtualFrame vFrame) {
				return;
			}

			public void onEnterBehaviorCallNode(VirtualFrame vFrame) {
				if (my_sourcesection.getStartLine() == amygdala.error_line_num &&
						my_sourcesection.getSource().getCharacters(amygdala.error_line_num).toString()
								.contains(amygdala.error_identifier_string)) {
					amygdala.error_event();
				}
				this.arguments_array.clear();
				// if CallNode has no arguments (Call0Node)
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			public void onEnterBehaviorFunctionBodyNode(VirtualFrame vFrame) {
				// Reset the current return value to default (undefined)
				amygdala.tracer.resetFunctionReturnValue();
			}

			public void onInputValueBehaviorDefault(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                                    Object inputValue) {
				return;
			}

			public void onInputValueBehaviorIfNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                                   Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 2;
				if (inputIndex == 0) { // Predicate
					Boolean taken = (Boolean) inputValue;
					amygdala.branching_event(node_hash, BranchingNodeAttribute.BRANCH, children.get(0).getLeft(),
                                             taken,
											 extractPredicate());
				}
			}

			public void onInputValueBehaviorWhileNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
                                                      Object inputValue) {
				if (!(my_sourcesection.getStartLine() == amygdala.main_loop_line_num &&
						my_sourcesection.getSource().getCharacters(amygdala.main_loop_line_num).toString()
								.contains(amygdala.main_loop_identifier_string))) {
					ArrayList<Pair<Integer, String>> children = getChildHashes();
					if (inputIndex == 0) { // TODO Predicate
						amygdala.branching_event(node_hash, BranchingNodeAttribute.LOOP, children.get(0).getLeft(),
												 (Boolean) inputValue, extractPredicate());
					}
				}
			}

			public void onInputValueBehaviorPropertyNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
														 Object inputValue) {
				// Save the hash of the object that is written to/read from
				if (inputIndex == 0) {
					object_context_hash = inputValue.hashCode();
				}
			}

			public void onInputValueBehaviorReadWriteElementNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
														 Object inputValue) {
				// Save the hash of the object that is written to/read from
				if (inputIndex == 0) {
					object_context_hash = inputValue.hashCode();
				}
				// Save the array index
				if (inputIndex == 1) {
					array_index = (int) inputValue;
				}
			}

			public void onInputBehaviorCallNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
			Object inputValue) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				// 0 is ? node, 1 is the function object to call
				// TODO a bit hacky
				if (inputIndex >= 2) {
					this.arguments_array.appendValue(amygdala.tracer.getIntermediate(children.get(inputIndex).getLeft()));
				}
				// Every call to onInput (or onEnter if Call0Node!)
				// inside a call node could
				// be the last input before the function gets called
				amygdala.tracer.setArgumentsArray(this.arguments_array);
			}

			// TODO extremely costly...
			private String extractPredicate() {
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					String extracted_predicate = my_sourcesection.getCharacters().toString().replaceAll("\\s+", " ");
					extracted_predicate = extracted_predicate
							.substring(extracted_predicate.indexOf("(") + 1, extracted_predicate.length());
					int last_index = extracted_predicate.indexOf(")");
					if (last_index != -1) {
						extracted_predicate = extracted_predicate.substring(0, last_index);
					}
					return extracted_predicate;
				} else {
					return "(NO SOURCE)";
				}
			}

			// Default Behavior is to just pass through any symbolic flow
			public void onReturnBehaviorDefault(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> child = getChildHashes();
				// Kann kein Verhalten aus mehrern Kindern ableiten
				if (child.size() == 1) {
					amygdala.tracer.passThroughIntermediate(node_hash, child.get(0).getLeft());
				}
			}

			// ===== JavaScript Read/Write =====

			public void onReturnBehaviorGlobalPropertyNode(VirtualFrame vFrame, Object result) {
				GlobalPropertyNode gpnode = (GlobalPropertyNode) my_node;
				amygdala.tracer.getSymbolicObjectProperty(object_context_hash, gpnode.getPropertyKey(), node_hash);
			}

			public void onReturnBehaviorPropertyNode(VirtualFrame vFrame, Object result) {
				PropertyNode pnode = (PropertyNode) my_node;
				amygdala.tracer.getSymbolicObjectProperty(object_context_hash, pnode.getPropertyKey().toString(), node_hash);
			}

			public void onReturnBehaviorWritePropertyNode(VirtualFrame vFrame, Object result) {
				WritePropertyNode wpnode = (WritePropertyNode) my_node;
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				amygdala.tracer.setSymbolicObjectProperty(object_context_hash, wpnode.getKey().toString(), children.get(1).getLeft());
			}

			public void onReturnBehaviorJSReadCurrentFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes != null && local_scopes.hasNext()) {
					Scope innermost_scope = local_scopes.next();
					Object root_instance = innermost_scope.getRootInstance();
					if (root_instance != null) {
						ArrayList<Integer> scope_hashes = new ArrayList<>();
						scope_hashes.add(root_instance.hashCode());
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
				if (local_scopes != null && local_scopes.hasNext()) {
					Scope innermost_scope = local_scopes.next();
					Object root_instance = innermost_scope.getRootInstance();
					if (root_instance != null) {
						ArrayList<Integer> scope_hashes = new ArrayList<>();
						scope_hashes.add(root_instance.hashCode());
						amygdala.tracer.intermediateToFrameSlot(scope_hashes, JSFrameUtil.getPublicName(jswfsn.getFrameSlot()), children.get(0).getLeft());
					} else {
						amygdala.logger.critical("onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(): Cannot get root instance.");
					}
				} else {
					amygdala.logger.critical("onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(): Cannot find any local scopes.");
				}
			}

			public void onReturnBehaviorJSReadScopeFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
				JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) my_node;
				Iterator<Scope> local_scopes = env.findLocalScopes(my_node, vFrame).iterator();
				if (local_scopes != null && local_scopes.hasNext()) {
					ArrayList<Integer> scope_hashes = new ArrayList<>();
					while (local_scopes.hasNext()) {
						Scope curr_scope = local_scopes.next();
						Object root_instance = curr_scope.getRootInstance();
						if (root_instance != null) {
							scope_hashes.add(root_instance.hashCode());
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
				if (local_scopes != null && local_scopes.hasNext()) {
					ArrayList<Integer> scope_hashes = new ArrayList<>();
					while (local_scopes.hasNext()) {
						Scope curr_scope = local_scopes.next();
						Object root_instance = curr_scope.getRootInstance();
						if (root_instance != null) {
							scope_hashes.add(root_instance.hashCode());
						} else {
							amygdala.logger.critical("onReturnBehaviorJSScopeFrameSlotNodeGen(): Cannot get root instance.");
						}
					}
					amygdala.tracer.intermediateToFrameSlot(scope_hashes, JSFrameUtil.getPublicName(jswfsn.getFrameSlot()), children.get(0).getLeft());
				} else {
					amygdala.logger.critical("onReturnBehaviorJSReadScopeFrameSlotNodeGen(): Cannot find any local scopes.");
				}
			}

			// ===== JavaScript Function Handling =====

			public void onReturnBehaviorCallNode(VirtualFrame vFrame, Object result) {
				amygdala.tracer.functionReturnValueToIntermediate(node_hash);
				amygdala.tracer.resetFunctionReturnValue();
			}

			public void onReturnBehaviorAccessIndexedArgumentNode(VirtualFrame vFrame, Object result) {
				AccessIndexedArgumentNode aian = (AccessIndexedArgumentNode) my_node;
				amygdala.tracer.argumentToIntermediate(aian.getIndex(), node_hash);
			}

			public void onReturnBehaviorTerminalPositionReturnNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				amygdala.tracer.intermediateToFunctionReturnValue(children.get(0).getLeft());
			}

			// ===== JavaScript General Nodes =====

			public void onReturnBehaviorBinaryOperation(VirtualFrame vFrame, Object result, Operation op) throws
					SymbolicException.WrongParameterSize {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 2;
				amygdala.tracer.addOperation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft(),
											 children.get(1).getLeft());
				invalidate_interim(children);
			}

			public void onReturnBehaviorUnaryOperation(VirtualFrame vFrame, Object result, Operation op) throws
					SymbolicException.WrongParameterSize {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				assert children.size() == 1;
				amygdala.tracer.addOperation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft());
				invalidate_interim(children);
			}

			public void onReturnBehaviorConstant(VirtualFrame vFrame, Object result, ExpressionType type) {
				if (this.isMainLoopInputNode) {
					if (!amygdala.isFirstRun()) {
						// An diesem Punkt ist das Programm eigentlich beendet, wird aber jetzt neu gestartet.
						amygdala.terminate_event();
						// Hier wird entschieden ob ein weiterer Fuzzing-Durchlauf stattfindet
						throw this.event_context.createUnwind(amygdala.calculateNextPath());
					}
					amygdala.tracer.reset(LanguageSemantic.JAVASCRIPT, getThisObjectHash(vFrame));
				} else if (this.isInputNode) {
					Object next_input = amygdala.getNextInputValue(this.inputVariableIdentifier);
					amygdala.tracer.addVariable(node_hash, LanguageSemantic.JAVASCRIPT, this.inputVariableIdentifier);
					throw this.event_context.createUnwind(next_input);
				} else {
					amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, type, result);
				}
			}

			public void onReturnBehaviorDefaultArrayLiteralNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				VariableContext new_array = new VariableContext(VariableContext.ContextType.ARRAY);
				for (Pair<Integer, String> child: children) {
					new_array.appendValue(amygdala.tracer.getIntermediate(child.getLeft()));
				}
				amygdala.tracer.addConstant(node_hash, LanguageSemantic.JAVASCRIPT, ExpressionType.OBJECT, null);
				amygdala.tracer.setSymbolicContext(result.hashCode(), new_array);
			}

			public void onReturnBehaviorReadElementNode(VirtualFrame vFrame, Object result) {
				amygdala.tracer.getSymbolicArrayIndex(object_context_hash, array_index, node_hash);
			}

			public void onReturnBehaviorWriteElementNode(VirtualFrame vFrame, Object result) {
				ArrayList<Pair<Integer, String>> children = getChildHashes();
				amygdala.tracer.setSymbolicArrayIndex(object_context_hash, array_index, children.get(2).getLeft());
			}
		};
	}
}