package org.fuzzingtool.symbolic.logical;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.fuzzingtool.symbolic.ExpressionType;
import org.fuzzingtool.symbolic.LanguageSemantic;
import org.fuzzingtool.symbolic.SymbolicException;
import org.fuzzingtool.symbolic.SymbolicNode;
import org.graalvm.collections.Pair;

public class GreaterThan extends SymbolicNode {
    public GreaterThan(LanguageSemantic s, SymbolicNode a, SymbolicNode b) throws SymbolicException.WrongParameterSize {
        this.languageSemantic = s;
        addChildren(2, a, b);
        /*if (a.type == b.type && (a.type == ExpressionType.INT || a.type == ExpressionType.REAL)) {
            this.type = ExpressionType.BOOLEAN;
            addChildren(2, a, b);
        } else {
            throw new SymbolicException.IncompatibleType(a.type, "GT");
        }*/
    }

    @Override
    public String toHRStringJS() {
        return parentheses(this.children[0].toString() + " > " + this.children[1].toString());
    }

    @Override
    public String toSMTExprJS() throws SymbolicException.NotImplemented {
        return parentheses("> " + this.children[0].toSMTExpr() + " " + this.children[1].toSMTExpr());
    }

    /**
     * Greater-than operator as in <a href="https://tc39.es/ecma262/2020/#sec-relational-operators-runtime-semantics-evaluation">ECMAScript® 2020 Language Specification</a>
     *
     * @param ctx Z3-Context
     * @return Result of the '>' operation, always BOOLEAN
     * @throws SymbolicException.NotImplemented
     */
    @Override
    public Pair<Expr, ExpressionType> toZ3ExprJS(Context ctx) throws SymbolicException.NotImplemented, SymbolicException.UndecidableExpression {
        Pair<Expr, ExpressionType> a = this.children[0].toZ3Expr(ctx);
        Pair<Expr, ExpressionType> b = this.children[1].toZ3Expr(ctx);

        Pair<Expr, ExpressionType> result = LessThan.abstractRelationalComparisonZ3JS(ctx, b, a);
        if (result.getRight() == ExpressionType.UNDEFINED) {
            return Pair.create(ctx.mkFalse(), ExpressionType.BOOLEAN);
        } else {
            return result;
        }
    }
}
