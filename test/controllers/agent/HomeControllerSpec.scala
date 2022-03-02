/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import config.featureswitch._
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService, MockWhatYouOweService}
import mocks.views.agent.MockHome
import models.financialDetails.FinancialDetailsErrorModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.Lang
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import play.i18n
import play.i18n.MessagesApi
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testTaxYear}
import testConstants.FinancialDetailsTestConstants.financialDetailsModel
import testConstants.MessagesLookUp
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException
import _root_.utils.CurrentDateProvider

import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}

class HomeControllerSpec extends TestSupport
  with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with MockNextUpdatesService
  with MockFinancialDetailsService
  with MockAuditingService
  with MockHome
  with MockWhatYouOweService
  with FeatureSwitching
  with Injecting {

  trait Setup {

    val mockCurrentDateProvider: CurrentDateProvider = mock[CurrentDateProvider]

    val controller = new HomeController(
      app.injector.instanceOf[views.html.Home],
      mockNextUpdatesService,
      mockFinancialDetailsService,
      mockIncomeSourceDetailsService,
      mockCurrentDateProvider,
      mockWhatYouOweService,
      mockAuditingService,
      mockAuthService
    )(app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig],
      mockItvcErrorHandler,
      app.injector.instanceOf[ExecutionContext])

    implicit val lang: Lang = Lang("en-US")

    val javaMessagesApi: MessagesApi = inject[play.i18n.MessagesApi]
    val overdueWarningMessageDunningLockTrue: String = javaMessagesApi.get(new i18n.Lang(lang), "home.overdue.message.dunningLock.true")
    val overdueWarningMessageDunningLockFalse: String = javaMessagesApi.get(new i18n.Lang(lang), "home.overdue.message.dunningLock.false")
    val expectedOverDuePaymentsText = "OVERDUE 31 January 2019"
    val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  }

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.show()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the call to retrieve income sources for the client returns an error" should {
      "return an internal server exception" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()

        val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

        result.failed.futureValue shouldBe an[InternalServerException]
        result.failed.futureValue.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }
    "the call to retrieve income sources for the client is successful" when {
      "retrieving their obligation due date details had a failure" should {
        "return an internal server exception" in new Setup {

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
          mockSingleBusinessIncomeSource()
          when(mockNextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
          setupMockGetWhatYouOweChargesListEmpty()

          val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

          result.failed.futureValue shouldBe an[InternalServerException]
          result.failed.futureValue.getMessage shouldBe "obligation test exception"
        }
      }
      "retrieving their obligation due date details was successful" when {
        "retrieving their charge due date details had a failure" should {
          "return an internal server exception" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsErrorModel(500, "error"))))
            setupMockGetWhatYouOweChargesListEmpty()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            result.failed.futureValue shouldBe an[InternalServerException]
            result.failed.futureValue.getMessage shouldBe "[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details"
          }
        }
        "retrieving their charge due date details was successful" should {
          "display the home page with right details and without dunning lock warning and one overdue payment" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(dueDateValue = Some(LocalDate.of(2021, 5, 15).toString)))))
            setupMockGetWhatYouOweChargesListEmpty()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe "OVERDUE 15 May 2021"
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockFalse"
          }
          "display the home page with right details and without dunning lock warning and one overdue payment from CESA" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
          }
          "display the home page with right details and with dunning lock warning and two overdue payments" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe "2 OVERDUE PAYMENTS"
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and two overdue payments from FinancialDetailsService and one from CESA" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")),
                financialDetailsModel(testTaxYear))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe "3 OVERDUE PAYMENTS"
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and one overdue payments from CESA" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order"), dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and without dunning lock warning and one overdue payments from CESA" in new Setup {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockCurrentDateProvider.getCurrentDate()).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = None, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe MessagesLookUp.HomePage.agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockFalse"
          }
        }
      }
    }
  }

}
