package org.fuzzingtool;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.binary.JSAddSubNumericUnitNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSScope;
import com.oracle.truffle.js.runtime.truffleinterop.InteropList;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import org.fuzzingtool.visualization.ASTVisualizer;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

class FuzzingNodeWrapperFactory implements ExecutionEventNodeFactory {
	private final TruffleInstrument.Env env;
	private int visualized_counter = 0;
	private final Logger logger;

	static final InteropLibrary INTEROP = LibraryFactory.resolve(InteropLibrary.class).getUncached();

	FuzzingNodeWrapperFactory(final TruffleInstrument.Env env, Logger l) {
		this.env = env;
		this.logger = l;
	}

	public ExecutionEventNode create(final EventContext ec) {
		if (ec.getInstrumentedNode().getClass().getSimpleName().equals("MaterializedFunctionBodyNode")) {
			ASTVisualizer av = new ASTVisualizer(ec.getInstrumentedNode(), this.logger);
			av.save_image(Paths.get(".").toAbsolutePath().normalize().toString() + "/function_visualization_" +
								  visualized_counter);
			visualized_counter += 1;
		}

		return new ExecutionEventNode() {
			private final EventContext event_context = ec;
			private final SourceSection my_sourcesection = ec.getInstrumentedSourceSection();
			private final Node my_node = ec.getInstrumentedNode();
			private final String node_type = my_node.getClass().getSimpleName();
			private final int node_hash = my_node.hashCode();

			protected String getSignature() {
				String node_type_padded = String.format("%1$-" + 36 + "s", node_type);
				String hash_padded = String.format("%1$-" + 12 + "s", node_hash);
				if (my_sourcesection != null && my_sourcesection.isAvailable()) {
					String line_padded = String.format("%1$" + 3 + "s", my_sourcesection.getStartLine());
					String characters = my_sourcesection.getCharacters().toString().replace("\n", "");
					String characters_cropped = characters.substring(0, Math.min(characters.length(), 16));
					return "[" + node_type_padded + " " + hash_padded + " " + line_padded + ":" + characters_cropped +
							"]";
				} else {
					return "[" + node_type_padded + " " + hash_padded + "     (NO SOURCE)]";
				}
			}

			protected String getLocalScopes(Node n, VirtualFrame vFrame) {
				StringBuilder builder = new StringBuilder();
				Iterable<Scope> a = env.findLocalScopes(n, vFrame);
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
								InteropList memebers = (InteropList) INTEROP.getMembers(s.getVariables());
								for (int i = 0; i < INTEROP.getArraySize(memebers); i++) {
									builder.append(INTEROP.readArrayElement(memebers, i).toString()).append("\n");
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

			@Override
			protected void onEnter(VirtualFrame vFrame) {
				//logger.log(getSignature() + " \033[32m→\033[0m");
				Node n = ec.getInstrumentedNode();

				/*if(n instanceof AccessIndexedArgumentNode) {
					logger.alert(n.getClass().toString());
					AccessIndexedArgumentNode aian = (AccessIndexedArgumentNode) n;
					logger.alert(String.valueOf(aian.getIndex()));
				}

				if(n instanceof JSFunctionCallNode) {
					logger.alert(n.getClass().toString());
					JSFunctionCallNode jsfcn = (JSFunctionCallNode) n;
					//logger.alert(String.valueOf(jsfcn.);
				}*/


				/*if(n instanceof JavaScriptFunctionCallNode) {
					logger.alert(n.getClass().toString());
					JavaScriptFunctionCallNode jsfcn = (JavaScriptFunctionCallNode) n;
					logger.log(jsfcn.getTarget().toString());
				}

				if (n instanceof FunctionBodyNode) {
					logger.alert(n.getClass().toString());
					FunctionBodyNode fbn = (FunctionBodyNode) n;
					logger.log(fbn.expressionToString());
				}*/

				/*if (n instanceof JSWriteFrameSlotNode) {
					logger.highlight(n.toString());

					Object this_obj = JSFrameUtil.getThisObj(vFrame);
					logger.log("this_obj: " + this_obj.toString());

					Object func_obj = JSFrameUtil.getFunctionObject(vFrame);
					logger.log("func_obj: " + func_obj.toString());

					logger.log(getLocalScopes(n, vFrame));
				}*/

				/*if (n instanceof JSReadFrameSlotNode) {
					logger.highlight(n.toString());

					JSReadFrameSlotNode jsrfsn = (JSReadFrameSlotNode) n;
					//logger.log(jsrfsn.expressionToString());

					FrameSlot slot = jsrfsn.getFrameSlot();
					//logger.log(slot.toString());

					//ScopeFrameNode sfn = jsrfsn.getLevelFrameNode();

					Object public_name = JSFrameUtil.getPublicName(slot);
					//logger.log(public_name.toString());

					Object this_obj = JSFrameUtil.getThisObj(vFrame);
					//logger.log(this_obj.toString());

					logger.log("Access " + public_name.toString() + " in Object " + this_obj.toString());

					logger.log("this_obj: " + this_obj.toString());

					Object func_obj = JSFrameUtil.getFunctionObject(vFrame);
					logger.log("func_obj: " + func_obj.toString());

					for (Node child: n.getChildren()) {
						logger.alert(child.toString());
						ScopeFrameNode sfn = (ScopeFrameNode) child;
						Frame rf = sfn.executeFrame(vFrame);
						logger.log(rf.toString());
						Object this_frame_obj = JSFrameUtil.getThisObj(rf);
						logger.log(this_frame_obj.toString());
					}

					logger.log(getLocalScopes(n ,vFrame));
				}*/

				/*if (n instanceof ObjectLiteralNode) {
					logger.highlight(n.toString());
					ObjectLiteralNode oln = (ObjectLiteralNode) n;
					Object this_obj = JSFrameUtil.getThisObj(vFrame);
					logger.log(this_obj.toString());
				}*/

				/*if (n instanceof JSConstantNode.JSConstantStringNode) {
					logger.highlight(n.toString());
					Object this_obj = JSFrameUtil.getThisObj(vFrame);
					logger.log(this_obj.toString());
				}*/

				/*if (n instanceof WritePropertyNode) {
					logger.highlight(n.toString());
					WritePropertyNode wpn = (WritePropertyNode) n;

					logger.log(wpn.getKey().toString());

					// gibt mir das target in das geschrieben wird: kann für sachen wie person.age=3434 genutzt
					// werden, um die id von person zu bekommen.
					//logger.log(wpn.evaluateTarget(vFrame).toString());

					//JavaScriptNode wpn_rhs = wpn.getRhs();
					//Object value_obj = wpn_rhs.execute(vFrame);
					//if (value_obj instanceof DynamicObject) {
					//	DynamicObject casted = (DynamicObject) value_obj;
					//	logger.log("Casted Object: " + casted.toString());
					//}
					//logger.log("wpn_rhs: " + value_obj.toString());
					Object this_obj = JSFrameUtil.getThisObj(vFrame);
					logger.log("this_obj: " + this_obj.toString());

					Object func_obj = JSFrameUtil.getFunctionObject(vFrame);
					logger.log("func_obj: " + func_obj.toString());
				}*/
			}

			@Override
			protected void onInputValue(VirtualFrame vFrame, EventContext inputContext, int inputIndex,
										Object inputValue) {
				//logger.log(getSignature() + " \033[34m•\033[0m");
				Node n = ec.getInstrumentedNode();

				/*if (n instanceof ObjectLiteralNode) {
					logger.alert(n.toString());
					ObjectLiteralNode oln = (ObjectLiteralNode) n;
					Set<Object> ident = vFrame.getFrameDescriptor().getIdentifiers();
					for (Object i: ident) {
						logger.log(i.toString());
					}
					logger.log(inputValue.getClass().toString());
				}*/

				/*if (n instanceof WritePropertyNode && inputIndex == 0) {
					logger.log("WritePropertyInput 0:");
					logger.log(inputValue.toString());
				}*/

				//logger.log("InputFrame: " + vFrame + " input Index: " + inputIndex);
			}

			@Override
			public void onReturnValue(VirtualFrame vFrame, Object result) {
				//logger.log(getSignature() + " \033[31m↵\033[0m");
				Node n = ec.getInstrumentedNode();

				/*if (n instanceof GlobalObjectNode) {
					logger.alert(result.toString());
				}

				if (n instanceof ArrayLiteralNode) {
					logger.alert(result.toString());
				}*/

				/*if (n instanceof ArrayLiteralNode) {
					ArrayLiteralNode aln = (ArrayLiteralNode) n;
					arrayToSymbolic((DynamicObject) result);
				}*/

				if (n instanceof JSAddSubNumericUnitNode) {
					SourceSection ss = ec.getInstrumentedSourceSection();
					logger.log(ss.toString());
					logger.log("index: " + String.valueOf(ss.getCharIndex()));
					logger.log(String.valueOf(ss.getSource().getCharacters().charAt(ss.getCharIndex())));
					String before = String.valueOf(ss.getSource().getCharacters().charAt(ss.getCharIndex() - 1));
					String after = String.valueOf(ss.getSource().getCharacters().charAt(ss.getCharEndIndex()));
					logger.log(before);
					logger.log(after);
				}

				if (n instanceof WritePropertyNode) {
					logger.log(result.toString());
				}

				/*if (n instanceof GlobalPropertyNode) {
					arrayToSymbolic((DynamicObject) result);
				}*/

				if (n instanceof DualNode) {
					DualNode dn = (DualNode) n;
					logger.log(dn.getRight().expressionToString());
				}

				/*if (n instanceof ObjectLiteralNode) {
					logger.alert(n.toString());
					logger.log(result.toString());
					DynamicObject dobj = (DynamicObject) result;
					Shape obj_shape = dobj.getShape();
					List<Object> keys = obj_shape.getKeyList();
					for (Object key: keys) {
						logger.log(key.toString());
					}
					Node child = n.getChildren().iterator().next();
					if (child instanceof ObjectLiteralNode.ObjectLiteralMemberNode) {
						ObjectLiteralNode.ObjectLiteralMemberNode mn = (ObjectLiteralNode.ObjectLiteralMemberNode) child;
						logger.log(String.valueOf(mn.isStatic()));
						logger.log(String.valueOf(mn.isField()));
						logger.log(String.valueOf(mn.isAnonymousFunctionDefinition()));
						logger.log(mn.getEncapsulatingSourceSection().toString());
						logger.log(mn.getDebugProperties().toString());
					} else {
						logger.log("nope");
					}
				}*/

				/*if (n instanceof GlobalPropertyNode) {
					logger.log(result.toString());
				}*/

				/*if(n instanceof GlobalPropertyNode){
					logger.log(((GlobalPropertyNode) n).getPropertyKey());
					logger.log("\t" + n.getRootNode().toString());
				}
				if(n instanceof JSReadFrameSlotNode){
					FrameSlot slot = ((JSReadFrameSlotNode) n).getFrameSlot();
					logger.log("Reading slot" + slot);
					logger.log(vFrame.toString());
					logger.log(result.toString());
					logger.log(("expression: " +((JSReadFrameSlotNode) n).expressionToString()));
				}
				if(n instanceof JSAddNodeGen){
					logger.log("result:"  + result);
				}*/


				/*Iterable<Scope> a = env.findLocalScopes(n, vFrame);
				if (a != null) {
					for (Scope s: env.findLocalScopes(n, vFrame)) {
						try {
							logger.log(s.getClass().toString());
							Object args = s.getArguments();
							if (args != null) {
								logger.log("---- Start");
								logger.log("args");
								logger.log(args.getClass().toString());
								logger.log(args.toString());
								logger.log("---- Done");
							}
							Object name = s.getName();
							if (name != null) {
								logger.log("---- Start");
								logger.log("name");
								logger.log(name.getClass().toString());
								logger.log(name.toString());
								logger.log("---- Done");
							}
							Node tmpN = s.getNode();
							if (tmpN != null) {
								logger.log("---- Start");
								logger.log("tmpN");
								logger.log(tmpN.getClass().toString());
								logger.log(tmpN.toString());
								logger.log("---- Done");
							}
							Object rec = s.getReceiver();
							if (rec != null) {
								logger.log("---- Start");
								logger.log("rec");
								logger.log(rec.getClass().toString());
								logger.log(rec.toString());
								logger.log("---- Done");
							}
							Object recName = s.getReceiverName();
							if (recName != null) {
								logger.log("---- Start");
								logger.log("recName");
								logger.log(recName.getClass().toString());
								logger.log(recName.toString());
								logger.log("---- Done");
							}
							Object root = s.getRootInstance();
							if (root != null) {
								logger.log("---- Start");
								logger.log("root");
								logger.log(root.getClass().toString());
								logger.log(root.toString());
								logger.log("---- Done");
							}
							Object vars = s.getVariables();
							if (vars != null) {
								logger.log("---- Start");
								logger.log("vars");
								logger.log(vars.getClass().toString());
								logger.log(vars.toString());
								logger.log("---- Done");
							}
							logger.log(s.getName() + ":::" + s.getNode().hashCode());
						} catch (java.lang.Exception ex) {
							ex.printStackTrace();
							logger.alert("not good");
						}
					}
				}*/
			}

			public void arrayToSymbolic(DynamicObject dyn_obj) {
				logger.alert("array2Symbolic start");
				try {
					long size = INTEROP.getArraySize(dyn_obj);
					for(int i = 0; i < size; i++) {
						try {
							DynamicObject array_elem = (DynamicObject) INTEROP.readArrayElement(dyn_obj, i);
							logger.log(String.valueOf(JSRuntime.isNullOrUndefined(array_elem)));
							logger.log(String.valueOf(JSGuards.isUndefined(array_elem)));
						} catch (ClassCastException e) {
							logger.log("cast err");
						}
					}
				} catch (UnsupportedMessageException | InvalidArrayIndexException | ClassCastException e) {
					logger.highlight("error");
				}
				logger.alert("array2Symbolic end");
			}

			public void propertyHash(DynamicObject dyn_obj) {
				logger.alert("array2Symbolic start");
				Shape obj_shape = dyn_obj.getShape();
				List<Property> keys = obj_shape.getPropertyList();
				for (Property key: keys) {
					logger.log(key.toString());
					logger.log(String.valueOf(key.getLocation().hashCode()));
				}
				logger.alert("array2Symbolic end");
			}

			@Override
			protected Object onUnwind(VirtualFrame frame, Object info) {
				return info;
			}
		};
	}
}