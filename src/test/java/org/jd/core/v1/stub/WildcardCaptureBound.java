package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class WildcardCaptureBound {

    public interface Watcher {
        void watch();
    }

    public static class PluginType {
        public Class<?> getPluginClass() {
            return null;
        }
    }

    public Watcher create(String name, PluginType pluginType) {
        return instantiate(name, (Class<? extends Watcher>) pluginType.getPluginClass());
    }

    public static <T extends Watcher> T instantiate(String name, Class<T> clazz) {
        return null;
    }
}
