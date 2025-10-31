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

package controllers.optIn.oldJourney

import controllers.ControllerISpecHelper
import controllers.optIn.oldJourney.ChooseTaxYearControllerISpec._
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import forms.optIn.ChooseTaxYearForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

object ChooseTaxYearControllerISpec {

  val headingText = "Voluntarily opting in to reporting quarterly"
  val descriptionText = "Opting in to the current tax year may result in you having overdue quarterly updates."
  val description2TextHeading = "Voluntarily reporting quarterly and overdue updates"
  val description2InsetText = "Because you would be voluntarily opted in, there would be no penalties for overdue quarterly updates."
  val description2Text = "If in future you are no longer voluntary and reporting quarterly is mandatory, then penalties would apply. We will send you a letter if reporting quarterly becomes mandatory for you."
  val dropdownHeading = "How to submit overdue updates"
  val dropdownInsetText1 = "A single quarterly update covers a 3-month period for one of your income sources. For example, if you have 2 income sources and have missed 2 quarters, you would have 4 overdue updates."
  val dropdownInsetText2 = "Depending on the compatible software you use, you may need to:"
  val dropdownBulletText1 = "submit each quarterly update separately in chronological order"
  val dropdownBulletText2 = "provide all your overdue updates as one submission"
  val taxYearChoiceOne = "2022 to 2023 onwards"
  val taxYearChoiceOneHint = "This could result in immediate overdue quarterly updates."
  val taxYearChoiceTwo = "2023 to 2024 onwards"
  val taxYearChoiceTwoHint = "There will be no quarterly updates to submit until then."
}

class ChooseTaxYearControllerISpec extends ControllerISpecHelper {

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/choose-tax-year"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the choose tax year page" in {
            enable(ReportingFrequencyPage, SignUpFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual)
            val result = buildGETMTDClient(path, additionalCookies).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(OK),
              elementTextByID("heading")(headingText),
              elementTextByID("description1")(descriptionText),
              elementTextByID("description2-heading")(description2TextHeading),
              elementTextByID("description2-insettext")(description2InsetText),
              elementTextByID("description2-text")(description2Text),
              elementTextBySelector("summary.govuk-details__summary:nth-child(1) > span:nth-child(1)")(dropdownHeading),
              elementTextByID("dropdown-insettext-p1")(dropdownInsetText1),
              elementTextByID("dropdown-insettext-p2")(dropdownInsetText2),
              elementTextByID("dropdown-insettext-p2-listitems1")(dropdownBulletText1),
              elementTextByID("dropdown-insettext-p2-listitems2")(dropdownBulletText2),
              elementTextBySelector("#whichTaxYear.govuk-fieldset legend.govuk-fieldset__legend.govuk-fieldset__legend--m")("Which tax year do you want to opt in from?"),
              elementTextBySelector("div.govuk-radios__item:nth-child(1) > label:nth-child(2)")(taxYearChoiceOne),
              elementTextByID("choice-year-0-item-hint")(taxYearChoiceOneHint),
              elementTextBySelector("div.govuk-radios__item:nth-child(2) > label:nth-child(2)")(taxYearChoiceTwo),
              elementTextByID("choice-year-1-item-hint")(taxYearChoiceTwoHint)
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
            enable(ReportingFrequencyPage, SignUpFs)
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual)

            val result = buildPOSTMTDPostClient(path, additionalCookies, body = formData).futureValue
            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            result should have(
              httpStatus(Status.SEE_OTHER),
              //todo add more asserts in MISUV-8006
            )
          }

          "return a BadRequest" when {
            "the form is invalid" in {
              enable(ReportingFrequencyPage, SignUpFs)
              val invalidFormData = Map(
                ChooseTaxYearForm.choiceField -> Seq()
              )
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
              setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary)
              val result = buildPOSTMTDPostClient(path, additionalCookies, body = invalidFormData).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

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
    await(repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString,
              currentYearStatus.toString,
              nextYearStatus.toString
            )), None)))))
  }

}