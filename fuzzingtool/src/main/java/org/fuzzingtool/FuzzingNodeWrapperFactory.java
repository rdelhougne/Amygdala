package org.fuzzingtool;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import org.fuzzingtool.components.Amygdala;
import org.fuzzingtool.components.BranchingNodeAttribute;
import org.fuzzingtool.components.VariableIdentifier;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.Operation;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.visualization.ASTVisualizer;
import org.graalvm.collections.Pair;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FuzzingNodeWrapperFactory implements ExecutionEventNodeFactory {
    private final TruffleInstrument.Env env;
    private Amygdala amygdala;
    private int visualized_counter = 0;

    private final Pattern assignment_pattern = Pattern.compile("(var\\s+|let\\s+|const\\s+)?\\s*([A-Za-z]\\w*)\\s*=.*"); //Capturing group 2 is variable name

    FuzzingNodeWrapperFactory(final TruffleInstrument.Env env, Amygdala amy) {
        this.env = env;
        this.amygdala = amy;
    }

    public ExecutionEventNode create(final EventContext ec) {
        if (ec.getInstrumentedNode().getClass().getSimpleName().equals("MaterializedFunctionBodyNode")) {
            ASTVisualizer av = new ASTVisualizer(ec.getInstrumentedNode(), amygdala.logger);
            av.save_image(Paths.get(".").toAbsolutePath().normalize().toString() + "/function_visualization_" + visualized_counter);
            visualized_counter += 1;
        }

        final boolean create_nodeIsInputNode;
        final VariableIdentifier create_inputVariableIdentifier;
        if (ec.getInstrumentedNode() instanceof JSConstantNode) {
            Pair<Boolean, VariableIdentifier> create_inputNodeConfiguration = amygdala.getInputNodeConfiguration(ec.getInstrumentedSourceSection().getStartLine());
            create_nodeIsInputNode = create_inputNodeConfiguration.getLeft();
            create_inputVariableIdentifier = create_inputNodeConfiguration.getRight();
        } else {
            create_nodeIsInputNode = false;
            create_inputVariableIdentifier = null;
        }

        final boolean create_nodeIsMainLoopInputNode;
        if (ec.getInstrumentedSourceSection() != null && ec.getInstrumentedSourceSection().isAvailable()) {
            String source_code_line = ec.getInstrumentedSourceSection().getSource().getCharacters(amygdala.main_loop_line_num).toString();
            create_nodeIsMainLoopInputNode = ec.getInstrumentedNode() instanceof JSConstantNode && ec.getInstrumentedSourceSection().getStartLine() == amygdala.main_loop_line_num && source_code_line.contains(amygdala.main_loop_identifier_string);
        } else {
            create_nodeIsMainLoopInputNode = false;
        }

        return new ExecutionEventNode() {
            private final EventContext event_context = ec;
            private final SourceSection my_sourcesection = ec.getInstrumentedSourceSection();
            private Node my_node = ec.getInstrumentedNode();
            private String node_type = my_node.getClass().getSimpleName();
            private int node_hash = my_node.hashCode();

            //Input node config
            private boolean isInputNode = create_nodeIsInputNode;
            private VariableIdentifier inputVariableIdentifier = create_inputVariableIdentifier;
            private boolean isMainLoopInputNode = create_nodeIsMainLoopInputNode;

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

                switch (node_type) {
                    case "Call1Node":
                        onEnterBehaviorCallNode(vFrame);
                        break;
                    default:
                        onEnterBehaviorDefault(vFrame);
                }
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

                switch (node_type) {
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

            @Override
            public void onReturnValue(VirtualFrame vFrame, Object result) {
                amygdala.logger.log(getSignature() + " \033[31m↵\033[0m");

                try {
                    switch (node_type) { // TODO big time
                        // ===== JavaScript Read/Write =====
                        case "JSReadCurrentFrameSlotNodeGen":
                            onReturnBehaviorJSReadCurrentFrameSlotNodeGen(vFrame, result);
                            break;
                        case "JSWriteCurrentFrameSlotNodeGen":
                            onReturnBehaviorJSWriteCurrentFrameSlotNodeGen(vFrame, result);
                            break;
                        case "WritePropertyNode":
                            onReturnBehaviorWritePropertyNode(vFrame, result);
                            break;
                        case "GlobalObjectNode":
                            onReturnBehaviorGlobalObjectNode(vFrame, result);
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
                        case "JSDivideNodeGen": // TODO richtiger Name?
                            onReturnBehaviorBinaryOperation(vFrame, result, Operation.DIVISION);
                            break;
                        case "JSUnaryMinusNodeGen":
                            onReturnBehaviorUnaryOperation(vFrame, result, Operation.UNARY_MINUS);
                            break;
                        case "JSUnaryPlusNodeGen": // TODO richtiger Name?
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

            public void onEnterBehaviorDefault(VirtualFrame vFrame) {
                return;
            }

            public void onEnterBehaviorCallNode(VirtualFrame vFrame) {
                if (my_sourcesection.getStartLine() == amygdala.error_line_num && my_sourcesection.getSource().getCharacters(amygdala.error_line_num).toString().contains(amygdala.error_identifier_string)) {
                    amygdala.error_event();
                }
            }

            public void onInputValueBehaviorDefault(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                return;
            }

            public void onInputValueBehaviorIfNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 2;
                if (inputIndex == 0) { // Predicate
                    Boolean taken = (Boolean) inputValue;
                    amygdala.branching_event(node_hash, BranchingNodeAttribute.BRANCH, children.get(0).getLeft(), taken, extractPredicate());
                }
            }

            public void onInputValueBehaviorWhileNode(VirtualFrame vFrame, EventContext inputContext, int inputIndex, Object inputValue) {
                if (!(my_sourcesection.getStartLine() == amygdala.main_loop_line_num && my_sourcesection.getSource().getCharacters(amygdala.main_loop_line_num).toString().contains(amygdala.main_loop_identifier_string))) {
                    ArrayList<Pair<Integer, String>> children = getChildHashes();
                    if (inputIndex == 0) { // TODO Predicate
                        amygdala.branching_event(node_hash, BranchingNodeAttribute.LOOP, children.get(0).getLeft(), (Boolean) inputValue, extractPredicate());
                    }
                }
            }

            // TODO extremely costly...
            private String extractPredicate() {
                if (my_sourcesection != null && my_sourcesection.isAvailable()) {
                    String extracted_predicate = my_sourcesection.getCharacters().toString().replaceAll("\\s+", " ");
                    extracted_predicate = extracted_predicate.substring(extracted_predicate.indexOf("(") + 1, extracted_predicate.length());
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

                String var_name = get_variable_name();
                if (var_name != null) {
                    amygdala.tracer.write_interim_to_frame(child.get(0).getLeft(), var_name);
                }
            }

            public void onReturnBehaviorGlobalObjectNode(VirtualFrame vFrame, Object result) {
                String variable_name = my_sourcesection.getCharacters().toString();
                amygdala.tracer.read_frame_to_interim(variable_name, node_hash);
            }

            public void onReturnBehaviorWritePropertyNode(VirtualFrame vFrame, Object result) {
                ArrayList<Pair<Integer, String>> child = getChildHashes();
                assert child.size() >= 2;

                String var_name = get_variable_name();
                if (var_name != null) {
                    amygdala.tracer.write_interim_to_frame(child.get(1).getLeft(), var_name);
                }
            }

            private String get_variable_name() {
                String source = my_sourcesection.getCharacters().toString().replace("\n", "");
                Matcher matcher = assignment_pattern.matcher(source);
                if (matcher.matches()) {
                    return matcher.group(2);
                } else {
                    return null;
                }
            }

            // ===== JavaScript General Nodes =====

            public void onReturnBehaviorBinaryOperation(VirtualFrame vFrame, Object result, Operation op) throws SymbolicException.WrongParameterSize {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 2;
                amygdala.tracer.add_operation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft(), children.get(1).getLeft());
                invalidate_interim(children);
            }

            public void onReturnBehaviorUnaryOperation(VirtualFrame vFrame, Object result, Operation op) throws SymbolicException.WrongParameterSize {
                ArrayList<Pair<Integer, String>> children = getChildHashes();
                assert children.size() == 1;
                amygdala.tracer.add_operation(node_hash, LanguageSemantic.JAVASCRIPT, op, children.get(0).getLeft());
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
                } else if (this.isInputNode) {
                    Object next_input = amygdala.getNextInputValue(this.inputVariableIdentifier);
                    amygdala.logger.log("Next input value for variable " + this.inputVariableIdentifier.getIdentifierString() + ": " + next_input);
                    amygdala.tracer.add_variable(node_hash, LanguageSemantic.JAVASCRIPT, this.inputVariableIdentifier, amygdala.getVariableType(this.inputVariableIdentifier));
                    throw this.event_context.createUnwind(next_input);
                } else {
                    amygdala.tracer.add_constant(node_hash, LanguageSemantic.JAVASCRIPT, type, result);
                }
            }
        };
    }
}