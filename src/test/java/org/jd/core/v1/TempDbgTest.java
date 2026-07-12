package org.jd.core.v1;

import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TempDbgTest extends AbstractJdTest {

    public void testDump() throws Exception {
        String internalName = "org/joda/time/format/DateTimeFormatterBuilder$TimeZoneOffset";
        byte[] classBytes = Files.readAllBytes(Paths.get("/tmp/jdcheck/" + internalName + ".class"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(internalName + ".class"));
            zos.write(classBytes);
            zos.closeEntry();
        }
        try (ByteArrayInputStream zin = new ByteArrayInputStream(baos.toByteArray())) {
            ZipLoader loader = new ZipLoader(zin);
            PlainTextPrinter printer = new PlainTextPrinter();
            classFileToJavaSourceDecompiler.decompile(loader, printer, internalName, Collections.emptyMap());
            System.out.println(printer.toString());
        }
    }
}
