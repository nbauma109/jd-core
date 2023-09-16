package org.jd.core.v1.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.UnusedCodeFixCore.RemoveAllCastOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;

import java.net.URI;
import java.util.LinkedHashSet;

public class RemoveUnnecessaryCastsCore {

    public static String process(String source, String unitName, URI jarURI) {
        Document document = new Document(source);
        ASTParser astParser = ASTParserFactory.getInstanceWithBindings().newASTParser(source.toCharArray(), unitName, jarURI);
        CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
        LinkedHashSet<CastExpression> unnecessaryCasts = new LinkedHashSet<>();
        IProblem[] problems = cu.getProblems();
        for (IProblem pb : problems) {
            if (pb.getID() == IProblem.UnnecessaryCast) {
                ProblemLocationCore problemLocationCore = new ProblemLocationCore(pb);
                ASTNode selectedNode = problemLocationCore.getCoveringNode(cu);
                ASTNode curr = ASTNodes.getUnparenthesedExpression(selectedNode);
                if (curr instanceof CastExpression) {
                    unnecessaryCasts.add((CastExpression) curr);
                }
            }
        }
        CompilationUnitRewrite compilationUnitRewrite = new CompilationUnitRewrite(null, cu);
        RemoveAllCastOperation removeAllCastOperation = new RemoveAllCastOperation(unnecessaryCasts);
        try {
            removeAllCastOperation.rewriteAST(compilationUnitRewrite, null);
            ASTRewrite astRewrite = compilationUnitRewrite.getASTRewrite();
            TextEdit textEdit = astRewrite.rewriteAST(document, null);
            textEdit.apply(document);
        } catch (MalformedTreeException | BadLocationException | CoreException e) {
            assert ExceptionUtil.printStackTrace(e);
        }
        return document.get();
    }
}
