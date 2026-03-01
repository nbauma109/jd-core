/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataInputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Calendar;
import java.util.Date;

public class OverloadBridgeCastTest extends AbstractJdTest {
    @Test
    public void testAppendableBridgeKeepsOverloadCasts() throws Exception {
        String internalClassName = AppendableBridge.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        String bridgeCall = "return (StringBuffer)apply(calendar, (Appendable)buf);";
        assertEquals(2, countOccurrences(source, bridgeCall));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testWideningOverloadBridgeKeepsCast() throws Exception {
        String internalClassName = DataInputBridge.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(1, countOccurrences(source, "return read((DataInput)dataInputStream);"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testFileChannelOverloadBridgeKeepsCast() throws Exception {
        String internalClassName = FileChannelBridge.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("return contentEquals((SeekableByteChannel)channel1, channel2, bufferCapacity);")
                || source.contains("return contentEquals((SeekableByteChannel)channel1, (SeekableByteChannel)channel2, bufferCapacity);"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testFileChannelBridgeWithReadableOverloadKeepsCast() throws Exception {
        String internalClassName = FileChannelBridgeWithReadable.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("return contentEquals((SeekableByteChannel)channel1, channel2, bufferCapacity);")
                || source.contains("return contentEquals((SeekableByteChannel)channel1, (SeekableByteChannel)channel2, bufferCapacity);"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    private static int countOccurrences(String source, String snippet) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(snippet, index)) >= 0) {
            count++;
            index += snippet.length();
        }
        return count;
    }

    @SuppressWarnings("unused")
    static final class AppendableBridge {
        <B extends Appendable> B apply(Calendar calendar, B buf) {
            return buf;
        }

        StringBuffer apply(Calendar calendar, StringBuffer buf) {
            return (StringBuffer) apply(calendar, (Appendable) buf);
        }

        StringBuffer format(Date date, StringBuffer buf) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return (StringBuffer) apply(calendar, (Appendable) buf);
        }
    }

    @SuppressWarnings("unused")
    static final class DataInputBridge {
        static String read(DataInput input) {
            return "base";
        }

        static String read(DataInputStream dataInputStream) {
            return read((DataInput) dataInputStream);
        }
    }

    @SuppressWarnings("unused")
    static final class FileChannelBridge {
        static boolean contentEquals(SeekableByteChannel channel1, SeekableByteChannel channel2, int bufferCapacity) {
            return true;
        }

        static boolean contentEquals(FileChannel channel1, FileChannel channel2, int bufferCapacity) {
            return contentEquals((SeekableByteChannel) channel1, channel2, bufferCapacity);
        }
    }

    @SuppressWarnings("unused")
    static final class FileChannelBridgeWithReadable {
        static boolean contentEquals(java.nio.channels.ReadableByteChannel channel1, java.nio.channels.ReadableByteChannel channel2, int bufferCapacity) {
            return true;
        }

        static boolean contentEquals(SeekableByteChannel channel1, SeekableByteChannel channel2, int bufferCapacity) {
            return contentEquals((java.nio.channels.ReadableByteChannel) channel1, channel2, bufferCapacity);
        }

        static boolean contentEquals(FileChannel channel1, FileChannel channel2, int bufferCapacity) {
            return contentEquals((SeekableByteChannel) channel1, channel2, bufferCapacity);
        }
    }
}
