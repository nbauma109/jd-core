package org.jd.core.v1;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Unknown;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeAnnotationReader;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The type annotation attributes are made of bytes, as the class files of the compilers write them */
public class TypeAnnotationReaderTest {
    private static final int FIELD = 0x13;
    private static final int FORMAL_PARAMETER = 0x16;
    private static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;
    private static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;

    private static final int ATTRIBUTE_NAME = 1;
    private static final int NULLABLE = 2;
    private static final int OTHER = 3;

    private static final ConstantPool CONSTANTS = new ConstantPool(new Constant[] {
        null, new ConstantUtf8("RuntimeVisibleTypeAnnotations"), new ConstantUtf8("Lp/Nullable;"), new ConstantUtf8("Lp/Other;")});

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static TypeAnnotationReader read(int count, Writer entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);

        out.writeShort(count);
        entries.write(out);
        return of(bytes.toByteArray());
    }

    private static TypeAnnotationReader of(byte[] content) {
        return TypeAnnotationReader.of(new Attribute[] {new Unknown(ATTRIBUTE_NAME, content.length, content, CONSTANTS)}, CONSTANTS);
    }

    private static void marker(DataOutputStream out, int target, int info, int type) throws IOException {
        out.writeByte(target);
        if (target == FORMAL_PARAMETER) {
            out.writeByte(info);
        } else if (target == CLASS_TYPE_PARAMETER_BOUND || target == METHOD_TYPE_PARAMETER_BOUND) {
            out.writeByte(info >> 8);
            out.writeByte(info & 255);
        }
        out.writeByte(0);
        out.writeShort(type);
        out.writeShort(0);
    }

    @Test
    public void testFieldParameterAndBounds() throws IOException {
        TypeAnnotationReader reader = read(4, out -> {
            marker(out, FIELD, 0, NULLABLE);
            marker(out, FORMAL_PARAMETER, 2, NULLABLE);
            marker(out, CLASS_TYPE_PARAMETER_BOUND, 0x0100, NULLABLE);
            marker(out, METHOD_TYPE_PARAMETER_BOUND, 0x0000, OTHER);
        });

        assertEquals(List.of("Lp/Nullable;"), reader.getDeclarationAnnotations());
        assertEquals(List.of("Lp/Nullable;"), reader.getParameterAnnotations(2));
        assertTrue(reader.getParameterAnnotations(0).isEmpty());
        assertTrue(reader.hasParameterAnnotations());
        // the first bound of the type parameter 1 (class) and of the type parameter 0 (method)
        assertEquals(List.of("Lp/Other;"), reader.getBoundAnnotations(0));
        assertEquals(List.of("Lp/Nullable;"), reader.getBoundAnnotations(1));
        assertTrue(reader.getBoundAnnotations(2).isEmpty());
        assertTrue(reader.hasBoundAnnotations());
    }

    @Test
    public void testOnlyAnnotationsOfTheWholeTypeAreKept() throws IOException {
        TypeAnnotationReader reader = read(3, out -> {
            // in the type argument of the type of the field
            out.writeByte(FIELD);
            out.writeByte(1);
            out.writeByte(3);
            out.writeByte(0);
            out.writeShort(NULLABLE);
            out.writeShort(0);
            // with an element
            out.writeByte(FIELD);
            out.writeByte(0);
            out.writeShort(NULLABLE);
            out.writeShort(1);
            out.writeShort(OTHER);
            out.writeByte('I');
            out.writeShort(OTHER);
            // for the second bound of a type parameter
            marker(out, METHOD_TYPE_PARAMETER_BOUND, 0x0001, NULLABLE);
        });

        assertTrue(reader.getDeclarationAnnotations().isEmpty());
        assertFalse(reader.hasBoundAnnotations());
        assertFalse(reader.hasParameterAnnotations());
    }

    @Test
    public void testTargetsWhichAreNotKeptAreSkipped() throws IOException {
        // each target has the size of its info: the annotation which follows them all is still found
        int[] sizes = {0x00, 1, 0x01, 1, 0x10, 2, 0x17, 2, 0x42, 2, 0x43, 2, 0x44, 2, 0x45, 2, 0x46, 2, 0x47, 3, 0x48, 3, 0x49, 3, 0x4A, 3, 0x4B, 3, 0x15, 0};
        TypeAnnotationReader reader = read(sizes.length / 2 + 1, out -> {
            for (int i = 0; i < sizes.length; i += 2) {
                out.writeByte(sizes[i]);
                out.write(new byte[sizes[i + 1]]);
                out.writeByte(0);
                out.writeShort(OTHER);
                out.writeShort(0);
            }
            marker(out, FIELD, 0, NULLABLE);
        });

        assertEquals(List.of("Lp/Nullable;"), reader.getDeclarationAnnotations());
    }

    @Test
    public void testTargetOfALocalVariableHasATable() throws IOException {
        TypeAnnotationReader reader = read(3, out -> {
            out.writeByte(0x40);
            out.writeShort(2);
            out.write(new byte[12]);
            out.writeByte(0);
            out.writeShort(OTHER);
            out.writeShort(0);
            out.writeByte(0x41);
            out.writeShort(0);
            out.writeByte(0);
            out.writeShort(OTHER);
            out.writeShort(0);
            marker(out, FIELD, 0, NULLABLE);
        });

        assertEquals(List.of("Lp/Nullable;"), reader.getDeclarationAnnotations());
    }

    @Test
    public void testElementValuesAreSkipped() throws IOException {
        TypeAnnotationReader reader = read(2, out -> {
            // an annotation of the whole type of a local variable with elements of every kind: a constant, an enum, a class, an annotation and an array
            out.writeByte(0x40);
            out.writeShort(0);
            out.writeByte(0);
            out.writeShort(OTHER);
            out.writeShort(5);
            for (int i = 0; i < 5; i++) {
                out.writeShort(OTHER);
                switch (i) {
                    case 0:
                        out.writeByte('s');
                        out.writeShort(OTHER);
                        break;
                    case 1:
                        out.writeByte('e');
                        out.writeShort(OTHER);
                        out.writeShort(OTHER);
                        break;
                    case 2:
                        out.writeByte('c');
                        out.writeShort(OTHER);
                        break;
                    case 3:
                        out.writeByte('@');
                        out.writeShort(OTHER);
                        out.writeShort(1);
                        out.writeShort(OTHER);
                        out.writeByte('I');
                        out.writeShort(OTHER);
                        break;
                    default:
                        out.writeByte('[');
                        out.writeShort(2);
                        out.writeByte('I');
                        out.writeShort(OTHER);
                        out.writeByte('I');
                        out.writeShort(OTHER);
                        break;
                }
            }
            marker(out, FIELD, 0, NULLABLE);
        });

        assertEquals(List.of("Lp/Nullable;"), reader.getDeclarationAnnotations());
    }

    @Test
    public void testAnAttributeWhichCannotBeReadIsDropped() {
        // truncated: one annotation announced
        TypeAnnotationReader truncated = of(new byte[] {0, 1, FIELD});
        // a descriptor which is not in the constant pool
        TypeAnnotationReader unknownType = of(new byte[] {0, 1, FIELD, 0, 0, 99, 0, 0});

        assertTrue(truncated.getDeclarationAnnotations().isEmpty());
        assertTrue(unknownType.getDeclarationAnnotations().isEmpty());
    }

    @Test
    public void testNothingIsKeptOfAnAttributeWhichIsMalformedAfterAValidAnnotation() throws IOException {
        // a valid annotation, then an entry which stops after its target
        TypeAnnotationReader reader = read(2, out -> {
            marker(out, FIELD, 0, NULLABLE);
            out.writeByte(FIELD);
        });

        assertTrue(reader.getDeclarationAnnotations().isEmpty());
    }

    @Test
    public void testOtherAttributesAreIgnored() {
        byte[] content = {0, 0};
        TypeAnnotationReader reader = TypeAnnotationReader.of(new Attribute[] {new Unknown(ATTRIBUTE_NAME, 2, content, new ConstantPool(new Constant[] {
            null, new ConstantUtf8("Other")}))}, CONSTANTS);

        assertTrue(reader.getDeclarationAnnotations().isEmpty());
        assertFalse(reader.hasParameterAnnotations());
    }
}
