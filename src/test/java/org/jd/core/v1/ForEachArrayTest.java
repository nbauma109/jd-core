package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.ForEachArray;
import org.junit.Test;

public class ForEachArrayTest extends AbstractJdTest {

    @Test
    public void testForEachArray() throws Exception {
        String internalClassName = ForEachArray.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        String expected = """
/*  1:  0 */ package org.jd.core.v1.stub;
/*  2:  0 */ 
/*  3:  0 */ public class ForEachArray {
/*  4:  0 */   void truePositive(String[] args) {
/*  5:  8 */     for (String s : args)
/*  6:  9 */       System.out.println(s); 
/*  7:  0 */   }
/*  8:  0 */   
/*  9:  0 */   void falsePositive1(String[] args) {
/* 10:  0 */     String[] arr$;
/* 11:  0 */     int j, i$;
/* 12: 16 */     for (arr$ = args, j = arr$.length, i$ = 0; i$ < j; ) {
/* 13: 16 */       String s = arr$[i$];
/* 14: 17 */       System.out.println(s);
/* 15: 18 */       i$++;
/* 16:  0 */     } 
/* 17:  0 */   }
/* 18:  0 */   
/* 19:  0 */   void falsePositive2(String[] args) {
/* 20:  0 */     String[] arr$;
/* 21:  0 */     int j, i$;
/* 22: 25 */     for (arr$ = args, j = arr$.length, i$ = 0; i$ < j; ) {
/* 23: 25 */       String s = arr$[i$];
/* 24: 26 */       System.out.println(s);
/* 25:  0 */       i$ *= 1;
/* 26:  0 */     } 
/* 27:  0 */   }
/* 28:  0 */ }
                """;
        assertEquals(expected.replaceAll("\s*\r?\n", "\n"), source.replaceAll("\s*\r?\n", "\n"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }


}
