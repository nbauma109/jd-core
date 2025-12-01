package org.jd.core.v1;

import java.lang.StackWalker.StackFrame;
import java.util.stream.Stream;

public class LambdaStackWalker2 {
    public Class<?> getCallerClass1() {
        return StackWalker.getInstance().walk(Stream::findFirst).map(StackFrame::getDeclaringClass).orElse(null);
    }

    public Class<?> getCallerClass2() {
        return StackWalker.getInstance().walk(Stream::findFirst).map(StackWalker.StackFrame::getDeclaringClass).orElse(null);
    }
}