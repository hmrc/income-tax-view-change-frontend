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

package controllers.optIn.newJourney

import controllers.ControllerISpecHelper
import controllers.optIn.oldJourney.ConfirmTaxYearControllerISpec.emptyBodyString
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.ITSAStatusUpdateConnectorStub
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{OptInOptOutContentUpdateR17, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

import scala.concurrent.Future

class SignUpTaxYearQuestionControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/sign-up"
  }

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  val forCurrentYearEnd: Int = dateService.getCurrentTaxYear.endYear
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forCurrentYearEnd)


  object signUpTaxYearQuestionMessages {
    val currentYearHeading = "Voluntarily signing up for the current tax year"
    val currentYearDesc = "Signing up to the current tax year could mean you would have at least one quarterly update overdue."
    val currentYearDesc2 = "The quarterly update deadlines are:"
    val currentYearBullet1 = "7 August 2022"
    val currentYearBullet2 = "7 November 2022"
    val currentYearBullet3 = "7 February 2023"
    val currentYearBullet4 = "7 May 2023"
    val currentYearSubheading = "Voluntarily signing up and overdue quarterly updates"
    val currentYearDesc3 = "Every 3 months an update is due for each of your property and sole trader income sources. Each quarterly update is a running total of income and expenses for the tax year so far."
    val currentYearDesc4 = "If you sign up to the current tax year and start now, the more likely you will have overdue updates. The later you sign up in the tax year, the more information you will need to submit."
    val currentYearInset = "Because you would be voluntarily signing up, there would be no penalties for overdue quarterly updates."
    val currentYearDesc5 = "If in future you are required to use Making Tax Digital for Income Tax, then penalties would apply. We will send you a letter if this happens."
    val currentYearQuestion = "Do you want to sign up for the current tax year?"
    val currentYearRadioHint = "Signing up could result in immediate overdue quarterly updates."
    val currentYearErrorMessage = "Select yes to sign up for the current tax year"

    val nextYearHeading = "Confirm and sign up from the 2023 to 2024 tax year onwards"
    val nextYearDesc = "If you sign up for the next tax year onwards, from 6 April 2023 you will need to submit your quarterly updates through software compatible with Making Tax Digital for Income Tax."
    val nextYearQuestion = "Do you want to sign up from the 2023 to 2024 tax year?"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"a user is a $mtdUserRole" that {
      "is authenticated with valid enrolment" should {
        "render the sign up tax year question page - CY onwards following annual" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear))

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.currentYearHeading),
            elementTextByID("sign-up-question-subheading")(signUpTaxYearQuestionMessages.currentYearSubheading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.currentYearDesc),
            elementTextByID("sign-up-question-desc-2")(signUpTaxYearQuestionMessages.currentYearDesc2),
            elementTextByID("sign-up-question-desc-3")(signUpTaxYearQuestionMessages.currentYearDesc3),
            elementTextByID("sign-up-question-desc-4")(signUpTaxYearQuestionMessages.currentYearDesc4),
            elementTextByID("sign-up-question-inset")(signUpTaxYearQuestionMessages.currentYearInset),
            elementTextByID("sign-up-question-desc-5")(signUpTaxYearQuestionMessages.currentYearDesc5),
            elementTextByID("quarterly-deadlines-1")(signUpTaxYearQuestionMessages.currentYearBullet1),
            elementTextByID("quarterly-deadlines-2")(signUpTaxYearQuestionMessages.currentYearBullet2),
            elementTextByID("quarterly-deadlines-3")(signUpTaxYearQuestionMessages.currentYearBullet3),
            elementTextByID("quarterly-deadlines-4")(signUpTaxYearQuestionMessages.currentYearBullet4),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.currentYearQuestion),
            elementTextByClass("govuk-hint")(signUpTaxYearQuestionMessages.currentYearRadioHint)
          )
        }

        "render the sign up tax year question page - CY onwards if CY+1 voluntary" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)


          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Voluntary, currentTaxYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.currentYearHeading),
            elementTextByID("sign-up-question-subheading")(signUpTaxYearQuestionMessages.currentYearSubheading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.currentYearDesc),
            elementTextByID("sign-up-question-desc-2")(signUpTaxYearQuestionMessages.currentYearDesc2),
            elementTextByID("sign-up-question-desc-3")(signUpTaxYearQuestionMessages.currentYearDesc3),
            elementTextByID("sign-up-question-desc-4")(signUpTaxYearQuestionMessages.currentYearDesc4),
            elementTextByID("sign-up-question-inset")(signUpTaxYearQuestionMessages.currentYearInset),
            elementTextByID("sign-up-question-desc-5")(signUpTaxYearQuestionMessages.currentYearDesc5),
            elementTextByID("quarterly-deadlines-1")(signUpTaxYearQuestionMessages.currentYearBullet1),
            elementTextByID("quarterly-deadlines-2")(signUpTaxYearQuestionMessages.currentYearBullet2),
            elementTextByID("quarterly-deadlines-3")(signUpTaxYearQuestionMessages.currentYearBullet3),
            elementTextByID("quarterly-deadlines-4")(signUpTaxYearQuestionMessages.currentYearBullet4),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.currentYearQuestion),
            elementTextByClass("govuk-hint")(signUpTaxYearQuestionMessages.currentYearRadioHint)
          )
        }

        "render the sign up tax year question page - CY onwards following mandated" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Mandated, currentTaxYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Mandated
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.currentYearHeading),
            elementTextByID("sign-up-question-subheading")(signUpTaxYearQuestionMessages.currentYearSubheading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.currentYearDesc),
            elementTextByID("sign-up-question-desc-2")(signUpTaxYearQuestionMessages.currentYearDesc2),
            elementTextByID("sign-up-question-desc-3")(signUpTaxYearQuestionMessages.currentYearDesc3),
            elementTextByID("sign-up-question-desc-4")(signUpTaxYearQuestionMessages.currentYearDesc4),
            elementTextByID("sign-up-question-inset")(signUpTaxYearQuestionMessages.currentYearInset),
            elementTextByID("sign-up-question-desc-5")(signUpTaxYearQuestionMessages.currentYearDesc5),
            elementTextByID("quarterly-deadlines-1")(signUpTaxYearQuestionMessages.currentYearBullet1),
            elementTextByID("quarterly-deadlines-2")(signUpTaxYearQuestionMessages.currentYearBullet2),
            elementTextByID("quarterly-deadlines-3")(signUpTaxYearQuestionMessages.currentYearBullet3),
            elementTextByID("quarterly-deadlines-4")(signUpTaxYearQuestionMessages.currentYearBullet4),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.currentYearQuestion),
            elementTextByClass("govuk-hint")(signUpTaxYearQuestionMessages.currentYearRadioHint)
          )
        }

        "render the sign up tax year question page - CY+1 onwards if CY is annual" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear.nextYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.nextYearHeading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.nextYearDesc),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.nextYearQuestion)
          )
        }

        "render the sign up tax year question page - CY+1 onwards if CY is mandated" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, currentTaxYear.nextYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Mandated,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.nextYearHeading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.nextYearDesc),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.nextYearQuestion)
          )
        }

        "render the sign up tax year question page - CY+1 onwards if CY is voluntary" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Annual, currentTaxYear.nextYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("sign-up-question-heading")(signUpTaxYearQuestionMessages.nextYearHeading),
            elementTextByID("sign-up-question-desc-1")(signUpTaxYearQuestionMessages.nextYearDesc),
            elementTextByClass("govuk-fieldset__legend--m")(signUpTaxYearQuestionMessages.nextYearQuestion)
          )
        }

        "redirect back to the reporting obligations page if the user has no annual CY or CY+1" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Voluntary, nextYearStatus = Voluntary, currentTaxYear))

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val reportingObligationsLink = mtdUserRole match {
            case MTDIndividual => "/report-quarterly/income-and-expenses/view/reporting-frequency"
            case _ => "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
          }

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(reportingObligationsLink)
          )
        }

        testAuthFailures(path, mtdUserRole)
      }

      "submit the answer to the sign up tax year question" in {
        val currentYear = "2022"
        val taxYear = TaxYear(2022, 2023)
        enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

        stubAuthorised(mtdUserRole)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
          taxYear = taxYear,
          `itsaStatusCY-1` = ITSAStatus.Annual,
          itsaStatusCY = ITSAStatus.Annual,
          `itsaStatusCY+1` = ITSAStatus.Annual
        )
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        ITSAStatusUpdateConnectorStub.stubItsaStatusUpdate(propertyOnlyResponse.nino,
          Status.NO_CONTENT, emptyBodyString
        )

        await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear))

        val result = buildPOSTMTDPostClient(s"$path?taxYear=$currentYear", additionalCookies, Map("sign-up-tax-year-question" -> Seq("Yes"))).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view$path/completed")
        )
      }

      "get an error message if the user incorrectly submits to the form" in {
        val currentYear = "2022"
        val taxYear = TaxYear(2022, 2023)
        enable(OptInOptOutContentUpdateR17, SignUpFs, ReportingFrequencyPage)

        stubAuthorised(mtdUserRole)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
          taxYear = taxYear,
          `itsaStatusCY-1` = ITSAStatus.Annual,
          itsaStatusCY = ITSAStatus.Annual,
          `itsaStatusCY+1` = ITSAStatus.Annual
        )
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = buildPOSTMTDPostClient(s"$path?taxYear=$currentYear", additionalCookies, Map("sign-up-tax-year-question" -> Seq(""))).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByClass("govuk-error-summary__title")("There is a problem"),
          elementTextByClass("govuk-error-summary__body")("Select yes to sign up for the current tax year")
        )
      }

      "has already completed the journey (according to session data)" should {
        "redirect to the cannot go back page" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Annual,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, currentTaxYear, journeyComplete = true))

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue
          val redirectUrl = {
            if(mtdUserRole != MTDIndividual)
              controllers.routes.SignUpOptOutCannotGoBackController.show(isAgent = true, isSignUpJourney = Some(true)).url
            else
            controllers.routes.SignUpOptOutCannotGoBackController.show(isAgent = false, isSignUpJourney = Some(true)).url
          }

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(redirectUrl)
          )
        }
      }
    }
  }

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value,
                                    nextYearStatus: ITSAStatus.Value, intent: TaxYear, journeyComplete: Boolean = false): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString,
              statusToString(status = currentYearStatus),
              statusToString(status = nextYearStatus))),
            Some(intent.toString),
            journeyIsComplete = Some(journeyComplete)
          ))))
  }
}
