/*
 * Copyright (c) 2026 @nbauma109.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.BaseType;

public class Utils {

    public static boolean isEmpty(BaseStatement statements) {
        return statements == null || statements.size() == 0;
    }

    public static boolean isEmptyStatements(Statements statements) {
    	return statements == null || statements.size() == 0;
    }
    
    public static boolean isEmpty(BaseExpression expression) {
        return expression == null || expression.size() == 0;
    }

	public static boolean isEmpty(BaseType type) {
        return type == null || type.size() == 0;
	}

    public static boolean isEmpty(BaseFormalParameter formalParameters) {
        return formalParameters == null || formalParameters.size() == 0;
    }

    public static <E> boolean isEmpty(Set<E> set) {
        return set == null || set.isEmpty();
    }

    public static <E> boolean isEmpty(List<E> list) {
    	return list == null || list.isEmpty();
    }
    
    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

}
