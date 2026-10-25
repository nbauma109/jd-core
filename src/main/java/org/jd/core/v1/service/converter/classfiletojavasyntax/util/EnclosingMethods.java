/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * A local class is only created by the initializer which declares it, or by the code of the local classes that initializer creates: the static
     * initializer of the outer class, when its '<clinit>', one of the lambdas it declares, or a method of a local class created that way, creates it
     */
    public static boolean isCreatedByTheStaticInitializer(ClassFile classFile, ClassFile outerClassFile) {
        return isCreatedByTheStaticInitializer(classFile, outerClassFile, new HashSet<>());
    }

    private static boolean isCreatedByTheStaticInitializer(ClassFile classFile, ClassFile outerClassFile, Set<String> visited) {
        if (!visited.add(classFile.getInternalTypeName())) {
            return false;
        }
        if (createsIn(outerClassFile, classFile, true)) {
            return true;
        }
        List<ClassFile> innerClassFiles = outerClassFile.getInnerClassFiles();

        if (innerClassFiles != null) {
            for (ClassFile other : innerClassFiles) {
                if (other != classFile && isDeclaredInAnInitializer(other) && createsIn(other, classFile, false)
                        && isCreatedByTheStaticInitializer(other, outerClassFile, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** @param initializerOnly only the static initializer and its lambdas count, else every method of the class */
    private static boolean createsIn(ClassFile holder, ClassFile created, boolean initializerOnly) {
        ConstantPoolGen constants = new ConstantPoolGen(holder.getConstantPool());
        String className = created.getInternalTypeName().replace('/', '.');

        for (Method method : holder.getMethods()) {
            Code code = method.getCode();

            if (code != null && (!initializerOnly || Const.STATIC_INITIALIZER_NAME.equals(method.getName()) || method.getName().startsWith("lambda$static$"))) {
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
