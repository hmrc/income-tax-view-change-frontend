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

import scala.io.Source
import scala.util.matching.Regex

object OptOutRulesMain extends App {

  val service = new OptOutRulesService()

  private val queryLines: List[String] = Source.fromInputStream(getClass.getResourceAsStream("/optout-rules-queries.txt"))
    .getLines().toList

  private val optOutQueryVsOptionRegex: Regex = """^(.*?,.*?,.*?,.*?),(.*?)""".r
  var unmatched = 0
  queryLines.filter(_.nonEmpty).foreach {
    case optOutQueryVsOptionRegex(query, expectedOptOutOption) =>
      val expectedOptOutOptionFormat = expectedOptOutOption.split("-").sortBy(_.trim).mkString(",")
      val outcome = service.findOptOutOptions(query).mkString(",")

      if(outcome == expectedOptOutOptionFormat) {
        println(s"$query:-> $outcome")
      } else {
        unmatched = unmatched + 1
        println(s"$query:-> $outcome - did not match! query: $query found: $outcome, expected: $expectedOptOutOptionFormat")
      }
    case _ => println("Error")
  }

  println(s"----------------------")
  println(s"Unmatched: $unmatched")
  println(s"----------------------")
}
