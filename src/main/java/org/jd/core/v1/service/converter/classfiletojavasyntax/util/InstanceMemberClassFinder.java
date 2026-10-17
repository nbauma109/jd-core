/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Tells whether a class is a non-static member class (an inner class which is created with an outer instance), by reading its class file. */
public class InstanceMemberClassFinder {
    private final Loader loader;
    private final Map<String, Boolean> instanceMemberClasses = new HashMap<>();

    public InstanceMemberClassFinder(Loader loader) {
        this.loader = loader;
    }

    public boolean isInstanceMemberClass(String internalName) {
        return instanceMemberClasses.computeIfAbsent(internalName, this::read);
    }

    private boolean read(String internalName) {
        try {
            if (!loader.canLoad(internalName)) {
                return false;
            }

            JavaClass javaClass = new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();
            ConstantPool constants = javaClass.getConstantPool();

            for (org.apache.bcel.classfile.Attribute attribute : javaClass.getAttributes()) {
                if (attribute instanceof InnerClasses innerClasses) {
                    for (InnerClass innerClass : innerClasses.getInnerClasses()) {
                        String name = constants.getConstantString(innerClass.getInnerClassIndex(), Const.CONSTANT_Class);

                        if (name.equals(internalName)) {
                            // (a member class has a name and an outer class: not a local nor an anonymous class)
                            return innerClass.getInnerNameIndex() != 0 && innerClass.getOuterClassIndex() != 0 && (innerClass.getInnerAccessFlags() & Const.ACC_STATIC) == 0;
                        }
                    }
                }
            }
            return false;
        } catch (IOException | ClassFormatException e) {
            return false;
        }
    }
}
