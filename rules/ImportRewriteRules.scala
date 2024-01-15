package fix

import metaconfig.Configured
import scalafix.v1._
import scala.meta._
import scalafix.patch.Patch

class ImportRewriteRules extends SemanticRule("LocalDate.now") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    Patch.replaceSymbols(
      "scala.concurrent.ExecutionContext.Implicits.global" -> "play.api.libs.concurrent.Execution.Implicits.defaultContext",
    ) + Patch.addGlobalImport(importer"uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport")
  }
}
