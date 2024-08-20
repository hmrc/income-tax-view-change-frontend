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

package services.optIn

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import services.optIn.OptInPropositionSpec.TestData
import services.optIn.core._
import testUtils.UnitSpec

object OptInPropositionSpec {
  case class TestData(currentTaxYearStatus: ITSAStatus, nextTaxYearStatus: ITSAStatus, currentTaxYear: TaxYear, intent: String, offered: Seq[String])
}

class OptInPropositionSpec extends UnitSpec {

  val forYearEnd = 2023
  private val currentTaxYear = TaxYear.forYearEnd(forYearEnd)

  private val validTestCases = List(
    TestData(Mandated, Annual, currentTaxYear, "NY", Seq("NY")),
    TestData(Voluntary, Annual, currentTaxYear, "NY", Seq("NY")),
    TestData(Annual, Mandated, currentTaxYear, "CY", Seq("CY")),
    TestData(Annual, NoStatus, currentTaxYear, "CY", Seq("CY", "NY")),
    TestData(Annual, NoStatus, currentTaxYear, "NY", Seq("CY", "NY")),
    TestData(Annual, Voluntary, currentTaxYear, "CY", Seq("CY")),
    TestData(Annual, Annual, currentTaxYear, "CY", Seq("CY", "NY")),
    TestData(Annual, Annual, currentTaxYear, "NY", Seq("CY", "NY")),
  )

  validTestCases.foreach {
    case TestData(currentStatus, nextStatus, currentYear, intent, offered) =>
      val test = optOutPropositionOffersTest _
      test(currentStatus, nextStatus, currentYear)(intent)(offered)
  }

  def optOutPropositionOffersTest(currentTaxYearStatus: ITSAStatus, nextTaxYearStatus: ITSAStatus,
                                  currentTaxYear: TaxYear)(intent: String)(offered: Seq[String]): Unit = {

    s"offer years ${offered.toString}" when {
      s"intent is $intent" when {
        s"proposition is currentTaxYearStatus: ${currentTaxYearStatus.toString}, " +
          s"nextTaxYearStatus: ${nextTaxYearStatus.toString}, currentTaxYear: ${currentTaxYear.toString}" in {

          val expectedAvailableOptInYearsSize = offered.size
          val currentOptInTaxYear = CurrentOptInTaxYear(currentTaxYearStatus, currentTaxYear)
          val nextOptInTaxYear = NextOptInTaxYear(nextTaxYearStatus, currentTaxYear.nextYear, currentOptInTaxYear)
          val proposition = OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

          proposition.isOneYearOptIn shouldBe offered.size == 1
          proposition.isMultiYearOptIn shouldBe offered.size > 1
          proposition.isNoOptInAvailable shouldBe offered.isEmpty

          proposition.availableOptInYears.isEmpty shouldBe false
          proposition.availableOptInYears.size == expectedAvailableOptInYearsSize shouldBe true

          proposition.availableTaxYearsForOptIn.isEmpty shouldBe false
          proposition.availableTaxYearsForOptIn.size == expectedAvailableOptInYearsSize shouldBe true

          val assertOptInPropositionType: Seq[() => Boolean] = Seq(
            () => proposition.optInPropositionType.get.isInstanceOf[OneYearOptInProposition],
            () => proposition.optInPropositionType.get.isInstanceOf[MultiYearOptInProposition]
          )

          assertOptInPropositionType(offered.size - 1)() shouldBe true

          val yearCodes = proposition.availableOptInYears.map {
            case _:CurrentOptInTaxYear => "CY"
            case _ => "NY"
          }.sortBy(_.trim)

          yearCodes shouldBe offered.sortBy(_.trim)
          yearCodes.contains(intent) shouldBe true

        }
      }
    }
  }
}