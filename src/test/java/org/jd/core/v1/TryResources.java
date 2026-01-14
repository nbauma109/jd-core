package org.jd.core.v1;

public abstract class TryResources<T extends AutoCloseable> {
    void tryResources() {
        try (T t = getResource()) {
            System.out.println(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract T getResource();
    
}
