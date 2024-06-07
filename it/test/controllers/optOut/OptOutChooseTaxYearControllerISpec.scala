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

package controllers.optOut

import controllers.optOut.OptOutChooseTaxYearControllerISpec._
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class OptOutChooseTaxYearControllerISpec extends ComponentSpecBase {

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val nextTaxYear = currentTaxYear.nextYear
  val previousTaxYear = currentTaxYear.previousYear

  val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)

  def testHappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"calling GET $chooseOptOutTaxYearPageUrl" should {
      s"render page for choose multi-year opt-out tax-year $chooseOptOutTaxYearPageUrl" when {
        "User is authorised" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
          CalculationListStub.stubGetLegacyCalculationList(testNino,
            previousTaxYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, currentTaxYear.toFinancialYearStart, currentTaxYear.toFinancialYearEnd, allObligations)

          val result = IncomeTaxViewChangeFrontendManageBusinesses.renderChooseOptOutTaxYearPageInMultiYearJourney()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextByID("heading")(headingText),
            elementTextByID("description1")(description1Text),
            elementTextByID("description2")(description2Text),
              elementTextByID("radio-tax-year-label-3")(radioLabel3),
          )
        }
      }
    }

  }

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003"
        ))
    ),
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#004"
        ))
    )
  ))

  "OptOutChooseTaxYearController - Individual" when {
    testHappyCase(isAgent = false)
  }

  "OptOutChooseTaxYearController - Agent" when {
    testHappyCase(isAgent = true)
  }

}

object OptOutChooseTaxYearControllerISpec {

  val headingText = "Confirm you want to opt out of quarterly reporting"
  val description1Text = "You can opt out from any of the tax years available and report annually from that tax year onwards. This applies to all your businesses (even if they are less than 2 years old)."
  val description2Text = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val radioLabel3 = "2023 to 2024 onwards"
}