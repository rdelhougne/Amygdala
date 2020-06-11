package org.fuzzingtool.components;

import org.fuzzingtool.Logger;
import org.fuzzingtool.symbolic.*;
import org.fuzzingtool.symbolic.arithmetic.Addition;
import org.fuzzingtool.symbolic.arithmetic.Division;
import org.fuzzingtool.symbolic.arithmetic.Multiplication;
import org.fuzzingtool.symbolic.arithmetic.Subtraction;
import org.fuzzingtool.symbolic.basic.*;
import org.fuzzingtool.symbolic.logical.*;

import java.util.HashMap;

public class Tracer {
    public Logger logger;
    // Speichert Zwischenergebnisse der Nodes um sie nach oben weiterzugeben
    private HashMap<Integer, SymbolicNode> interim_results = new HashMap<>();

    // Speichert die aktuelle Variablenbelegung der symbolischen Variablen.
    // Zugriff nur durch JSReadCurrentFrameSlotNodeGen & JSWriteCurrentFrameSlotNodeGen!
    private HashMap<String, SymbolicNode> symbolic_frame = new HashMap<>();

    public Tracer(Logger l) {
        this.logger = l;
    }

    /**
     * Removes the interim result of the specified node.
     *
     * @param node_id The hash-code of the node
     */
    public void remove_interim(Integer node_id) {
        interim_results.remove(node_id);
    }

    /**
     * Returns an intermediate results for a given node-hash.
     *
     * @param node_id The hash-code of the node
     */
    public SymbolicNode get_interim(Integer node_id) {
        if (interim_results.containsKey(node_id)) {
            return interim_results.get(node_id);
        } else {
            return null;
            // TODO Exception
        }
    }

    /**
     * Pass through an intermediate result from old node to new node.
     * The old result is not deleted.
     *
     * @param new_node_id The hash-code of the new node
     * @param old_interim_result The hash-code of the old node
     */
    public void pass_through_interim(Integer new_node_id, Integer old_interim_result) {
        if (interim_results.containsKey(old_interim_result)) {
            interim_results.put(new_node_id, interim_results.get(old_interim_result));
        } else {
            //alert("onReturnBehaviorDefault()"); // TODO Exception
        }
    }

    /**
     * This function reads a variable from the symbolic frame an transfers the variable to an intermidiate result.
     * This should only be used by read-operational nodes (e.g. JSReadCurrentFrameSlotNode)!
     *
     * @param variable_name Name of the symbolic variable in the current symbolic frame
     * @param to_node node-hash for the interim result.
     */
    public void read_frame_to_interim(String variable_name, Integer to_node) {
        if (symbolic_frame.containsKey(variable_name)) {
            interim_results.put(to_node, symbolic_frame.get(variable_name));
        } else {
            // TODO Typbestimmung oder Exception! Es wird versucht eine Variable zu lesen die nicht existiert! Oder nachschauen ob sie existiert...
            interim_results.put(to_node, new SymbolicVariable(LanguageSemantic.JAVASCRIPT, new VariableIdentifier(variable_name), ExpressionType.NUMBER_INTEGER));
        }
    }

    /**
     * Write the result of an interim result to a slot in the symbolic frame.
     * This Method should only be used by write-operational nodes (e.g. JSWriteCurrentFrameSlotNode)!
     *
     * @param from_node node-hash of the interim result
     * @param variable_name slot name of the symbolic frame
     */
    public void write_interim_to_frame(Integer from_node, String variable_name) {
        if (interim_results.containsKey(from_node)) {
            symbolic_frame.put(variable_name, interim_results.get(from_node));
        } else {
            //alert("onReturnBehaviorJSWriteCurrentFrameSlotNodeGen()"); // TODO Exception
        }
    }

    /**
     * Deletes all intermediate results from interim_results and the symbolic frame.
     * This should be used by terminate(), e.g. when the program restarts.
     */
    public void clearAll() {
        interim_results.clear();
        symbolic_frame.clear();
    }

    /**
     * This function adds a new symbolic operation to the interim results of the given node-hash.
     *
     * @param node_target Node-hash of the new interim result
     * @param op ExpressionType of Operation, see {@link Operation} for all available operation types
     * @param node_source_a Left-hand operator
     * @param node_source_b Right-hand operator
     * @throws SymbolicException.IncompatibleType
     * @throws SymbolicException.WrongParameterSize
     */
    public void add_operation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source_a, Integer node_source_b) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (interim_results.containsKey(node_source_a) && interim_results.containsKey(node_source_b)) {
            SymbolicNode a = interim_results.get(node_source_a);
            SymbolicNode b = interim_results.get(node_source_b);
            switch (op) {
                case ADDITION:
                    interim_results.put(node_target, new Addition(s, a, b));
                    break;
                case SUBTRACTION:
                    interim_results.put(node_target, new Subtraction(s, a, b));
                    break;
                case MULTIPLICATION:
                    interim_results.put(node_target, new Multiplication(s, a, b));
                    break;
                case DIVISION:
                    interim_results.put(node_target, new Division(s, a, b));
                    break;
                case AND:
                    interim_results.put(node_target, new And(s, a, b));
                    break;
                case OR:
                    interim_results.put(node_target, new Or(s, a, b));
                    break;
                case EQUAL:
                    interim_results.put(node_target, new Equal(s, a, b));
                    break;
                case GREATER_EQUAL:
                    interim_results.put(node_target, new GreaterEqual(s, a, b));
                    break;
                case GREATER_THAN:
                    interim_results.put(node_target, new GreaterThan(s, a, b));
                    break;
                case LESS_EQUAL:
                    interim_results.put(node_target, new LessEqual(s, a, b));
                    break;
                case LESS_THAN:
                    interim_results.put(node_target, new LessThan(s, a, b));
                    break;
                default:
                    // TODO exception
            }
        } else {
            // TODO Exception
        }
    }

    /**
     * This is an overloaded method of {@link #add_operation(Integer, LanguageSemantic, Operation, Integer, Integer)} for unary operations.
     *
     * @param node_target Node-hash of the new interim result
     * @param op ExpressionType of Operation, see {@link Operation} for all available operation types
     * @param node_source child operator
     * @throws SymbolicException.IncompatibleType
     * @throws SymbolicException.WrongParameterSize
     */
    public void add_operation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (interim_results.containsKey(node_source)) {
            SymbolicNode k = interim_results.get(node_source);
            switch (op) {
                case NOT:
                    interim_results.put(node_target, new Not(s, k));
                    break;
                default:
                    // TODO Exception
            }
        } else {
            // TODO Exception
        }
    }

    /**
     * Add a new symbolic constant to the interim results of node_target.
     *
     * @param node_target The node-hash of the interim result
     * @param s Semantic of the language
     * @param t ExpressionType of the new constant value, see {@link ExpressionType}
     * @param v Value of the constant, the value is automatically casted
     */
    public void add_constant(Integer node_target, LanguageSemantic s, ExpressionType t, Object v) {
        try {
            interim_results.put(node_target, new SymbolicConstant(s, t, v));
        } catch (SymbolicException.IncompatibleType incompatibleType) {
            logger.critical(incompatibleType.getMessage());
        }
    }
}
