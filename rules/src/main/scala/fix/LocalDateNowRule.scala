package fix

import metaconfig.Configured
import scalafix.v1._
import scala.meta._
import scalafix.patch.Patch

class LocalDateNowRule extends SemanticRule("LocalDate.now") {

  case class LocalDateLn(position: Position) extends Diagnostic {
    def message = "Dont use LocalDate.now in the code base"
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case now @ Term.Select(Term.Name("LocalDate"),e) if e.toString == "now" =>
        Patch.lint(LocalDateLn(now.pos))
    }.asPatch
  }
}