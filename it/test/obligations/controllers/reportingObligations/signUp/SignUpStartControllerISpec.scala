/*
 * Copyright 2025 HM Revenue & Customs
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

package obligations.controllers.reportingObligations.signUp

import common.controllers.ControllerISpecHelper
import common.enums.JourneyType.{Opt, SignUpJourney}
import common.enums.MTDIndividual
import common.helpers.servicemocks.ITSAStatusDetailsStub
import common.models.admin.SignUpFs
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus
import helpers.servicemocks.IncomeTaxViewChangeStub
import obligations.models.reportingObligations.signUp.SignUpSessionData
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import common.testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import common.testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse
import shared.models.UIJourneySessionData
import shared.repositories.UIJourneySessionDataRepository

class SignUpStartControllerISpec extends ControllerISpecHelper {

  val forYearEnd = 2023
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear: TaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(isAgent: Boolean): String = {
    if (isAgent) "/agents/sign-up/start" else "/sign-up/start"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(isAgent)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the sign up start page" in {
            stubAuthorised(mtdUserRole, List(SignUpFs))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupsignUpSessionData(currentTaxYear)

            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              taxYear = currentTaxYear,
              `itsaStatusCY-1` = ITSAStatus.Annual,
              itsaStatusCY = ITSAStatus.Annual,
              `itsaStatusCY+1` = ITSAStatus.Annual
            )

            val result = buildGETMTDClient(s"$path?taxYear=2022", additionalCookies).futureValue

            result should have(
              httpStatus(OK),
              pageTitle(mtdUserRole, SignUpStartControllerISpec.title),
              elementTextByID("sign-up-start-heading")(SignUpStartControllerISpec.heading),
              elementTextByID("sign-up-start-description")(SignUpStartControllerISpec.description),
              elementTextByID("sign-up-inset")(SignUpStartControllerISpec.inset),
              elementTextByID("sign-up-reporting-quarterly-heading")(SignUpStartControllerISpec.reportingQuarterlyHeading),
              elementTextByID("sign-up-reporting-quarterly-text-1")(SignUpStartControllerISpec.reportingQuarterlyText),
              elementTextByID("sign-up-if-you-change-your-mind-heading")(SignUpStartControllerISpec.ifYouChangeYourMindHeading),
              elementTextByID("sign-up-if-you-change-your-mind-text")(SignUpStartControllerISpec.ifYouChangeYourMindText),
              elementTextByID("sign-up-continue-button")(SignUpStartControllerISpec.button)
            )
          }

          "render the sign up start page with CY only description" in {
            stubAuthorised(mtdUserRole, List(SignUpFs))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupsignUpSessionData(currentTaxYear)

            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              taxYear = currentTaxYear,
              `itsaStatusCY-1` = ITSAStatus.Annual,
              itsaStatusCY = ITSAStatus.Annual,
              `itsaStatusCY+1` = ITSAStatus.Mandated
            )

            val result = buildGETMTDClient(s"$path?taxYear=2022", additionalCookies).futureValue

            result should have(
              httpStatus(OK),
              elementTextByID("sign-up-reporting-quarterly-text-2")(SignUpStartControllerISpec.cyOnlyDesc)
            )
          }
        }

        "has already completed the sign-up journey (according to session data)" should {
          "redirect to the cannot go back page" in {
            stubAuthorised(mtdUserRole, List(SignUpFs))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            setupsignUpSessionData(currentTaxYear, journeyComplete = true)

            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              taxYear = currentTaxYear,
              `itsaStatusCY-1` = ITSAStatus.Annual,
              itsaStatusCY = ITSAStatus.Annual,
              `itsaStatusCY+1` = ITSAStatus.Mandated
            )
            val result = buildGETMTDClient(s"$path?taxYear=2022", additionalCookies).futureValue
            val redirectUrl: String = obligations.controllers.errors.routes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(true)).url

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(redirectUrl)
            )
          }
        }
      }
    }
  }

  private def setupsignUpSessionData(currentTaxYear: TaxYear, journeyComplete: Boolean = false): Unit = {
    await(repository.set(
      UIJourneySessionData(testSessionId,
        Opt(SignUpJourney).toString,
        signUpSessionData =
          Some(SignUpSessionData(None, Some(currentTaxYear.toString), journeyIsComplete = Some(journeyComplete))))))
  }
}

object SignUpStartControllerISpec {
  val title = "Signing up to Making Tax Digital for Income Tax"
  val heading = "Signing up to Making Tax Digital for Income Tax"
  val description = "This allows HMRC to give you a more precise forecast of how much tax you owe to help you budget more accurately."
  val inset = "If you voluntarily sign up, you will need software compatible with Making Tax Digital for Income Tax. There are both paid and free options for you or your agent to choose from."
  val reportingQuarterlyHeading ="Reporting quarterly"
  val reportingQuarterlyText = "Voluntarily signing up will mean you need to:"
  val bullet1 = "keep digital records of your sole trader and property income and expenses"
  val bullet2 = "submit an update every 3 months for each of these income sources"
  val bullet3 = "still file a tax return"
  val cyOnlyDesc = "If for this tax year you have already used software to submit income and expenses to HMRC, you will need to resubmit this information in your next quarterly update."
  val ifYouChangeYourMindHeading = "If you change your mind"
  val ifYouChangeYourMindText = "As you would be voluntarily signed up, you could decide at any time to opt out."
  val button = "Sign up"
}
