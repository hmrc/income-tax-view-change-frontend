/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.newHomePage

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{NewHomePage, RecentActivity}
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.obligations.*
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testIncomeSource, testMtditid, testNino}
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2TradingStart}
import testConstants.NextUpdatesIntegrationTestConstants.currentDate

import java.time.LocalDate

class RecentActivityControllerISpec extends ControllerISpecHelper {

  val taxYearStartDate = LocalDate.of(2023, 4, 6)
  val taxYearEndDate = LocalDate.of(2024, 4, 5)
  val calendarStartDate = LocalDate.of(2023, 4, 1)
  val calendarEndDate = LocalDate.of(2023, 6, 30)

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/recent-activity"
  }

  def getTitle(mtdRole: MTDUserRole): String = {
    if (mtdRole == MTDIndividual) "home.heading.new" else "home.agent.heading"
  }

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(currentDate, currentDate.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd),
      Some(b2TradingStart),
      None,
      Some(CessationModel(Some(b2CessationDate))),
      address = Some(address)
    )),
    properties = Nil
  )

  object YourTasksViewMessages {
    val noActivityText = "You have no recent activity."

    val annualSubmissionLinkText = "View 2023 to 2024 tax year summary"
    val annualSubmissionContent = "You made an annual tax return submission."
    val annualDateContent = "Sent 6 March 2023"

    val quarterlySubmissionLinkText = "View your tax year summary"
    val quarterlySubmissionContent = "You submitted a quarterly update."
    val quarterlyDateContent = "Sent 6 March 2023"
  }

  def getTaxYearSummaryLink(mtdUserRole: MTDUserRole): String = {
    if (mtdUserRole == MTDIndividual) {
      controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(2024).url
    } else {
      controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(2024).url
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path for $mtdUserRole" when {
      "an authenticated user" should {
        "render the recent activity page" which {
          "displays the no activity card" when {
            if (mtdUserRole != MTDSupportingAgent) {
              "the user has no current tasks" in new TestSetup(mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("no-recent-activity-text")(YourTasksViewMessages.noActivityText)
                )
              }
            } else {
              "the user is a supporting agent" in new TestSetup(mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("no-recent-activity-text")(YourTasksViewMessages.noActivityText)
                )
              }
            }
          }
          if (mtdUserRole != MTDSupportingAgent) {
            "display submission recent activity cards" when {
              "the user has submitted their tax return within 90 days" in new TestSetup(obligationsModel = obligationsWithRecentAnnualSubmission, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.annualSubmissionLinkText),
                  elementAttributeBySelector("#recent-activity-card-heading-link-0", "href")(getTaxYearSummaryLink(mtdUserRole)),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.annualSubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.annualDateContent),
                )
              }

              "the user has submitted a quarterly update within 90 days" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlySubmission, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.quarterlySubmissionLinkText),
                  elementAttributeBySelector("#recent-activity-card-heading-link-0", "href")(getTaxYearSummaryLink(mtdUserRole)),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.quarterlySubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.quarterlyDateContent)
                )
              }

              "the user has submitted a quarterly update within 90 days and the income source is reporting for calendar periods" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlySubmissionCalendar, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.quarterlySubmissionLinkText),
                  elementAttributeBySelector("#recent-activity-card-heading-link-0", "href")(getTaxYearSummaryLink(mtdUserRole)),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.quarterlySubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.quarterlyDateContent)
                )
              }

              "the user submitted multiple tax returns and quarterly updates within 90 days" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlyAndAnnualSubmission, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.annualSubmissionLinkText),
                  elementAttributeBySelector("#recent-activity-card-heading-link-0", "href")(getTaxYearSummaryLink(mtdUserRole)),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.annualSubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.annualDateContent),
                  elementTextByID("recent-activity-card-heading-1")(YourTasksViewMessages.quarterlySubmissionLinkText),
                  elementTextByID("recent-activity-card-content-1")(YourTasksViewMessages.quarterlySubmissionContent),
                  elementTextByID("recent-activity-hint-1")(YourTasksViewMessages.quarterlyDateContent)
                )
              }
            }
          }
          if (mtdUserRole != MTDSupportingAgent) {
            "not display the quarterly updates card" when {
              "user's ITSA status is Annual" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlyAndAnnualSubmission, mtdUserRole = mtdUserRole, currentItsaStatus = ITSAStatus.Annual) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.annualSubmissionLinkText),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.annualSubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.annualDateContent),
                  elementTextByID("recent-activity-card-heading-1")(""),
                  elementTextByID("recent-activity-card-content-1")(""),
                  elementTextByID("recent-activity-hint-1")("")
                )
              }

              "user's ITSA status is Exempt" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlyAndAnnualSubmission, mtdUserRole = mtdUserRole, currentItsaStatus = ITSAStatus.Exempt) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.annualSubmissionLinkText),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.annualSubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.annualDateContent),
                  elementTextByID("recent-activity-card-heading-1")(""),
                  elementTextByID("recent-activity-card-content-1")(""),
                  elementTextByID("recent-activity-hint-1")("")
                )
              }

              "user's ITSA status is Digitally Exempt" in new TestSetup(obligationsModel = obligationsWithRecentQuarterlyAndAnnualSubmission, mtdUserRole = mtdUserRole, currentItsaStatus = ITSAStatus.DigitallyExempt) {
                enable(NewHomePage, RecentActivity)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("recent-activity-card-heading-0")(YourTasksViewMessages.annualSubmissionLinkText),
                  elementTextByID("recent-activity-card-content-0")(YourTasksViewMessages.annualSubmissionContent),
                  elementTextByID("recent-activity-hint-0")(YourTasksViewMessages.annualDateContent),
                  elementTextByID("recent-activity-card-heading-1")(""),
                  elementTextByID("recent-activity-card-content-1")(""),
                  elementTextByID("recent-activity-hint-1")("")
                )
              }
            }
          }
        }
      }
    }
  }

  class TestSetup(currentItsaStatus: ITSAStatus = ITSAStatus.Voluntary,
                  obligationsModel: ObligationsModel = noRecentObligationsModel,
                  mtdUserRole: MTDUserRole) {

    stubAuthorised(mtdUserRole)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(status = OK, response = incomeSourceDetailsModel)
    ITSAStatusDetailsStub.stubGetITSAStatusDetails(currentItsaStatus.toString, "2022-23")
    IncomeTaxViewChangeStub.stubGetFulfilledNextUpdates(nino = testNino, deadlines = obligationsModel)
  }

  private val noRecentObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(91)), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private val obligationsWithRecentAnnualSubmission: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Crystallisation", Some(currentDate.minusDays(30)), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private val obligationsWithRecentQuarterlySubmission: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(30)), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private val obligationsWithRecentQuarterlySubmissionCalendar: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(calendarStartDate, calendarEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(30)), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private val obligationsWithRecentQuarterlyAndAnnualSubmission: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(91)), "testPeriodKey", StatusFulfilled),
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(60)), "testPeriodKey", StatusFulfilled),
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Quarterly", Some(currentDate.minusDays(30)), "testPeriodKey", StatusFulfilled)
      )),
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Crystallisation", Some(currentDate.minusDays(30)), "testPeriodKey", StatusFulfilled),
        SingleObligationModel(taxYearStartDate, taxYearEndDate, currentDate, "Crystallisation", Some(currentDate.minusDays(60)), "testPeriodKey", StatusFulfilled)
      ))
  ))
}
