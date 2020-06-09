package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class Or extends SymbolicNode {
    public Or(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
        this.languageSemantic = s;
        addChildren(2, a, b);
        /*if (a.type == ExpressionType.BOOLEAN && b.type == ExpressionType.BOOLEAN) {
            this.type = ExpressionType.BOOLEAN;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "OR");
        }*/
    }

    @Override
    public String toHRStringJS() {
        return parentheses(this.children[0].toString() + " || " + this.children[1].toString());
    }

    @Override
    public String toSMTExprJS() throws SymbolicException.NotImplemented {
        return parentheses("or " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    /**
     * Logic or operator as in <a href="https://tc39.es/ecma262/2020/#sec-binary-logical-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
     *
     * @param ctx Z3-Context
     * @return Result of the '||' operation
     * @throws SymbolicException.NotImplemented
     */
    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        // TODO solve expr like this: (('in' || 8) + '_hello') === 'in_hello'
        Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
        Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);
        if (checkTypeAll(ExpressionType.BOOLEAN, a, b)) {
            return Pair.create(ctx.mkOr((BoolExpr) a.getLeft(), (BoolExpr) b.getLeft()), ExpressionType.BOOLEAN);
        } else {
            throw new SymbolicException.NotImplemented("Expression 'OR' can only handle boolean children for now.");
        }
    }
}
