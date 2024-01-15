package fix

import metaconfig.Configured
import scalafix.v1._
import scala.meta._
import scalafix.patch.Patch

class ImportRewriteRules extends SemanticRule("ImportRewrite") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ importer"scala.concurrent.ExecutionContext.Implicits.{..$importees}" =>
        importees.foldLeft(Patch.empty)(_ + Patch.removeImportee(_))
        Patch.addGlobalImport(importer"play.api.libs.concurrent.Execution.Implicits.defaultContext")
      case _ =>
        Patch.empty
    }.asPatch
  }

}
