package fix

import metaconfig.Configured
import scalafix.v1._
import scala.meta._

class CaseClassRewrite extends SemanticRule("CaseClassRewrite") {

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Defn.Class.After_4_6_0(a, className, c, constructorDef, extendParentClassOrTrait) if a.toString() == "List(case)" =>
        //println(s"$className ==> $extendParentClassOrTrait ===> $constructorDef")
        Patch.addLeft(t, "final ")
    }.asPatch
  }

}