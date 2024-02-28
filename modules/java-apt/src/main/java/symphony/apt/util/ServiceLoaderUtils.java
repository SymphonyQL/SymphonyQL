package symphony.apt.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

public final class ServiceLoaderUtils {

  private ServiceLoaderUtils() {
    throw new UnsupportedOperationException();
  }

  public static <T> Collection<T> load(final Class<T> beanClass) {
    final var classLoader = ServiceLoaderUtils.class.getClassLoader();
    final var loader = ServiceLoader.load(beanClass, classLoader);
    final var iterator = loader.iterator();

    final var list = new ArrayList<T>();
    while (iterator.hasNext()) {
      list.add(iterator.next());
    }
    return list;
  }
}
