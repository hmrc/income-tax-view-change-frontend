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

import scala.io.Source
import OptOutSymbol._

import scala.util.matching.Regex

case class OptOutSymbol(code: String)
object OptOutSymbol {

  val FinalizedYes = OptOutSymbol("Y")
  val FinalizedNo = OptOutSymbol("N")
  val FinalizedAny = OptOutSymbol("_")

  val Empty = OptOutSymbol("E")
  val Unknown = OptOutSymbol("U")

  val Mandatory = OptOutSymbol("M")
  val Voluntary = OptOutSymbol("V")
  val Annual = OptOutSymbol("A")
}

case class OptOutOutcome(code: String)
object OptOutOutcome {
  val PreviousYear = OptOutOutcome("PY")
  val CurrentYear = OptOutOutcome("CY")
  val NextYear = OptOutOutcome("NY")
  val NoOptOut = OptOutOutcome("NO")
  def parse(symbol: String): OptOutOutcome = symbol match {
    case "PY" => PreviousYear
    case "CY" => CurrentYear
    case "NY" => NextYear
  }
}

case class OptOutQuery(finalised: OptOutSymbol,
                       previousYear: OptOutSymbol,
                       currentYear: OptOutSymbol,
                       nextYear: OptOutSymbol) {
  def asText(): String = {
    List(finalised.code, previousYear.code, currentYear.code, nextYear.code).mkString(",")
  }
  def asText2(): String = {
    List(finalised.code, previousYear.code, currentYear.code, nextYear.code).mkString(",")
  }
}

object OptOutRules {
  val regex: Regex = """^.*?,.*?,.*?,.*?,(.*?)""".r
  private def splitYN(s: String): List[String] = if(s.startsWith("_")) List(s.replace("_", "Y"), s.replace("_", "N"), s) else List(s)
  val rules: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules.csv")).getLines()
    .flatMap(l => splitYN(l))
    .toList

  val rulesRegex: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules-regex.csv")).getLines()
    .toList

  def query(query: OptOutQuery): OptOutOutcome = {
    val matched = rules.find(l => l.startsWith(query.asText()))
    matched.map {
      case regex(outcome) => OptOutOutcome.parse(outcome)
      case _ => OptOutOutcome.NoOptOut
    }.getOrElse(OptOutOutcome.NoOptOut)
  }

  def query2(query: OptOutQuery): Set[OptOutOutcome] = {
    val queryString = query.asText()
    val allMatched = rulesRegex
      .map(v => (v.substring(0, v.lastIndexOf(",")), v))
//      .map(v => {
//        println(v._1)
//        v
//      })
      .filter(l => l._1.r.matches(queryString))
      .map(t => t._2)

    allMatched.map {
      case regex(outcome) => OptOutOutcome.parse(outcome)
      case _ => OptOutOutcome.NoOptOut
    }.toSet
  }


}
object SomeMain extends App {

  val queryStrings = List(
    OptOutQuery(FinalizedNo, Voluntary, Empty, Empty),
    OptOutQuery(FinalizedNo, Empty, Voluntary, Empty),
//
    OptOutQuery(FinalizedNo, Empty, Empty, Voluntary),
    OptOutQuery(FinalizedYes, Empty, Empty, Voluntary),
    OptOutQuery(FinalizedAny, Empty, Empty, Voluntary),

    OptOutQuery(FinalizedNo, Empty, Voluntary, Unknown),

    OptOutQuery(FinalizedNo, Voluntary, Voluntary, Voluntary)
  )

  println()
  println("Rules In File:")
  OptOutRules.rules.foreach(println)

  println()
  println("Query Expressions:")
  queryStrings.map(q => q.asText()).foreach(q => println(s"${q}"))

//  println()
//  println("Query Results:")
//  queryStrings.map(q => (q.asText(), OptOutRules.query(q).code)).foreach(t => println(s"${t._1} -> ${t._2}"))

  println()
  println("Query2 Results:")
  queryStrings.map(q => (q.asText(), OptOutRules.query2(q).map(r => r.code).mkString(","))).foreach(t => println(s"${t._1} -> \t${t._2}"))

  println()
  println("Key Definitions:")
  println("Y = FinalizedYes")
  println("N = FinalizedNo")
  println("_ = FinalizedAny")

  println("E = Empty")
  println("U = Unknown")

  println("M = Mandatory")
  println("V = Voluntary")
  println("A = Annual")

  println("PY = Can offer previous year")
  println("CY = Can offer current year")
  println("NY = Can offer next year")
  println("NO = No opt-out can be offered")


}
