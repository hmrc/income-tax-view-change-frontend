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

package controllers

import auth.authV2.AuthActions
import controllers.agent.sessionUtils.SessionKeys
import mocks.auth.MockAuthActions
import mocks.services.*
import models.admin.CreditsRefundsRepay
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail, StatusReason}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusOpen}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, session, status}
import services.reportingObligations.optOut.OptOutService
import services.reportingObligations.signUp.SignUpService
import services.*
import testConstants.{ANewCreditAndRefundModel, BaseTestConstants}
import views.html.HandleYourTasksView

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
  given MessagesControllerComponents = app.injector.instanceOf(classOf[MessagesControllerComponents])

  val authActions: AuthActions = app.injector.instanceOf(classOf[AuthActions])
  val view: HandleYourTasksView = app.injector.instanceOf(classOf[HandleYourTasksView])

  val nextPaymentYear: String = "2019"
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val staticTaxYear: TaxYear = TaxYear(fixedDate.getYear - 1, fixedDate.getYear)
  val baseStatusDetail: StatusDetail = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Annual, StatusReason.SignupReturnAvailable, Some(8000.25))

  val expectedYourTasksTitle = s"${messages("newHome.navigation.yourTasks")}"
  val expectedOverdueAmount = s"${messages("newHome.yourTasks.selfAssessment.overdueCharge.single", "1000.0 ")}Was due 31 Jan 2019"
  val expectedCredit = s"${messages("newHome.yourTasks.selfAssessment.money-in-account", "1000")}"
  val expectedUpcomingAnnualSubmissionTag = s"${messages("new.home.yourTasks.upcoming-annual-updates-label", "15 Mar 2024")}"
  val expectedUpcomingAnnualSubmissionDeadlineBody = "You have an upcoming annual submission deadline."
  val expectedOverdueQuarterlySubmissionDeadlineBody = "You have an overdue quarterly submission."
  val expectedOverdueQuarterlySubmissionTag = s"${messages("new.home.yourTasks.overdue-quarterly-submission-single-label", "15 Dec 2022")}"

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
      mockNextUpdatesService)


    setupMockUserAuth
    mockSingleBusinessIncomeSource()
    when(mockDateServiceInjected.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInjected.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1
    val testHomeController = app.injector.instanceOf[HomeController]
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
    when(mockDateServiceInjected.getCurrentDate) thenReturn fixedDate
    when(mockDateServiceInjected.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1
  }

  "show()" when {
    "an authenticated use" should {
      "render the Handle your tasks page with a Your tasks tab open" in new Setup {
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
        when(mockNextUpdatesService.getNextDueDates()(any(), any()))
          .thenReturn(Future.successful(None, None))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        session(result).get(SessionKeys.mandationStatus) shouldBe Some("on")

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("#main-content h2").text shouldBe expectedYourTasksTitle
        document.select("#yourTasksTile .tile-body div").text shouldBe expectedOverdueAmount
      }

      "render the Handle your tasks page with a Your tasks tab open and have money in your account card present" in new Setup {
        setupMockUserAuth
        mockItsaStatusRetrievalAction()
        enable(CreditsRefundsRepay)
        val financialDetails = List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, Some(BigDecimal(1000)), None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            mainTransaction = Some("4920"), transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate))))))))
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(financialDetails))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
        setupMockGetFilteredChargesListFromFinancialDetails(financialDetails.flatMap(_.asChargeItems))
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
        setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
        when(mockedSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedOptOutService.updateJourneyStatusInSessionData(any())(any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedCreditService.getAllCredits(any(), any()))
          .thenReturn(Future.successful(
            ANewCreditAndRefundModel().withTotalCredit(BigDecimal(1000))
              .model
          ))
        when(mockNextUpdatesService.getNextDueDates()(any(), any()))
          .thenReturn(Future.successful(None, None))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        session(result).get(SessionKeys.mandationStatus) shouldBe Some("on")

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("#main-content h2").text shouldBe expectedYourTasksTitle
        document.select("#moenyInYourAccount .tile-body div").text shouldBe expectedCredit
      }

      "render the Handle your tasks page with a Your tasks tab open and have view submission deadlines and overdue submissions and money in your account card present" in new Setup {
        setupMockUserAuth
        mockItsaStatusRetrievalAction()
        enable(CreditsRefundsRepay)
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
          .thenReturn(Future.successful(List.empty[FinancialDetailsModel]))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
        setupMockGetFilteredChargesListFromFinancialDetails(List.empty[FinancialDetailsModel].flatMap(_.asChargeItems))
        setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
        setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
        when(mockedSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedOptOutService.updateJourneyStatusInSessionData(any())(any(), any()))
          .thenReturn(Future.successful(true))
        when(mockedCreditService.getAllCredits(any(), any()))
          .thenReturn(Future.successful(
            ANewCreditAndRefundModel().withTotalCredit(BigDecimal(1000))
              .model
          ))
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

        when(mockNextUpdatesService.getNextDueDates()(any(), any()))
          .thenReturn(Future.successful(None, Some(nextTaxReturnDueDate)))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        session(result).get(SessionKeys.mandationStatus) shouldBe Some("on")

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("#main-content h2").text shouldBe expectedYourTasksTitle
        document.select("#moenyInYourAccount .tile-body div").text shouldBe expectedCredit

        document.select(".govuk-body").get(2).text() shouldBe expectedOverdueQuarterlySubmissionDeadlineBody
        document.select(".govuk-tag").get(1).select("span").text() shouldBe expectedOverdueQuarterlySubmissionTag
        document.select(".govuk-tag").get(1).select("span").hasClass("govuk-tag govuk-tag--red") shouldBe true

        document.select(".govuk-tag").get(2).select("span").prev().text() shouldBe expectedUpcomingAnnualSubmissionDeadlineBody
        document.select(".govuk-tag").get(2).select("span").text() shouldBe expectedUpcomingAnnualSubmissionTag
        document.select(".govuk-tag").get(2).select("span").hasClass("govuk-tag govuk-tag--green") shouldBe true
      }
    }
  }
}
