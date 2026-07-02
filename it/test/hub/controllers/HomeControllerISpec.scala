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

package hub.controllers

import businessDetails.testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2TradingStart}
import common.auth.MtdItUser
import common.controllers.ControllerISpecHelper
import common.enums.MTDIndividual
import common.helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import common.helpers.servicemocks.FeatureSwitchStub.stubGetFeatureSwitches
import common.helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxBusinessDetailsStub, MTDIndividualAuthStub}
import common.implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import common.models.core.{AccountingPeriodModel, CessationModel}
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, TaxYear}
import common.testConstants.BaseIntegrationTestConstants.*
import common.testConstants.messages.HomeMessages.*
import financials.models.*
import financials.testConstants.OutstandingChargesIntegrationTestConstants.{validOutStandingChargeResponseJsonWithAciAndBcdCharges, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges}
import hub.audit.models.HomeAudit
import hub.helpers.serviceMocks.PenaltyDetailsStub
import obligations.testConstants.NextUpdatesIntegrationTestConstants.*
import play.api.http.Status.*
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import shared.models.audit.NextUpdatesResponseAuditModel
import shared.models.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate

class HomeControllerISpec extends ControllerISpecHelper {

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
      None,
      Some(CessationModel(Some(b2CessationDate))),
      address = Some(address)
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = getTestUser(MTDIndividual, incomeSourceDetailsModel)

  val path = "/"

  import implicitDateFormatter.longDate

  "GET /" when {
    "an authenticated user" should {
      "render the home page" which {
        "displays the next upcoming payment and charge" when {
          "there are payments upcoming and nothing is overdue" in {
            stubGetFeatureSwitches()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(nextUpdateDue(currentDate.toLongDate)),
              elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate)
            )

            verifyAuditContainsDetail(HomeAudit(testUser, Some(Left(currentDate -> false)), Left(currentDate -> false), userIsCYPlusOne = false).detail)
            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }
        }

        "displays no upcoming payment" when {
          "there are no upcoming payments" in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(nextUpdateDue(currentDate.toLongDate)),
              elementTextBySelector("#payments-tile p:nth-child(2)")(noPaymentsDue)
            )

            verifyAuditContainsDetail(HomeAudit(testUser, None, Left(currentDate -> false), userIsCYPlusOne = false).detail)
            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }
        }

        "displays an overdue payment and an overdue obligation" when {
          "there is a single payment overdue and a single obligation overdue" in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(overdue),
              elementTextBySelector("#payments-tile p:nth-child(2)")(s"$overdue ${currentDate.minusDays(1).toLongDate}")
            )

            verifyAuditContainsDetail(HomeAudit(
              testUser,
              Some(Left(currentDate.minusDays(1) -> true)),
              Left(currentDate.minusDays(1) -> true),
              userIsCYPlusOne = false).detail)
            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "there is a single payment overdue and a single obligation overdue and one overdue CESA " in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, getCurrentTaxYearEnd.minusYears(1).getYear.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(overdue),
              elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments(numberOverdue = "2"))
            )

            verifyAuditContainsDetail(HomeAudit(
              testUser,
              Some(Right(2)),
              Left(currentDate.minusDays(1) -> true),
              userIsCYPlusOne = false).detail)
            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }
        }
        "display a count of the overdue payments a count of overdue obligations" when {
          "there is more than one payment overdue and more than one obligation overdue" in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    documentDescription = Some("ITSA- POA 2"),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId1"),
                    items = Some(Seq(SubItem(Some(currentDate.minusDays(1)))))
                  ),
                  FinancialDetail(
                    taxYear = getCurrentTaxYearEnd.getYear.toString,
                    mainType = Some("SA Payment on Account 2"),
                    mainTransaction = Some("4930"),
                    transactionId = Some("testTransactionId2"),
                    items = Some(Seq(SubItem(Some(currentDate.minusDays(2)))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(overdueUpdates(numberOverdue = "2")),
              elementTextBySelector("#payments-tile p:nth-child(2)")(overduePayments(numberOverdue = "2"))
            )

            verifyAuditContainsDetail(HomeAudit(testUser, Some(Right(2)), Right(2), userIsCYPlusOne = false).detail)
            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }
        }

        "display Income Sources tile" when {
          "using the manage businesses journey" in {
            stubGetFeatureSwitches()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(nextUpdateDue(currentDate.toLongDate)),
              elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate),
              elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Your businesses")
            )
          }
        }
        "display Your Businesses tile" when {
          "using the manage businesses journey" in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#updates-tile p:nth-child(2)")(nextUpdateDue(currentDate.toLongDate)),
              elementTextBySelector("#payments-tile p:nth-child(2)")(currentDate.toLongDate),
              elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Your businesses")
            )
          }
        }

        "display the Reporting Obligations tile" when {
          "Reporting Frequency feature switches are enabled" in {
            stubGetFeatureSwitches()
            MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

            IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

            IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
              nino = testNino,
              from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
              to = getCurrentTaxYearEnd.toString
            )(
              status = OK,
              response = Json.toJson(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
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
                    mainTransaction = Some("4920"),
                    transactionId = Some("testTransactionId"),
                    items = Some(Seq(SubItem(Some(currentDate))))
                  )
                )
              ))
            )

            IncomeTaxBusinessDetailsStub.stubGetOutstandingChargesResponse(
              "utr", testSaUtr.toLong, (getCurrentTaxYearEnd.minusYears(1).getYear).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

            PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

            val result = buildGETMTDClient(path).futureValue

            result should have(
              httpStatus(OK),
              pageTitleInd("home.heading.new"),
              elementTextBySelector("#reporting-obligations-tile p:nth-child(2)")("For the 2022 to 2023 tax year you need to:"),
              elementTextBySelector("#reporting-obligations-tile h2:nth-child(1)")("Your reporting obligations")
            )
          }
        }
      }

      "render the error page" when {

        "retrieving the charges was unsuccessful" in {
          stubGetFeatureSwitches()
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
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

          IncomeTaxBusinessDetailsStub.stubGetNextUpdates(
            nino = testNino,
            deadlines = currentObligations
          )

          IncomeTaxBusinessDetailsStub.stubGetFinancialDetailsByDateRange(
            nino = testNino,
            from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
            to = getCurrentTaxYearEnd.toString
          )(
            status = INTERNAL_SERVER_ERROR,
            response = Json.obj()
          )

          PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

          val result = buildGETMTDClient(path).futureValue

          result should have(
            httpStatus(INTERNAL_SERVER_ERROR),
            pageTitleInd(titleInternalServer, isErrorPage = true)
          )

          verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
        }
      }
      "retrieving the obligations was unsuccessful" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsModel
        )

        IncomeTaxBusinessDetailsStub.stubGetNextUpdatesError(testNino)

        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleInd(titleInternalServer, isErrorPage = true)
        )
      }
      "retrieving the income sources was unsuccessful" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxBusinessDetailsStub.stubGetIncomeSourceDetailsErrorResponse(testMtditid)(
          status = INTERNAL_SERVER_ERROR)

        PenaltyDetailsStub.stubNoPenaltiesDataResponse(testMtditid)

        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleInd(titleInternalServer, isErrorPage = true)
        )
      }
    }

    testAuthFailures(path, MTDIndividual)
  }
}
