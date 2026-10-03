package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.apache.bcel.classfile.ClassParser;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.UpdateJavaSyntaxTreeProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateJavaSyntaxTreeStep2Visitor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/** The signatures of the public API which existed before the loader was plumbed through are still there. */
public class UpdateJavaSyntaxTreeCompatibilityTest {
    private static CompilationUnit emptyClass() throws Exception {
        String internalName = "java/lang/Runnable";
        byte[] bytes = new ClassPathLoader().load(internalName);
        ClassFile classFile = new ClassFile(new ClassParser(new ByteArrayInputStream(bytes), internalName).parse());
        ClassFileBodyDeclaration body = new ClassFileBodyDeclaration(classFile, null, null, null);

        body.setFieldDeclarations(new ArrayList<>());
        body.setMethodDeclarations(new ArrayList<>());
        body.setInnerTypeDeclarations(new ArrayList<>());

        return new CompilationUnit(new ClassFileClassDeclaration(null, 0, "Runnable", internalName, null, null, null, body));
    }

    @Test
    public void testProcessWithoutLoader() throws Exception {
        new UpdateJavaSyntaxTreeProcessor().process(emptyClass(), new TypeMaker(new ClassPathLoader()));
    }

    @Test
    public void testStep2VisitorWithoutLoader() throws Exception {
        new UpdateJavaSyntaxTreeStep2Visitor(new TypeMaker(new ClassPathLoader())).visit(emptyClass());
    }
}
