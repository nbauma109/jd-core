package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileLocalVariableDeclarator;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;

/**
 * Completes the bytecode cleanup when the enclosing-instance access could not
 * be copied into the available space. Only a stable qualified this is copied.
 */
public final class OuterReceiverCleanupVisitor extends AbstractJavaSyntaxVisitor {
    @Override
    public void visit(MethodDeclaration declaration) {
        if (declaration.getStatements() instanceof Statements statements) {
            cleanUp(statements);
        }
        super.visit(declaration);
    }

    @Override
    public void visit(NewExpression expression) {
        super.visit(expression);
        safeAccept(expression.getBodyDeclaration());
    }

    private static void cleanUp(Statements statements) {
        // Restrict the rewrite to a body with no other use of the local.
        if (statements.size() != 2
                || !(statements.get(0) instanceof LocalVariableDeclarationStatement declaration)
                || !(declaration.getLocalVariableDeclarators() instanceof ClassFileLocalVariableDeclarator declarator)
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || !isQualifiedThis(initializer.getExpression())
                || !declaration.getType().equals(initializer.getExpression().getType())
                || !(statements.get(1) instanceof ReturnExpressionStatement returned)
                || !(returned.getExpression() instanceof MethodInvocationExpression call)
                || !call.getExpression().isObjectTypeReferenceExpression()) {
            return;
        }

        BaseExpression arguments = call.getParameters();
        if (arguments == null || !arguments.isList() || arguments.size() != 2
                || !(arguments.getList().get(0) instanceof ClassFileLocalVariableReferenceExpression first)
                || !(arguments.getList().get(1) instanceof MethodInvocationExpression secondCall)
                || secondCall.getParameterTypeCount() != 0
                || !(secondCall.getExpression() instanceof ClassFileLocalVariableReferenceExpression second)
                || first.getLocalVariable() != declarator.getLocalVariable()
                || second.getLocalVariable() != declarator.getLocalVariable()) {
            return;
        }

        Expression receiver = initializer.getExpression();
        arguments.getList().set(0, receiver.copyTo(first.getLineNumber()));
        secondCall.setExpression(receiver.copyTo(second.getLineNumber()));
        statements.remove(0);
    }

    private static boolean isQualifiedThis(Expression expression) {
        return expression instanceof FieldReferenceExpression reference
                && "this".equals(reference.getName())
                && reference.getExpression() != null
                && reference.getExpression().isObjectTypeReferenceExpression();
    }
}
