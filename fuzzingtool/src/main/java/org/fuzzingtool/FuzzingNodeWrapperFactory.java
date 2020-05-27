package org.fuzzingtool;

import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.fuzzingtool.components.Amygdala;
import org.fuzzingtool.components.BranchingNodeAttribute;
import org.fuzzingtool.symbolic.Operation;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.Type;
import org.fuzzingtool.visualization.ASTVisualizer;
import org.graalvm.collections.Pair;

import java.nio.file.Paths;
import java.util.ArrayList;

class FuzzingNodeWrapperFactory implements ExecutionEventNodeFactory {
    private final TruffleInstrument.Env env;
    private Amygdala amygdala;
    private int visualized_counter = 0;

    FuzzingNodeWrapperFactory(final TruffleInstrument.Env env, Amygdala amy) {
        this.env = env;
        this.amygdala = amy;
    }

    public ExecutionEventNode create(final EventContext ec) {
        if (ec.getInstrumentedNode().getClass().getSimpleName().equals("MaterializedFunctionBodyNode")) {
            ASTVisualizer av = new ASTVisualizer(ec.getInstrumentedNode());
            av.save_image(Paths.get(".").toAbsolutePath().normalize().toString() + "/function_visualization_" + visualized_counter);
            visualized_counter += 1;
        }

        return new ExecutionEventNode() {
            private final SourceSection my_sourcesection = ec.getInstrumentedSourceSection();
            private Node my_node = ec.getInstrumentedNode();
            private String node_type = my_node.getClass().getSimpleName();
            private int node_hash = my_node.hashCode();
            private boolean constraints_satisfied = calcConstraints();

            protected boolean calcConstraints() {
                Scope scope = env.findLocalScopes(my_node, null).iterator().next();
                boolean scope_constraint_satisfied = false;
                if (scope != null && scope.getName().equals("factorial")) { // TODO
                    scope_constraint_satisfied = true;
                }
                return scope_constraint_satisfied;
            }

            protected String getSignature() {
                String node_type_padded = String.format("%1$-" + 36 + "s", node_type);
                String hash_padded = String.format("%1$-" + 12 + "s", node_hash);
                if (my_sourcesection != null && my_sourcesection.isAvailable()) {
                    String line_padded = String.format("%1$" + 3 + "s", my_sourcesection.getStartLine());
                    String characters = my_sourcesection.getCharacters().toString().replace("\n", "");
                    String characters_cropped = characters.substring(0, Math.min(characters.length(), 16));

                    return "[" + node_type_padded + " " + hash_padded + " "
                            + line_padded + ":"
                            + characters_cropped + "]";
                } else {
                    return "[" + node_type_padded + " " + hash_padded + "     (NO SOURCE)]";
                }
            }

            @Override
            protected void onEnter(VirtualFrame vFrame) {
                amygdala.logger.log(getSignature() + " \033[32m→\033[0m");
                    /*highlight("Entering vFrame: " + vFrame);

                    Node n = ec.getInstrumentedNode();
                    Iterable<Scope> a = env.findLocalScopes(n, vFrame);
                    if (a != null) {
                        for (Scope s : env.findLocalScopes(n, vFrame)) {
                            try {
                                highlight("Local: " + s.getName());
                                Object args = s.getArguments();
                            } catch (java.lang.Exception ex) {
                                ex.printStackTrace();
                                outStream.println("not good");
                            }
                        }
                    }

                    a = env.findTopScopes("js");
                    if (a != null) {
                        for (Scope s : env.findLocalScopes(n, vFrame)) {
                            try {
                                highlight("Global:" + s.getName());
                                Object args = s.getArguments();
                            } catch (java.lang.Exception ex) {
                                ex.printStackTrace();
                                outStream.println("not good");
                            }
                        }
                    }*/
            }

            @Override
            protected void onInputValue(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                amygdala.logger.log(getSignature() + " \033[34m•\033[0m");

                if (constraints_satisfied) {
                    switch (node_type) { // TODO big time
                        case "IfNode":
                            onInputValueBehaviorIfNode(vFrame, inputContext, inputIndex, inputValue);
                            break;
                        case "WhileNode":
                            onInputValueBehaviorWhileNode(vFrame, inputContext, inputIndex, inputValue);
                            break;
                        default:
                            onInputValueBehaviorDefault(vFrame, inputContext, inputIndex, inputValue);
                    }
                }
            }

            @Override
            public void onReturnValue(VirtualFrame vFrame, Object result) {
                amygdala.logger.log(getSignature() + " \033[31m↵\033[0m");

                if (constraints_satisfied) {
                    try {
                        switch (node_type) { // TODO big time
                            // ===== JavaScript Read/Write =====
                            case "JSReadCurrentFrameSlotNodeGen":
                                onReturnBehaviorJSReadCurrentFrameSlotNodeGen(vFrame, result);
                                break;
                            case "JSWriteCurrentFrameSlotNodeGen":
                                onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(vFrame, result);
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
                            case "JSDivisionNodeGen": // TODO richtiger Name?
                                onReturnBehaviorBinaryOperation(vFrame, result, Operation.DIVISION);
                                break;


                            // ===== JavaScript Constant Nodes =====
                            case "JSConstantIntegerNode":
                                onReturnBehaviorConstant(vFrame, result, Type.INT);
                                break;
                            case "JSConstantStringNode":
                                onReturnBehaviorConstant(vFrame, result, Type.STRING);
                                break;
                            case "JSConstantNullNode":
                                onReturnBehaviorConstant(vFrame, result, Type.VOID);
                                break;


                            // ===== JavaScript Logic Nodes =====
                            case "JSLessThanNodeGen":
                                onReturnBehaviorBinaryOperation(vFrame, result, Operation.LESS_THAN);
                                break;
                            case "JSEqualNodeGen":
                                onReturnBehaviorBinaryOperation(vFrame, result, Operation.EQUAL);
                                break;
                            case "JSAndNode":
                                onReturnBehaviorBinaryOperation(vFrame, result, Operation.AND);
                                break;
                            case "JSNotNodeGen":
                                onReturnBehaviorUnaryOperation(vFrame, result, Operation.NOT);
                                break;
                            default:
                                onReturnBehaviorDefault(vFrame, result);
                        }
                    } catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize ex) {
                        amygdala.logger.alert(ex.getMessage());
                    }
                }
            }

            // This method is only for testing for inconsistencies in the symbolic flow, should be removed later
            public void invalidate_interim(ArrayList<Pair<Integer, String>> old_results) {
                for (Pair<Integer, String> to_destroy: old_results) {
                    amygdala.tracer.remove_interim(to_destroy.getLeft());
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

            public void onInputValueBehaviorDefault(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                return;
            }

            public void onInputValueBehaviorIfNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 2;
                if (inputIndex == 0) { // Predicate
                    Boolean taken = (Boolean) inputValue;
                    amygdala.branching_event(node_hash, BranchingNodeAttribute.BRANCH, children.get(0).getLeft(), taken);
                }
            }

            public void onInputValueBehaviorWhileNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                if (inputIndex == 0) { // TODO Predicate
                    Boolean taken = (Boolean) inputValue;
                    amygdala.branching_event(node_hash, BranchingNodeAttribute.LOOP, children.get(0).getLeft(), taken);
                }
            }

            // Default Behavior is to just pass through any symbolic flow
            public void onReturnBehaviorDefault(VirtualFrame vFrame, Object result) {
                ArrayList<Pair<Integer, String>> child = getChildHashes();
                // Kann kein Verhalten aus mehrern Kindern ableiten
                if (child.size() == 1) {
                    amygdala.tracer.pass_through_interim(node_hash, child.get(0).getLeft());
                }
            }

            // ===== JavaScript Read/Write =====

            public void onReturnBehaviorJSReadCurrentFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
                String variable_name = my_sourcesection.getCharacters().toString();
                amygdala.tracer.read_frame_to_interim(variable_name, node_hash);
            }

            public void onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(VirtualFrame vFrame, Object result) {
                ArrayList<Pair<Integer, String>> child = getChildHashes();
                assert child.size() == 1;

                String source = my_sourcesection.getCharacters().toString(); // TODO Benennung
                String[] splitted = source.split("=");
                String target = splitted[0].trim();
                if (target.startsWith("var ")) {
                    target = target.substring(4);
                }
                target = target.replace(" ", "");

                amygdala.tracer.write_interim_to_frame(child.get(0).getLeft(), target);
            }

            // ===== JavaScript General Nodes =====

            public void onReturnBehaviorBinaryOperation(VirtualFrame vFrame, Object result, Operation op) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 2;
                amygdala.tracer.add_operation(node_hash, op, children.get(0).getLeft(), children.get(1).getLeft());
                invalidate_interim(children);
            }

            public void onReturnBehaviorUnaryOperation(VirtualFrame vFrame, Object result, Operation op) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 1;
                amygdala.tracer.add_operation(node_hash, op, children.get(0).getLeft());
                invalidate_interim(children);
            }

            public void onReturnBehaviorConstant(VirtualFrame vFrame, Object result, Type type) {
                amygdala.tracer.add_constant(node_hash, type, result);
            }
        };
    }
}