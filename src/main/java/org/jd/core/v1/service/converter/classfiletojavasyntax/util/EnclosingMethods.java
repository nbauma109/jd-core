package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EnclosingMethod;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.NEW;

import java.util.Arrays;

public final class EnclosingMethods {
    private EnclosingMethods() {
    }

    /** A local class of a static method has no outer instance, whatever its outer class is */
    public static boolean isDeclaredInStaticMethod(ClassFile classFile, ClassFile outerClassFile) {
        EnclosingMethod enclosingMethod = classFile.getAttribute(Const.ATTR_ENCLOSING_METHOD);

        if (enclosingMethod == null || enclosingMethod.getEnclosingMethodIndex() == 0) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantNameAndType nameAndType = enclosingMethod.getEnclosingMethod();
        String name = nameAndType.getName(constants);
        String signature = nameAndType.getSignature(constants);

        return Arrays.stream(outerClassFile.getMethods())
                .filter(method -> method.getName().equals(name) && method.getSignature().equals(signature))
                .anyMatch(Method::isStatic);
    }

    /** A local class of an initializer or of a field initializer: the attribute names no method, and the initializer may be static */
    public static boolean isDeclaredInAnInitializer(ClassFile classFile) {
        EnclosingMethod enclosingMethod = classFile.getAttribute(Const.ATTR_ENCLOSING_METHOD);

        return enclosingMethod != null && enclosingMethod.getEnclosingMethodIndex() == 0;
    }

    /** A local class is only created by the initializer which declares it: the static initializer of the outer class, when its '<clinit>' creates it */
    public static boolean isCreatedByTheStaticInitializer(ClassFile classFile, ClassFile outerClassFile) {
        ConstantPoolGen constants = new ConstantPoolGen(outerClassFile.getConstantPool());
        String className = classFile.getInternalTypeName().replace('/', '.');

        for (Method method : outerClassFile.getMethods()) {
            Code code = method.getCode();

            if (Const.STATIC_INITIALIZER_NAME.equals(method.getName()) && code != null) {
                for (InstructionHandle handle : new InstructionList(code.getCode())) {
                    if (handle.getInstruction() instanceof NEW creation && creation.getLoadClassType(constants).getClassName().equals(className)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
