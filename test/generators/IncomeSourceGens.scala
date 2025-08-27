/*
 * Copyright 2023 HM Revenue & Customs
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

package generators

import forms.manageBusinesses.add.BusinessTradeForm
import org.scalacheck.Gen

import java.time.LocalDate
import scala.util.{Random, Try}

object IncomeSourceGens {

  private val businessNamePermittedCharacters = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) ++ Seq(' ', ',', '.', '&', '\'')

  private val businessTradePermittedCharacters = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) ++ Seq(' ', ',', '.', '&', '\'', '-')
  val alphabet = ('a' to 'z') ++ ('A' to 'Z')

  case class Day(day: String, month: String, year: String)

  val businessNameGenerator: Gen[List[Char]] = Gen.listOf(Gen.oneOf(businessNamePermittedCharacters))

  val businessTradeGenerator: Gen[List[Char]] = {
    for {
      body <- Gen.listOf(Gen.oneOf(businessTradePermittedCharacters))
      twoChars <- Gen.listOfN(2, Gen.oneOf(alphabet))
    } yield {
      val candidate = Random.shuffle(twoChars ++ body)
      if (candidate.length > BusinessTradeForm.MAX_LENGTH)
        candidate.take(BusinessTradeForm.MAX_LENGTH)
      else
        candidate
    }
  }

  val dateGenerator = (currentDate: LocalDate) => {
    for {
      day <- Gen.oneOf(1 to 5)
      month <- Gen.oneOf(1 to 12)
      year <- Gen.oneOf(1965 to 2075)
      if Try {
        LocalDate.of(year, month, day)
      }.toOption.isDefined
      if LocalDate.of(year, month, day).toEpochDay < currentDate.toEpochDay
    } yield Day(day.toString, month.toString, year.toString)
  }

}
