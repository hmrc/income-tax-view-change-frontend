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

import controllers.optIn.CheckYourAnswersControllerISpec._
import forms.optIn.ChooseTaxYearForm
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.mvc.Http.Status
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse
import utils.OptInJourney

import scala.concurrent.Future

object CheckYourAnswersControllerISpec {
  val headingText = "Check your answers"
  val optInSummary = "If you opt in, you will need to submit your quarterly update through compatible software."
  val optInSummaryNextYear = "If you opt in from the next tax year onwards, from 6 April 2025 you will need to submit " +
    "your quarterly updates through compatible software."
  val optin = "Opt in from"
  val selectTaxYear = "2024 to 2025 tax year onwards"
  val selectTaxYearNextYear = "2025 to 2026 tax year onwards"
  val change = "Change"
}

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val forYearEnd = 2025
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def testShowHappyCase(isAgent: Boolean): Unit = {

    val chooseOptInTaxYearPageUrl = routes.ChooseYearController.show(isAgent).url

    s"show page, calling GET $chooseOptInTaxYearPageUrl" should {

      s"successfully render opt-in multi choice page" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear).futureValue shouldBe true

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

      s"successfully render opt-in multi choice page 2" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear.nextYear).futureValue shouldBe true

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

        val formData: Map[String, Seq[String]] = Map(
          ChooseTaxYearForm.choiceField -> Seq(currentTaxYear.toString)
        )

        val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptInChooseTaxYear(formData)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(Status.SEE_OTHER),
          //todo add more asserts in MISUV-8006
        )
      }
    }
  }

  def testSubmitUnhappyCase(isAgent: Boolean): Unit = {

    val chooseOptOutTaxYearPageUrl = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

    s"no tax-year choice is made and" when {
      s"submit page form, calling POST $chooseOptOutTaxYearPageUrl" should {
        s"show page again with error message" in {

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary, currentTaxYear)

          val formData: Map[String, Seq[String]] = Map(
            ChooseTaxYearForm.choiceField -> Seq()
          )

          val result = IncomeTaxViewChangeFrontendManageBusinesses.submitChoiceOnOptInChooseTaxYear(formData)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(Status.BAD_REQUEST),
            elementTextBySelector(".bold > a:nth-child(1)")("Select the tax year that you want to report quarterly from"),
            elementTextBySelector("#choice-error")("Error: Select the tax year that you want to report quarterly from")
          )
        }
      }
    }


  }

  "ChooseYearController - Individual" when {
    testShowHappyCase(isAgent = false)
//    testSubmitHappyCase(isAgent = false)
//    testSubmitUnhappyCase(isAgent = false)
  }

  "ChooseYearController - Agent" when {
//    testShowHappyCase(isAgent = true)
//    testSubmitHappyCase(isAgent = true)
//    testSubmitUnhappyCase(isAgent = true)
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value, nextYearStatus: ITSAStatus.Value, intent: TaxYear): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        OptInJourney.Name,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString, statusToString(currentYearStatus), statusToString(nextYearStatus))), Some(intent.toString)))))
  }

}