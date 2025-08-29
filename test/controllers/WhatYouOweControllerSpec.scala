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

import controllers.routes.{ChargeSummaryController, CreditAndRefundController, PaymentController}
import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.gatewayPage
import mocks.auth.MockAuthActions
import models.admin.{CreditsRefundsRepay, PenaltiesAndAppeals}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import models.financialDetails.WhatYouOweViewModel
import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{DateService, WhatYouOweService}
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class WhatYouOweControllerSpec extends MockAuthActions
  with ChargeConstants {

  lazy val whatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[WhatYouOweService].toInstance(whatYouOweService),
      api.inject.bind[DateService].toInstance(dateService),
    ).build()

  lazy val testController = app.injector.instanceOf[WhatYouOweController]

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

  def whatYouOweChargesListWithReviewReconcile: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    financialDetailsReviewAndReconcileCi,
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some(LocalDate.parse("2020-12-31")), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
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

  def ctaViewModel(isFSEnabled: Boolean, poaTaxYear: Option[TaxYear]): WYOClaimToAdjustViewModel = {
    if (isFSEnabled) {
      WYOClaimToAdjustViewModel(poaTaxYear = poaTaxYear)
    } else {
      WYOClaimToAdjustViewModel(poaTaxYear = poaTaxYear)
    }
  }

  def wyoViewModel(isAgent: Boolean,
                   charges: WhatYouOweChargesList = whatYouOweChargesListFull,
                   currentTaxYear: Int = fixedDate.getYear,
                   hasLpiWithDunningLock: Boolean = false,
                   dunningLock: Boolean = false,
                   taxYear: Int = fixedDate.getYear,
                   adjustPaymentsOnAccountFSEnabled: Boolean = false,
                   claimToAdjustViewModel: Option[WYOClaimToAdjustViewModel] = None,
                   LPP2Url: String = "",
                   hasOverdueOrAccruingInterestCharges: Boolean = false,
                   poaTaxYear: Option[TaxYear] = None
                  ): WhatYouOweViewModel = WhatYouOweViewModel(
    currentDate = dateService.getCurrentDate,
    hasOverdueOrAccruingInterestCharges = hasOverdueOrAccruingInterestCharges,
    whatYouOweChargesList = charges,
    hasLpiWithDunningLock = hasLpiWithDunningLock,
    currentTaxYear = currentTaxYear,
    backUrl = "testBackURL",
    utr = Some("1234567890"),
    dunningLock = dunningLock,
    creditAndRefundUrl = if (isAgent) CreditAndRefundController.showAgent().url else CreditAndRefundController.show().url,
    creditAndRefundEnabled = true,
    taxYearSummaryUrl = _ => if (isAgent)
      controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
    else
      controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear).url,
    claimToAdjustViewModel = claimToAdjustViewModel.getOrElse(ctaViewModel(adjustPaymentsOnAccountFSEnabled, poaTaxYear)),
    lpp2Url = LPP2Url,
    adjustPoaUrl = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url,
    chargeSummaryUrl = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) => if (isAgent)
      ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url
    else
      ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url,
    paymentHandOffUrl = PaymentController.paymentHandoff(_, None).url,
    selfServeTimeToPayEnabled = true,
    selfServeTimeToPayStartUrl = "/self-serve-time-to-pay"
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
          "render the what you owe page" that {
            "has payments owed" when {
              "the user has a fill list of charges" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
              }

              "the user has no charges" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListEmpty))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
              }
            }

            "displays the money in your account" when {
              "the user has available credit in his account and CreditsRefundsRepay FS enabled" in {
                def whatYouOweWithAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(
                  BalanceDetails(1.00, 2.00, 3.00, Some(300.00), None, None, None, None), List.empty)

                setupMockSuccess(mtdUserRole)
                enable(CreditsRefundsRepay)
                mockSingleBISWithCurrentYearAsMigrationYear()
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithAvailableCredits))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, whatYouOweWithAvailableCredits))))


                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
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
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweWithZeroAvailableCredits))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, whatYouOweWithZeroAvailableCredits))))


                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
                val doc: Document = Jsoup.parse(contentAsString(result))
                Option(doc.getElementById("money-in-your-account")).isDefined shouldBe false
              }
            }

            "contains the adjust POA" when {
              "there are adjustable POA" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()

                val poaModel: WYOClaimToAdjustViewModel = ctaViewModel(true, Some(TaxYear(2017, 2018)))

                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, claimToAdjustViewModel = Some(poaModel)))))


                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the 2017 to 2018 tax year") shouldBe true
              }
              "there are no adjustable POAs" in {
                setupMockSuccess(mtdUserRole)
                mockSingleBISWithCurrentYearAsMigrationYear()

                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListFull))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent))))


                val result = action(fakeRequest)
                contentAsString(result).contains("Adjust payments on account for the") shouldBe false

              }
            }

            "that includes poa extra charges in charges table" when {
              "ReviewAndReconcilePoa FS is enabled" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)

                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithReviewReconcile))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithReviewReconcile))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                contentAsString(result).contains("First payment on account: extra amount from your tax return") shouldBe true
              }
            }

            "that includes a interest charges warning" when {
              "an overdue charge exists" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithOverdueCharge))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithOverdueCharge, hasOverdueOrAccruingInterestCharges = true))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
              }

              "Review and Reconcile charge with accruing interest exists" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithOverdueCharge))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithOverdueCharge, hasOverdueOrAccruingInterestCharges = true))))

                val result = action(fakeRequest)

                status(result) shouldBe Status.OK
                Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
              }
            }

            "that hides the interest charge warning" when {
              "there are no overdue charges or unpaid Review & Reconcile charges" in {
                mockSingleBISWithCurrentYearAsMigrationYear()
                setupMockSuccess(mtdUserRole)
                when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                  .thenReturn(Future.successful(whatYouOweChargesListWithBalancingChargeNotOverdue))
                when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                  .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithBalancingChargeNotOverdue))))

                val result = action(fakeRequest)
                status(result) shouldBe Status.OK
                Option(Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning")).isDefined shouldBe false
              }
            }

            "user has a second late payment penalty with a chargeReference, so url can be generated" in {
              enable(PenaltiesAndAppeals)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole)

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLpp2))
              when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(Some(wyoViewModel(isAgent, charges = whatYouOweChargesListWithLpp2))))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "render the error page" when {
            "PaymentsDueService returns an exception" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.failed(new Exception("failed to retrieve data")))
              when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))


              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "fetching POA entry point fails" in {
              setupMockSuccess(mtdUserRole)
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListFull))
              when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "user has a second late payment penalty without a chargeReference, so url cannot be generated" in {
              enable(PenaltiesAndAppeals)
              mockSingleBISWithCurrentYearAsMigrationYear()
              setupMockSuccess(mtdUserRole)

              when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
                .thenReturn(Future.successful(whatYouOweChargesListWithLPP2NoChargeRef))
              when(whatYouOweService.createWhatYouOweViewModel(any(), any(), any(), any(), any(), any(), any())(any(), any()))
                .thenReturn(Future(None))

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
