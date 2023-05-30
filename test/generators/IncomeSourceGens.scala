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

import forms.incomeSources.add.BusinessTradeForm
import org.scalacheck.Gen

object IncomeSourceGens {

  private val businessNamePermittedCharacters = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) ++ Seq(' ', ',', '.', '&', '\'')


  //"^[A-Za-z0-9 ,.&'\\\\/-]+$".r
  private val businessTradePermittedCharacters = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) ++ Seq(' ', ',', '.', '&', '\'', '-')
  val alphabet = ('a' to 'z') ++ ('A' to 'Z')

  def containsTimes(s: List[Char], times: Int): Boolean = {
    val x = s.foldLeft(0) { (acc, char) => if (alphabet.contains(char)) acc + 1 else acc }
    println(s"Times: $x")
    x >= times
  }

  val businessNameGenerator: Gen[List[Char]] = Gen.listOf( Gen.oneOf(businessNamePermittedCharacters))

  val businessTradeGenerator: Gen[List[Char]] = {
    for {
      body <- Gen.listOf(Gen.oneOf(businessTradePermittedCharacters))
      twoChars <- Gen.listOfN(2, Gen.oneOf(alphabet))
    } yield {
      val candidate = twoChars ++ body
      if (candidate.length > BusinessTradeForm.maxLength)
        candidate.take(BusinessTradeForm.maxLength)
      else
        candidate
    }
  }

}
