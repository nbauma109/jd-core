package org.jd.core.v1;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import java.io.IOException;
import java.io.OutputStream;

public class Dup2X2ExampleCreator {
  private InstructionFactory _factory;
  private ConstantPoolGen    _cp;
  private ClassGen           _cg;

  public Dup2X2ExampleCreator() {
    _cg = new ClassGen("jd.core.test.Dup2X2Example", "java.lang.Object", "Dup2X2Example.java", Const.ACC_PUBLIC | Const.ACC_SUPER | Const.ACC_ABSTRACT, new String[] {  });
    _cg.setMajor(61);
    _cg.setMinor(0);

    _cp = _cg.getConstantPool();
    _factory = new InstructionFactory(_cg, _cp);
  }

  public void create(OutputStream out) throws IOException {
    createConstructor();
    createAbstractMethodForm1Result();
    createAbstractMethodForm2Result();
    createAbstractMethodForm3Result();
    createAbstractMethodForm4Result();
    createMethodForm1();
    createMethodForm2();
    createMethodForm3();
    createMethodForm4();
    _cg.getJavaClass().dump(out);
  }

  private void createConstructor() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<init>", "jd.core.test.Dup2X2Example", il, _cp);

    il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
    il.append(InstructionFactory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createAbstractMethodForm1Result() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PROTECTED | Const.ACC_ABSTRACT, Type.VOID, new Type[] { Type.INT, Type.INT, Type.INT, Type.INT, Type.INT, Type.INT }, new String[] { "arg0", "arg1", "arg2", "arg3", "arg4", "arg5" }, "form1Result", "jd.core.test.Dup2X2Example", il, _cp);

    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createAbstractMethodForm2Result() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PROTECTED | Const.ACC_ABSTRACT, Type.VOID, new Type[] { Type.LONG, Type.INT, Type.INT, Type.LONG }, new String[] { "arg0", "arg1", "arg2", "arg3" }, "form2Result", "jd.core.test.Dup2X2Example", il, _cp);

    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createAbstractMethodForm3Result() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PROTECTED | Const.ACC_ABSTRACT, Type.VOID, new Type[] { Type.INT, Type.INT, Type.LONG, Type.INT, Type.INT }, new String[] { "arg0", "arg1", "arg2", "arg3", "arg4" }, "form3Result", "jd.core.test.Dup2X2Example", il, _cp);

    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createAbstractMethodForm4Result() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PROTECTED | Const.ACC_ABSTRACT, Type.VOID, new Type[] { Type.LONG, Type.LONG, Type.LONG }, new String[] { "arg0", "arg1", "arg2" }, "form4Result", "jd.core.test.Dup2X2Example", il, _cp);

    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createMethodForm1() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, new Type[] { Type.INT, Type.INT, Type.INT, Type.INT }, new String[] { "arg0", "arg1", "arg2", "arg3" }, "form1", "jd.core.test.Dup2X2Example", il, _cp);

    il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
    il.append(InstructionFactory.createLoad(Type.INT, 1));
    il.append(InstructionFactory.createLoad(Type.INT, 2));
    il.append(InstructionFactory.createLoad(Type.INT, 3));
    il.append(InstructionFactory.createLoad(Type.INT, 4));
    il.append(InstructionFactory.createDup_2(2));
    il.append(_factory.createInvoke("jd.core.test.Dup2X2Example", "form1Result", Type.VOID, new Type[] { Type.INT, Type.INT, Type.INT, Type.INT, Type.INT, Type.INT }, Const.INVOKEVIRTUAL));
    il.append(InstructionFactory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createMethodForm2() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, new Type[] { Type.INT, Type.INT, Type.LONG }, new String[] { "arg0", "arg1", "arg2" }, "form2", "jd.core.test.Dup2X2Example", il, _cp);

    il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
    il.append(InstructionFactory.createLoad(Type.INT, 1));
    il.append(InstructionFactory.createLoad(Type.INT, 2));
    il.append(InstructionFactory.createLoad(Type.LONG, 3));
    il.append(InstructionFactory.createDup_2(2));
    il.append(_factory.createInvoke("jd.core.test.Dup2X2Example", "form2Result", Type.VOID, new Type[] { Type.LONG, Type.INT, Type.INT, Type.LONG }, Const.INVOKEVIRTUAL));
    il.append(InstructionFactory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createMethodForm3() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, new Type[] { Type.LONG, Type.INT, Type.INT }, new String[] { "arg0", "arg1", "arg2" }, "form3", "jd.core.test.Dup2X2Example", il, _cp);

    il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
    il.append(InstructionFactory.createLoad(Type.LONG, 1));
    il.append(InstructionFactory.createLoad(Type.INT, 3));
    il.append(InstructionFactory.createLoad(Type.INT, 4));
    il.append(InstructionFactory.createDup_2(2));
    il.append(_factory.createInvoke("jd.core.test.Dup2X2Example", "form3Result", Type.VOID, new Type[] { Type.INT, Type.INT, Type.LONG, Type.INT, Type.INT }, Const.INVOKEVIRTUAL));
    il.append(InstructionFactory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }

  private void createMethodForm4() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, new Type[] { Type.LONG, Type.LONG }, new String[] { "arg0", "arg1" }, "form4", "jd.core.test.Dup2X2Example", il, _cp);

    il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
    il.append(InstructionFactory.createLoad(Type.LONG, 1));
    il.append(InstructionFactory.createLoad(Type.LONG, 3));
    il.append(InstructionFactory.createDup_2(2));
    il.append(_factory.createInvoke("jd.core.test.Dup2X2Example", "form4Result", Type.VOID, new Type[] { Type.LONG, Type.LONG, Type.LONG }, Const.INVOKEVIRTUAL));
    il.append(InstructionFactory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
}
