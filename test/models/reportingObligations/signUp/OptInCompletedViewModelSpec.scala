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

package models.reportingObligations.signUp

import models.incomeSourceDetails.TaxYear
import models.reportingObligations.signUp.OptInCompletedViewModel
import testUtils.UnitSpec

class OptInCompletedViewModelSpec extends UnitSpec {

  val endTaxYear = 2023
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(endTaxYear)

  "OptInCompletedViewModel" when {

    val optInTaxYear = currentTaxYear.nextYear

    ".startDateNextYear()" should {

      "return the correctly formatted date, formatting = d MMMM uuuu" in {

        val model =
          OptInCompletedViewModel(
            isAgent = false,
            optInTaxYear = optInTaxYear,
            isCurrentYear = true,
            showAnnualReportingAdvice = false,
            optInIncludedNextYear = false,
            annualWithFollowingYearMandated = false
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
              annualWithFollowingYearMandated = true
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
              annualWithFollowingYearMandated = false
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
              annualWithFollowingYearMandated = false
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
              annualWithFollowingYearMandated = false
            )

          model.headingMessageKey shouldBe "optin.completedOptIn.ny.heading.desc"
        }
      }
    }

    "the isAgent flag is true, the NextUpdate Agent url is set" in {

      val model =
        OptInCompletedViewModel(
          isAgent = true,
          optInTaxYear = optInTaxYear,
          isCurrentYear = true,
          showAnnualReportingAdvice = false,
          optInIncludedNextYear = false,
          annualWithFollowingYearMandated = true,
        )

      model.nextUpdatesLink shouldBe controllers.routes.NextUpdatesController.showAgent().url
    }

    "the isAgent flag is false, the non agent user NextUpdate url is set" in {

      val model =
        OptInCompletedViewModel(
          isAgent = false,
          optInTaxYear = optInTaxYear,
          isCurrentYear = true,
          showAnnualReportingAdvice = false,
          optInIncludedNextYear = false,
          annualWithFollowingYearMandated = true,
        )

      model.nextUpdatesLink shouldBe controllers.routes.NextUpdatesController.show().url
    }
  }
}
