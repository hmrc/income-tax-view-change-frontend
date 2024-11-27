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

import audit.models.{HomeAudit, NextUpdatesResponseAuditModel}
import auth.MtdItUser
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.admin.{IncomeSourcesFs, IncomeSourcesNewJourney, NavBarFs}
import models.core.{AccountingPeriodModel, CessationModel}
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2CessationReason, b2TradingStart}
import testConstants.NextUpdatesIntegrationTestConstants.currentDate
import testConstants.OutstandingChargesIntegrationTestConstants._
import testConstants.messages.HomeMessages.{noPaymentsDue, overdue, overduePayments, overdueUpdates}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class HomeControllerPrimaryAgentISpec extends ControllerISpecHelper {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

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
      Some(CessationModel(Some(b2CessationDate), Some(b2CessationReason))),
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, incomeSourceDetailsModel, None,
    Some("1234567890"), Some("12345-credId"), Some(Agent), Some("1")
  )(FakeRequest())

  val path = "/agents"

  import implicitDateFormatter.longDate

  "GET /" when {
      val isSupportingAgent = false
      val additionalCookies = getAgentClientDetailsForCookie(false, true)
      s"there is a primary agent" that {
        s"is a authenticated for a client" should {
          "render the home page" which {
            "displays the next upcoming payment and charge" when {
              "there are payments upcoming and nothing is overdue" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate),
                        documentDueDate = Some(currentDate)
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate)
                )

                verifyAuditContainsDetail(HomeAudit(testUser, Some(Left(currentDate -> false)), Left(currentDate -> false)).detail)
                verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
              }
            }

            "displays no upcoming payment" when {
              "there are no upcoming payments" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 0,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29)
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(noPaymentsDue)
                )

                verifyAuditContainsDetail(HomeAudit(testUser, None, Left(currentDate -> false)).detail)
                verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
              }
            }

            "displays an overdue payment and an overdue obligation" when {
              "there is a single payment overdue and a single obligation overdue" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate.minusDays(1)),
                        documentDueDate = Some(currentDate.minusDays(1))
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(s"$overdue ${currentDate.minusDays(1).toLongDate}"),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(s"$overdue ${currentDate.minusDays(1).toLongDate}")
                )

                verifyAuditContainsDetail(HomeAudit(testUser, Some(Left(currentDate.minusDays(1) -> true)), Left(currentDate.minusDays(1) -> true)).detail)
                verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
              }

              "there is a single payment overdue and a single obligation overdue and one overdue CESA " in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate.minusDays(1)),
                        documentDueDate = Some(currentDate.minusDays(1))
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, getCurrentTaxYearEnd.minusYears(1).getYear.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(s"$overdue ${currentDate.minusDays(1).toLongDate}"),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments(numberOverdue = "2"))
                )

                verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(2)), Left(currentDate.minusDays(1) -> true)).detail)
                verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
              }
            }
            "display a count of the overdue payments a count of overdue obligations" when {
              "there is more than one payment overdue and more than one obligation overdue" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel =
                  ObligationsModel(
                    Seq(
                      GroupedObligationsModel(
                        identification = "testId",
                        obligations = List(
                          SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled),
                          SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(2), "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId1",
                        documentText = Some("documentText"),
                        documentDescription = Some("ITSA- POA 1"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate.minusDays(1)),
                        documentDueDate = Some(currentDate.minusDays(1)),
                      ),
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId2",
                        documentText = Some("documentText"),
                        documentDescription = Some("ITSA - POA 2"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate.minusDays(1)),
                        documentDueDate = Some(currentDate.minusDays(1))
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId1"),
                        items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                      ),
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 2"),
                        transactionId = Some("testTransactionId2"),
                        items = Some(Seq(SubItem(Some(currentDate.minusDays(2)))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(overdueUpdates(numberOverdue = "2")),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments(numberOverdue = "2"))
                )

                verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(2)), Right(2)).detail)
                verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
              }
            }
            "display Income Sources tile" when {
              "IncomeSources feature switch is enabled" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                enable(IncomeSourcesFs)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate),
                        documentDueDate = Some(currentDate)
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Income Sources")
                )
              }
            }
            "display Your Businesses tile" when {
              "IncomeSources and IncomeSourcesNewJourney feature switches are enabled" in {
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                enable(IncomeSourcesFs)
                enable(IncomeSourcesNewJourney)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetailsModel
                )

                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
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
                    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                    documentDetails = List(
                      DocumentDetail(
                        taxYear = getCurrentTaxYearEnd.getYear,
                        transactionId = "testTransactionId",
                        documentDescription = Some("ITSA- POA 1"),
                        documentText = Some("documentText"),
                        outstandingAmount = 500.00,
                        originalAmount = 1000.00,
                        documentDate = LocalDate.of(2018, 3, 29),
                        effectiveDateOfPayment = Some(currentDate),
                        documentDueDate = Some(currentDate)
                      )
                    ),
                    financialDetails = List(
                      FinancialDetail(
                        taxYear = getCurrentTaxYearEnd.getYear.toString,
                        mainType = Some("SA Payment on Account 1"),
                        transactionId = Some("testTransactionId"),
                        items = Some(Seq(SubItem(Some(currentDate))))
                      )
                    )
                  ))
                )

                IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
                  "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

                val result = buildGETMTDClient(path, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  pageTitleAgent("home.agent.heading"),
                  elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate),
                  elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Your businesses")
                )
              }
            }
          }

          "render the error page" when {

            "retrieving the charges was unsuccessful" in {
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
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

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(INTERNAL_SERVER_ERROR),
                pageTitleAgent(titleInternalServer, isErrorPage = true)
              )

              verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
            }
          }
          "retrieving the obligations was unsuccessful" in {
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsModel
            )

            IncomeTaxViewChangeStub.stubGetNextUpdatesError(testNino)

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitleAgent(titleInternalServer, isErrorPage = true)
            )
          }
          "retrieving the income sources was unsuccessful" in {
            disable(NavBarFs)
            enable(IncomeSourcesFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsErrorResponse(testMtditid)(
              status = INTERNAL_SERVER_ERROR)

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitleAgentLogin(titleInternalServer, isErrorPage = true)
            )
          }
        }

        testAuthFailuresForMTDAgent(path, isSupportingAgent)
      }
    testNoClientDataFailure(path)
  }

}
