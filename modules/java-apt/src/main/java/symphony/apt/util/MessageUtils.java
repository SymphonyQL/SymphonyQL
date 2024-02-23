package symphony.apt.util;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import symphony.apt.context.ProcessorContextHolder;

public final class MessageUtils {

  private MessageUtils() {
    throw new UnsupportedOperationException();
  }

  public static void note(final String message) {
    message(Diagnostic.Kind.NOTE, message);
  }

  public static void error(final String message) {
    message(Diagnostic.Kind.ERROR, message);
  }

  public static void message(final Diagnostic.Kind type, final String message) {
    message(type, message, null);
  }

  public static void message(
      final Diagnostic.Kind type, final String message, final Element element) {
    final var messager = getMessager();
    messager.printMessage(type, message, element);
  }

  private static Messager getMessager() {
    final var env = ProcessorContextHolder.getProcessingEnvironment();
    return env.getMessager();
  }
}
