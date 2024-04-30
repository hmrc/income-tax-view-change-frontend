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

package services.optout

import services.optout.OptOutRulesService._

/* todo: to be removed */
object OptOutRulesMain extends App {

  val service = new OptOutRulesService()

  val queryStrings = List(
    toQuery("N", "V", "U", "U"),
    toQuery("N", "U", "V", "U"),
    toQuery("N", "U", "U", "V"),
    toQuery("N", "U", "V", "V"),

    toQuery("Y", "V", "U", "U"),
    toQuery("Y", "U", "V", "U"),
    toQuery("Y", "U", "U", "V"),
    toQuery("Y", "U", "V", "V"),
  )

  println()
  println("Rules In File:")
  service.onlyRuleLines.foreach(println)

  println()
  println("Queries:")
  queryStrings.map(q => q).foreach(q => println(s"${q}"))

  println()
  println("OptOut Options:")
  queryStrings.map(q => (q, service.findOptOutOptions(q).mkString(",")))
    .map(t => (t._1, if(t._2.isEmpty) "NO" else t._2))
    .foreach(t => println(s"${t._1} -> \t${t._2}"))
}
