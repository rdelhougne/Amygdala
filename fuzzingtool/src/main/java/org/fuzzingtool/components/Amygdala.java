package org.fuzzingtool.components;

import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.fuzzingtool.symbolic.logical.Not;

import java.util.ArrayList;

public class Amygdala {
    public Tracer tracer;

    // Speichert die Prädikate, die an den SMT-Solver weitergegeben werden.
    ArrayList<SymbolicNode> predicates = new ArrayList<>();

    public Amygdala() {
        this.tracer = new Tracer();
    }

    public void branching_event(Integer branching_node, Integer predicate_interim_key, Boolean taken) throws SymbolicException.IncompatibleType, SymbolicException.WrongParameterSize {
        if (taken) { // Branch taken
            predicates.add(tracer.get_interim(predicate_interim_key));
        } else {
            predicates.add(new Not(tracer.get_interim(predicate_interim_key)));
        }
    }

    public String lastRunToSMTExpr() {
        StringBuilder exp_str = new StringBuilder();
        exp_str.append("(declare-const n Int)\n");
        exp_str.append("(assert\n");
        exp_str.append("\t(and\n");
        for (SymbolicNode n: predicates) {
            exp_str.append("\t\t").append(n.toSMTExpr()).append("\n");
        }
        exp_str.append("\t)\n");
        exp_str.append(")");
        return exp_str.toString();
    }

    public String lastRunToHumanReadableExpr() {
        StringBuilder exp_str = new StringBuilder();
        for (SymbolicNode n: predicates) {
            exp_str.append(n.toString()).append("\n");
        }
        return exp_str.toString();
    }
}
