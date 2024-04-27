/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.optOut

import OptOutSymbol._

object OptOutRulesMain extends App {

  val queryStrings = List(
    OptOutQuery(FinalizedNo, Voluntary, Unknown, Unknown),
    OptOutQuery(FinalizedNo, Unknown, Voluntary, Unknown),
    OptOutQuery(FinalizedNo, Unknown, Unknown, Voluntary),
    OptOutQuery(FinalizedNo, Unknown, Voluntary, Voluntary),
  )

  println()
  println("Rules In File:")
  OptOutRules.rulesRegex.foreach(println)

  println()
  println("Query Expressions:")
  queryStrings.map(q => q.asText()).foreach(q => println(s"${q}"))

  println()
  println("Query2 Results:")
  queryStrings.map(q => (q.asText(), OptOutRules.query(q).map(r => r.code).mkString(","))).foreach(t => println(s"${t._1} -> \t${t._2}"))

  println()
  println("Key Definitions:")
  println("Y = FinalizedYes")
  println("N = FinalizedNo")
  println("_ = FinalizedAny")

  println("U = Unknown")

  println("M = Mandatory")
  println("V = Voluntary")
  println("A = Annual")

  println("PY = Can offer previous year")
  println("CY = Can offer current year")
  println("NY = Can offer next year")
  println("NO = No opt-out can be offered")


}
