/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.loader;

import org.jd.core.v1.api.loader.Loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CompositeLoader implements Loader {
    private final Loader primaryLoader;
    private final Loader fallbackLoader;

    public CompositeLoader(InputStream in) throws IOException {
        this(new ZipLoader(in), new ClassPathLoader());
    }

    public CompositeLoader(Loader primaryLoader, Loader fallbackLoader) {
        this.primaryLoader = primaryLoader;
        this.fallbackLoader = fallbackLoader;
    }

    @Override
    public boolean canLoad(String internalName) {
        return primaryLoader.canLoad(internalName) || fallbackLoader.canLoad(internalName);
    }

    @Override
    public byte[] load(String internalName) throws IOException {
        if (primaryLoader.canLoad(internalName)) {
            return primaryLoader.load(internalName);
        }
        return fallbackLoader.load(internalName);
    }

    public Map<String, byte[]> getMap() {
        if (primaryLoader instanceof ZipLoader zipLoader) {
            return zipLoader.getMap();
        }
        throw new UnsupportedOperationException("Primary loader does not expose zip entries");
    }
}
