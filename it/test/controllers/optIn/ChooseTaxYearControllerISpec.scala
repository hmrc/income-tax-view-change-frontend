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


import controllers.ControllerISpecHelper
import controllers.optIn.ChooseYearControllerISpec.{descriptionText, headingText, taxYearChoiceOne, taxYearChoiceTwo}
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import forms.optIn.ChooseTaxYearForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
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

object ChooseYearControllerISpec {
  val headingText = "Voluntarily opting in to reporting quarterly"
  val descriptionText = "If you opt in to the next tax year, you will not have to submit a quarterly update until then."
  val taxYearChoiceOne = "2022 to 2023 onwards"
  val taxYearChoiceTwo = "2023 to 2024 onwards"
}

class ChooseYearControllerISpec extends ControllerISpecHelper {

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/choose-tax-year"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the choose tax year page" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual)
            val result = buildGETMTDClient(path, additionalCookies).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),
              elementTextByID("description1")(descriptionText),
              elementTextBySelector("#whichTaxYear.govuk-fieldset legend.govuk-fieldset__legend.govuk-fieldset__legend--m")("Which tax year do you want to opt in from?"),
              elementTextBySelector("div.govuk-radios__item:nth-child(1) > label:nth-child(2)")(taxYearChoiceOne),
              elementTextBySelector("div.govuk-radios__item:nth-child(2) > label:nth-child(2)")(taxYearChoiceTwo),
            )
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        val formData: Map[String, Seq[String]] = Map(
          ChooseTaxYearForm.choiceField -> Seq(currentTaxYear.toString)
        )
        "is authenticated, with a valid enrolment" should {
          "redirect to check your answers page" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual)

            val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(Status.SEE_OTHER),
              //todo add more asserts in MISUV-8006
            )
          }

          "return a BadRequest" when {
            "the form is invalid" in {
              val invalidFormData = Map(
                ChooseTaxYearForm.choiceField -> Seq()
              )
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
              setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary)
              val result = buildPOSTMTDPostClient(path, additionalCookies, body = invalidFormData).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(Status.BAD_REQUEST),
                elementTextBySelector(".bold > a:nth-child(1)")("Select the tax year that you want to report quarterly from"),
                elementTextBySelector("#choice-error")("Error: Select the tax year that you want to report quarterly from")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(formData))
      }
    }
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value, nextYearStatus: ITSAStatus.Value): Unit = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString, statusToString(currentYearStatus), statusToString(nextYearStatus))), None))))
  }

}