/*
 * Copyright 2017 HM Revenue & Customs
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
package controllers.agent

import assets.BaseIntegrationTestConstants._
import audit.models.{ReportDeadlinesRequestAuditModel, ReportDeadlinesResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class NextUpdatesControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val incomeSourceDetails: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
      None, None, None, None, None, None, None, None,
      Some(getCurrentTaxYearEnd)
    )),
    property = None
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))),
    incomeSourceDetails, Some("1234567890"), None, Some("Agent"), Some("1")
  )(FakeRequest())

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  "Calling the NextUpdatesController" when {
    "the Agent viewer Feature is enabled" when {
      "the user is unauthorised" in {
        enable(AgentViewer)

        stubAuthorisedAgentUser(authorised = false)

        val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithoutConfirmation)

        Then("the page redirects to clients UTR page")
        res should have(
          httpStatus(SEE_OTHER)
          //          pageTitle("What is your client’s Unique Taxpayer Reference?") //need to add this check when page title is fixed
        )
      }
      "the user does not have confirmed client" in {
        enable(AgentViewer)

        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetails
        )

        val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithoutConfirmation)

        Then("the page redirects to clients UTR page")
        res should have(
          httpStatus(SEE_OTHER)
//          pageTitle("What is your client’s Unique Taxpayer Reference?") //need to add this check when page title is fixed
        )
      }

      "the user has obligations" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val currentObligations: ObligationsModel = ObligationsModel(Seq(
          ReportDeadlinesModel(
            identification = "testId",
            obligations = List(
              ReportDeadlineModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey")
            ))
        ))

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetails
        )

        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = currentObligations
        )

        val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyReportDeadlinesCall(testNino)

        Then("the next update view displays the correct title")
        res should have(
          httpStatus(OK),
          pageTitle("Next updates - Business Tax account - GOV.UK")
        )

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
      }

      "the user has no obligations" in {
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetails
        )

        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = ObligationsModel(Seq())
        )

        val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        IncomeTaxViewChangeStub.verifyGetReportDeadlines(testNino)

        Then("then Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
      }
    }
    "the Agent viewer Feature is disabled" when {
      "the user is trying to access next updates page when AgentViewer is disabled" in {
        disable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)

        val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

        Then(s"A not found page is returned to the user")
        res should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }
  }
}
