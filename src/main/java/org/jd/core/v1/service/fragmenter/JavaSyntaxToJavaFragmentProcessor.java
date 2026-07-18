/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.fragmenter;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.javafragment.ImportsFragment;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.token.ReferenceToken;
import org.jd.core.v1.model.token.TextToken;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.visitor.CompilationUnitVisitor;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.visitor.SearchImportsVisitor;

import static org.apache.bcel.Const.MAJOR_1_5;
import static org.jd.core.v1.api.printer.Printer.TYPE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_FLOAT;

/** Converts Java syntax to fragments while handling member types declared in anonymous classes. */
public class JavaSyntaxToJavaFragmentProcessor {

    public void process(CompilationUnit compilationUnit, DecompileContext decompileContext) {
        Loader loader = decompileContext.getLoader();
        String mainInternalTypeName = decompileContext.getMainInternalTypeName();
        int majorVersion = decompileContext.getMajorVersion();

        SearchImportsVisitor importsVisitor = new SearchImportsVisitor(loader, mainInternalTypeName);
        importsVisitor.visit(compilationUnit);
        ImportsFragment importsFragment = importsVisitor.getImportsFragment();
        decompileContext.setMaxLineNumber(importsVisitor.getMaxLineNumber());

        CompilationUnitVisitor visitor = new ParenthesizingCompilationUnitVisitor(
                loader, mainInternalTypeName, majorVersion, importsFragment);
        visitor.visit(compilationUnit);
        decompileContext.setBody(visitor.getFragments());
    }

    private static final class ParenthesizingCompilationUnitVisitor extends CompilationUnitVisitor {
        private final int majorVersion;

        private ParenthesizingCompilationUnitVisitor(Loader loader, String mainInternalTypeName,
                int majorVersion, ImportsFragment importsFragment) {
            super(loader, mainInternalTypeName, majorVersion, importsFragment);
            this.majorVersion = majorVersion;
        }

        @Override
        public void visit(BinaryOperatorExpression expression) {
            if (expression.getRightExpression() instanceof BinaryOperatorExpression right
                    && right.getPriority() == expression.getPriority()
                    && requiresRightParentheses(expression, right)) {
                // Equal-precedence operators are only left-associative in source. Preserve a
                // right-nested bytecode tree: flattening it can change floating-point rounding,
                // integer division, string concatenation, and subtraction semantics.
                visit(expression, expression.getLeftExpression());
                tokens.add(TextToken.SPACE);
                tokens.add(newTextToken(expression.getOperator()));
                tokens.add(TextToken.SPACE);
                visit(expression, expression.getRightExpression(), true);
            } else {
                super.visit(expression);
            }
        }

        private static boolean requiresRightParentheses(BinaryOperatorExpression expression,
                BinaryOperatorExpression right) {
            String operator = expression.getOperator();
            String rightOperator = right.getOperator();
            if ("-".equals(operator) || "/".equals(operator) || "%".equals(operator)) {
                return true;
            }
            if ("*".equals(operator) && !"*".equals(rightOperator)) {
                return true;
            }
            if ("+".equals(operator) && "-".equals(rightOperator)) {
                return true;
            }
            return ("+".equals(operator) || "*".equals(operator))
                    && (TYPE_FLOAT.equals(expression.getType()) || TYPE_DOUBLE.equals(expression.getType()));
        }

        @Override
        public void visit(InnerObjectType type) {
            ObjectType outerType = type.getOuterType();
            if (outerType != null && (outerType.getName() == null || outerType.getName().isEmpty())) {
                // An anonymous class has no source-level name. Its member types are directly in scope
                // inside its body and must not be prefixed with the anonymous class's empty name.
                tokens.add(new ReferenceToken(TYPE, type.getInternalName(), type.getName(), null, currentType));
                if (majorVersion >= MAJOR_1_5) {
                    BaseTypeArgument typeArguments = type.getTypeArguments();
                    if (typeArguments != null) {
                        visitTypeArgumentList(typeArguments);
                    }
                }
                visitDimension(type.getDimension());
            } else {
                super.visit(type);
            }
        }
    }
}
