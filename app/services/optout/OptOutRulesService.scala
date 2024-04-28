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

import models.itsaStatus.StatusDetail

import scala.io.Source
import scala.util.matching.Regex

case class Rule(ruleWithOption: String) {
  private val ruleWithoutOption: String = ruleWithOption.substring(0, ruleWithOption.lastIndexOf(","))
  def ruleWithoutOptionRegEx: Regex =  ruleWithoutOption.r
}

object OptOutRulesService {

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
  def toQuery(finalised: String,
              previousYear: String,
              currentYear: String,
              nextYear: String): String = {
    List(finalised, previousYear, currentYear, nextYear).mkString(",")
  }
}

class OptOutRulesService {

  /* todo: needs to be made testable! */
  private val fileLines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules.csv"))
    .getLines().toList

  def onlyRuleLines: List[String] = fileLines.filter(onlyRuleLinesFun)

  def findOptOutOptions(query: String): Set[String] = {

    val allMatched = onlyRuleLines
      .map(toRule)
      .filter(isMatch(_)(query))
      .map(rule => rule.ruleWithOption)

    allMatched.map {
      case optOutOptionRegex(outcome) => outcome
      case _ => "NO"
    }.toSet
  }

  private val optOutOptionRegex: Regex = """^.*?,.*?,.*?,.*?,(.*?)""".r
  private val onlyRuleLinesFun: String => Boolean = v => v.nonEmpty && !v.startsWith("-")
  private val toRule: String => Rule = v => Rule(v)
  private val isMatch: Rule => String => Boolean = r => query => r.ruleWithoutOptionRegEx.matches(query)
}