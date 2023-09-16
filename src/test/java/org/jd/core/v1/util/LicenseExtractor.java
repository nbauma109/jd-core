package org.jd.core.v1.util;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LicenseExtractor {

    public static String extractLicense(Path filePath) throws IOException {
        String source = new String(Files.readAllBytes(filePath));
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        String license = "";

        @SuppressWarnings("unchecked")
        List<Comment> commentList = cu.getCommentList();
        for (Comment comment : commentList) {
            if (comment.isBlockComment()) {
                int start = comment.getStartPosition();
                int end = start + comment.getLength();
                license = source.substring(start, end);
                break;
            }
        }

        return license;
    }
}
