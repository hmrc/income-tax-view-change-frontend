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
import controllers.optIn.ConfirmTaxYearControllerISpec._
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.BAD_REQUEST
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse
import utils.OptInJourney

import scala.concurrent.Future

object ConfirmTaxYearControllerISpec {
  val headingText = "Confirm and opt in for 2022 to 2023 tax year"
  val desc = "Opting in will mean you need to submit your quarterly updates through compatible software."
  val text: String = "If you have submitted any income and expenses for this tax year to HMRC, this will be deleted from our records. " +
    "So make sure you keep hold of this information because you will need to include it in your quarterly updates."
  val emptyBodyString = ""
  val confirmButton = "Confirm and save"
  val cancelButton = "Cancel"
}

object ConfirmNextTaxYearMessages {
  val headingText = "Confirm and opt in from 2023 to 2024 tax year onwards"
  val desc = "If you opt in for the next tax year, from 6 April 2023 you will need to submit your quarterly updates through compatible software."
  val emptyBodyString = ""
  val confirmButton = "Confirm and save"
  val cancelButton = "Cancel"
}

class ConfirmTaxYearControllerISpec extends ComponentSpecBase {

  val forCurrentYearEnd: Int = dateService.getCurrentTaxYear.endYear
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forCurrentYearEnd)

  val forNextYearEnd: Int = forCurrentYearEnd + 1
  val nextTaxYear: TaxYear = TaxYear.forYearEnd(forNextYearEnd)

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val itsaStatusUpdateConnector: ITSAStatusUpdateConnector = app.injector.instanceOf[ITSAStatusUpdateConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def show(isAgent: Boolean): Unit = {

    val confirmTaxYearControllerURL = routes.ConfirmTaxYearController.show(isAgent).url

    s"show page, calling GET $confirmTaxYearControllerURL" should {

      s"successfully render opt-in confirm current tax year page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val intent = currentTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent).futureValue shouldBe true

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmTaxYear()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(headingText),

          elementTextByID("confirm-tax-year-desc")(desc),
          elementTextByID("insetText_confirmYear")(text),

          elementTextByID("confirm-button")(confirmButton),
          elementTextByID("cancel-button")(cancelButton),
        )
      }

      s"successfully render opt-in confirm next tax year page" in {
        beforeEach()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val intent = nextTaxYear
        setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, intent).futureValue shouldBe true

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getConfirmTaxYear()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          elementTextByID("heading")(ConfirmNextTaxYearMessages.headingText),

          elementTextByID("confirm-tax-year-desc")(ConfirmNextTaxYearMessages.desc),

          elementTextByID("confirm-button")(confirmButton),
          elementTextByID("cancel-button")(cancelButton),
        )
      }

    }
  }

  def submit(isAgent: Boolean): Unit = {

    val confirmTaxYearControllerURL = routes.ConfirmTaxYearController.show(isAgent).url

    s"submit page form, calling POST $confirmTaxYearControllerURL" should {

      s"successfully render opt-in confirm current tax year page" in {

        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(2023)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitConfirmTaxYear()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(Status.SEE_OTHER),
        )
      }

      s"successfully render opt-in confirm next tax year page" in {

        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(2023)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, intent = nextTaxYear)

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
          Status.NO_CONTENT, emptyBodyString
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitConfirmTaxYear()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(Status.SEE_OTHER),
        )
      }
    }
  }

  def submitError(isAgent: Boolean): Unit = {

    val confirmTaxYearControllerURL = routes.ConfirmTaxYearController.show(isAgent).url

    s"no tax-year choice is made and" when {

      s"submit page form, calling POST $confirmTaxYearControllerURL" should {

        s"show page again with error message" in {

          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(2023)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary, currentTaxYear)

          ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
            BAD_REQUEST, Json.toJson(ITSAStatusUpdateResponseFailure.defaultFailure()).toString()
          )

          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitConfirmTaxYear()
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(Status.SEE_OTHER),
          )
        }
      }
    }


  }

  "ChooseYearController - Individual" when {
    show(isAgent = false)
    submit(isAgent = false)
    submitError(isAgent = false)
  }

  "ChooseYearController - Agent" when {
    show(isAgent = true)
    submit(isAgent = true)
    submitError(isAgent = true)
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value,
                                    nextYearStatus: ITSAStatus.Value, intent: TaxYear): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        OptInJourney.Name,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString, statusToString(currentYearStatus),
              statusToString(nextYearStatus))), Some(intent.toString)))))
  }

}