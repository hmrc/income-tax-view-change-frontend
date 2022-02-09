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
import testConstants.messages.HomeMessages.{agentTitle, title}
import audit.models.{HomeAudit, NextUpdatesResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditDoesNotContainsDetail}
import helpers.servicemocks.IncomeTaxViewChangeStub
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.OutstandingChargesIntegrationTestConstants._
import uk.gov.hmrc.auth.core.retrieve.Name
import java.time.LocalDate

import helpers.servicemocks.AuthStub.{titleInternalServer, titleTechError}

class HomeControllerISpec extends ComponentSpecBase with FeatureSwitching {

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

  val testArn: String = "1"

  import implicitDateFormatter.longDate

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
		yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
			None,
      Some(getCurrentTaxYearEnd)
    )),
    property = None
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsModel,
		None, Some("1234567890"), None, Some("Agent"), Some(testArn)
  )(FakeRequest())


	s"GET ${routes.HomeController.show().url}" should {
		s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
			"the user is not authenticated" in {
				stubAuthorisedAgentUser(authorised = false)

				val result: WSResponse = IncomeTaxViewChangeFrontend.getAgentHome()

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

				val result: WSResponse = IncomeTaxViewChangeFrontend.getAgentHome()

				Then(s"Technical difficulties are shown with status OK")
				result should have(
					httpStatus(OK),
					pageTitleAgent(titleInternalServer)
				)
			}
		}
		s"return $SEE_OTHER" when {
			"the agent does not have client details in session" in {
				stubAuthorisedAgentUser(authorised = true)

				val result: WSResponse = IncomeTaxViewChangeFrontend.getAgentHome()

				result should have(
					httpStatus(SEE_OTHER),
					redirectURI(routes.EnterClientsUTRController.show().url)
				)
			}
			"the agent has client details in session but no confirmation flag" in {
				stubAuthorisedAgentUser(authorised = true)

				val result: WSResponse = IncomeTaxViewChangeFrontend.getAgentHome()

				result should have(
					httpStatus(SEE_OTHER),
					redirectURI(routes.EnterClientsUTRController.show().url)
				)
			}
		}
	}

	s"GET ${routes.HomeController.show().url}" when {
		"retrieving the client's income sources was successful" when {
			"retrieving the client's obligations was successful" when {
				"retrieving the client's charges was successful" should {
					"display the page with the next upcoming payment and charge" when {
						"there are payments upcoming and nothing is overdue with TxmEventsApproved FS enabled" in {

							stubAuthorisedAgentUser(authorised = true)

							IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
								status = OK,
								response = incomeSourceDetailsModel
							)

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                NextUpdatesModel(
                  identification = "testId",
                  obligations = List(
                    NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now, "Quarterly", None, "testPeriodKey")
                  ))
              ))

							IncomeTaxViewChangeStub.stubGetNextUpdates(
								nino = testNino,
								deadlines =currentObligations
							)

							IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
								nino = testNino,
								from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
								to = getCurrentTaxYearEnd.toString
							)(
								status = OK,
								response = Json.toJson(FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId",
											documentDescription = Some("ITSA- POA 1"),
											documentText = Some("documentText"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										)
									),
									financialDetails = List(
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 1"),
											transactionId = Some("testTransactionId"),
											items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
										)
									)
								))
							)

							IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
						"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

							enable(TxmEventsApproved)
							val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

							result should have(
								httpStatus(OK),
								pageTitleAgent(agentTitle),
								elementTextBySelector("#updates-tile p:nth-child(2)")(LocalDate.now.toLongDate),
								elementTextBySelector("#payments-tile p:nth-child(2)")(LocalDate.now.toLongDate),
								elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
							)

							verifyAuditContainsDetail(HomeAudit(testUser, Some(Left(LocalDate.now -> false)), Left(LocalDate.now -> false)).detail)
							verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
						}
						"there are payments upcoming and nothing is overdue with TxmEventsApproved FS disabled" in {

							stubAuthorisedAgentUser(authorised = true)

							IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
								status = OK,
								response = incomeSourceDetailsModel
							)

							val currentObligations: ObligationsModel = ObligationsModel(Seq(
								NextUpdatesModel(
									identification = "testId",
									obligations = List(
										NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now, "Quarterly", None, "testPeriodKey")
									))
							))

							IncomeTaxViewChangeStub.stubGetNextUpdates(
								nino = testNino,
								deadlines =currentObligations
							)

							IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
								nino = testNino,
								from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
								to = getCurrentTaxYearEnd.toString
							)(
								status = OK,
								response = Json.toJson(FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId",
											documentDescription = Some("ITSA- POA 1"),
											documentText = Some("documentText"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										)
									),
									financialDetails = List(
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 1"),
											transactionId = Some("testTransactionId"),
											items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
										)
									)
								))
							)

							IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
								"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

							disable(TxmEventsApproved)
							val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

							result should have(
								httpStatus(OK),
								pageTitleAgent(agentTitle),
								elementTextBySelector("#updates-tile p:nth-child(2)")(LocalDate.now.toLongDate),
								elementTextBySelector("#payments-tile p:nth-child(2)")(LocalDate.now.toLongDate),
								elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
							)

							verifyAuditDoesNotContainsDetail(HomeAudit(testUser, Some(Left(LocalDate.now -> false)), Left(LocalDate.now -> false)).detail)
							verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
						}
					}
					"display the page with no upcoming payment" when {
						"there are no upcoming payments for the client with TxmEventsApproved FS enabled" in {
							enable(TxmEventsApproved)

							stubAuthorisedAgentUser(authorised = true)

							IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
								status = OK,
								response = incomeSourceDetailsModel
							)

							val currentObligations: ObligationsModel = ObligationsModel(Seq(
								NextUpdatesModel(
									identification = "testId",
									obligations = List(
										NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now, "Quarterly", None, "testPeriodKey")
									))
							))

							IncomeTaxViewChangeStub.stubGetNextUpdates(
								nino = testNino,
								deadlines = currentObligations
							)

							IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
								nino = testNino,
								from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
								to = getCurrentTaxYearEnd.toString
							)(
								status = OK,
								response = Json.toJson(FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId",
											documentDescription = Some("ITSA- POA 1"),
											documentText = Some("documentText"),
											outstandingAmount = Some(0),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										)
									),
									financialDetails = List(
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 1"),
											transactionId = Some("testTransactionId"),
											items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
										)
									)
								))
							)

							IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
								"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

							val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

							result should have(
								httpStatus(OK),
								pageTitleAgent(agentTitle),
								elementTextBySelector("#updates-tile p:nth-child(2)")(LocalDate.now.toLongDate),
								elementTextBySelector("#payments-tile p:nth-child(2)")("No payments due"),
								elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
							)

							verifyAuditContainsDetail(HomeAudit(testUser, None, Left(LocalDate.now -> false)).detail)
							verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
						}
						"display the page with no upcoming payment with TxmEventsApproved FS disabled" when {
							"there are no upcoming payments for the client" in {
								disable(TxmEventsApproved)

								stubAuthorisedAgentUser(authorised = true)

								IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
									status = OK,
									response = incomeSourceDetailsModel
								)

								val currentObligations: ObligationsModel = ObligationsModel(Seq(
									NextUpdatesModel(
										identification = "testId",
										obligations = List(
											NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now, "Quarterly", None, "testPeriodKey")
										))
								))

								IncomeTaxViewChangeStub.stubGetNextUpdates(
									nino = testNino,
									deadlines = currentObligations
								)

								IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
									nino = testNino,
									from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
									to = getCurrentTaxYearEnd.toString
								)(
									status = OK,
									response = Json.toJson(FinancialDetailsModel(
										balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
										documentDetails = List(
											DocumentDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												transactionId = "testTransactionId",
												documentDescription = Some("ITSA- POA 1"),
												documentText = Some("documentText"),
												outstandingAmount = Some(0),
												originalAmount = Some(1000.00),
												documentDate = LocalDate.of(2018, 3, 29)
											)
										),
										financialDetails = List(
											FinancialDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												mainType = Some("SA Payment on Account 1"),
												transactionId = Some("testTransactionId"),
												items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
											)
										)
									))
								)

								IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
									"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

								val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

								result should have(
									httpStatus(OK),
									pageTitleAgent(agentTitle),
									elementTextBySelector("#updates-tile p:nth-child(2)")(LocalDate.now.toLongDate),
									elementTextBySelector("#payments-tile p:nth-child(2)")("No payments due"),
									elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
								)

								verifyAuditDoesNotContainsDetail(HomeAudit(testUser, None, Left(LocalDate.now -> false)).detail)
								verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
							}
						}
						"display the page with an overdue payment and an overdue obligation with TxmEventsApproved FS enabled" when {
							"there is a single payment overdue and a single obligation overdue" in {
								enable(TxmEventsApproved)

								stubAuthorisedAgentUser(authorised = true)

								IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
									status = OK,
									response = incomeSourceDetailsModel
								)

								val currentObligations: ObligationsModel = ObligationsModel(Seq(
									NextUpdatesModel(
										identification = "testId",
										obligations = List(
											NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey")
										))
								))

								IncomeTaxViewChangeStub.stubGetNextUpdates(
									nino = testNino,
									deadlines = currentObligations
								)

								IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
									nino = testNino,
									from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
									to = getCurrentTaxYearEnd.toString
								)(
									status = OK,
									response = Json.toJson(FinancialDetailsModel(
										balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
										documentDetails = List(
											DocumentDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												transactionId = "testTransactionId",
												documentDescription = Some("ITSA- POA 1"),
												documentText = Some("documentText"),
												outstandingAmount = Some(500.00),
												originalAmount = Some(1000.00),
												documentDate = LocalDate.of(2018, 3, 29)
											)
										),
										financialDetails = List(
											FinancialDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												mainType = Some("SA Payment on Account 1"),
												transactionId = Some("testTransactionId"),
												items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))
											)
										)
									))
								)

								IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
									"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

								val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

								result should have(
									httpStatus(OK),
									pageTitleAgent(agentTitle),
									elementTextBySelector("#updates-tile p:nth-child(2)")(s"OVERDUE ${LocalDate.now.minusDays(1).toLongDate}"),
									elementTextBySelector("#payments-tile p:nth-child(2)")(s"OVERDUE ${LocalDate.now.minusDays(1).toLongDate}"),
									elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
								)

								verifyAuditContainsDetail(HomeAudit(testUser, Some(Left(LocalDate.now.minusDays(1) -> true)), Left(LocalDate.now.minusDays(1) -> true)).detail)
								verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
							}
							"there is a single payment overdue and a single obligation overdue and one overdue CESA " in {
								enable(TxmEventsApproved)

								stubAuthorisedAgentUser(authorised = true)

								IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
									status = OK,
									response = incomeSourceDetailsModel
								)

								val currentObligations: ObligationsModel = ObligationsModel(Seq(
									NextUpdatesModel(
										identification = "testId",
										obligations = List(
											NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey")
										))
								))

								IncomeTaxViewChangeStub.stubGetNextUpdates(
									nino = testNino,
									deadlines = currentObligations
								)

								IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
									nino = testNino,
									from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
									to = getCurrentTaxYearEnd.toString
								)(
									status = OK,
									response = Json.toJson(FinancialDetailsModel(
										balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
										documentDetails = List(
											DocumentDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												transactionId = "testTransactionId",
												documentDescription = Some("ITSA- POA 1"),
												documentText = Some("documentText"),
												outstandingAmount = Some(500.00),
												originalAmount = Some(1000.00),
												documentDate = LocalDate.of(2018, 3, 29)
											)
										),
										financialDetails = List(
											FinancialDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												mainType = Some("SA Payment on Account 1"),
												transactionId = Some("testTransactionId"),
												items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))
											)
										)
									))
								)

								IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
									"utr", testSaUtr.toLong, getCurrentTaxYearEnd.minusYears(1).getYear.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

								val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

								result should have(
									httpStatus(OK),
									pageTitleAgent(agentTitle),
									elementTextBySelector("#updates-tile p:nth-child(2)")(s"OVERDUE ${LocalDate.now.minusDays(1).toLongDate}"),
									elementTextBySelector("#payments-tile p:nth-child(2)")(s"2 OVERDUE PAYMENTS"),
									elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
								)

								verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(2)), Left(LocalDate.now.minusDays(1) -> true)).detail)
								verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
							}
						}
						"display the page with an overdue payment and an overdue obligation with TxmEventsApproved FS disabled" when {
							"there is a single payment overdue and a single obligation overdue" in {
								disable(TxmEventsApproved)

								stubAuthorisedAgentUser(authorised = true)

								IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
									status = OK,
									response = incomeSourceDetailsModel
								)

								val currentObligations: ObligationsModel = ObligationsModel(Seq(
									NextUpdatesModel(
										identification = "testId",
										obligations = List(
											NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey")
										))
								))

								IncomeTaxViewChangeStub.stubGetNextUpdates(
									nino = testNino,
									deadlines = currentObligations
								)

								IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
									nino = testNino,
									from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
									to = getCurrentTaxYearEnd.toString
								)(
									status = OK,
									response = Json.toJson(FinancialDetailsModel(
										balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
										documentDetails = List(
											DocumentDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												transactionId = "testTransactionId",
												documentDescription = Some("ITSA- POA 1"),
												documentText = Some("documentText"),
												outstandingAmount = Some(500.00),
												originalAmount = Some(1000.00),
												documentDate = LocalDate.of(2018, 3, 29)
											)
										),
										financialDetails = List(
											FinancialDetail(
												taxYear = getCurrentTaxYearEnd.getYear.toString,
												mainType = Some("SA Payment on Account 1"),
												transactionId = Some("testTransactionId"),
												items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))
											)
										)
									))
								)

								IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
									"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

								val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

								result should have(
									httpStatus(OK),
									pageTitleAgent(agentTitle),
									elementTextBySelector("#updates-tile p:nth-child(2)")(s"OVERDUE ${LocalDate.now.minusDays(1).toLongDate}"),
									elementTextBySelector("#payments-tile p:nth-child(2)")(s"OVERDUE ${LocalDate.now.minusDays(1).toLongDate}"),
									elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
								)

								verifyAuditDoesNotContainsDetail(HomeAudit(testUser, Some(Left(LocalDate.now.minusDays(1) -> true)), Left(LocalDate.now.minusDays(1) -> true)).detail)
								verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
							}
						}
					}
					"display the page with a count of the overdue payments a count of overdue obligations with TxmEventsApproved FS enabled" when {
						"there is more than one payment overdue and more than one obligation overdue" in {
							enable(TxmEventsApproved)

							stubAuthorisedAgentUser(authorised = true)

							IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
								status = OK,
								response = incomeSourceDetailsModel
							)

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                NextUpdatesModel(
                  identification = "testId",
                  obligations = List(
                    NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey"),
                    NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(2), "Quarterly", None, "testPeriodKey")
                  ))
              ))

							IncomeTaxViewChangeStub.stubGetNextUpdates(
								nino = testNino,
								deadlines = currentObligations
							)

							IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
								nino = testNino,
								from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
								to = getCurrentTaxYearEnd.toString
							)(
								status = OK,
								response = Json.toJson(FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId1",
											documentText = Some("documentText"),
											documentDescription = Some("ITSA- POA 1"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										),
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId2",
											documentText = Some("documentText"),
											documentDescription = Some("ITSA - POA 2"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										)
									),
									financialDetails = List(
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 1"),
											transactionId = Some("testTransactionId1"),
											items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))
										),
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 2"),
											transactionId = Some("testTransactionId2"),
											items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(2).toString))))
										)
									)
								))
							)

							IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
								"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

							val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

							result should have(
								httpStatus(OK),
								pageTitleAgent(agentTitle),
								elementTextBySelector("#updates-tile p:nth-child(2)")("2 OVERDUE UPDATES"),
								elementTextBySelector("#payments-tile p:nth-child(2)")("2 OVERDUE PAYMENTS"),
								elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
							)

              verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(2)), Right(2)).detail)
              verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
						}
					}
					"display the page with a count of the overdue payments a count of overdue obligations with TxmEventsApproved FS disabled" when {
						"there is more than one payment overdue and more than one obligation overdue" in {
							disable(TxmEventsApproved)

							stubAuthorisedAgentUser(authorised = true)

							IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
								status = OK,
								response = incomeSourceDetailsModel
							)

							val currentObligations: ObligationsModel = ObligationsModel(Seq(
								NextUpdatesModel(
									identification = "testId",
									obligations = List(
										NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(1), "Quarterly", None, "testPeriodKey"),
										NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now.minusDays(2), "Quarterly", None, "testPeriodKey")
									))
							))

							IncomeTaxViewChangeStub.stubGetNextUpdates(
								nino = testNino,
								deadlines = currentObligations
							)

							IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
								nino = testNino,
								from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
								to = getCurrentTaxYearEnd.toString
							)(
								status = OK,
								response = Json.toJson(FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId",
											documentText = Some("documentText"),
											documentDescription = Some("ITSA- POA 1"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										),
										DocumentDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											transactionId = "testTransactionId2",
											documentText = Some("documentText"),
											documentDescription = Some("ITSA - POA 2"),
											outstandingAmount = Some(500.00),
											originalAmount = Some(1000.00),
											documentDate = LocalDate.of(2018, 3, 29)
										)
									),
									financialDetails = List(
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 1"),
											transactionId = Some("testTransactionId"),
											items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))
										),
										FinancialDetail(
											taxYear = getCurrentTaxYearEnd.getYear.toString,
											mainType = Some("SA Payment on Account 2"),
											transactionId = Some("testTransactionId2"),
											items = Some(Seq(SubItem(Some(LocalDate.now.minusDays(2).toString))))
										)
									)
								))
							)

							IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
								"utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

							val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

							result should have(
								httpStatus(OK),
								pageTitleAgent(agentTitle),
								elementTextBySelector("#updates-tile p:nth-child(2)")("2 OVERDUE UPDATES"),
								elementTextBySelector("#payments-tile p:nth-child(2)")("2 OVERDUE PAYMENTS"),
								elementTextBySelector(".govUk-hint")("UTR: 1234567890 Client’s name Test User")
							)

							verifyAuditDoesNotContainsDetail(HomeAudit(testUser, Some(Right(2)), Right(2)).detail)
							verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
						}
					}
				}
				"retrieving the client's charges was unsuccessful" in {
					enable(TxmEventsApproved)

					stubAuthorisedAgentUser(authorised = true)

					IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
						status = OK,
						response = incomeSourceDetailsModel
					)

          val currentObligations: ObligationsModel = ObligationsModel(Seq(
            NextUpdatesModel(
              identification = "testId",
              obligations = List(
                NextUpdateModel(LocalDate.now, LocalDate.now.plusDays(1), LocalDate.now, "Quarterly", None, "testPeriodKey")
              ))
          ))

          IncomeTaxViewChangeStub.stubGetNextUpdates(
						nino = testNino,
						deadlines = currentObligations
					)

					IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
						nino = testNino,
						from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
						to = getCurrentTaxYearEnd.toString
					)(
						status = INTERNAL_SERVER_ERROR,
						response = Json.obj()
					)

					val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

					result should have(
						httpStatus(INTERNAL_SERVER_ERROR),
						pageTitleIndividual(titleTechError)
					)

          verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
				}
			}
			"retrieving the client's obligations was unsuccessful" in {

				stubAuthorisedAgentUser(authorised = true)

				IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
					status = OK,
					response = incomeSourceDetailsModel
				)

				IncomeTaxViewChangeStub.stubGetNextUpdatesError(testNino)

				val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

				result should have(
					httpStatus(INTERNAL_SERVER_ERROR),
					pageTitleIndividual(titleTechError)
				)
			}
		}
		"retrieving the client's income sources was unsuccessful" in {

			stubAuthorisedAgentUser(authorised = true)

			IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
				status = INTERNAL_SERVER_ERROR,
				response = IncomeSourceDetailsModel(
					mtdbsa = testMtditid,
					yearOfMigration = None,
					businesses = List(BusinessDetailsModel(
						Some("testId"),
						Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
						None,
						Some(getCurrentTaxYearEnd)
					)),
					property = None
				)
			)

			val result = IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation)

			result should have(
				httpStatus(INTERNAL_SERVER_ERROR),
				pageTitleIndividual(titleTechError)
			)
		}
	}

	"API#1171 GetBusinessDetails Caching" when {
		"2nd incomeSourceDetails call SHOULD be cached" in {
			testIncomeSourceDetailsCaching(false, 1,
				() => IncomeTaxViewChangeFrontend.getAgentHome(clientDetailsWithConfirmation))
		}
	}
}
