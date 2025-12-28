package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

public class RecordTest extends AbstractJdTest {
    @Test
    public void testRecordWithEmptyBody() throws Exception {
        String internalClassName = RecordWithEmptyBody.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /*   1:   0 */ package org.jd.core.v1;
                /*   2:   0 */ 
                /*   3:   0 */ public record RecordWithEmptyBody(String a, double d) {}
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRecordWithGetters() throws Exception {
        String internalClassName = RecordWithGetters.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /*  1:  0 */ package org.jd.core.v1;
                /*  2:  0 */ 
                /*  3:  0 */ public record RecordWithGetters(String a, double d) {
                /*  4:  0 */   public String getA() {
                /*  5:  6 */     return this.a;
                /*  6:  0 */   }
                /*  7:  0 */   
                /*  8:  0 */   public double getD() {
                /*  9: 10 */     return this.d;
                /* 10:  0 */   }
                /* 11:  0 */ }
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
    @Test
    public void testRecordWithGetters2() throws Exception {
        String internalClassName = RecordWithGetters2.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /*   1:   0 */ package org.jd.core.v1;
                /*   2:   0 */ 
                /*   3:   0 */ public record RecordWithGetters2(String a, double d) {}
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
    @Test
    public void testRecordWithOverrides() throws Exception {
        String internalClassName = RecordWithOverrides.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /*  1:  0 */ package org.jd.core.v1;
                /*  2:  0 */ 
                /*  3:  0 */ public record RecordWithOverrides(String a, double d) {
                /*  4:  0 */   public final int hashCode() {
                /*  5:  6 */     throw new UnsupportedOperationException();
                /*  6:  0 */   }
                /*  7:  0 */   
                /*  8:  0 */   public final boolean equals(Object o) {
                /*  9: 11 */     throw new UnsupportedOperationException();
                /* 10:  0 */   }
                /* 11:  0 */   
                /* 12:  0 */   public final String toString() {
                /* 13: 16 */     throw new UnsupportedOperationException();
                /* 14:  0 */   }
                /* 15:  0 */ }
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
    @Test
    public void testRecordWithConstructor() throws Exception {
        String internalClassName = RecordWithConstructor.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /* 1: 0 */ package org.jd.core.v1;
                /* 2: 0 */ 
                /* 3: 0 */ public record RecordWithConstructor(Object a, Object b) {
                /* 4: 0 */   public RecordWithConstructor {
                /* 5: 6 */     if (a == null || b == null)
                /* 6: 7 */       throw new IllegalArgumentException("neither a nor b can be null"); 
                /* 7: 0 */   }
                /* 8: 0 */ }
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
    @Test
    public void testRecordWithAnnotations() throws Exception {
        String internalClassName = RecordWithAnnotations.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertEqualsIgnoreEOL("""
                /*   1:   0 */ package org.jd.core.v1;
                /*   2:   0 */
                /*   3:   0 */ public record RecordWithAnnotations(@Sensitive("C1") String a, @Sensitive("C0") double d) {}
                """, source);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
