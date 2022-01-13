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

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse
import testConstants.messages.TaxDueSummaryMessages.{taxDueSummaryHeadingAgent, taxDueSummaryTitleAgent}
import audit.models.TaxCalculationDetailsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import controllers.agent.utils.SessionKeys
import enums.Crystallised
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalcDisplayModel, Calculation, CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

import java.time.{LocalDate, LocalDateTime}

class TaxDueSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None,
    multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some("1")
  )(FakeRequest())

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

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        Some(getCurrentTaxYearEnd)
      )
    )
  )

  "Calling the TaxDueSummaryController.showIncomeSummary(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Your client’s Income Tax details - GOV.UK")
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " +
      "feature switch TxMEventsApproved is enabled" should {
      "return the correct tax due page with a full Calculation" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        When(s"I call GET ${routes.TaxDueSummaryController.showTaxDueSummary(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(taxDueSummaryTitleAgent),
          elementTextBySelector("h1")(taxDueSummaryHeadingAgent)
        )

        val expectedCalculation = estimatedCalculationFullJson.as[Calculation]
        verifyAuditContainsDetail(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt).detail)
      }

      "return the correct tax due page with only gift aid Additional Charge in the Calculation" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = giftAidCalculationJson
        )

        When(s"I call GET ${routes.TaxDueSummaryController.showTaxDueSummary(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(taxDueSummaryTitleAgent),
          elementTextBySelector("h1")(taxDueSummaryHeadingAgent)
        )

        val expectedCalculation = giftAidCalculationJson.as[Calculation]
        verifyAuditContainsDetail(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt).detail)
      }

      "return the correct tax due page with only pensions savings Additional Charge in the Calculation" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = pensionSavingsCalculationJson
        )

        When(s"I call GET ${routes.TaxDueSummaryController.showTaxDueSummary(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(taxDueSummaryTitleAgent),
          elementTextBySelector("h1")(taxDueSummaryHeadingAgent)
        )

        val expectedCalculation = pensionSavingsCalculationJson.as[Calculation]
        verifyAuditContainsDetail(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt).detail)
      }

      "return the correct tax due page with only pensions lump sum Additional Charge in the Calculation" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = pensionLumpSumCalculationJson
        )

        When(s"I call GET ${routes.TaxDueSummaryController.showTaxDueSummary(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(taxDueSummaryTitleAgent),
          elementTextBySelector("h1")(taxDueSummaryHeadingAgent)
        )

        val expectedCalculation = pensionLumpSumCalculationJson.as[Calculation]
        verifyAuditContainsDetail(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt).detail)
      }

      "return the correct tax due page with a minimal Calculation" in {
        enable(TxmEventsApproved)

        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        When(s"I call GET ${routes.TaxDueSummaryController.showTaxDueSummary(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(taxDueSummaryTitleAgent),
          elementTextBySelector("h1")(taxDueSummaryHeadingAgent)
        )

        val expectedCalculation = estimatedCalculationFullJson.as[Calculation]
        verifyAuditContainsDetail(TaxCalculationDetailsResponseAuditModel(testUser, CalcDisplayModel("", 1, expectedCalculation, Crystallised), testYearInt).detail)
      }
    }
  }
}
