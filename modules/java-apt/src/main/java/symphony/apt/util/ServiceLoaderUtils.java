package symphony.apt.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public final class ServiceLoaderUtils {

    private ServiceLoaderUtils() {
        throw new UnsupportedOperationException();
    }


    public static <T> Collection<T> load(final Class<T> beanClass) {
        final ClassLoader classLoader = ServiceLoaderUtils.class.getClassLoader();
        final ServiceLoader<T> loader = ServiceLoader.load(beanClass, classLoader);
        final Iterator<T> iterator = loader.iterator();

        final List<T> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

}
