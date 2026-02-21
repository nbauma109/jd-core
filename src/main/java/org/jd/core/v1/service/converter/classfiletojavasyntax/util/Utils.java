/*
 * Copyright (c) 2026 @nbauma109.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import java.util.Collection;
import java.util.Map;

import org.jd.core.v1.util.Base;

public class Utils {

	private Utils() {
	}

	public static <T> boolean isEmpty(Base<T> base) {
        return base == null || base.size() == 0;
    }

	public static <T> boolean isEmptyCollection(Collection<T> coll) {
		return coll == null || coll.isEmpty();
	}

	public static <K, V> boolean isEmpty(Map<K, V> map) {
		return map == null || map.isEmpty();
	}
	
    public static <T> boolean isSingleton(Base<T> base) {
        return base != null && !base.isList();
    }

    public static <T> boolean isList(Base<T> base) {
    	return base != null && base.isList();
    }
}
