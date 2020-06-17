package org.fuzzingtool.components;

import org.fuzzingtool.Logger;
import org.fuzzingtool.symbolic.*;
import org.fuzzingtool.symbolic.arithmetic.*;
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
            assert interim_results.get(node_id) != null;
            return interim_results.get(node_id);
        } else {
            logger.warning("Tracer::get_interim(): Cannot get interim results for hash " + node_id);
            return null;
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
            //logger.debug("pass_through_interim(): No interim result for key " + old_interim_result);
        }
    }

    /**
     * This function reads a variable from the symbolic frame an transfers the variable to an intermediate result.
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
            logger.critical("Tracer::read_frame_to_interim(): Symbolic frame does not contain key '" + variable_name + "'");
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
            logger.critical("Tracer::write_interim_to_frame(): No interim result for key " + from_node);
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
     * @throws SymbolicException.WrongParameterSize
     */
    public void add_operation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source_a, Integer node_source_b) throws SymbolicException.WrongParameterSize {
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
                case STRICT_EQUAL:
                    interim_results.put(node_target, new StrictEqual(s, a, b));
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
                    logger.critical("Tracer::add_operation(): Unknown operation " + op.toString());
            }
        } else {
            if (!interim_results.containsKey(node_source_a) && !interim_results.containsKey(node_source_b)) {
                logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() + " but interim result from " + node_source_a + " and " + node_source_b + " does not exist");
            } else if (!interim_results.containsKey(node_source_a)) {
                logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() + " but interim result from " + node_source_a + " does not exist");
            } else {
                logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() + " but interim result from " + node_source_b + " does not exist");
            }
        }
    }

    /**
     * This is an overloaded method of {@link #add_operation(Integer, LanguageSemantic, Operation, Integer, Integer)} for unary operations.
     *
     * @param node_target Node-hash of the new interim result
     * @param op ExpressionType of Operation, see {@link Operation} for all available operation types
     * @param node_source child operator
     * @throws SymbolicException.WrongParameterSize
     */
    public void add_operation(Integer node_target, LanguageSemantic s, Operation op, Integer node_source) throws SymbolicException.WrongParameterSize {
        if (interim_results.containsKey(node_source)) {
            SymbolicNode k = interim_results.get(node_source);
            switch (op) {
                case NOT:
                    interim_results.put(node_target, new Not(s, k));
                    break;
                case UNARY_MINUS:
                    interim_results.put(node_target, new UnaryMinus(s, k));
                    break;
                case UNARY_PLUS:
                    interim_results.put(node_target, new UnaryPlus(s, k));
                    break;
                default:
                    logger.critical("Tracer::add_operation(): Unknown operation " + op.toString());
            }
        } else {
            logger.critical("Tracer::add_operation(): Trying to add operation " + op.toString() + " but interim result from " + node_source + " does not exist");
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

    /**
     * Add a new symbolic variable to the interim results of node_target.
     *
     * @param node_target The node-hash of the interim result
     * @param s Semantic of the language
     * @param id VariableIdentifier of the new variable, see {@link VariableIdentifier}
     * @param t ExpressionType of the new variable, see {@link ExpressionType}
     */
    public void add_variable(Integer node_target, LanguageSemantic s, VariableIdentifier id, ExpressionType t) {
        interim_results.put(node_target, new SymbolicVariable(s, id, t));
    }

    public void initialize_program_context(LanguageSemantic sem) {
        if (sem == LanguageSemantic.JAVASCRIPT) {
            // Initialize JavaScript global objects
            try {
                // TODO biggest todo ever
                // output from jsglobalsearch.js
                symbolic_frame.put("Object", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Function", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("String", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Date", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Number", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Boolean", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("RegExp", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Math", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("JSON", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("NaN", new SymbolicConstant(sem, ExpressionType.NUMBER_NAN, null));
                symbolic_frame.put("Infinity", new SymbolicConstant(sem, ExpressionType.NUMBER_POS_INFINITY, null));
                symbolic_frame.put("undefined", new SymbolicConstant(sem, ExpressionType.UNDEFINED, null));
                symbolic_frame.put("isNaN", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("isFinite", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("parseFloat", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("parseInt", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("encodeURI", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("encodeURIComponent", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("decodeURI", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("decodeURIComponent", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("eval", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("escape", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("unescape", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Error", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("EvalError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("RangeError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("ReferenceError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("SyntaxError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("TypeError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("URIError", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("ArrayBuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Int8Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Uint8Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Uint8ClampedArray", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Int16Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Uint16Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Int32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Uint32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Float32Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Float64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("BigInt64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("BigUint64Array", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("DataView", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("BigInt", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Polyglot", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Map", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Set", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("WeakMap", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("WeakSet", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Symbol", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Reflect", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Proxy", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Promise", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("SharedArrayBuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Atomics", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("globalThis", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Graal", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("quit", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("readline", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("read", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("readbuffer", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("load", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("loadWithNewGlobal", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("console", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("print", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("printErr", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("performance", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Java", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("Packages", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("java", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("javafx", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("javax", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("com", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("org", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("edu", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
                symbolic_frame.put("arguments", new SymbolicConstant(sem, ExpressionType.OBJECT, null));
            } catch (SymbolicException.IncompatibleType incompatibleType) {
                logger.critical("Tracer::initialize_program_context(): Cannot initialize JavaScript program context");
            }
        } else {
            logger.warning("Tracer::initialize_program_context(): Cannot initialize context with semantic '" + sem.toString() + "'");
        }
    }
}
