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

package controllers.agent.optOut

import connectors.optout.ITSAStatusUpdateConnector
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseFailure
import helpers.ITSAStatusUpdateConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
//import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.Mandated
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.{BAD_REQUEST, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmOptOutControllerISpec extends ComponentSpecBase {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))

  val isAgent: Boolean = true
  val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  val submitConfirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent).url
  val optOutErrorControllerUrl = controllers.optOut.routes.OptOutErrorController.show(isAgent).url

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)
  val taxableEntityId = "123"

  val expectedTitle = s"Confirm and opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year"
  val summary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val infoMessage = s"In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
  val emptyBodyString = ""

  val optOutExpectedTitle = s"Check your answers"
  val optOutSummary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val optOutWarning = "In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."

  s"calling GET $confirmOptOutPageUrl" should {
    s"render $confirmOptOutPageUrl" when {
      s"following year to opt-out year is $Mandated" when {
        "User is authorised" in {
          stubAuthorisedAgentUser(authorised = true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.NoStatus)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())


          val result = IncomeTaxViewChangeFrontend.getConfirmOptOut(clientDetailsWithConfirmation)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextByID("heading")(expectedTitle),
            elementTextByID("summary")(summary),
            elementTextByID("info-message")(infoMessage)
          )
        }
      }

      "in a multi year opt-out scenario" when {
        "User is authorised" in {
          stubAuthorisedAgentUser(authorised = true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          optOutService.saveIntent(TaxYear.forYearEnd(2024)).futureValue shouldBe true

          val result = IncomeTaxViewChangeFrontend.getConfirmOptOut(clientDetailsWithConfirmation)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            elementTextByID("heading")(optOutExpectedTitle),
            elementTextByID("optOut-summary")(optOutSummary),
            elementTextByID("optOut-warning")(optOutWarning)
          )
        }
      }
    }
  }

  s"calling POST $submitConfirmOptOutPageUrl" when {
    s"user confirms opt-out for one-year scenario" should {
      "show opt-out complete page" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString,
          Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
        )

        val result = IncomeTaxViewChangeFrontend.postConfirmOptOut(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER)
        )

      }
    }

    s"user confirms opt-out for one-year scenario and missing header" should {
      "show opt-out complete page" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString,
          Map("missing-header-name" -> "missing-header-value")
        )

        val result = IncomeTaxViewChangeFrontend.postConfirmOptOut(clientDetailsWithConfirmation)

        result should have(
          httpStatus(Status.SEE_OTHER)
        )

      }
    }

    s"user confirms opt-out for one-year scenario and update fails" should {
      "show Opt Out error page" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          BAD_REQUEST, Json.toJson(OptOutUpdateResponseFailure.defaultFailure()).toString(),
          Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
        )

        val result = IncomeTaxViewChangeFrontend.postConfirmOptOut(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(optOutErrorControllerUrl)
        )

      }
    }

    s"user confirms opt-out for multi-year scenario and update fails" should {
      "show Opt Out error page" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          BAD_REQUEST, Json.toJson(OptOutUpdateResponseFailure.defaultFailure()).toString(),
          Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
        )

        val result = IncomeTaxViewChangeFrontend.postConfirmOptOut(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(optOutErrorControllerUrl)
        )

      }
    }
  }
}
