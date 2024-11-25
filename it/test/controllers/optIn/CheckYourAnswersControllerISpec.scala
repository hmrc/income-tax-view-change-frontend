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

package controllers.optIn

import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseFailure
import controllers.optIn.CheckYourAnswersControllerISpec._
import enums.JourneyType.{OptInJourney, Opt}
import helpers.servicemocks.IncomeTaxViewChangeStub
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.BAD_REQUEST
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

import scala.concurrent.Future

object CheckYourAnswersControllerISpec {
  val headingText = "Check your answers"
  val optInSummary = "Opting in will mean you need to submit your quarterly updates through compatible software."
  val optInSummaryNextYear = "If you opt in from the next tax year onwards, from 6 April 2023 you will need to submit " +
    "your quarterly updates through compatible software."
  val optin = "Opt in from"
  val selectTaxYear = "2022 to 2023 tax year onwards"
  val selectTaxYearNextYear = "2023 to 2024 tax year onwards"
  val change = "Change"

  val emptyBodyString = ""
}

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val forYearEnd = dateService.getCurrentTaxYear.endYear
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val itsaStatusUpdateConnector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def testShowHappyCase(isAgent: Boolean): Unit = {

    val chooseOptInTaxYearPageUrl = routes.CheckYourAnswersController.show(isAgent).url

    s"show page, calling GET $chooseOptInTaxYearPageUrl" should {

      s"successfully render opt-in check-your-answers page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent).futureValue shouldBe true

        val result = IncomeTaxViewChangeFrontendManageBusinesses.renderCheckYourAnswersOptInJourney()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(headingText),

          elementTextBySelector(".govuk-summary-list__key")(optin),
          elementTextBySelector(".govuk-summary-list__value")(selectTaxYear),
          elementTextBySelector("#change")(change),

          elementTextBySelector("#optIn-summary")(optInSummary),

          elementTextByID("confirm-button")("Confirm and save"),
          elementTextByID("cancel-button")("Cancel"),
        )
      }

      s"successfully render opt-in check-your-answers page 2" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val intent = currentTaxYear.nextYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent).futureValue shouldBe true

        val result = IncomeTaxViewChangeFrontendManageBusinesses.renderCheckYourAnswersOptInJourney()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(headingText),

          elementTextBySelector(".govuk-summary-list__key")(optin),
          elementTextBySelector(".govuk-summary-list__value")(selectTaxYearNextYear),
          elementTextBySelector("#change")(change),

          elementTextBySelector("#optIn-summary")(optInSummaryNextYear),

          elementTextByID("confirm-button")("Confirm and save"),
          elementTextByID("cancel-button")("Cancel"),
        )
      }
    }
  }

  def testSubmitHappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"submit page form, calling POST $chooseOptOutTaxYearPageUrl" should {
      s"successfully render opt-in check your answers page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitCheckYourAnswersOptInJourney()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(Status.SEE_OTHER),
          //todo add more asserts in MISUV-8007
        )
      }
    }
  }

  def testSubmitUnhappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = routes.CheckYourAnswersController.show(isAgent).url

    s"no tax-year choice is made and" when {
      s"submit page form, calling POST $chooseOptOutTaxYearPageUrl" should {
        s"show page again with error message" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary, currentTaxYear)

          ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
            BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString()
          )

          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitCheckYourAnswersOptInJourney()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(Status.SEE_OTHER),
          )
        }
      }
    }


  }

  "ChooseYearController - Individual" when {
    testShowHappyCase(isAgent = false)
    testSubmitHappyCase(isAgent = false)
    testSubmitUnhappyCase(isAgent = false)
  }

  "ChooseYearController - Agent" when {
    testShowHappyCase(isAgent = true)
    testSubmitHappyCase(isAgent = true)
    testSubmitUnhappyCase(isAgent = true)
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value,
                                    nextYearStatus: ITSAStatus.Value, intent: TaxYear): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString, statusToString(currentYearStatus),
              statusToString(nextYearStatus))), Some(intent.toString)))))
  }

}