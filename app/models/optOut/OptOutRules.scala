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

import models.itsaStatus.StatusDetail

import scala.io.Source
import scala.util.matching.Regex

case class OptOutSymbol(code: String)
object OptOutSymbol {

  val FinalizedYes = OptOutSymbol("Y")
  val FinalizedNo = OptOutSymbol("N")
  val FinalizedAny = OptOutSymbol("_")

  val Unknown = OptOutSymbol("U")

  val Mandatory = OptOutSymbol("M")
  val Voluntary = OptOutSymbol("V")
  val Annual = OptOutSymbol("A")

  def toSymbol(statusDetail: StatusDetail): OptOutSymbol = {
    (statusDetail.isAnnual, statusDetail.isVoluntary, statusDetail.isMandated) match {
      case (true, false, false) => Annual
      case (false, true, false) => Voluntary
      case (false, false, true) => Mandatory
      case _ => Unknown
    }
  }

  def toFinalized(isFinalized: Boolean): OptOutSymbol = isFinalized match {
    case true => FinalizedYes
    case false => FinalizedNo
  }
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
}

object OptOutRules {
  val optOutOutcomeRegex: Regex = """^.*?,.*?,.*?,.*?,(.*?)""".r

  val rulesRegex: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules-regex.csv")).getLines()
    .toList

  def query(query: OptOutQuery): Set[OptOutOutcome] = {
    val queryString = query.asText()
    val allMatched = rulesRegex
      .map(v => (v.substring(0, v.lastIndexOf(",")), v))
      .filter(l => l._1.r.matches(queryString))
      .map(t => t._2)

    allMatched.map {
      case optOutOutcomeRegex(outcome) => OptOutOutcome.parse(outcome)
      case _ => OptOutOutcome.NoOptOut
    }.toSet
  }
}