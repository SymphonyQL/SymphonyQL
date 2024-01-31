package symphony.parser

import scala.annotation.switch

/** The inverse of a `Parser` over some type A. A renderer can be used to render a value of type A to a string in either
 *  a regular or compact format.
 *
 *  For specializations actually relevant to graphql see [[symphony.parser.ValueRenderer]] and
 *  [[symphony.parser.DocumentRenderer]]
 */
trait SymphonyQLRenderer[-A] { self =>

  def render(a: A): String = {
    val sb = new StringBuilder
    unsafeRender(a, Some(0), sb)
    sb.toString()
  }

  def renderCompact(a: A): String = {
    val sb = new StringBuilder
    unsafeRender(a, None, sb)
    sb.toString()
  }

  /** Combines this renderer with another renderer sequentially. Semantically equivalent to `this andThen that`
   */
  def ++[A1 <: A](that: SymphonyQLRenderer[A1]): SymphonyQLRenderer[A1] = self match {
    case SymphonyQLRenderer.Combined(renderers) => SymphonyQLRenderer.Combined(renderers :+ that)
    case _                                      => SymphonyQLRenderer.Combined(List(self, that))
  }

  /** Contramaps the input of this renderer with the given function producing a renderer that now operates on type B
   */
  def contramap[B](f: B => A): SymphonyQLRenderer[B] = (value: B, indent: Option[Int], write: StringBuilder) =>
    self.unsafeRender(f(value), indent, write)

  /** Returns an optional renderer that will only render the value if it is defined
   */
  def optional: SymphonyQLRenderer[Option[A]] = (value: Option[A], indent: Option[Int], write: StringBuilder) =>
    value.foreach(self.unsafeRender(_, indent, write))

  /** Returns a renderer that renders a list of A where the underlying renderer is responsible for rendering the
   *  separator between each element.
   */
  def list: SymphonyQLRenderer[List[A]] =
    list(SymphonyQLRenderer.empty)

  /** Returns a renderer that renders a list of A but where the separator is rendered by provided argument renderer. The
   *  second parameter determines whether to print the separator before the first element or not.
   */
  def list[A1 <: A](separator: SymphonyQLRenderer[A1], omitFirst: Boolean = true): SymphonyQLRenderer[List[A1]] =
    (value: List[A1], indent: Option[Int], write: StringBuilder) => {
      var first = omitFirst
      value.foreach { v =>
        if (first) first = false
        else separator.unsafeRender(v, indent, write)
        self.unsafeRender(v, indent, write)
      }
    }

  /** Returns a renderer that renders a set of A but where the separator is rendered by provided argument renderer.
   */
  def set[A1 <: A](separator: SymphonyQLRenderer[A1]): SymphonyQLRenderer[Set[A1]] =
    (value: Set[A1], indent: Option[Int], write: StringBuilder) => {
      var first = true
      value.foreach { v =>
        if (first) first = false
        else separator.unsafeRender(v, indent, write)
        self.unsafeRender(v, indent, write)
      }
    }

  /** Returns a renderer that will only render when the provided predicate is true.
   */
  def when[A1 <: A](pred: A1 => Boolean): SymphonyQLRenderer[A1] =
    (value: A1, indent: Option[Int], write: StringBuilder) => if (pred(value)) self.unsafeRender(value, indent, write)

  /** Provides the actual unsafe rendering logic.
   */
  def unsafeRender(value: A, indent: Option[Int], write: StringBuilder): Unit
}

object SymphonyQLRenderer {

  def combine[A](renderers: SymphonyQLRenderer[A]*): SymphonyQLRenderer[A] =
    Combined(renderers.toList)

  /** A Renderer which always renders a single character.
   */
  def char(char: Char): SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    write.append(char)

  def comma: SymphonyQLRenderer[Any] = char(',')

  /** A Renderer which always renders a string.
   */
  def string(str: String): SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    write.append(str)

  /** A Renderer which simply renders the input string
   */
  lazy val string: SymphonyQLRenderer[String] = (value: String, indent: Option[Int], write: StringBuilder) =>
    write.append(value)

  lazy val escapedString: SymphonyQLRenderer[String] = new SymphonyQLRenderer[String] {

    override def unsafeRender(value: String, indent: Option[Int], write: StringBuilder): Unit =
      unsafeFastEscape(value, write)

    private def unsafeFastEscape(value: String, writer: StringBuilder): Unit = {
      var i = 0
      while (i < value.length) {
        (value.charAt(i): @switch) match {
          case '\\' => writer.append("\\\\")
          case '\b' => writer.append("\\b")
          case '\f' => writer.append("\\f")
          case '\n' => writer.append("\\n")
          case '\r' => writer.append("\\r")
          case '\t' => writer.append("\\t")
          case '"'  => writer.append("\\\"")
          case c    => writer.append(c)
        }
        i += 1
      }
    }
  }

  /** A Renderer which doesn't render anything.
   */
  lazy val empty: SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) => ()

  lazy val spaceOrEmpty: SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    if (indent.isDefined) write.append(' ')

  /** A Renderer which renders a newline character when in non-compact mode otherwise it renders a comma
   */
  lazy val newlineOrComma: SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    if (indent.isDefined) write.append('\n') else write.append(',')

  /** A Renderer which renders a newline character when in non-compact mode otherwise it renders a space
   */
  lazy val newlineOrSpace: SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    if (indent.isDefined) write.append('\n') else write.append(' ')

  lazy val newlineOrEmpty: SymphonyQLRenderer[Any] = (value: Any, indent: Option[Int], write: StringBuilder) =>
    if (indent.isDefined) write.append('\n')

  lazy val newline: SymphonyQLRenderer[Any] = char('\n')

  def map[K, V](
    keyRender: SymphonyQLRenderer[K],
    valueRender: SymphonyQLRenderer[V],
    separator: SymphonyQLRenderer[Any],
    delimiter: SymphonyQLRenderer[Any]
  ): SymphonyQLRenderer[Map[K, V]] = (value: Map[K, V], indent: Option[Int], write: StringBuilder) => {
    var first = true
    value.foreach { case (k, v) =>
      if (first) first = false
      else separator.unsafeRender((), indent, write)
      keyRender.unsafeRender(k, indent, write)
      delimiter.unsafeRender((), indent, write)
      valueRender.unsafeRender(v, indent, write)
    }
  }

  private final case class Combined[-A](renderers: List[SymphonyQLRenderer[A]]) extends SymphonyQLRenderer[A] {

    override def unsafeRender(value: A, indent: Option[Int], write: StringBuilder): Unit =
      renderers.foreach(_.unsafeRender(value, indent, write))
  }
}
