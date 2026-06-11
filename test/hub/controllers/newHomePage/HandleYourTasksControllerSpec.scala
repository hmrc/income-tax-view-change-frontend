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

package hub.controllers.newHomePage

import common.auth.AuthActions
import common.config.{AgentItvcErrorHandler, ItvcErrorHandler}
import common.mocks.auth.MockAuthActions
import common.mocks.services.{MockDateService, MockITSAStatusService}
import common.models.itsaStatus.{ITSAStatus, ITSAStatusResponseModel, StatusDetail, StatusReason}
import common.services.{AuditingService, DateService, DateServiceInterface, ITSAStatusService}
import common.testConstants.BaseTestConstants
import common.utils.sessionUtils.SessionKeys
import financials.services.*
import financials.testConstants.ANewCreditAndRefundModel
import hub.models.newHomePage.HandleYourTasksViewModel
import hub.models.newHomePage.MaturityLevel.Upcoming
import hub.models.newHomePage.YourTaskCardType.FINANCIALS
import hub.models.newHomePage.YourTasksCard.UpcomingTaskCard
import hub.services.newHomePage.HandleYourTasksService
import hub.views.html.newHomePage.NewHomeYourTasksView
import mocks.services.*
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import obligations.mocks.services.MockNextUpdatesService
import obligations.models.*
import obligations.services.NextUpdatesService
import obligations.services.reportingObligations.optOut.OptOutService
import obligations.services.reportingObligations.signUp.SignUpService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, session, status}

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class HandleYourTasksControllerSpec extends MockAuthActions
  with MockDateService
  with MockFinancialDetailsService
  with MockITSAStatusService
  with MockWhatYouOweService
  with MockNextUpdatesService {

  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[FinancialDetailsService].toInstance(mockFinancialDetailsService),
      api.inject.bind[WhatYouOweService].toInstance(mockWhatYouOweService),
      api.inject.bind[DateService].toInstance(mockDateServiceInjected),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService)
    ).build()

  given mockedSignUpService: SignUpService = mock(classOf[SignUpService])
  given mockedOptOutService: OptOutService = mock(classOf[OptOutService])
  given mockedCreditService: CreditService = mock(classOf[CreditService])
  given mockedHandleYourTasksService: HandleYourTasksService = mock(classOf[HandleYourTasksService])
  given MessagesControllerComponents = app.injector.instanceOf(classOf[MessagesControllerComponents])
  given ItvcErrorHandler = mock(classOf[ItvcErrorHandler])
  given AgentItvcErrorHandler = mock(classOf[AgentItvcErrorHandler])

  val authActions: AuthActions = app.injector.instanceOf(classOf[AuthActions])
  val view: NewHomeYourTasksView = app.injector.instanceOf(classOf[NewHomeYourTasksView])
  val auditingService: AuditingService = app.injector.instanceOf(classOf[AuditingService])

  val nextPaymentYear: String = "2019"
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val staticTaxYear: TaxYear = TaxYear(fixedDate.getYear - 1, fixedDate.getYear)
  val baseStatusDetail: StatusDetail = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Mandated, StatusReason.SignupReturnAvailable, Some(8000.25))
  val futureDueDates: Seq[LocalDate] = Seq(LocalDate.of(2100, 1, 1))
  val expectedYourTasksTitle = "Your tasks"

  trait Setup {
    val controller: HandleYourTasksController = HandleYourTasksController(
      authActions,
      view,
      mockedSignUpService,
      mockedOptOutService,
      mockITSAStatusService,
      mockWhatYouOweService,
      mockedCreditService,
      mockDateServiceInjected,
      mockFinancialDetailsService,
      mockNextUpdatesService,
      mockedHandleYourTasksService,
      auditingService)

    setupMockUserAuth
    mockSingleBusinessIncomeSource()
    when(mockDateServiceInjected.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInjected.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1

    val nextTaxReturnDueDate: LocalDate = mockDateServiceInjected.getCurrentDate.plusMonths(3)
    val quarterlyOverdueDueDate: LocalDate = mockDateServiceInjected.getCurrentDate.minusMonths(12)

    val obligationsModel: ObligationsModel = ObligationsModel(
      Seq(
        GroupedObligationsModel(
          BaseTestConstants.testSelfEmploymentId,
          List(
            SingleObligationModel(
              mockDateServiceInjected.getCurrentDate.minusMonths(6),
              nextTaxReturnDueDate,
              nextTaxReturnDueDate,
              "Crystallisation",
              None,
              "#001",
              StatusOpen
            )
          )
        ),
        GroupedObligationsModel(
          BaseTestConstants.testPropertyIncomeId,
          List(
            SingleObligationModel(
              mockDateServiceInjected.getCurrentDate.minusMonths(16),
              quarterlyOverdueDueDate,
              quarterlyOverdueDueDate,
              "Quarterly",
              None,
              "#002",
              StatusOpen
            )
          )
        )
      )
    )
    when(mockNextUpdatesService.getOpenObligations()(any(), any())).thenReturn(Future.successful(obligationsModel))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockDateServiceInjected.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInjected.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1
  }

  "show()" when {
    "an authenticated use" should {
      "render the Handle your tasks page with a Your tasks tab open" in new Setup {
        setupMockFeatureSwitches()
        setupMockUserAuth
        mockItsaStatusRetrievalAction()
        val financialDetails = List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            mainTransaction = Some("4920"), transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate))))))))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(financialDetails))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
        setupMockGetFilteredChargesListFromFinancialDetails(financialDetails.flatMap(_.asChargeItems))
        setupMockITSAStatusDetail(staticTaxYear)(Future.successful(List(ITSAStatusResponseModel(staticTaxYear.toString, Some(List(StatusDetail("", ITSAStatus.Mandated, StatusReason.MtdItsaOptIn)))))))
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
        setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
        when(mockedSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedOptOutService.updateJourneyStatusInSessionData(any())(any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedCreditService.getAllCredits(any(), any()))
          .thenReturn(Future.successful(
            ANewCreditAndRefundModel()
              .model
          ))
        when(mockNextUpdatesService.getNextDueDates(any())(any(), any()))
          .thenReturn(Future.successful(None, None))

        when(mockedHandleYourTasksService.getYourTasksCards(any(), any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq(UpcomingTaskCard("", "", "", "", None, None, Upcoming, FINANCIALS))))
        when(mockNextUpdatesService.getDueDates(any())(any(), any())).thenReturn(Future.successful(Right(futureDueDates)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        session(result).get(SessionKeys.mandationStatus) shouldBe Some("on")

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("#main-content h2").text shouldBe expectedYourTasksTitle
      }
    }
  }
}