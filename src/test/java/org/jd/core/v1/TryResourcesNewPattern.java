package org.jd.core.v1;

public abstract class TryResourcesNewPattern<T extends AutoCloseable> {
    void tryResources() {
        try (T t = getResource()) {
            System.out.println(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void tryResources(T t) {
        try (t) {
            System.out.println(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void tryResources(T t1, T t2) {
        try (t1; t2) {
            System.out.println(t1);
            System.out.println(t2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    void tryResourcesMixed(T t1, T t2) {
        try (t1; t2; T t = getResource()) {
            System.out.println(t1);
            System.out.println(t2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    abstract T getResource();
}
