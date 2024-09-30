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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.utils.SessionKeys.gatewayPage
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClaimToAdjustService
import models.admin.{AdjustPaymentsOnAccount, CreditsRefundsRepay, ReviewAndReconcilePoa}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{status, _}
import services.WhatYouOweService
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.FinancialDetailsTestConstants._
import testConstants.{BaseTestConstants, ChargeConstants}
import testUtils.TestSupport
import views.html.WhatYouOwe

import java.time.LocalDate
import scala.concurrent.Future

class WhatYouOweControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions with MockClaimToAdjustService with TestSupport with FeatureSwitching with ChargeConstants {

  override def beforeEach(): Unit = {
    disableAllSwitches()
    super.beforeEach()
  }

  trait Setup {

    val whatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])

    disable(AdjustPaymentsOnAccount)

    val controller = new WhatYouOweController(
      whatYouOweService,
      mockClaimToAdjustService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      mockAuthService,
      mockAuditingService,
      dateService,
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[WhatYouOwe],
      testAuthenticator
    )
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  def whatYouOweChargesListFull: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(chargeItemModel(2019))
      ++ List(chargeItemModel(2020))
      ++ List(chargeItemModel(2021)),
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

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List.empty)

  def whatYouOweChargesListWithAllChargesPaid: WhatYouOweChargesList = whatYouOweChargesListWithReviewReconcile
    .copy(
      chargesList = List(
        whatYouOweChargesListWithReviewReconcile
          .chargesList
          .head
          .copy(outstandingAmount = 0)
      )
    )

  val noFinancialDetailErrors = List(testFinancialDetail(2018))
  val hasFinancialDetailErrors = List(testFinancialDetail(2018), testFinancialDetailsErrorModel)
  val hasAFinancialDetailError = List(testFinancialDetailsErrorModel)
  val interestChargesWarningText = "! Warning Interest charges will keep increasing every day until the charges they relate to are paid in full."

  "The WhatYouOweController.viewPaymentsDue function" when {
    "obtaining a users charge" should {
      "send the user to the paymentsOwe page with full data of charges" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val interestChargesWarningText = "! Warning Interest charges will keep increasing every day until the charges they relate to are paid in full."

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")

      }

      "return success page with empty data in WhatYouOwe model" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListEmpty))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")

      }

      "send the user to the Internal error page with PaymentsDueService returning exception in case of error" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(new Exception("failed to retrieve data")))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "User fails to be authorised" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER

      }

      def whatYouOweWithAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00, Some(300.00), None, None, None, None), List.empty)

      "show money in your account if the user has available credit in his account" in new Setup {
        enable(CreditsRefundsRepay)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())


        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweWithAvailableCredits))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        val doc: Document = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("money-in-your-account")).isDefined shouldBe (true)
        doc.select("#money-in-your-account").select("div h2").text() shouldBe messages("whatYouOwe.moneyOnAccount")

        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        val docAgent: Document = Jsoup.parse(contentAsString(resultAgent))
        Option(docAgent.getElementById("money-in-your-account")).isDefined shouldBe (true)
        docAgent.select("#money-in-your-account").select("div h2").text() shouldBe messages("whatYouOwe.moneyOnAccount-agent")
      }

      def whatYouOweWithZeroAvailableCredits: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00, Some(0.00), None, None, None, None), List.empty)

      "hide money in your account if the credit and refund feature switch is disabled" in new Setup {
        disable(CreditsRefundsRepay)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweWithZeroAvailableCredits))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        val doc: Document = Jsoup.parse(contentAsString(result))
        Option(doc.getElementById("money-in-your-account")).isDefined shouldBe (false)

        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(gatewayPage) shouldBe Some("whatYouOwe")
        val docAgent: Document = Jsoup.parse(contentAsString(resultAgent))
        Option(docAgent.getElementById("money-in-your-account")).isDefined shouldBe (false)
      }
    }

    "AdjustPaymentsOnAccount FS is disabled" should {
      "Render the page without the POA journey entry point" in new Setup {
        disable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentAsString(result).contains("Adjust payments on account for the") shouldBe false
        status(resultAgent) shouldBe Status.OK
        contentAsString(resultAgent).contains("Adjust payments on account for the") shouldBe false
      }
    }
    "AdjustPaymentsOnAccount FS is enabled" should {
      "render the page with the POA journey entry point when there is an adjustable POA" in new Setup {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentAsString(result).contains("Adjust payments on account for the 2017 to 2018 tax year") shouldBe true
        status(resultAgent) shouldBe Status.OK
        contentAsString(resultAgent).contains("Adjust payments on account for the 2017 to 2018 tax year") shouldBe true
      }
      "render the page without the POA journey entry point when there are no adjustable POAs" in new Setup {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetPoaTaxYearForEntryPointCall(Right(None))

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentAsString(result).contains("Adjust payments on account for the") shouldBe false
        status(resultAgent) shouldBe Status.OK
        contentAsString(resultAgent).contains("Adjust payments on account for the") shouldBe false
      }
      "redirect to the internal server error page when there is an exception returned when fetching the POA entry point" in new Setup {
        enable(AdjustPaymentsOnAccount)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetPoaTaxYearForEntryPointCall(Left(new Exception("")))

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    "ReviewAndReconcilePoa FS is enabled" should {
      "render poa extra charges in charges table" in new Setup{
        enable(ReviewAndReconcilePoa)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetPoaTaxYearForEntryPointCall(Right(Some(TaxYear(2017, 2018))))

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListWithReviewReconcile))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentAsString(result).contains("First payment on account: extra amount from your tax return") shouldBe true
        status(resultAgent) shouldBe Status.OK
        contentAsString(resultAgent).contains("First payment on account: extra amount from your tax return") shouldBe true
      }
      "render the Interest Charges Warning when an overdue charge exists" in new Setup {
        enable(ReviewAndReconcilePoa)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
        status(resultAgent) shouldBe Status.OK
        Jsoup.parse(contentAsString(resultAgent)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
      }
      "render the Interest Charges Warning when an unpaid Review and Reconcile charge exists" in new Setup {
        enable(ReviewAndReconcilePoa)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListWithReviewReconcile))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
        status(resultAgent) shouldBe Status.OK
        Jsoup.parse(contentAsString(resultAgent)).getElementById("interest-charges-warning").text() shouldBe interestChargesWarningText
      }

      "hide the Interest Charges Warning when there are no overdue charges or unpaid Review & Reconcile charges" in new Setup {
        enable(ReviewAndReconcilePoa)
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListWithAllChargesPaid))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result: Future[Result] = controller.show()(fakeRequestWithNinoAndOrigin("PTA"))
        val resultAgent: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        Option(Jsoup.parse(contentAsString(result)).getElementById("interest-charges-warning")).isDefined shouldBe false
        status(resultAgent) shouldBe Status.OK
        Option(Jsoup.parse(contentAsString(resultAgent)).getElementById("interest-charges-warning")).isDefined shouldBe false
      }
    }
  }
}
