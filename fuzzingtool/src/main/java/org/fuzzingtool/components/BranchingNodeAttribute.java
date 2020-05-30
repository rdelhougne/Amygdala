package org.fuzzingtool.components;

public enum BranchingNodeAttribute {
    BRANCH, // This node is a simple branch (e.g. if)
    LOOP, // This node is a loop predicate, important for loop unrolling limits
    // EXPLORED, // All possible subsequent paths were already explored
    // The following Nodes can't have any childs
    UNKNOWN, // The following path was never taken
    UNREACHABLE, // The SMT-Solver was unable to solve the expression for this path
    TERMINATE, // The program terminates in this node
    ERROR // This path causes the program to crash
}
