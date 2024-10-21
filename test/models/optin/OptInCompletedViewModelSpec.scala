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

package models.optin

import models.incomeSourceDetails.TaxYear
import testUtils.UnitSpec

class OptInCompletedViewModelSpec extends UnitSpec {

  val endTaxYear = 2023
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(endTaxYear)

  s"OptInCompletedViewModel - for year ${currentTaxYear.toString}" should {

    val optInTaxYear = currentTaxYear

    s"Individual" in {

      val model =
        OptInCompletedViewModel(
          isAgent = false,
          optInTaxYear = optInTaxYear,
          isCurrentYear = false,
          showAnnualReportingAdvice = false,
          optInIncludedNextYear = false,
          annualWithFollowingYearMandated = false,
          reportingFrequencyLink = "some link"
        )

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe false
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2024"
      model.annualWithFollowingYearMandated shouldBe false
      model.reportingFrequencyLink shouldBe "some link"
    }

    s"Individual with following year Voluntary" in {
      val model = OptInCompletedViewModel(
        isAgent = false,
        optInTaxYear = optInTaxYear,
        isCurrentYear = false,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = true,
        annualWithFollowingYearMandated = false,
        reportingFrequencyLink = "some link"
      )

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe false
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2024"
      model.optInIncludedNextYear shouldBe true
      model.annualWithFollowingYearMandated shouldBe false
      model.reportingFrequencyLink shouldBe "some link"
    }

    s"Individual with this year Annual and following year Mandatory" in {
      val model = OptInCompletedViewModel(
        isAgent = false,
        optInTaxYear = optInTaxYear,
        isCurrentYear = false,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = false,
        annualWithFollowingYearMandated = true,
        reportingFrequencyLink = "some link"
      )

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe false
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2024"
      model.optInIncludedNextYear shouldBe false
      model.annualWithFollowingYearMandated shouldBe true
      model.reportingFrequencyLink shouldBe "some link"
    }

    s"Agent" in {
      val model = OptInCompletedViewModel(
        isAgent = true,
        optInTaxYear = optInTaxYear,
        isCurrentYear = true,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = false,
        annualWithFollowingYearMandated = false,
        reportingFrequencyLink = "some link"
      )

      model.startYear shouldBe "2022"
      model.endYear shouldBe "2023"
      model.isAgent shouldBe true
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2024"
      model.annualWithFollowingYearMandated shouldBe false
      model.reportingFrequencyLink shouldBe "some link"
    }
  }

  s"OptInCompletedViewModel - for year ${currentTaxYear.nextYear.toString}" should {

    val optInTaxYear = currentTaxYear.nextYear

    s"Individual" in {
      val model = OptInCompletedViewModel(
        isAgent = false,
        optInTaxYear = optInTaxYear,
        isCurrentYear = true,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = false,
        annualWithFollowingYearMandated = false,
        reportingFrequencyLink = "some link"
      )

      model.startYear shouldBe "2023"
      model.endYear shouldBe "2024"
      model.isAgent shouldBe false
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2025"
      model.reportingFrequencyLink shouldBe "some link"
    }

    s"Agent" in {
      val model = OptInCompletedViewModel(
        isAgent = true,
        optInTaxYear = optInTaxYear,
        isCurrentYear = true,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = false,
        annualWithFollowingYearMandated = false,
        reportingFrequencyLink = "some link"
      )

      model.startYear shouldBe "2023"
      model.endYear shouldBe "2024"
      model.isAgent shouldBe true
      model.showAnnualReportingAdvice shouldBe false
      model.nextYear shouldBe "2025"
      model.reportingFrequencyLink shouldBe "some link"
    }
  }

  "OptInCompletedViewModel" when {

    val optInTaxYear = currentTaxYear.nextYear

    ".startDateNextYear()" should {

      "return the correctly formatted date" in {
        val model =
          OptInCompletedViewModel(
            isAgent = false,
            optInTaxYear = optInTaxYear,
            isCurrentYear = true,
            showAnnualReportingAdvice = false,
            optInIncludedNextYear = false,
            annualWithFollowingYearMandated = false,
            reportingFrequencyLink = "some link"
          )

        model.startDateNextYear shouldBe "6 April 2024"
      }
    }


    ".headingMessageKey()" when {

      "both isCurrentYear & annualWithFollowingYearMandated are true" should {

        "return the correct message key" in {
          val model =
            OptInCompletedViewModel(
              isAgent = false,
              optInTaxYear = optInTaxYear,
              isCurrentYear = true,
              showAnnualReportingAdvice = false,
              optInIncludedNextYear = false,
              annualWithFollowingYearMandated = true,
              reportingFrequencyLink = "some link"
            )

          model.headingMessageKey shouldBe "optin.completedOptIn.followingVoluntary.heading.desc"
        }
      }

      "both isCurrentYear & optInIncludedNextYear are true" should {

        "return the correct message key" in {
          val model =
            OptInCompletedViewModel(
              isAgent = false,
              optInTaxYear = optInTaxYear,
              isCurrentYear = true,
              showAnnualReportingAdvice = false,
              optInIncludedNextYear = true,
              annualWithFollowingYearMandated = false,
              reportingFrequencyLink = "some link"
            )

          model.headingMessageKey shouldBe "optin.completedOptIn.followingVoluntary.heading.desc"
        }
      }

      "isCurrentYear is true & showAnnualReportingAdvice is false" should {

        "return the correct message key" in {
          val model =
            OptInCompletedViewModel(
              isAgent = false,
              optInTaxYear = optInTaxYear,
              isCurrentYear = true,
              showAnnualReportingAdvice = false,
              optInIncludedNextYear = false,
              annualWithFollowingYearMandated = false,
              reportingFrequencyLink = "some link"
            )

          model.headingMessageKey shouldBe "optin.completedOptIn.cy.heading.desc"
        }
      }

      "all (isCurrentYear, showAnnualReportingAdvice, annualWithFollowingYearMandated) three content flags are false" should {

        "return the correct message key" in {
          val model =
            OptInCompletedViewModel(
              isAgent = false,
              optInTaxYear = optInTaxYear,
              isCurrentYear = false,
              showAnnualReportingAdvice = false,
              optInIncludedNextYear = false,
              annualWithFollowingYearMandated = false,
              reportingFrequencyLink = "some link"
            )

          model.headingMessageKey shouldBe "optin.completedOptIn.ny.heading.desc"
        }
      }
    }
  }
}
