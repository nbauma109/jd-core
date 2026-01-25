package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.AbstractJdTest;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.cfg.MethodUtil;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ByteCodeWriterTest extends AbstractJdTest {

    @Test
    public void testWriteArray() throws Exception {
        testWrite("/jar/array-types-jdk17.0.11.jar", "org/jd/core/v1/ArrayTypes", "arrays", "()V");
    }
    
    @Test
    public void testWriteGOTOW() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "methodWithLargeJump", "(I)V");
    }
    
    @Test
    public void testWriteWIDEI() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "largeVarIndexInt", "()I");
    }

    @Test
    public void testWriteWIDEF() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "largeVarIndexFloat", "()F");
    }
    
    @Test
    public void testWriteWIDED() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "largeVarIndexDouble", "()D");
    }
    
    @Test
    public void testWriteWIDEJ() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "largeVarIndex", "()J");
    }
    
    @Test
    public void testWriteWIDEL() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "largeVarIndexLong", "()Ljava/lang/Long;");
    }
    
    @Test
    public void testWriteWIDEIINC() throws Exception {
        testWrite("/jar/wide-instructions-jdk17.0.11.jar", "org/jd/core/v1/WideInstruction", "exerciseWideAndLocals", "()V");
    }

    @Test
    public void testGetLineNumberTableAsStatements() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/array-types-jdk17.0.11.jar")) {
            Loader loader = new ZipLoader(is);
            TypeMaker typeMaker = new TypeMaker(loader);
            String internalTypeName = "org/jd/core/v1/ArrayTypes";
            String methodName = "arrays";
            String methodDescriptor = "()V";
            Method method = MethodUtil.searchMethod(loader, typeMaker, internalTypeName, methodName, methodDescriptor);
            List<Statement> statements = ByteCodeWriter.getLineNumberTableAsStatements(method);
            StringBuilder sb = new StringBuilder();
            for (Statement statement : statements) {
                sb.append(statement);
                sb.append('\n');
            }
            assertEqualsIgnoreEOL(getResourceAsString("/txt/" + methodName + "_ln.txt"), sb.toString());
        }
    }

    private void testWrite(String archiveName, String internalTypeName, String methodName, String methodDescriptor) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(archiveName)) {
            Loader loader = new ZipLoader(is);
            ByteCodeWriter byteCodeWriter = new ByteCodeWriter();
            TypeMaker typeMaker = new TypeMaker(loader);
            Method method = MethodUtil.searchMethod(loader, typeMaker, internalTypeName, methodName, methodDescriptor);
            String byteCode = byteCodeWriter.write("//", method);
            assertEqualsIgnoreEOL(getResourceAsString("/txt/" + methodName + ".txt"), byteCode);
        }
    }
    
}
