package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EnclosingMethod;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;

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
}
