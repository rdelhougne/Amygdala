package org.fuzzingtool.symbolic.arithmetic;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

/**
 * Class for abstract '/' operation
 */
public class Division extends SymbolicNode {
    public Division(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
        this.languageSemantic = s;
        addChildren(2, a, b);
        /*if (a.type == b.type && (a.type == ExpressionType.INT || a.type == ExpressionType.REAL)) { //TODO
            this.type = a.type;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, b.type, "DIV");
        }*/
    }

    @Override
    public String toHRStringJS() {
        return parentheses(this.children[0].toString() + " / " + this.children[1].toString());
    }

    @Override
    public String toSMTExprJS() throws SymbolicException.NotImplemented {
        return parentheses("/ " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    /**
     * Division operator as in <a href="https://tc39.es/ecma262/2020/#sec-multiplicative-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
     *
     * @param ctx Z3-Context
     * @return Result of the '/' operation
     * @throws SymbolicException.NotImplemented
     */
    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        Pair<Expr, ExpressionType> a_numeric = toNumericZ3JS(ctx, this.children[0].toZ3Expr(ctx));
        Pair<Expr, ExpressionType> b_numeric = toNumericZ3JS(ctx, this.children[1].toZ3Expr(ctx));

        // https://tc39.es/ecma262/2020/#sec-numeric-types-number-divide
        if (checkTypeContains(ExpressionType.NUMBER_NAN, a_numeric, b_numeric)) {
            return Pair.create(null, ExpressionType.NUMBER_NAN);
        }
        if (checkTypeAll(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric)) {
            return Pair.create(null, ExpressionType.NUMBER_NAN);
        }
        if (checkTypeAll(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
            return Pair.create(null, ExpressionType.NUMBER_NAN);
        }
        if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) && checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
            return Pair.create(null, ExpressionType.NUMBER_NAN);
        }
        if (checkTypeContains(ExpressionType.NUMBER_POS_INFINITY, a_numeric, b_numeric) || checkTypeContains(ExpressionType.NUMBER_NEG_INFINITY, a_numeric, b_numeric)) {
            //TODO evtl anderen Ausdruck nach const auflösen
            throw new SymbolicException.UndecidableExpression("Z3", "Cannot solve division with infinity and non-infinity parameters.");
        }
        //TODO zero rules
        if (checkTypeAll(ExpressionType.BIGINT, a_numeric, b_numeric)) {
            //TODO
            throw new SymbolicException.NotImplemented("Division for type BigInt not implemented");
        }
        // Division for "Number" type is always Real
        return Pair.create(ctx.mkDiv((ArithExpr) a_numeric.getLeft(), (ArithExpr) b_numeric.getLeft()), ExpressionType.NUMBER_REAL);
    }
}
