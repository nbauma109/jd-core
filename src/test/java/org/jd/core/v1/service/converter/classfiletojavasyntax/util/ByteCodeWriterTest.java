package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.Method;
import org.apache.commons.io.IOUtils;
import org.jd.core.v1.AbstractJdTest;
import org.jd.core.v1.cfg.MethodUtil;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ByteCodeWriterTest extends AbstractJdTest {

    @Test
    public void testWriteArray() throws Exception {
        testWrite("org/jd/core/v1/ArrayTypes", "arrays", "()V");
    }

    @Test
    public void testWrite() throws Exception {
        testWrite("jd/core/process/analyzer/classfile/reconstructor/PreIncReconstructor", "Reconstruct", "(Ljava/util/List;)V");
    }
    
    @Test
    public void testWriteGOTOW() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "methodWithLargeJump", "(I)V");
    }
    
    @Test
    public void testWriteWIDEI() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "largeVarIndexInt", "()I");
    }

    @Test
    public void testWriteWIDEF() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "largeVarIndexFloat", "()F");
    }
    
    @Test
    public void testWriteWIDED() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "largeVarIndexDouble", "()D");
    }
    
    @Test
    public void testWriteWIDEJ() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "largeVarIndex", "()J");
    }
    
    @Test
    public void testWriteWIDEL() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "largeVarIndexLong", "()Ljava/lang/Long;");
    }
    
    @Test
    public void testWriteWIDEIINC() throws Exception {
        testWrite("org/jd/core/v1/WideInstruction", "exerciseWideAndLocals", "()V");
    }

    @Test
    public void testGetLineNumberTableAsStatements() throws Exception {
        ClassPathLoader classPathLoader = new ClassPathLoader();
        TypeMaker typeMaker = new TypeMaker(classPathLoader);
        String internalTypeName = "org/jd/core/v1/ArrayTypes";
        String methodName = "arrays";
        String methodDescriptor = "()V";
        Method method = MethodUtil.searchMethod(classPathLoader, typeMaker, internalTypeName, methodName, methodDescriptor);
        List<Statement> statements = ByteCodeWriter.getLineNumberTableAsStatements(method);
        int lineNumber = 0;
        int count = 0;
        for (Statement statement : statements) {
            if (statement instanceof AssertStatement as && as.getCondition().getLineNumber() > lineNumber) {
                count++;
            }
        }
        assertEquals(23, count);
    }

    private void testWrite(String internalTypeName, String methodName, String methodDescriptor) throws IOException {
        ByteCodeWriter byteCodeWriter = new ByteCodeWriter();
        ClassPathLoader classPathLoader = new ClassPathLoader();
        TypeMaker typeMaker = new TypeMaker(classPathLoader);
        Method method = MethodUtil.searchMethod(classPathLoader, typeMaker, internalTypeName, methodName, methodDescriptor);
        String byteCode = byteCodeWriter.write("//", method);
        assertEqualsIgnoreEOL(IOUtils.toString(getClass().getResource("/txt/" + methodName + ".txt"), StandardCharsets.UTF_8), byteCode);
    }
    
}
