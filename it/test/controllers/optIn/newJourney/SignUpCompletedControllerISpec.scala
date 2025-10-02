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
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.UIJourneySessionData
import models.admin.{NavBarFs, OptInOptOutContentUpdateR17, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Mandated}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class SignUpCompletedControllerISpec extends ControllerISpecHelper {

  val forYearEnd = 2026
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/sign-up/completed"
  }

  object SignUpCompletedViewMessages {
    val currentYearCompletedHeading = "Sign up completed"
    val currentYearCompletedTitle = "Sign up completed"
    val currentYearCompletedPanelDesc = "You need to use Making Tax Digital for Income Tax from now on"
    val currentYearCompletedDesc = "You may have overdue updates for the 2025 to 2026 tax year. You must submit these updates with all required income and expenses through your compatible software."

    val currentYearYourRevisedDeadlinesHeading = "Your revised deadlines"
    val currentYearYourRevisedDeadlinesDesc = "Your revised deadlines will be available in the next few minutes."
    val currentYearYourRevisedDeadlinesDesc2 = "Even if they are not displayed right away on the updates and deadlines page, your account has been updated."
    val currentYearYourRevisedDeadlinesDesc3 = "You can decide at any time to opt out of Making Tax Digital for Income Tax for all your businesses on your reporting obligations page."

    val currentYearSubmitUpdatesHeading = "Submit updates in software"
    val currentYearSubmitUpdatesDesc = "For any tax year you are using Making Tax Digital for Income Tax, you need compatible software (opens in new tab)."

    val currentYearReportingObligationsHeading = "Your reporting obligations in the future"
    val currentYearReportingObligationsDesc = "You are now voluntarily signed up from the 2025 to 2026 tax year onwards, but in future you could be required to use Making Tax Digital for Income Tax if:"
    val currentYearReportingObligationsBullet1 = "HMRC lowers the income threshold for it"
    val currentYearReportingObligationsBullet2 = "you report an increase in your qualifying income in a tax return"
    val currentYearReportingObligationsInset = "For example, if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold in the 2025 to 2026 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2027."
    val currentYearReportingObligationsDesc2 = "If this happens, we will write to you to let you know."
    val currentYearReportingObligationsDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

    val currentYearReportingObligationsMandatedHeading = "Your reporting obligations from the next tax year onwards"
    val currentYearReportingObligationsMandatedDesc = "You have just voluntarily signed up from the 2025 to 2026 tax year."
    val currentYearReportingObligationsMandatedInset = "From 6 April 2026, you will be required to use Making Tax Digital for Income Tax."
    val currentYearReportingObligationsMandatedDesc2 = "This could be because:"
    val currentYearReportingObligationsMandatedBullet1 = "HMRC lowered the income threshold for it"
    val currentYearReportingObligationsMandatedBullet2 = "you reported an increase in your qualifying income in a tax return"
    val currentYearReportingObligationsMandatedDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

    val nextYearCompletedHeading = "Sign up completed"
    val nextYearCompletedTitle = "Sign up completed"
    val nextYearCompletedPanelDesc = "You need to use Making Tax Digital for Income Tax from the 2026 to 2027 tax year onwards"

    val nextYearYourRevisedDeadlinesHeading = "Your revised deadlines"
    val nextYearYourRevisedDeadlinesDesc = "Your deadlines for this business will be available in the next few minutes."
    val nextYearYourRevisedDeadlinesDesc2 = "Even if they are not displayed right away on the updates and deadlines page, your account has been updated."
    val nextYearYourRevisedDeadlinesDesc3 = "You can decide at any time to opt out of Making Tax Digital for Income Tax for all your businesses on your reporting obligations page."

    val nextYearSubmitUpdatesHeading = "Submit updates in software"
    val nextYearSubmitUpdatesDesc = "For any tax year you are using Making Tax Digital for Income Tax, you need compatible software (opens in new tab)."
    val nextYearSubmitUpdatesDescAnnual = "When you are opted out, you can find out here how to file your Self Assessment tax return (opens in new tab)."

    val nextYearReportingObligationsHeading = "Your reporting obligations in the future"
    val nextYearReportingObligationsDesc = "You have just chosen to sign up from the next tax year onwards, but in future you could have to use Making Tax Digital for Income Tax if:"
    val nextYearReportingObligationsBullet1 = "HMRC lowers the income threshold for it"
    val nextYearReportingObligationsBullet2 = "you report an increase in your qualifying income in a tax return"
    val nextYearReportingObligationsInset = "For example, if your income from self-employment or property, or both, exceeds the threshold in the 2026 to 2027 tax year, you would have to report quarterly from 6 April 2028."
    val nextYearReportingObligationsDesc2 = "If this happens, we will write to you to let you know."
    val nextYearReportingObligationsDesc3 = "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the completed page" that {
            "is for the current tax year (CY+1 not mandated)" in {
              enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessesAndPropertyIncome)


              val intent = currentTaxYear
              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, SignUpCompletedViewMessages.currentYearCompletedTitle),
                elementTextBySelector("h1")(SignUpCompletedViewMessages.currentYearCompletedHeading),
                elementTextByClass("govuk-panel__body")(SignUpCompletedViewMessages.currentYearCompletedPanelDesc),
                elementTextByID("overdue-updates-inset")(SignUpCompletedViewMessages.currentYearCompletedDesc),
                elementTextByID("your-revised-deadlines-heading")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesHeading),
                elementTextByID("your-revised-deadlines-desc")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc),
                elementTextByID("your-revised-deadlines-desc-2")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc2),
                elementTextByID("your-revised-deadlines-desc-3")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc3),
                elementTextByID("submit-updates-heading")(SignUpCompletedViewMessages.currentYearSubmitUpdatesHeading),
                elementTextByID("submit-updates-desc")(SignUpCompletedViewMessages.currentYearSubmitUpdatesDesc),
                elementTextByID("your-reporting-obligations-heading")(SignUpCompletedViewMessages.currentYearReportingObligationsHeading),
                elementTextByID("your-reporting-obligations-desc")(SignUpCompletedViewMessages.currentYearReportingObligationsDesc),
                elementTextByClass("govuk-list govuk-list--bullet")(SignUpCompletedViewMessages.currentYearReportingObligationsBullet1 + " " + SignUpCompletedViewMessages.currentYearReportingObligationsBullet2),
                elementTextByID("your-reporting-obligations-inset")(SignUpCompletedViewMessages.currentYearReportingObligationsInset),
                elementTextByID("your-reporting-obligations-desc-2")(SignUpCompletedViewMessages.currentYearReportingObligationsDesc2),
                elementTextByID("your-reporting-obligations-desc-3")(SignUpCompletedViewMessages.currentYearReportingObligationsDesc3)
              )
            }
            "is for the current tax year (CY+1 mandated)" in {
              enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessesAndPropertyIncome)


              val intent = currentTaxYear
              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Mandated, intent))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, SignUpCompletedViewMessages.currentYearCompletedTitle),
                elementTextBySelector("h1")(SignUpCompletedViewMessages.currentYearCompletedHeading),
                elementTextByClass("govuk-panel__body")(SignUpCompletedViewMessages.currentYearCompletedPanelDesc),
                elementTextByID("overdue-updates-inset")(SignUpCompletedViewMessages.currentYearCompletedDesc),
                elementTextByID("your-revised-deadlines-heading")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesHeading),
                elementTextByID("your-revised-deadlines-desc")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc),
                elementTextByID("your-revised-deadlines-desc-2")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc2),
                elementTextByID("your-revised-deadlines-desc-3")(SignUpCompletedViewMessages.currentYearYourRevisedDeadlinesDesc3),
                elementTextByID("submit-updates-heading")(SignUpCompletedViewMessages.currentYearSubmitUpdatesHeading),
                elementTextByID("submit-updates-desc")(SignUpCompletedViewMessages.currentYearSubmitUpdatesDesc),
                elementTextByID("your-reporting-obligations-ny-mandated-heading")(SignUpCompletedViewMessages.currentYearReportingObligationsMandatedHeading),
                elementTextByID("your-reporting-obligations-ny-mandated-desc")(SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc),
                elementTextByClass("govuk-list govuk-list--bullet")(
                  SignUpCompletedViewMessages.currentYearReportingObligationsMandatedBullet1 + " " +
                    SignUpCompletedViewMessages.currentYearReportingObligationsMandatedBullet2
                ),
                elementTextByID("your-reporting-obligations-ny-mandated-inset")(SignUpCompletedViewMessages.currentYearReportingObligationsMandatedInset),
                elementTextByID("your-reporting-obligations-ny-mandated-desc-2")(SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc2),
                elementTextByID("your-reporting-obligations-ny-mandated-desc-3")(SignUpCompletedViewMessages.currentYearReportingObligationsMandatedDesc3)
              )
            }
            "is for the next tax year" in {
              enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessesAndPropertyIncome)

              val intent = currentTaxYear.nextYear
              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual, intent))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, SignUpCompletedViewMessages.nextYearCompletedTitle),
                elementTextBySelector("h1")(SignUpCompletedViewMessages.nextYearCompletedHeading),
                elementTextByClass("govuk-panel__body")(SignUpCompletedViewMessages.nextYearCompletedPanelDesc),
                elementTextByID("your-revised-deadlines-heading")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesHeading),
                elementTextByID("your-revised-deadlines-inset")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc),
                elementTextByID("your-revised-deadlines-desc-2")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc2),
                elementTextByID("your-revised-deadlines-desc-3")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc3),
                elementTextByID("submit-updates-heading")(SignUpCompletedViewMessages.nextYearSubmitUpdatesHeading),
                elementTextByID("submit-updates-desc")(SignUpCompletedViewMessages.nextYearSubmitUpdatesDesc),
                elementTextByID("submit-updates-desc-2")(SignUpCompletedViewMessages.nextYearSubmitUpdatesDescAnnual),
                elementTextByID("your-reporting-obligations-heading")(SignUpCompletedViewMessages.nextYearReportingObligationsHeading),
                elementTextByID("your-reporting-obligations-desc")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc),
                elementTextByClass("govuk-list govuk-list--bullet")(SignUpCompletedViewMessages.nextYearReportingObligationsBullet1 + " " + SignUpCompletedViewMessages.nextYearReportingObligationsBullet2),
                elementTextByID("your-reporting-obligations-inset")(SignUpCompletedViewMessages.nextYearReportingObligationsInset),
                elementTextByID("your-reporting-obligations-desc-2")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc2),
                elementTextByID("your-reporting-obligations-desc-3")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc3)
              )
            }
            "is for the next tax year (CY is not annual)" in {
              enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessesAndPropertyIncome)

              val intent = currentTaxYear.nextYear
              await(setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Annual, intent))

              val result = buildGETMTDClient(path, additionalCookies).futureValue
              IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, SignUpCompletedViewMessages.nextYearCompletedTitle),
                elementTextBySelector("h1")(SignUpCompletedViewMessages.nextYearCompletedHeading),
                elementTextByClass("govuk-panel__body")(SignUpCompletedViewMessages.nextYearCompletedPanelDesc),
                elementTextByID("your-revised-deadlines-heading")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesHeading),
                elementTextByID("your-revised-deadlines-inset")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc),
                elementTextByID("your-revised-deadlines-desc-2")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc2),
                elementTextByID("your-revised-deadlines-desc-3")(SignUpCompletedViewMessages.nextYearYourRevisedDeadlinesDesc3),
                elementTextByID("submit-updates-heading")(SignUpCompletedViewMessages.nextYearSubmitUpdatesHeading),
                elementTextByID("submit-updates-desc")(SignUpCompletedViewMessages.nextYearSubmitUpdatesDesc),
                elementTextByID("your-reporting-obligations-heading")(SignUpCompletedViewMessages.nextYearReportingObligationsHeading),
                elementTextByID("your-reporting-obligations-desc")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc),
                elementTextByClass("govuk-list govuk-list--bullet")(SignUpCompletedViewMessages.nextYearReportingObligationsBullet1 + " " + SignUpCompletedViewMessages.nextYearReportingObligationsBullet2),
                elementTextByID("your-reporting-obligations-inset")(SignUpCompletedViewMessages.nextYearReportingObligationsInset),
                elementTextByID("your-reporting-obligations-desc-2")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc2),
                elementTextByID("your-reporting-obligations-desc-3")(SignUpCompletedViewMessages.nextYearReportingObligationsDesc3)
              )

              result shouldNot have(
                elementTextByID("submit-updates-desc-2")(SignUpCompletedViewMessages.nextYearSubmitUpdatesDescAnnual)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
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
