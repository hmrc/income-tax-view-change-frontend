/*
 * Copyright 2023 HM Revenue & Customs
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

import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.gatewayPage
import mocks.auth.MockAuthActions
import mocks.services.MockClaimToAdjustService
import models.admin.{AdjustPaymentsOnAccount, CreditsRefundsRepay, PenaltiesAndAppeals, ReviewAndReconcilePoa}
import models.financialDetails.{BalanceDetails, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel, WhatYouOweChargesList}
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{status, _}
import services.{ClaimToAdjustService, DateService, SelfServeTimeToPayService, WhatYouOweService}
import testConstants.BaseTestConstants.testSetUpPaymentPlanUrl
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._

import java.time.LocalDate
import scala.concurrent.Future

class YourSelfAssessmentChargesControllerSpec extends MockAuthActions
  with MockClaimToAdjustService with ChargeConstants {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(whatYouOweService)
    reset(mockSelfServeTimeToPayService)
  }

  lazy val whatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])
  lazy val mockSelfServeTimeToPayService: SelfServeTimeToPayService = mock(classOf[SelfServeTimeToPayService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[WhatYouOweService].toInstance(whatYouOweService),
      api.inject.bind[ClaimToAdjustService].toInstance(mockClaimToAdjustService),
      api.inject.bind[DateService].toInstance(dateService),
      api.inject.bind[SelfServeTimeToPayService].toInstance(mockSelfServeTimeToPayService)
    ).build()

  lazy val testController: YourSelfAssessmentChargesController = app.injector.instanceOf[YourSelfAssessmentChargesController]

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  def whatYouOweChargesListFull: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(chargeItemModel(TaxYear.forYearEnd(2019)))
      ++ List(chargeItemModel(TaxYear.forYearEnd(2020)))
      ++ List(chargeItemModel(TaxYear.forYearEnd(2021))),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListFuture: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(chargeItemModel(TaxYear.forYearEnd(2049), interestOutstandingAmount = None, dueDate = Some(LocalDate.of(2050, 1, 1)))),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2050-12-31")), 10.23, 1234))
    ))
  )

  def whatYouOweChargesListWithReviewReconcile: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsReviewAndReconcileCi,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListOnlyReconciliation: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsReviewAndReconcileCi
  )

  def whatYouOweChargesListWithOverdueCharge: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsOverdueCharges,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("POA1RR-debit", Some(LocalDate.parse("2010-12-31")), 10.23, 1234), OutstandingChargeModel("POA1RR-debit", Some(LocalDate.parse("2010-12-31")), 1.23, 1234))
    ))
  )

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List.empty)

  def whatYouOweChargesListWithBalancingChargeNotOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsBalancingChargeNotOverdue,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("BCD", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListWithLpp2: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsLPP2,
    Some(OutstandingChargesModel(List()))
  )

  def whatYouOweChargesListWithLPP2NoChargeRef: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsLPP2NoChargeRef,
    Some(OutstandingChargesModel(List()))
  )

  val noFinancialDetailErrors = List(testFinancialDetail(2018))
  val hasFinancialDetailErrors = List(testFinancialDetail(2018), testFinancialDetailsErrorModel)
  val hasAFinancialDetailError = List(testFinancialDetailsErrorModel)
  val interestChargesWarningText = "! Warning Interest charges will keep increasing every day until the charges they relate to are paid in full."

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgent() else testController.show()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the your self assessment charges page" that {
            "has charges owed" when {
              "the user has a fill list of charges" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("yourSelfAssessmentChargeSummary")
              }

              "the user has no charges" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListEmpty))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("yourSelfAssessmentChargeSummary")
              }
            }
            "displays the Charges due now tab and warning banner" when {
              "the user has overdue charges" in {
                disable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))

                val result = action(fakeRequest)
                contentAsString(result).contains("Important") shouldBe true
                contentAsString(result).contains("Charges due now: £2.00") shouldBe true
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("charges-due-now-table")).isDefined shouldBe true
              }
              "the user has PoA reconciliation debits accruing interest and ReviewAndReconcilePoa FS is enabled" in {
                enable(ReviewAndReconcilePoa)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListOnlyReconciliation))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                contentAsString(result).contains("Important") shouldBe true
                contentAsString(result).contains("Charges due now: £2.00") shouldBe true
                contentAsString(result).contains("First payment on account: extra amount from your tax return") shouldBe true
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("charges-due-now-table")).isDefined shouldBe true
              }
            }
            "not display the Charges due now tab or warning banner" when {
              "the user has no charges that are overdue or accruing interest" in {
                disable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFuture))

                val result = action(fakeRequest)
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("overdue-important-warning")).isDefined shouldBe false
                Option(doc.getElementById("charges-due-now")).isDefined shouldBe false
              }
            }

            "displays the money in your account" when {
              "the user has available credit in his account and CreditsRefundsRepay FS enabled" in {
                def whatYouOweWithAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 3.00, Some(300.00), None, None, None, None), List.empty)

                setupMockSuccess(mtdUserRole)
                enable(CreditsRefundsRepay)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithAvailableCredits))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("yourSelfAssessmentChargeSummary")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe true
                doc.select("#money-in-your-account").select("div h2").text() shouldBe messages("whatYouOwe.moneyOnAccount" + {
                  if (isAgent) "-agent" else ""
                })
              }
            }

            "does not display the money in your account" when {
              "the user has available credit in his account but CreditsRefundsRepay FS disabled" in {
                def whatYouOweWithZeroAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 3.00, Some(0.00), None, None, None, None), List.empty)

                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithZeroAvailableCredits))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("yourSelfAssessmentChargeSummary")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe false
              }
            }

            "contains the adjust POA" when {
              "the AdjustPaymentsOnAccount FS is enabled and there are adjustable POA" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))

                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the 2017 to 2018 tax year") shouldBe true
              }
              "the AdjustPaymentsOnAccount FS is enabled and there are no adjustable POAs" in {
                enable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockGetPoaTaxYearForEntryPointCall(Right(None))

                when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                  .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))

                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the") shouldBe false

              }
            }

            "does not contain the adjust POA" when {
              "the AdjustPaymentsOnAccount FS is disabled" in {
                disable(AdjustPaymentsOnAccount)
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))

                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the") shouldBe false
              }
            }

            "user has a second late payment penalty with a chargeReference, so url can be generated" in {
              enable(PenaltiesAndAppeals)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole)
              setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLpp2))
              when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                .thenReturn(Future.successful(Right(testSetUpPaymentPlanUrl)))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "render the error page" when {
            "PaymentsDueService returns an exception" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                .thenReturn(Future.failed(new Exception("failed to retrieve data")))
              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.failed(new Exception("failed to retrieve data")))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "fetching POA entry point fails" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdUserRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockGetPoaTaxYearForEntryPointCall(Left(new Exception("")))

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListFull))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "SelfServeTimeToPayService returns an exception" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListFull))
              when(mockSelfServeTimeToPayService.startSelfServeTimeToPayJourney(any())(any()))
                .thenReturn(Future.failed(new Exception("failed to retrieve data")))

              val result = action(fakeRequest)

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR


            }

            "user has a second late payment penalty without a chargeReference, so url cannot be generated" in {
              enable(PenaltiesAndAppeals)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole)
              setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLPP2NoChargeRef))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, supportingAgentAccessAllowed = false)(fakeRequest)
    }
  }
}
