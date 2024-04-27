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

case class Rule(text: String) {
  def regExp: Regex =  text.substring(0, text.lastIndexOf(",")).r
}

object OptOutRulesSupport {

  def toSymbol(statusDetail: StatusDetail): String = {
    (statusDetail.isAnnual, statusDetail.isVoluntary, statusDetail.isMandated) match {
      case (true, false, false) => "A"
      case (false, true, false) => "V"
      case (false, false, true) => "M"
      case _ => "U"
    }
  }
  def toFinalized(isFinalized: Boolean): String = isFinalized match {
    case true => "Y"
    case false => "N"
  }
  def toCsv(finalised: String,
            previousYear: String,
            currentYear: String,
            nextYear: String): String = {
    List(finalised, previousYear, currentYear, nextYear).mkString(",")
  }
}

object OptOutRules {

  val optOutOptionRegex: Regex = """^.*?,.*?,.*?,.*?,(.*?)-.*?""".r

  val fileLines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules-regex.csv")).getLines()
    .toList

  def query(queryString: String): Set[String] = {
    val allMatched = fileLines
      .filter(l => !l.startsWith("-"))
      .map(v => Rule(v))
      .filter(rule => rule.regExp.matches(queryString))
      .map(rule => rule.text)

    allMatched.map {
      case optOutOptionRegex(outcome) => outcome
      case _ => "NO"
    }.toSet
  }
}