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
import controllers.optOut.ConfirmedOptOutControllerSpec.emptyBodyString
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub}
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseFailure
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

object ConfirmedOptOutControllerSpec {
  val emptyBodyString = ""
}
class ConfirmedOptOutControllerSpec extends ComponentSpecBase {

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)
  val taxableEntityId = "123"

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

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

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

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

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

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

      result should have(
        httpStatus(INTERNAL_SERVER_ERROR),
      )

    }
  }

}
