/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class JavaLoopTest extends AbstractJdTest {

    @Test
    public void testJdk170While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
            assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
            assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
            assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
            assertNotEquals(-1, source.indexOf("while (i == 4 && i == 5 && i == 6)"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))"));
            assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
            assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
            assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk901While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
            assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
            assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
            assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
            assertNotEquals(-1, source.indexOf("while (i == 4 && i == 5 && i == 6)"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))"));
            assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
            assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
            assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk1002While() throws Exception {
        String internalClassName = "org/jd/core/test/While";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  15 */", "while (i-- > 0)")));
            assertTrue(source.matches(PatternMaker.make(":  23 */", "while (i < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  42 */", "while (i0 > 20)")));
            assertTrue(source.matches(PatternMaker.make("/* 113:   0 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 128:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/* 158:   0 */", "while (true)")));
            assertTrue(source.matches(PatternMaker.make(": 232 */", "while (++i < 10)")));
            assertNotEquals(-1, source.indexOf("while (i == 4 && i == 5 && i == 6)"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4))"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4))"));
            assertFalse(source.matches(PatternMaker.make("[ 348:   0 */", "default:")));
            assertFalse(source.matches(PatternMaker.make("[ 350: 348 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/* 404: 404 */", "System.out.println(\"a\");")));
            assertTrue(source.matches(PatternMaker.make("/* 431: 431 */", "System.out.println(\"a\");")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
            assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
            assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
            assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
            assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk901DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-9.0.1.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
            assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
            assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
            assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
            assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk1002DoWhile() throws Exception {
        String internalClassName = "org/jd/core/test/DoWhile";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-10.0.2.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  24 */", "} while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  32 */", "} while (this == null);")));
            assertTrue(source.matches(PatternMaker.make(":  44 */", "++i;")));
            assertTrue(source.matches(PatternMaker.make(":  46 */", "while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(":  72 */", "while (i0 < 10)")));
            assertTrue(source.matches(PatternMaker.make(":  77 */", "i1--;")));
            assertTrue(source.matches(PatternMaker.make(":  79 */", "while (i1 > 0);")));
            assertTrue(source.matches(PatternMaker.make(":  98 */", "while (--i > 0.0F);")));
            assertTrue(source.matches(PatternMaker.make(": 108 */", "while (i-- > 0.0F);")));
            assertNotEquals(-1, source.indexOf("while ((i == 1 || (i == 5 && i == 6 && i == 7) || i == 8 || (i == 9 && i == 10 && i == 11)) && (i == 4 || i % 200 > 50) && (i > 3 || i > 4));"));
            assertNotEquals(-1, source.indexOf("while ((i == 1 && (i == 5 || i == 6 || i == 7) && i == 8 && (i == 9 || i == 10 || i == 11)) || (i == 4 && i % 200 > 50) || (i > 3 && i > 4));"));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170BreakContinue() throws Exception {
        String internalClassName = "org/jd/core/test/BreakContinue";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("/*  15:  15 */", "if (i == 1)")));
            assertTrue(source.matches(PatternMaker.make("/*  16:   0 */", "continue;")));
            assertTrue(source.matches(PatternMaker.make("/*  18:  18 */", "if (i == 2)")));
            assertTrue(source.matches(PatternMaker.make("/*  19:   0 */", "continue;")));

            assertTrue(source.matches(PatternMaker.make("/*  31:  31 */", "label18: while (i > 1)")));
            assertTrue(source.matches(PatternMaker.make("/*  37:   0 */", "continue label18;")));
            assertTrue(source.matches(PatternMaker.make("/*  40:   0 */", "break label18;")));

            assertTrue(source.matches(PatternMaker.make("/*  54:  54 */", "label17: while (i > 1)")));
            assertTrue(source.matches(PatternMaker.make("/*  60:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/*  63:   0 */", "continue label17;")));

            assertTrue(source.matches(PatternMaker.make("/*  78:   0 */", "label13:")));
            assertTrue(source.matches(PatternMaker.make("/*  83:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/*  86:   0 */", "break label13;")));

            assertTrue(source.matches(PatternMaker.make("/* 101:   0 */", "label15:", "do {")));
            assertTrue(source.matches(PatternMaker.make("/* 106:   0 */", "break;")));
            assertTrue(source.matches(PatternMaker.make("/* 109:   0 */", "break label15;")));

            assertTrue(source.matches(PatternMaker.make("/* 123:   0 */", "label24:", "do {")));
            assertTrue(source.matches(PatternMaker.make("/* 133:   0 */", "continue label24;")));
            assertTrue(source.matches(PatternMaker.make("/* 135:   0 */", "break label24;")));
            assertTrue(source.matches(PatternMaker.make("/* 138:   0 */", "break label23;")));

            assertTrue(source.matches(PatternMaker.make("/* 155:   0 */", "label16:", "do {")));
            assertTrue(source.matches(PatternMaker.make("/* 162:   0 */", "break label16;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            String expected = Files.readString(Paths.get(getClass().getResource("/txt/For.txt").toURI()));
            assertEqualsIgnoreEOL(expected, source);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk170NoDebugInfoFor() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0-no-debug-info.zip")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("for (int i = 0; i < 10; i++)")));
            assertTrue(source.matches(PatternMaker.make("for (int i = 0;; i++)")));
            assertTrue(source.matches(PatternMaker.make("for (String str : paramList)")));
            assertTrue(source.matches(PatternMaker.make("for (paramInt = 0; paramInt < 10; paramInt++)")));
            assertTrue(source.matches(PatternMaker.make("for (int j : new int[] { 4 })")));
            assertTrue(source.matches(PatternMaker.make("for (String str : paramArrayOfString)")));
            assertTrue(source.matches(PatternMaker.make("for (String str : paramList)")));
        }

        // Recompile decompiled source code and check errors
        //assertTrue(CompilerUtil.compile("1.7", new JavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJdk150For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.5.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  20 */", "for (int i = 0; i < 10; i++)")));
            assertTrue(source.matches(PatternMaker.make(":  88 */", "while (paramInt < 10)")));
            assertTrue(source.matches(PatternMaker.make(": 273 */", "for (paramInt = 0; paramInt < 10; paramInt++)")));
            assertTrue(source.matches(PatternMaker.make(": 310 */", "for (int j : new int[] { 4 })")));
            assertTrue(source.matches(PatternMaker.make("/* 347:   0 */", "do {")));
            assertTrue(source.matches(PatternMaker.make(": 349 */", "while (i < 10);")));
            assertTrue(source.matches(PatternMaker.make(": 385 */", "for (String str : paramArrayOfString)")));
            assertTrue(source.matches(PatternMaker.make(": 399 */", "for (String str : paramList)")));
            assertTrue(source.matches(PatternMaker.make(": 411 */", "Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(getClass().getInterfaces()).iterator()")));
            assertTrue(source.matches(PatternMaker.make(": 427 */", "for (int i = 0; i < 3; i++)")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testJdk160For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.6.0.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            String expected = Files.readString(Paths.get(getClass().getResource("/txt/For.txt").toURI()));
            assertEqualsIgnoreEOL(expected, source);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.6", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testIbmJ9For() throws Exception {
        String internalClassName = "org/jd/core/test/For";
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-ibm-j9_vm.zip")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            String expected = Files.readString(Paths.get(getClass().getResource("/txt/For.txt").toURI()));
            assertEqualsIgnoreEOL(expected, source);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.5", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
