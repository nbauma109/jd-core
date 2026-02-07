/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationDefault;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Module;
import org.apache.bcel.classfile.ModuleExports;
import org.apache.bcel.classfile.ModuleOpens;
import org.apache.bcel.classfile.ModuleProvides;
import org.apache.bcel.classfile.ModuleRequires;
import org.apache.bcel.classfile.RecordComponentInfo;
import org.apache.bcel.classfile.RuntimeInvisibleAnnotations;
import org.apache.bcel.classfile.RuntimeVisibleAnnotations;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.Declaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.ModuleDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.RecordDeclaration.RecordComponent;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReferences;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.BaseElementValue;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileAnnotationDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileEnumDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileInterfaceDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileRecordDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileTypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.AnnotationConverter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.RecordHelper;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeParameterVisitor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

/**
 * Convert ClassFile model to Java syntax model.<br><br>
 *
 * Input:  {@link org.jd.core.v1.model.classfile.ClassFile}<br>
 * Output: {@link org.jd.core.v1.model.javasyntax.CompilationUnit}<br>
 */
public class ConvertClassFileProcessor {
    private static final String JAVA_LANG_OVERRIDE = "java/lang/Override";

    private final Map<String, LoadedType> loadedTypeCache = new HashMap<>();
    private Loader loader;

    private final PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor() {
        @Override
        public void visit(TypeParameter parameter) {
            bindings.put(parameter.getIdentifier(), new GenericType(parameter.getIdentifier()));
        }
        @Override
        public void visit(TypeParameterWithTypeBounds parameter) {
            bindings.put(parameter.getIdentifier(), new GenericType(parameter.getIdentifier()));
            typeBounds.put(parameter.getIdentifier(), parameter.getTypeBounds());
        }
    };

    public CompilationUnit process(ClassFile classFile, TypeMaker typeMaker, DecompileContext decompileContext) {
        loader = decompileContext == null ? null : decompileContext.getLoader();
        loadedTypeCache.clear();
        loadedTypeCache.put(classFile.getInternalTypeName(), LoadedType.fromClassFile(classFile));
        AnnotationConverter annotationConverter = new AnnotationConverter(typeMaker);

        TypeDeclaration typeDeclaration;

        if (classFile.isEnum()) {
            typeDeclaration = convertEnumDeclaration(typeMaker, annotationConverter, classFile, null);
        } else if (classFile.isAnnotation()) {
            typeDeclaration = convertAnnotationDeclaration(typeMaker, annotationConverter, classFile, null);
        } else if (classFile.isModule()) {
            typeDeclaration = convertModuleDeclaration(classFile);
        } else if (classFile.isInterface()) {
            typeDeclaration = convertInterfaceDeclaration(typeMaker, annotationConverter, classFile, null);
        } else if (classFile.isRecord()) {
            typeDeclaration = convertRecordDeclaration(typeMaker, annotationConverter, classFile, null);
        } else {
            typeDeclaration = convertClassDeclaration(typeMaker, annotationConverter, classFile, null);
        }

        decompileContext.setMajorVersion(classFile.getMajorVersion());
        decompileContext.setMinorVersion(classFile.getMinorVersion());
        return new CompilationUnit(typeDeclaration);
    }

    protected ClassFileInterfaceDeclaration convertInterfaceDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileInterfaceDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getTypeParameters(), typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileEnumDeclaration convertEnumDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileEnumDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileAnnotationDeclaration convertAnnotationDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileAnnotationDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                bodyDeclaration);
    }

    protected ClassFileClassDeclaration convertClassDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileClassDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getTypeParameters(), typeTypes.getSuperType(),
                typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileRecordDeclaration convertRecordDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        List<RecordComponent> recordComponents = convertRecordComponents(parser, converter, classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileRecordDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getTypeParameters(), recordComponents,
                typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileBodyDeclaration convertBodyDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, BaseTypeParameter typeParameters, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;

        if (!classFile.isStatic() && outerClassFileBodyDeclaration != null) {
            bindings = outerClassFileBodyDeclaration.getBindings();
            typeBounds = outerClassFileBodyDeclaration.getTypeBounds();
        } else {
            bindings = Collections.emptyMap();
            typeBounds = Collections.emptyMap();
        }

        if (typeParameters != null) {
            bindings=new HashMap<>(bindings);
            typeBounds=new HashMap<>(typeBounds);
            populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
            typeParameters.accept(populateBindingsWithTypeParameterVisitor);
        }

        ClassFileBodyDeclaration bodyDeclaration = new ClassFileBodyDeclaration(classFile, bindings, typeBounds, outerClassFileBodyDeclaration);

        bodyDeclaration.setFieldDeclarations(convertFields(parser, converter, classFile));
        bodyDeclaration.setMethodDeclarations(convertMethods(parser, converter, bodyDeclaration, classFile));
        bodyDeclaration.setInnerTypeDeclarations(convertInnerTypes(parser, converter, classFile, bodyDeclaration));

        return bodyDeclaration;
    }

    protected List<ClassFileFieldDeclaration> convertFields(TypeMaker parser, AnnotationConverter converter, ClassFile classFile) {
        Field[] fields = classFile.getFields();

        DefaultList<ClassFileFieldDeclaration> list = new DefaultList<>(fields.length);
        BaseAnnotationReference annotationReferences;
        Type typeField;
        ExpressionVariableInitializer variableInitializer;
        FieldDeclarator fieldDeclarator;
        for (Field field : fields) {
            annotationReferences = convertAnnotationReferences(converter, field);
            typeField = parser.parseFieldSignature(classFile, field);
            variableInitializer = convertFieldInitializer(field, typeField);
            fieldDeclarator = new FieldDeclarator(field.getName(), variableInitializer);
            if (!classFile.isRecord()) {
                list.add(new ClassFileFieldDeclaration(annotationReferences, field.getAccessFlags(), typeField, fieldDeclarator));
            }
        }
        return list;
    }

    protected List<RecordComponent> convertRecordComponents(TypeMaker parser, AnnotationConverter converter, ClassFile classFile) {
        List<RecordComponent> recordComponents = new ArrayList<>();
        org.apache.bcel.classfile.Record recordAttribute = classFile.getAttribute(Const.ATTR_RECORD);
        BaseAnnotationReference annotationReferences;
        RecordComponentInfo[] recordComponentInfos = recordAttribute.getComponents();
        if (recordComponentInfos != null) {
            for (RecordComponentInfo recordComponentInfo : recordComponentInfos) {
                annotationReferences = convertAnnotationReferences(converter, recordComponentInfo);
                int index = recordComponentInfo.getIndex();
                int descriptorIndex = recordComponentInfo.getDescriptorIndex();
                String recordComponentName = classFile.getConstantPool().getConstantString(index, Const.CONSTANT_Utf8);
                String recordComponentSignature = classFile.getConstantPool().getConstantString(descriptorIndex, Const.CONSTANT_Utf8);
                ObjectType recordComponentType = parser.makeFromDescriptor(recordComponentSignature);
                recordComponents.add(new RecordComponent(annotationReferences, recordComponentType, recordComponentName));
            }
        }
        return recordComponents;
    }

    protected List<ClassFileConstructorOrMethodDeclaration> convertMethods(TypeMaker parser, AnnotationConverter converter, ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile) {
        Method[] methods = classFile.getMethods();
        if (classFile.isRecord()) {
            methods = RecordHelper.removeImplicitDefaultRecordMethods(classFile);
        }
        DefaultList<ClassFileConstructorOrMethodDeclaration> list = new DefaultList<>(methods.length);
        String name;
        BaseAnnotationReference annotationReferences;
        BaseElementValue defaultAnnotationValue;
        TypeMaker.MethodTypes methodTypes;
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;
        Code code;
        int firstLineNumber;
        for (Method method : methods) {
            name = method.getName();
            annotationReferences = convertAnnotationReferences(converter, method);
            AnnotationDefault annotationDefault = (AnnotationDefault) Stream.of(method.getAttributes()).filter(AnnotationDefault.class::isInstance).findAny().orElse(null);
            defaultAnnotationValue = null;

            if (annotationDefault != null) {
                defaultAnnotationValue = converter.convert(annotationDefault.getDefaultValue());
            }

            methodTypes = parser.parseMethodSignature(classFile, method);
            if ((method.getAccessFlags() & Const.ACC_STATIC) == 0) {
                bindings = bodyDeclaration.getBindings();
                typeBounds = bodyDeclaration.getTypeBounds();
            } else {
                bindings = Collections.emptyMap();
                typeBounds = Collections.emptyMap();
            }

            if (methodTypes.getTypeParameters() != null) {
                bindings=new HashMap<>(bindings);
                typeBounds=new HashMap<>(typeBounds);
                populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
                methodTypes.getTypeParameters().accept(populateBindingsWithTypeParameterVisitor);
           }

            code = method.getCode();
            firstLineNumber = 0;

            if (code != null) {
                LineNumberTable lineNumberTable = code.getLineNumberTable();
                if (lineNumberTable != null) {
                    firstLineNumber = lineNumberTable.getLineNumberTable()[0].getLineNumber();
                }
            }

            if (StringConstants.INSTANCE_CONSTRUCTOR.equals(name)) {
                if (classFile.isRecord()) {
                    list.add(new ClassFileRecordConstructorDeclaration(
                            bodyDeclaration, classFile, method, annotationReferences, methodTypes.getTypeParameters(),
                            methodTypes.getParameterTypes(), methodTypes.getExceptionTypes(), bindings, typeBounds, firstLineNumber));
                } else {
                    list.add(new ClassFileConstructorDeclaration(
                            bodyDeclaration, classFile, method, annotationReferences, methodTypes.getTypeParameters(),
                            methodTypes.getParameterTypes(), methodTypes.getExceptionTypes(), bindings, typeBounds, firstLineNumber));
                }
            } else if (StringConstants.CLASS_CONSTRUCTOR.equals(name)) {
                list.add(new ClassFileStaticInitializerDeclaration(bodyDeclaration, classFile, method, bindings, typeBounds, firstLineNumber));
            } else {
                annotationReferences = addOverrideAnnotationIfNecessary(parser, classFile, method, annotationReferences);
                ClassFileMethodDeclaration methodDeclaration = new ClassFileMethodDeclaration(
                        bodyDeclaration, classFile, method, annotationReferences, name, methodTypes.getTypeParameters(),
                        methodTypes.getReturnedType(), methodTypes.getParameterTypes(), methodTypes.getExceptionTypes(), defaultAnnotationValue,
                        bindings, typeBounds, firstLineNumber);
                if (classFile.isInterface()) {
                    // For interfaces, add 'default' access flag on public methods
                    if (methodDeclaration.getFlags() == Const.ACC_PUBLIC) {
                        methodDeclaration.setFlags(Const.ACC_PUBLIC|Declaration.FLAG_DEFAULT);
                    }
                    if (methodDeclaration.getFlags() == (Const.ACC_PUBLIC|Const.ACC_VARARGS)) {
                        methodDeclaration.setFlags(Const.ACC_PUBLIC|Const.ACC_VARARGS|Declaration.FLAG_DEFAULT);
                    }
                }
                list.add(methodDeclaration);
            }
        }
        return list;
    }

    protected BaseAnnotationReference addOverrideAnnotationIfNecessary(TypeMaker typeMaker, ClassFile classFile, Method method, BaseAnnotationReference annotationReferences) {
        if (classFile.getMajorVersion() < Const.MAJOR_1_5) {
            return annotationReferences;
        }

        int accessFlags = method.getAccessFlags();
        if ((accessFlags & (Const.ACC_STATIC | Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_BRIDGE)) != 0) {
            return annotationReferences;
        }

        String name = method.getName();
        if (StringConstants.INSTANCE_CONSTRUCTOR.equals(name) || StringConstants.CLASS_CONSTRUCTOR.equals(name)) {
            return annotationReferences;
        }
        if (containsOverrideAnnotation(annotationReferences)) {
            return annotationReferences;
        }

        String parameterDescriptor = extractParameterDescriptor(method.getSignature());
        boolean overrides = overridesClassMethod(classFile, name, parameterDescriptor);

        if (!overrides && classFile.getMajorVersion() >= Const.MAJOR_1_6) {
            overrides = overridesInterfaceMethod(classFile, name, parameterDescriptor);
        }
        if (!overrides) {
            return annotationReferences;
        }

        AnnotationReference overrideAnnotation = new AnnotationReference(typeMaker.makeFromInternalTypeName(JAVA_LANG_OVERRIDE));
        return prependAnnotation(annotationReferences, overrideAnnotation);
    }

    protected boolean containsOverrideAnnotation(BaseAnnotationReference annotationReferences) {
        if (annotationReferences == null) {
            return false;
        }

        if (annotationReferences instanceof AnnotationReference annotationReference) {
            ObjectType type = annotationReference.getType();
            return type != null && JAVA_LANG_OVERRIDE.equals(type.getInternalName());
        }

        if (annotationReferences instanceof AnnotationReferences<?> annotationReferenceList) {
            for (Object annotation : annotationReferenceList) {
                if (annotation instanceof AnnotationReference annotationReference) {
                    ObjectType type = annotationReference.getType();
                    if (type != null && JAVA_LANG_OVERRIDE.equals(type.getInternalName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected BaseAnnotationReference prependAnnotation(BaseAnnotationReference annotationReferences, AnnotationReference annotationReference) {
        if (annotationReferences == null) {
            return annotationReference;
        }

        if (annotationReferences instanceof AnnotationReference singleAnnotationReference) {
            AnnotationReferences<AnnotationReference> mergedAnnotations = new AnnotationReferences<>(2);
            mergedAnnotations.add(annotationReference);
            mergedAnnotations.add(singleAnnotationReference);
            return mergedAnnotations;
        }

        if (annotationReferences instanceof AnnotationReferences<?> annotationReferenceList) {
            AnnotationReferences<AnnotationReference> mergedAnnotations = new AnnotationReferences<>(annotationReferenceList.size() + 1);
            mergedAnnotations.add(annotationReference);
            for (Object annotation : annotationReferenceList) {
                mergedAnnotations.add((AnnotationReference)annotation);
            }
            return mergedAnnotations;
        }

        return annotationReferences;
    }

    protected boolean overridesClassMethod(ClassFile classFile, String methodName, String parameterDescriptor) {
        String currentTypeName = classFile.getInternalTypeName();
        String parentTypeName = getSuperTypeName(classFile);
        Set<String> visited = new HashSet<>();

        while (parentTypeName != null && visited.add(parentTypeName)) {
            LoadedType parentType = loadType(parentTypeName);
            if (parentType == null) {
                break;
            }
            if (declaresOverridableMethod(parentType, currentTypeName, methodName, parameterDescriptor, false)) {
                return true;
            }
            parentTypeName = parentType.getSuperTypeName();
        }

        return false;
    }

    protected boolean overridesInterfaceMethod(ClassFile classFile, String methodName, String parameterDescriptor) {
        String currentTypeName = classFile.getInternalTypeName();
        Deque<String> interfacesToScan = new ArrayDeque<>();
        Set<String> visitedInterfaces = new HashSet<>();

        LoadedType currentType = loadedTypeCache.getOrDefault(classFile.getInternalTypeName(), LoadedType.fromClassFile(classFile));
        Set<String> visitedClasses = new HashSet<>();
        while (currentType != null && visitedClasses.add(currentType.getInternalTypeName())) {
            Collections.addAll(interfacesToScan, currentType.getInterfaceTypeNames());
            currentType = loadType(currentType.getSuperTypeName());
        }

        while (!interfacesToScan.isEmpty()) {
            String interfaceTypeName = interfacesToScan.removeFirst();
            if (!visitedInterfaces.add(interfaceTypeName)) {
                continue;
            }
            LoadedType interfaceType = loadType(interfaceTypeName);
            if (interfaceType == null) {
                continue;
            }
            if (declaresOverridableMethod(interfaceType, currentTypeName, methodName, parameterDescriptor, true)) {
                return true;
            }
            Collections.addAll(interfacesToScan, interfaceType.getInterfaceTypeNames());
        }

        return false;
    }

    protected static String getSuperTypeName(ClassFile classFile) {
        if (classFile == null || classFile.getSuperclassNameIndex() == 0) {
            return null;
        }
        return classFile.getSuperTypeName();
    }

    protected boolean declaresOverridableMethod(LoadedType loadedType, String currentTypeName, String methodName, String parameterDescriptor, boolean fromInterface) {
        for (LoadedMethod loadedMethod : loadedType.getMethods()) {
            if (!methodName.equals(loadedMethod.getName())) {
                continue;
            }
            if (!parameterDescriptor.equals(loadedMethod.getParameterDescriptor())) {
                continue;
            }

            int accessFlags = loadedMethod.getAccessFlags();
            if ((accessFlags & (Const.ACC_STATIC | Const.ACC_PRIVATE | Const.ACC_SYNTHETIC | Const.ACC_BRIDGE)) != 0) {
                continue;
            }
            if (!fromInterface && isPackagePrivate(accessFlags) && !isSamePackage(currentTypeName, loadedType.getInternalTypeName())) {
                continue;
            }
            return true;
        }

        return false;
    }

    protected static boolean isPackagePrivate(int accessFlags) {
        return (accessFlags & (Const.ACC_PUBLIC | Const.ACC_PROTECTED | Const.ACC_PRIVATE)) == 0;
    }

    protected static boolean isSamePackage(String leftInternalTypeName, String rightInternalTypeName) {
        return packageName(leftInternalTypeName).equals(packageName(rightInternalTypeName));
    }

    protected static String packageName(String internalTypeName) {
        if (internalTypeName == null) {
            return "";
        }
        int index = internalTypeName.lastIndexOf('/');
        return index == -1 ? "" : internalTypeName.substring(0, index);
    }

    protected static String extractParameterDescriptor(String methodDescriptor) {
        int index = methodDescriptor.indexOf(')');
        if (index == -1) {
            return methodDescriptor;
        }
        return methodDescriptor.substring(0, index + 1);
    }

    protected LoadedType loadType(String internalTypeName) {
        if (internalTypeName == null) {
            return null;
        }
        if (loadedTypeCache.containsKey(internalTypeName)) {
            return loadedTypeCache.get(internalTypeName);
        }

        LoadedType loadedType = loadType(loader, internalTypeName);
        loadedTypeCache.put(internalTypeName, loadedType);
        return loadedType;
    }

    protected LoadedType loadType(Loader classLoader, String internalTypeName) {
        if ((classLoader == null) || !classLoader.canLoad(internalTypeName)) {
            return null;
        }

        try {
            byte[] data = classLoader.load(internalTypeName);
            return parseLoadedType(internalTypeName, data);
        } catch (Throwable t) {
            return null;
        }
    }

    protected LoadedType parseLoadedType(String internalTypeName, byte[] data) throws IOException {
        if (data == null) {
            return null;
        }

        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = reader.readInt();
            if (magic != 0xCAFEBABE) {
                return null;
            }

            // Skip 'minorVersion' & 'majorVersion'
            reader.skipBytes(2 * 2);

            Object[] constants = loadConstants(reader);

            // Skip 'accessFlags' & 'thisClassIndex'
            reader.skipBytes(2 * 2);

            int superClassIndex = reader.readUnsignedShort();
            String superTypeName = constantClassName(constants, superClassIndex);

            int interfaceCount = reader.readUnsignedShort();
            String[] interfaceTypeNames = new String[interfaceCount];
            for (int i = 0; i < interfaceCount; i++) {
                interfaceTypeNames[i] = constantClassName(constants, reader.readUnsignedShort());
            }

            skipMembers(reader);

            int methodCount = reader.readUnsignedShort();
            List<LoadedMethod> methods = new ArrayList<>(methodCount);
            for (int i = 0; i < methodCount; i++) {
                int accessFlags = reader.readUnsignedShort();
                String name = constantUtf8(constants, reader.readUnsignedShort());
                String descriptor = constantUtf8(constants, reader.readUnsignedShort());
                skipAttributes(reader);

                if (name == null || descriptor == null) {
                    continue;
                }
                methods.add(new LoadedMethod(name, extractParameterDescriptor(descriptor), accessFlags));
            }

            return new LoadedType(internalTypeName, superTypeName, interfaceTypeNames, methods);
        }
    }

    protected static Object[] loadConstants(DataInputStream reader) throws IOException {
        int count = reader.readUnsignedShort();
        Object[] constants = new Object[count];

        for (int i = 1; i < count; i++) {
            int tag = reader.readUnsignedByte();
            switch (tag) {
                case Const.CONSTANT_Utf8:
                    constants[i] = reader.readUTF();
                    break;
                case Const.CONSTANT_Integer:
                case Const.CONSTANT_Float:
                case Const.CONSTANT_Fieldref:
                case Const.CONSTANT_Methodref:
                case Const.CONSTANT_InterfaceMethodref:
                case Const.CONSTANT_NameAndType:
                case Const.CONSTANT_Dynamic:
                case Const.CONSTANT_InvokeDynamic:
                    reader.skipBytes(4);
                    break;
                case Const.CONSTANT_Long:
                case Const.CONSTANT_Double:
                    reader.skipBytes(8);
                    i++;
                    break;
                case Const.CONSTANT_Class:
                case Const.CONSTANT_String:
                case Const.CONSTANT_MethodType:
                case Const.CONSTANT_Module:
                case Const.CONSTANT_Package:
                    constants[i] = reader.readUnsignedShort();
                    break;
                case Const.CONSTANT_MethodHandle:
                    reader.skipBytes(3);
                    break;
                default:
                    throw new IOException("Invalid constant pool tag: " + tag);
            }
        }

        return constants;
    }

    protected static void skipMembers(DataInputStream reader) throws IOException {
        int count = reader.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            // Skip 'accessFlags', 'nameIndex' & 'descriptorIndex'
            reader.skipBytes(3 * 2);
            skipAttributes(reader);
        }
    }

    protected static void skipAttributes(DataInputStream reader) throws IOException {
        int attributeCount = reader.readUnsignedShort();
        for (int i = 0; i < attributeCount; i++) {
            reader.skipBytes(2); // attributeNameIndex
            int attributeLength = reader.readInt();
            reader.skipBytes(attributeLength);
        }
    }

    protected static String constantUtf8(Object[] constants, int index) {
        if (index <= 0 || index >= constants.length) {
            return null;
        }
        Object value = constants[index];
        return value instanceof String ? (String)value : null;
    }

    protected static String constantClassName(Object[] constants, int classIndex) {
        if (classIndex <= 0 || classIndex >= constants.length) {
            return null;
        }
        Object nameIndex = constants[classIndex];
        if (!(nameIndex instanceof Integer)) {
            return null;
        }
        return constantUtf8(constants, (Integer)nameIndex);
    }

    protected static final class LoadedMethod {
        private final String name;
        private final String parameterDescriptor;
        private final int accessFlags;

        protected LoadedMethod(String name, String parameterDescriptor, int accessFlags) {
            this.name = name;
            this.parameterDescriptor = parameterDescriptor;
            this.accessFlags = accessFlags;
        }

        protected String getName() {
            return name;
        }

        protected String getParameterDescriptor() {
            return parameterDescriptor;
        }

        protected int getAccessFlags() {
            return accessFlags;
        }
    }

    protected static final class LoadedType {
        private static final String[] NO_INTERFACES = new String[0];

        private final String internalTypeName;
        private final String superTypeName;
        private final String[] interfaceTypeNames;
        private final List<LoadedMethod> methods;

        protected LoadedType(String internalTypeName, String superTypeName, String[] interfaceTypeNames, List<LoadedMethod> methods) {
            this.internalTypeName = internalTypeName;
            this.superTypeName = superTypeName;
            this.interfaceTypeNames = interfaceTypeNames == null ? NO_INTERFACES : interfaceTypeNames;
            this.methods = methods;
        }

        protected static LoadedType fromClassFile(ClassFile classFile) {
            Method[] methods = classFile.getMethods();
            List<LoadedMethod> loadedMethods = new ArrayList<>(methods.length);
            for (Method method : methods) {
                loadedMethods.add(new LoadedMethod(method.getName(), extractParameterDescriptor(method.getSignature()), method.getAccessFlags()));
            }
            return new LoadedType(classFile.getInternalTypeName(), ConvertClassFileProcessor.getSuperTypeName(classFile), classFile.getInterfaceTypeNames(), loadedMethods);
        }

        protected String getInternalTypeName() {
            return internalTypeName;
        }

        protected String getSuperTypeName() {
            return superTypeName;
        }

        protected String[] getInterfaceTypeNames() {
            return interfaceTypeNames;
        }

        protected List<LoadedMethod> getMethods() {
            return methods;
        }
    }

    protected List<ClassFileTypeDeclaration> convertInnerTypes(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();

        if (innerClassFiles == null) {
            return null;
        }
        DefaultList<ClassFileTypeDeclaration> list = new DefaultList<>(innerClassFiles.size());
        ClassFileTypeDeclaration innerTypeDeclaration;
        for (ClassFile innerClassFile : innerClassFiles) {
            if (innerClassFile.isEnum()) {
                innerTypeDeclaration = convertEnumDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else if (innerClassFile.isAnnotation()) {
                innerTypeDeclaration = convertAnnotationDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else if (innerClassFile.isInterface()) {
                innerTypeDeclaration = convertInterfaceDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else {
                innerTypeDeclaration = convertClassDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            }

            list.add(innerTypeDeclaration);
        }
        return list;
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, ClassFile classFile) {
        Annotations visible = classFile.getAttribute(Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS);
        Annotations invisibles = classFile.getAttribute(Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS);

        AnnotationEntry[] visibleEntries = visible == null ? null : visible.getAnnotationEntries();
        AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();

        return converter.convert(visibleEntries, invisibleEntries);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, FieldOrMethod fieldOrMethod) {
        Annotations visible = fieldOrMethod.getAttribute(Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS);
        Annotations invisibles = fieldOrMethod.getAttribute(Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS);

        AnnotationEntry[] visibleEntries = visible == null ? null : visible.getAnnotationEntries();
        AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();

        return converter.convert(visibleEntries, invisibleEntries);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, RecordComponentInfo recordComponentInfo) {
        Annotations visible = (Annotations) Stream.of(recordComponentInfo.getAttributes()).filter(RuntimeVisibleAnnotations.class::isInstance).findAny().orElse(null);
        Annotations invisibles = (Annotations) Stream.of(recordComponentInfo.getAttributes()).filter(RuntimeInvisibleAnnotations.class::isInstance).findAny().orElse(null);

        AnnotationEntry[] visibleEntries = visible == null ? null : visible.getAnnotationEntries();
        AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();

        return converter.convert(visibleEntries, invisibleEntries);
    }

    protected ExpressionVariableInitializer convertFieldInitializer(Field field, Type typeField) {
        ConstantValue acv = field.getConstantValue();
        if (acv == null) {
            return null;
        }
        Constant constantValue = acv.getConstantPool().getConstant(acv.getConstantValueIndex());
        Expression expression = switch (constantValue.getTag()) {
            case Const.CONSTANT_Integer -> new IntegerConstantExpression(typeField, ((ConstantInteger)constantValue).getBytes());
            case Const.CONSTANT_Float -> new FloatConstantExpression(((ConstantFloat)constantValue).getBytes());
            case Const.CONSTANT_Long -> new LongConstantExpression(((ConstantLong)constantValue).getBytes());
            case Const.CONSTANT_Double -> new DoubleConstantExpression(((ConstantDouble)constantValue).getBytes());
            case Const.CONSTANT_String -> new StringConstantExpression(((ConstantString)constantValue).getBytes(acv.getConstantPool()));
            default -> throw new ConvertClassFileException("Invalid attributes");
        };
        return new ExpressionVariableInitializer(expression);
    }

    protected ModuleDeclaration convertModuleDeclaration(ClassFile classFile) {
        Module attributeModule = classFile.getAttribute(Const.ATTR_MODULE);
        final String[] usedClassNames = attributeModule.getUsedClassNames(classFile.getConstantPool(), false);
        List<ModuleDeclaration.ModuleInfo> requires = convertModuleRequiresToModuleInfo(attributeModule.getRequiresTable(), classFile.getConstantPool());
        List<ModuleDeclaration.PackageInfo> exports = convertModuleExportsToPackageInfo(attributeModule.getExportsTable(), classFile.getConstantPool());
        List<ModuleDeclaration.PackageInfo> opens = convertModuleOpensToPackageInfo(attributeModule.getOpensTable(), classFile.getConstantPool());
        DefaultList<String> uses = new DefaultList<>(usedClassNames);
        List<ModuleDeclaration.ServiceInfo> provides = convertModuleProvidesToServiceInfo(attributeModule.getProvidesTable(), classFile.getConstantPool());

        int moduleFlags = attributeModule.getModuleFlags();
        String moduleName = attributeModule.getModuleName(classFile.getConstantPool());
        String moduleVersion = attributeModule.getVersion(classFile.getConstantPool());
        return new ModuleDeclaration(
                moduleFlags, classFile.getInternalTypeName(), moduleName,
                moduleVersion, requires, exports, opens, uses, provides);
    }

    protected List<ModuleDeclaration.ModuleInfo> convertModuleRequiresToModuleInfo(ModuleRequires[] moduleRequires, ConstantPool constantPool) {
        if (moduleRequires == null || moduleRequires.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ModuleInfo> list = new DefaultList<>(moduleRequires.length);
        for (ModuleRequires moduleRequire : moduleRequires) {
            int requiresFlags = moduleRequire.getRequiresFlags();
            String moduleName = moduleRequire.getModuleName(constantPool);
            String version = moduleRequire.getVersion(constantPool);
            list.add(new ModuleDeclaration.ModuleInfo(moduleName, requiresFlags, version));
        }
        return list;
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleOpensToPackageInfo(ModuleOpens[] moduleOpens, ConstantPool constantPool) {
        if (moduleOpens == null || moduleOpens.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(moduleOpens.length);
        for (ModuleOpens moduleOpen : moduleOpens) {
            String[] toModuleNames = moduleOpen.getToModuleNames(constantPool);
            int opensFlags = moduleOpen.getOpensFlags();
            String packageName = moduleOpen.getPackageName(constantPool);
            list.add(new ModuleDeclaration.PackageInfo(packageName, opensFlags, new DefaultList<>(toModuleNames)));
        }
        return list;
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleExportsToPackageInfo(ModuleExports[] moduleExports, ConstantPool constantPool) {
        if (moduleExports == null || moduleExports.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(moduleExports.length);
        for (ModuleExports moduleExport : moduleExports) {
            String[] toModuleNames = moduleExport.getToModuleNames(constantPool);
            int exportsFlags = moduleExport.getExportsFlags();
            String packageName = moduleExport.getPackageName(constantPool);
			list.add(new ModuleDeclaration.PackageInfo(packageName, exportsFlags, new DefaultList<>(toModuleNames)));
        }
        return list;
    }

    protected List<ModuleDeclaration.ServiceInfo> convertModuleProvidesToServiceInfo(ModuleProvides[] moduleProvides, ConstantPool constantPool) {
        if (moduleProvides == null || moduleProvides.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ServiceInfo> list = new DefaultList<>(moduleProvides.length);
        for (ModuleProvides serviceInfo : moduleProvides) {
            String[] implementationClassNames = serviceInfo.getImplementationClassNames(constantPool, false);
            String interfaceName = serviceInfo.getInterfaceName(constantPool);
            list.add(new ModuleDeclaration.ServiceInfo(interfaceName, new DefaultList<>(implementationClassNames)));
        }
        return list;
    }
}
