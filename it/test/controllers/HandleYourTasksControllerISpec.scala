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

package controllers

import enums.ChargeType.ITSA_NI
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import helpers.WiremockHelper
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{CreditsRefundsRepay, NewHomePage, PenaltiesAndAppeals}
import models.core.{AccountingPeriodModel, CessationModel}
import models.creditsandrefunds.CreditsModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.obligations.*
import play.api.http.Status.OK
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import testConstants.BaseIntegrationTestConstants.{testIncomeSource, testMtditid, testNino, testYear}
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2TradingStart}
import testConstants.NextUpdatesIntegrationTestConstants.currentDate
import java.time.LocalDate

class HandleYourTasksControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/your-tasks"
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

  val genericCharge = "4910"
  val lateSubmissionPenalty = "4027"
  val firstLatePaymentPenalty = "4028"

  def taskLink(id: String) = s"#$id > a"

  def submissionsLink(mtdUserRole: MTDUserRole) = if(mtdUserRole != MTDIndividual) "/report-quarterly/income-and-expenses/view/agents/submission-deadlines" else "/report-quarterly/income-and-expenses/view/submission-deadlines"
  def whatYouOweLink(mtdUserRole: MTDUserRole) = if(mtdUserRole != MTDIndividual) "/report-quarterly/income-and-expenses/view/agents/what-your-client-owes" else "/report-quarterly/income-and-expenses/view/what-you-owe"
  def lspAndLppLink(mtdUserRole: MTDUserRole, chargeId: String = "1040000123") = if(mtdUserRole != MTDIndividual) s"/report-quarterly/income-and-expenses/view/agents/tax-years/2018/charge?id=$chargeId" else s"/report-quarterly/income-and-expenses/view/tax-years/2018/charge?id=$chargeId"
  def lspTabLink(mtdUserRole: MTDUserRole) = if(mtdUserRole != MTDIndividual) "http://localhost:9185/view-penalty/self-assessment/agent#lspTab" else "http://localhost:9185/view-penalty/self-assessment#lspTab"
  def lppTabLink(mtdUserRole: MTDUserRole) = if(mtdUserRole != MTDIndividual) "http://localhost:9185/view-penalty/self-assessment/agent#lppTab" else "http://localhost:9185/view-penalty/self-assessment#lppTab"
  def moneyInYourAccountLink(mtdUserRole: MTDUserRole) = if(mtdUserRole != MTDIndividual) "/report-quarterly/income-and-expenses/view/agents/money-in-your-account" else "/report-quarterly/income-and-expenses/view/money-in-your-account"

  object YourTasksViewMessages {
    val noTasksHeading = "No tasks"
    val noTasksContent = "You have no tasks to complete at the moment."
    val noTasksSupportingAgentContent = "You have no tasks to complete at the moment. Note that your customer may have tasks that you are unable to see as a support agent."

    val overdueChargeHeading = "Check what you owe and make a payment"
    val overdueChargeContent = "You have an overdue amount of £100.00"
    val overdueChargeTag = "Was due 29 Mar 2018"

    val multipleOverdueChargesHeading = "Check what you owe and make a payment"
    val multipleOverdueChargesContent = "You have an overdue amount of £200.00"
    val multipleOverdueChargesTag = "Oldest charge due 29 Mar 2017"

    val overdueLspHeading = "View your late submission penalty"
    val overdueLspContent = "You have a late submission penalty."
    val overdueLspTag = "Was due 29 Mar 2018"

    val multipleOverdueLspHeading = "View your late submission penalties"
    val multipleOverdueLspContent = "You have 2 late submission penalties."
    val multipleOverdueLspTag = "Oldest penalty due 29 Mar 2017"

    val overdueLppHeading = "View your late payment penalty"
    val overdueLppContent = "You have a late payment penalty."
    val overdueLppTag = "Was due 29 Mar 2018"

    val multipleOverdueLppHeading = "View your late payment penalties"
    val multipleOverdueLppContent = "You have 2 late payment penalties."
    val multipleOverdueLppTag = "Oldest penalty due 29 Mar 2017"

    val upcomingChargeHeading = "Check what you owe and make a payment"
    val upcomingChargeContent = "You have an upcoming payment."
    val upcomingChargeTag = "Due 29 Mar 2100"

    val overdueAnnualSubmissionHeading = "View submission deadlines"
    val overdueAnnualSubmissionContent = "You have an overdue annual submission."
    val overdueAnnualSubmissionTag = "Was due 5 Apr 2021"

    val multipleOverdueAnnualSubmissionsHeading = "View submission deadlines"
    val multipleOverdueAnnualSubmissionsContent = "You have 2 overdue annual submissions."
    val multipleOverdueAnnualSubmissionsTag = "Oldest submission due 5 Apr 2021"

    val overdueQuarterlySubmissionHeading = "View submission deadlines"
    val overdueQuarterlySubmissionContent = "You have an overdue quarterly submission."
    val overdueQuarterlySubmissionTag = "Was due 5 Apr 2021"

    val multipleOverdueQuarterlySubmissionsHeading = "View submission deadlines"
    val multipleOverdueQuarterlySubmissionsContent = "You have 2 overdue quarterly submissions."
    val multipleOverdueQuarterlySubmissionsTag = "Oldest submission due 5 Apr 2021"

    val upcomingAnnualSubmissionHeading = "View submission deadlines"
    val upcomingAnnualSubmissionContent = "You have an upcoming annual submission deadline."
    val upcomingAnnualSubmissionTag = "Due 5 Apr 2123"

    val upcomingQuarterlySubmissionHeading = "View submission deadlines"
    val upcomingQuarterlySubmissionContent = "You have an upcoming quarterly submission deadline."
    val upcomingQuarterlySubmissionTag = "Due 5 Apr 2123"

    val moneyInYourAccountHeading = "Check for money in your account and claim a refund"
    val moneyInYourAccountContent = "You have £100.00 in your account."

    val overallOverdueChargeTag = "Was due 29 Mar 2017"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path for $mtdUserRole" when {
      "an authenticateduser" should {
        "render the your tasks page" which {
          "displays the no tasks card" when {
            if(mtdUserRole != MTDSupportingAgent) {
              "the user has no current tasks" in new TestSetup(mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("noTaskCard-heading")(YourTasksViewMessages.noTasksHeading),
                  elementTextByID("noTaskCard-content")(YourTasksViewMessages.noTasksContent)
                )
              }
            } else {
              "the user has tasks relating to penalties and financials but they are a supporting agent" in new TestSetup(mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("noTaskCard-heading")(YourTasksViewMessages.noTasksHeading),
                  elementTextByID("noTaskCard-content")(YourTasksViewMessages.noTasksSupportingAgentContent)
                )
              }
            }
          }

          "display overdue submissions task cards" when {
            "the user has an overdue annual submission" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueAnnualObligationsModel())), mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueAnnualSubmissionHeading),
                elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueAnnualSubmissionContent),
                elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueAnnualSubmissionTag),
                elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--red")(2) // NextUpdatesService fallsback to a default when there's no tax return date
              )
            }

            "the user has an overdue quarterly submission" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Mandated, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueQuarterlySubmissionHeading),
                elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueQuarterlySubmissionContent),
                elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueQuarterlySubmissionTag),
                elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--red")(1)
              )
            }

            "the user has multiple overdue annual submissions" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueAnnualObligationsModel(), overdueAnnualObligationsModel(currentDate.minusYears(1)))), mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueAnnualSubmissionsHeading),
                elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueAnnualSubmissionsContent),
                elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueAnnualSubmissionsTag),
                elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--red")(2) // NextUpdatesService fallsback to a default when there's no tax return date
              )
            }

            "the user has multiple overdue quarterly submissions" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel(), overdueQuarterlyObligationsModel(currentDate.minusYears(1)))), currentItsaStatus = ITSAStatus.Mandated, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueQuarterlySubmissionsHeading),
                elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueQuarterlySubmissionsContent),
                elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueQuarterlySubmissionsTag),
                elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--red")(1)
              )
            }
          }

          if(mtdUserRole != MTDSupportingAgent) {
            "display overdue penalties and financial task cards" when {
              "the user has an overdue charge" in new TestSetup(chargesJson = overdueChargeJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueChargeHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueChargeContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueChargeTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has multiple overdue charges" in new TestSetup(chargesJson = multipleOverdueChargeJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueChargesHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueChargesContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueChargesTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has an overdue late submission penalty" in new TestSetup(chargesJson = overdueLspJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueLspHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueLspContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueLspTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(lspAndLppLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has multiple overdue late submission penalties" in new TestSetup(chargesJson = multipleOverdueLspJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueLspHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueLspContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueLspTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(lspTabLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has an overdue late payment penalty" in new TestSetup(chargesJson = overdueLppJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueLppHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueLppContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueLppTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(lspAndLppLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has multiple overdue late payment penalties" in new TestSetup(chargesJson = multipleOverdueLppJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueLppHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueLppContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueLppTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(lppTabLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "the user has overdue charges & overdue submissions" in new TestSetup(
                obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel(), overdueAnnualObligationsModel())),
                chargesJson = overdueAllChargesJson,
                currentItsaStatus = ITSAStatus.Mandated, mtdUserRole = mtdUserRole) {

                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueChargeHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueChargeContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overallOverdueChargeTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-1")(YourTasksViewMessages.overdueLppHeading),
                  elementTextByID("overdueTaskCard-content-1")(YourTasksViewMessages.overdueLppContent),
                  elementTextByID("overdueTaskCard-tag-1")(YourTasksViewMessages.overallOverdueChargeTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-1"), "href")(lspAndLppLink(mtdUserRole, "1040000124")),
                  elementTextByID("overdueTaskCard-heading-2")(YourTasksViewMessages.overdueLspHeading),
                  elementTextByID("overdueTaskCard-content-2")(YourTasksViewMessages.overdueLspContent),
                  elementTextByID("overdueTaskCard-tag-2")(YourTasksViewMessages.overallOverdueChargeTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-2"), "href")(lspAndLppLink(mtdUserRole, "1040000122")),
                  elementTextByID("overdueTaskCard-heading-3")(YourTasksViewMessages.overdueAnnualSubmissionHeading),
                  elementTextByID("overdueTaskCard-content-3")(YourTasksViewMessages.overdueAnnualSubmissionContent),
                  elementTextByID("overdueTaskCard-tag-3")(YourTasksViewMessages.overdueAnnualSubmissionTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-3"), "href")(submissionsLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-4")(YourTasksViewMessages.overdueQuarterlySubmissionHeading),
                  elementTextByID("overdueTaskCard-content-4")(YourTasksViewMessages.overdueQuarterlySubmissionContent),
                  elementTextByID("overdueTaskCard-tag-4")(YourTasksViewMessages.overdueQuarterlySubmissionTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-4"), "href")(submissionsLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(6) // Needs an extra one due to the fallback caused by the discrepancy between the static and dynamic .now()
                )
              }
            }
          }

          "not display the overdue quarterly submissions cards" when {
            "user's ITSA status is Annual" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Annual, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(""),
                elementTextByID("overdueTaskCard-content-0")(""),
                elementTextByID("overdueTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--red")(0)
              )
            }

            "user's ITSA status is Exempt" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Exempt, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(""),
                elementTextByID("overdueTaskCard-content-0")(""),
                elementTextByID("overdueTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--red")(0)
              )
            }

            "user's ITSA status is Digitally Exempt" in new TestSetup(obligationsModel = ObligationsModel(Seq(overdueQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.DigitallyExempt, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("overdueTaskCard-heading-0")(""),
                elementTextByID("overdueTaskCard-content-0")(""),
                elementTextByID("overdueTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--red")(0)
              )
            }
          }

          if(mtdUserRole != MTDSupportingAgent) {
            "display dateless submission cards" when {
              "the user has money in their account" in new TestSetup(creditAmount = 100, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("datelessTaskCard-heading-0")(YourTasksViewMessages.moneyInYourAccountHeading),
                  elementTextByID("datelessTaskCard-content-0")(YourTasksViewMessages.moneyInYourAccountContent),
                  elementAttributeBySelector(taskLink("datelessTaskCard-heading-0"), "href")(moneyInYourAccountLink(mtdUserRole)),
                )
              }
            }
          }

          "not display the money in your account task" when {
            if(mtdUserRole != MTDSupportingAgent) {
              "the user has no money in their account" in new TestSetup(creditAmount = 0, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("datelessTaskCard-heading-0")(""),
                  elementTextByID("datelessTaskCard-content-0")("")
                )
              }

              "creditsRefundsRepayEnabled FS is disabled" in new TestSetup(creditAmount = 100, mtdUserRole = mtdUserRole) {
                enable(NewHomePage)
                disable(CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("datelessTaskCard-heading-0")(""),
                  elementTextByID("datelessTaskCard-content-0")("")
                )
              }
            } else {
              "user is a supporting agent" in new TestSetup(creditAmount = 100, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("datelessTaskCard-heading-0")(""),
                  elementTextByID("datelessTaskCard-content-0")("")
                )
              }
            }
          }

          if(mtdUserRole != MTDSupportingAgent) {
            "shows the due dates in the correct tag colour" when {
              "task is overdue" in new TestSetup(chargesJson = overdueLppJson, mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementCountBySelector(".govuk-tag--red")(1)
                )
              }

              "upcoming task is due today" in new TestSetup(chargesJson = upcomingChargeJson(LocalDate.now()), mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementCountBySelector(".govuk-tag--pink")(1)
                )
              }

              "upcoming task is due within 30 days" in new TestSetup(chargesJson = upcomingChargeJson(LocalDate.now().plusDays(30)), mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementCountBySelector(".govuk-tag--yellow")(1)
                )
              }

              "upcoming task is due in over 30 days" in new TestSetup(chargesJson = upcomingChargeJson(LocalDate.now().plusDays(31)), mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementCountBySelector(".govuk-tag--green")(1)
                )
              }
            }
          }

          "show upcoming submissions task cards" when {
            "user has an upcoming annual submission" in new TestSetup(obligationsModel = ObligationsModel(Seq(upcomingAnnualObligationsModel())), mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingAnnualSubmissionHeading),
                elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingAnnualSubmissionContent),
                elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingAnnualSubmissionTag),
                elementAttributeBySelector(taskLink("upcomingTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--green")(1)
              )
            }

            "user has an upcoming quarterly submission" in new TestSetup(obligationsModel = ObligationsModel(Seq(upcomingQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Voluntary, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingQuarterlySubmissionHeading),
                elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingQuarterlySubmissionContent),
                elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingQuarterlySubmissionTag),
                elementAttributeBySelector(taskLink("upcomingTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                elementCountBySelector(".govuk-tag--green")(1)
              )
            }
          }

          if(mtdUserRole != MTDSupportingAgent) {
            "show upcoming penalties and financials task cards" when {
              "user has an upcoming charge" in new TestSetup(chargesJson = upcomingChargeJson(), mtdUserRole = mtdUserRole) {
                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingChargeHeading),
                  elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingChargeContent),
                  elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingChargeTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--green")(1)
                )
              }

              "user has multiple upcoming submissions and upcoming charges" in new TestSetup(
                obligationsModel = ObligationsModel(Seq(upcomingAnnualObligationsModel(), upcomingQuarterlyObligationsModel())),
                currentItsaStatus = ITSAStatus.Voluntary,
                mtdUserRole = mtdUserRole,
                chargesJson = upcomingChargeJson()
              ) {

                enable(NewHomePage, CreditsRefundsRepay)
                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingChargeHeading),
                  elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingChargeContent),
                  elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingChargeTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementTextByID("upcomingTaskCard-heading-1")(YourTasksViewMessages.upcomingAnnualSubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-1")(YourTasksViewMessages.upcomingAnnualSubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-1")(YourTasksViewMessages.upcomingAnnualSubmissionTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-1"), "href")(submissionsLink(mtdUserRole)),
                  elementTextByID("upcomingTaskCard-heading-2")(YourTasksViewMessages.upcomingQuarterlySubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-2")(YourTasksViewMessages.upcomingQuarterlySubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-2")(YourTasksViewMessages.upcomingQuarterlySubmissionTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-2"), "href")(submissionsLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--green")(3)
                )
              }
            }
          }

          "not display the upcoming quarterly submissions cards" when {
            "user's ITSA status is Annual" in new TestSetup(obligationsModel = ObligationsModel(Seq(upcomingQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Annual, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("upcomingTaskCard-heading-0")(""),
                elementTextByID("upcomingTaskCard-content-0")(""),
                elementTextByID("upcomingTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--green")(0)
              )
            }
            "user's ITSA status is Exempt" in new TestSetup(obligationsModel = ObligationsModel(Seq(upcomingQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.Exempt, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("upcomingTaskCard-heading-0")(""),
                elementTextByID("upcomingTaskCard-content-0")(""),
                elementTextByID("upcomingTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--green")(0)
              )
            }

            "user's ITSA status is Digitally Exempt" in new TestSetup(obligationsModel = ObligationsModel(Seq(upcomingQuarterlyObligationsModel())), currentItsaStatus = ITSAStatus.DigitallyExempt, mtdUserRole = mtdUserRole) {
              enable(NewHomePage, CreditsRefundsRepay)
              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                elementTextByID("upcomingTaskCard-heading-0")(""),
                elementTextByID("upcomingTaskCard-content-0")(""),
                elementTextByID("upcomingTaskCard-tag-0")(""),
                elementCountBySelector(".govuk-tag--green")(0)
              )
            }
          }

          "display multiple cards" when {
            "the user has multiple submissions, charges and money in their account" in new TestSetup(
              obligationsModel = ObligationsModel(Seq(upcomingAnnualObligationsModel(), overdueAnnualObligationsModel(), overdueQuarterlyObligationsModel(), upcomingQuarterlyObligationsModel())),
              currentItsaStatus = ITSAStatus.Mandated,
              mtdUserRole = mtdUserRole,
              creditAmount = 100,
              chargesJson = multipleAllChargesJson) {

              enable(NewHomePage, CreditsRefundsRepay, PenaltiesAndAppeals)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              if(mtdUserRole == MTDSupportingAgent) {
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.overdueAnnualSubmissionHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.overdueAnnualSubmissionContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.overdueAnnualSubmissionTag),
                  elementTextByID("overdueTaskCard-heading-1")(YourTasksViewMessages.overdueQuarterlySubmissionHeading),
                  elementTextByID("overdueTaskCard-content-1")(YourTasksViewMessages.overdueQuarterlySubmissionContent),
                  elementTextByID("overdueTaskCard-tag-1")(YourTasksViewMessages.overdueQuarterlySubmissionTag),
                  elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingAnnualSubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingAnnualSubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingAnnualSubmissionTag),
                  elementTextByID("upcomingTaskCard-heading-1")(YourTasksViewMessages.upcomingQuarterlySubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-1")(YourTasksViewMessages.upcomingQuarterlySubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-1")(YourTasksViewMessages.upcomingQuarterlySubmissionTag),
                  elementCountBySelector(".govuk-tag--red")(2),
                  elementCountBySelector(".govuk-tag--green")(2)
                )
              } else {
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, getTitle(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-0")(YourTasksViewMessages.multipleOverdueChargesHeading),
                  elementTextByID("overdueTaskCard-content-0")(YourTasksViewMessages.multipleOverdueChargesContent),
                  elementTextByID("overdueTaskCard-tag-0")(YourTasksViewMessages.multipleOverdueChargesTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-0"), "href")(whatYouOweLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-1")(YourTasksViewMessages.multipleOverdueLppHeading),
                  elementTextByID("overdueTaskCard-content-1")(YourTasksViewMessages.multipleOverdueLppContent),
                  elementTextByID("overdueTaskCard-tag-1")(YourTasksViewMessages.multipleOverdueLppTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-1"), "href")(lppTabLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-2")(YourTasksViewMessages.multipleOverdueLspHeading),
                  elementTextByID("overdueTaskCard-content-2")(YourTasksViewMessages.multipleOverdueLspContent),
                  elementTextByID("overdueTaskCard-tag-2")(YourTasksViewMessages.multipleOverdueLspTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-2"), "href")(lspTabLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-3")(YourTasksViewMessages.overdueAnnualSubmissionHeading),
                  elementTextByID("overdueTaskCard-content-3")(YourTasksViewMessages.overdueAnnualSubmissionContent),
                  elementTextByID("overdueTaskCard-tag-3")(YourTasksViewMessages.overdueAnnualSubmissionTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-3"), "href")(submissionsLink(mtdUserRole)),
                  elementTextByID("overdueTaskCard-heading-4")(YourTasksViewMessages.overdueQuarterlySubmissionHeading),
                  elementTextByID("overdueTaskCard-content-4")(YourTasksViewMessages.overdueQuarterlySubmissionContent),
                  elementTextByID("overdueTaskCard-tag-4")(YourTasksViewMessages.overdueQuarterlySubmissionTag),
                  elementAttributeBySelector(taskLink("overdueTaskCard-heading-4"), "href")(submissionsLink(mtdUserRole)),
                  elementTextByID("datelessTaskCard-heading-0")(YourTasksViewMessages.moneyInYourAccountHeading),
                  elementTextByID("datelessTaskCard-content-0")(YourTasksViewMessages.moneyInYourAccountContent),
                  elementTextByID("upcomingTaskCard-heading-0")(YourTasksViewMessages.upcomingAnnualSubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-0")(YourTasksViewMessages.upcomingAnnualSubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-0")(YourTasksViewMessages.upcomingAnnualSubmissionTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-0"), "href")(submissionsLink(mtdUserRole)),
                  elementTextByID("upcomingTaskCard-heading-1")(YourTasksViewMessages.upcomingQuarterlySubmissionHeading),
                  elementTextByID("upcomingTaskCard-content-1")(YourTasksViewMessages.upcomingQuarterlySubmissionContent),
                  elementTextByID("upcomingTaskCard-tag-1")(YourTasksViewMessages.upcomingQuarterlySubmissionTag),
                  elementAttributeBySelector(taskLink("upcomingTaskCard-heading-1"), "href")(submissionsLink(mtdUserRole)),
                  elementCountBySelector(".govuk-tag--red")(5),
                  elementCountBySelector(".govuk-tag--green")(2)
                )
              }

            }
          }
        }
      }
    }
  }

  class TestSetup(currentItsaStatus: ITSAStatus = ITSAStatus.Annual,
                  creditAmount: BigDecimal = 0,
                  obligationsModel: ObligationsModel = noObligationsModel,
                  chargesJson: JsValue = noChargesModel(),
                  mtdUserRole: MTDUserRole) {

    val testCreditModel = CreditsModel(0.0, 0.0, 0.0, creditAmount, None, None, Nil)
    val response: String = Json.toJson(testCreditModel).toString()

    val url = s"/income-tax-view-change/$testNino/financial-details/credits/from/2022-04-06/to/2023-04-05"

    stubAuthorised(mtdUserRole)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(status = OK, response = incomeSourceDetailsModel)
    WiremockHelper.stubGet(url, OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, "2022-04-06", "2023-04-05")(OK, chargesJson)
    ITSAStatusDetailsStub.stubGetITSAStatusDetails(currentItsaStatus.toString, "2022-23")
    IncomeTaxViewChangeStub.stubGetNextUpdates(nino = testNino, deadlines = obligationsModel)
  }

  private val overdueChargeJson = baseChargesModel(genericCharge, "2018-03-29")
  private val overdueLspJson = baseChargesModel(lateSubmissionPenalty, "2018-03-29")
  private val overdueLppJson = baseChargesModel(firstLatePaymentPenalty, "2018-03-29")
  private val overdueAllChargesJson = baseChargesModel(genericCharge, "2017-03-29", Seq(baseDocument("2017-03-29", "1040000122"), baseDocument("2017-03-29", "1040000124")), Seq(baseFinancial(lateSubmissionPenalty, "2017-03-29", "1040000122"), baseFinancial(firstLatePaymentPenalty, "2017-03-29", "1040000124")))

  private val multipleOverdueChargeJson = baseChargesModel(genericCharge, "2018-03-29", Seq(baseDocument("2017-03-29")), Seq(baseFinancial(genericCharge, "2017-03-29")))
  private val multipleOverdueLspJson = baseChargesModel(lateSubmissionPenalty, "2018-03-29", Seq(baseDocument("2017-03-29")), Seq(baseFinancial(lateSubmissionPenalty, "2017-03-29")))
  private val multipleOverdueLppJson = baseChargesModel(firstLatePaymentPenalty, "2018-03-29", Seq(baseDocument("2017-03-29")), Seq(baseFinancial(firstLatePaymentPenalty, "2017-03-29")))

  private val multipleAllChargesJson = baseChargesModel(
    genericCharge,
    "2100-03-29",
    Seq(
      baseDocument("2017-03-29", "1040000122"),
      baseDocument("2017-03-29", "1040000124"),
      baseDocument("2017-03-29", "1040000125"),
      baseDocument("2017-03-29", "1040000126"),
      baseDocument("2017-03-29", "1040000127")),
    Seq(
      baseFinancial(genericCharge, "2017-03-29", "1040000122"),
      baseFinancial(firstLatePaymentPenalty, "2017-03-29", "1040000124"),
      baseFinancial(firstLatePaymentPenalty, "2017-03-29", "1040000125"),
      baseFinancial(lateSubmissionPenalty, "2017-03-29", "1040000126"),
      baseFinancial(lateSubmissionPenalty, "2017-03-29", "1040000127"),
    ))

  private def upcomingChargeJson(date: LocalDate = LocalDate.of(2100, 3, 29)) = baseChargesModel(genericCharge, date.toString())

  private def overdueAnnualObligationsModel(baseDate: LocalDate = currentDate.minusYears(2)) = baseObligationModel(baseDate, "Crystallisation")
  private def overdueQuarterlyObligationsModel(baseDate: LocalDate = currentDate.minusYears(2)) = baseObligationModel(baseDate, "Quarterly")

  private def upcomingAnnualObligationsModel(baseDate: LocalDate = currentDate.plusYears(100)) = baseObligationModel(baseDate, "Crystallisation")
  private def upcomingQuarterlyObligationsModel(baseDate: LocalDate = currentDate.plusYears(100)) = baseObligationModel(baseDate, "Quarterly")

  private val noObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", Some(currentDate), "testPeriodKey", StatusFulfilled)
      ))
  ))

  private def baseObligationModel(baseDate: LocalDate, obligationType: String): GroupedObligationsModel = GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(baseDate, baseDate, baseDate, obligationType, None, "testPeriodKey", StatusOpen)
      ))

  private def noChargesModel(): JsValue = Json.obj(
    "balanceDetails" -> Json.obj(
      "balanceDueWithin30Days" -> 1.00,
      "overDueAmount" -> 2.00,
      "balanceNotDuein30Days" -> 4.00,
      "totalBalance" -> 3.00
    ),
    "codingDetails" -> Json.arr(),
    "documentDetails" -> Json.arr(),
    "financialDetails" -> Json.arr()
  )

  private def baseChargesModel(mainTransaction: String,
                               date: String,
                               additionalDocuments: Seq[JsObject] = Seq.empty,
                               additionalFinancials: Seq[JsObject] = Seq.empty) = {

    Json.obj(
      "balanceDetails" -> Json.obj(
        "balanceDueWithin30Days" -> 1.00,
        "overDueAmount" -> 2.00,
        "balanceNotDuein30Days" -> 4.00,
        "totalBalance" -> 3.00
      ),
      "codingDetails" -> Json.arr(),
      "documentDetails" -> JsArray(Seq(baseDocument(date)) ++ additionalDocuments),
      "financialDetails" -> JsArray(Seq(baseFinancial(mainTransaction, date)) ++ additionalFinancials)
    )
  }

  private def baseDocument(date: String, transactionId: String = "1040000123") = Json.obj(
    "taxYear" -> 2018,
    "transactionId" -> transactionId,
    "documentDescription" -> "TRM New Charge",
    "documentText" -> "TRM New Charge",
    "outstandingAmount" -> 100,
    "originalAmount" -> 100,
    "documentDate" -> "2018-03-29",
    "interestFromDate" -> "2018-03-29",
    "interestEndDate" -> "2018-03-29",
    "interestRate" -> "3",
    "accruingInterestAmount" -> 100,
    "interestOutstandingAmount" -> 80.0,
    "effectiveDateOfPayment" -> "2018-03-29",
    "documentDueDate" -> date,
    "poaRelevantAmount" -> 0
  )

  private def baseFinancial(mainTransaction: String, date: String, transactionId: String = "1040000123") = Json.obj(
    "taxYear" -> "2018",
    "mainType" -> "SA Balancing Charge",
    "mainTransaction" -> mainTransaction,
    "transactionId" -> transactionId,
    "chargeType" -> ITSA_NI,
    "chargeReference" -> "ABCD1234",
    "originalAmount" -> 100,
    "items" -> Json.arr(
      Json.obj(
        "amount" -> 10000,
        "clearingDate" -> "2019-08-13",
        "dueDate" -> date
      )
    )
  )
}
