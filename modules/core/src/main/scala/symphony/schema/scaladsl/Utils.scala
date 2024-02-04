package symphony.schema.scaladsl

import scala.compiletime.*
import scala.quoted.*

object Utils {
  inline def printTree[T](inline any: T): T                                    = ${ printDerived('any) }
  private def printDerived[T: Type](any: Expr[T])(using qctx: Quotes): Expr[T] = {
    import qctx.reflect.*
    println(Printer.TreeShortCode.show(any.asTerm))
    any
  }
}
