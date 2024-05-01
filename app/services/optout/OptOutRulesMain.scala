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

  val queryStrings: List[(String, String)] = List(
    (toQuery("N", "M", "M", "V"), "NY"),
    (toQuery("Y", "M", "M", "V"), "NY"),

    (toQuery("Y", "M", "V", "U"), "CY,NY"),
    (toQuery("N", "M", "V", "U"), "CY,NY"),

    (toQuery("Y", "M", "V", "V"), "CY,NY"),
    (toQuery("N", "M", "V", "V"), "CY,NY"),

    (toQuery("Y", "M", "V", "A"), "CY"),
    (toQuery("N", "M", "V", "A"), "CY"),

    (toQuery("Y", "M", "A", "V"), "NY"),
    (toQuery("N", "M", "A", "V"), "NY"),

    ("N,V,M,M", "PY"),
    ("N,V,M,U", "PY"),

    ("N,V,M,V", "NY,PY"),

    //("N,V,M,V", "PY"),
    ("Y,V,M,A", "PY"),

//    (toQuery("N", "V", "U", "U"), "PY"),
//    (toQuery("N", "U", "V", "U"), "CY,NY"),
//    (toQuery("N", "U", "U", "V"), "NY"),
//    (toQuery("N", "U", "V", "V"), ""),
//
//    (toQuery("Y", "V", "U", "U"), ""),
//    (toQuery("Y", "U", "V", "U"), "CY,NY"),
//    (toQuery("Y", "U", "U", "V"), "NY"),
//    (toQuery("Y", "U", "V", "V"), ""),
  )

//  println()
//  println("Rules In File:")
//  service.onlyRuleLines.foreach(println)

//  println()
//  println("Queries:")
//  queryStrings.foreach(q => println(s"$q"))

//  println()
//  println("OptOut Options:")
//  queryStrings.map(q => (q, service.findOptOutOptions(q).mkString(",")))
//    .map(t => (t._1, if(t._2.isEmpty) "NO" else t._2))
//    .foreach(t => println(s"${t._1} -> \t${t._2}"))


  queryStrings.foreach(t => {
    val outcome = service.findOptOutOptions(t._1).mkString(",")
    println(s"${t._1}:-> $outcome")
    assert(outcome == t._2, s"query: ${t._1} found: $outcome, expected: ${t._2}")
  })

}
