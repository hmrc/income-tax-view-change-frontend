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

import connectors.optout.ITSAStatusUpdateConnector
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseFailure
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutControllerISpec extends ComponentSpecBase {
  val isAgent: Boolean = false
  val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  val submitConfirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent).url

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)
  val taxableEntityId = "123"

  val expectedTitle = s"Confirm and opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year"
  val summary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val infoMessage = s"In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
  val emptyBodyString = ""

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  s"calling GET $confirmOptOutPageUrl" should {

    s"render confirm single year opt out page $confirmOptOutPageUrl" when {
      "User is authorised" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmOptOut()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(expectedTitle),
          elementTextByID("summary")(summary),
          elementTextByID("info-message")(infoMessage),
        )
      }
    }

//    s"render confirm multi-year opt out page $confirmOptOutPageUrl" when {
//      "User is authorised" in {
//
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
//
//        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
//        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
//        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
//
//        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmOptOut()
//        verifyIncomeSourceDetailsCall(testMtditid)
//
//        result should have(
//          httpStatus(OK),
//          elementTextByID("heading")(expectedTitle),
//          //todo add more asserts as part of MISUV-7538
//        )
//      }
//    }
  }

  s"calling POST $submitConfirmOptOutPageUrl" when {
    s"user confirms opt-out for one-year scenario" should {
      "show opt-out complete page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString,
          Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(SEE_OTHER),
        )

      }
    }

    s"user confirms opt-out for one-year scenario and missing header" should {
      "show opt-out complete page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString,
          Map("missing-header-name" -> "missing-header-value")
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(Status.SEE_OTHER),
        )

      }
    }

    s"user confirms opt-out for one-year scenario and update fails" should {
      "show error page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          BAD_REQUEST, Json.toJson(OptOutUpdateResponseFailure.defaultFailure()).toString(),
          Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postConfirmOptOut()

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
        )

      }
    }
  }
}
