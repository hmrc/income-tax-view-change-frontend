package fix

import metaconfig.Configured
import scalafix.v1._
import scala.meta._
import scalafix.patch.Patch

/*
   ScalaFix re-write rule:
    1. Future.successful(args) => args.asFuture
    2. Add required import to each document impacted;
 */
class FutureSuccessfulRule extends SemanticRule("Future.successful") {

  case class FutureErrorLn(position: Position) extends Diagnostic {
    def message = "Future.successful found ..."
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case x2@Term.Apply(select @ Term.Select( x@Term.Name("Future"),e), args) if e.toString == "successful" && !args.mkString("").contains("view") && !args.mkString("").contains("Ok") =>
        //println(s"Data: ${args.mkString("").contains("Ok")}")
        Patch.addGlobalImport(importer"utils.Utilities.ToFutureSuccessful") + Patch.replaceTree(select, " ") + Patch.addLeft(x2, "(") + Patch.addRight(x2, " ).asFuture ")
    }.asPatch
  }
}

