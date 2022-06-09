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

package controllers

import audit.mocks.MockAuditingService
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService, MockWhatYouOweService}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.i18n.Lang
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import play.i18n
import play.i18n.MessagesApi
import services.{DateService, FinancialDetailsService, NextUpdatesService, WhatYouOweService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testTaxYear}
import testConstants.FinancialDetailsTestConstants.financialDetailsModel
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class HomeControllerSpec extends TestSupport with MockIncomeSourceDetailsService with MockFrontendAuthorisedFunctions
  with MockAuditingService with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with BeforeAndAfterEach
  with MockItvcErrorHandler with MockNextUpdatesService with MockFinancialDetailsService with MockWhatYouOweService with Injecting {

  val updateYear: String = "2018"
  val nextPaymentYear: String = "2019"
  val nextPaymentYear2: String = "2018"
  val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(updateYear.toInt, Month.JANUARY, 1), Seq.empty[LocalDate])
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val nextPaymentDate2: LocalDate = LocalDate.of(nextPaymentYear2.toInt, Month.JANUARY, 31)
  val emptyWhatYouOweChargesListIndividual: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.0, 0.0, 0.0, None, None, None,None))
  val oneOverdueBCDPaymentInWhatYouOweChargesListIndividual: WhatYouOweChargesList =
    emptyWhatYouOweChargesListIndividual.copy(
      outstandingChargesModel = Some(OutstandingChargesModel(List(OutstandingChargeModel("BCD", Some("2019-01-31"), 1.67, 2345))))
    )
  val homePageTitle = s"${messages("titlePattern.serviceName.govUk", messages("home.heading"))}"
  val agentTitle = s"${messages("agent.title_pattern.service_name.govuk", messages("home.agent.heading"))}"

  trait Setup {
    val mockDateService: DateService = mock[DateService]
    val NextUpdatesService: NextUpdatesService = mock[NextUpdatesService]
    val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
    val whatYouOweService: WhatYouOweService = mock[WhatYouOweService]

    val controller = new HomeController(
      app.injector.instanceOf[views.html.Home],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      NextUpdatesService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      mockIncomeSourceDetailsService,
      financialDetailsService,
      mockDateService,
      whatYouOweService,
      app.injector.instanceOf[NavBarPredicate],
      mockAuditingService)(
      ec,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig]
    )
    when(mockDateService.getCurrentDate) thenReturn LocalDate.now()

    val overdueWarningMessageDunningLockTrue: String = messages("home.overdue.message.dunningLock.true")
    val overdueWarningMessageDunningLockFalse: String = messages("home.overdue.message.dunningLock.false")
    val expectedOverDuePaymentsText = s"${messages("home.overdue.date")} 31 January 2019"
    val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  }

  //new setup for agent
  implicit val lang: Lang = Lang("en-US")
  val mockDateService: DateService = mock[DateService]
  val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  val javaMessagesApi: MessagesApi = inject[play.i18n.MessagesApi]
  val overdueWarningMessageDunningLockTrue: String = javaMessagesApi.get(new i18n.Lang(lang), "home.overdue.message.dunningLock.true")
  val overdueWarningMessageDunningLockFalse: String = javaMessagesApi.get(new i18n.Lang(lang), "home.overdue.message.dunningLock.false")
  val expectedOverDuePaymentsText = s"${messages("home.overdue.date")} 31 January 2019"
  val twoOverduePayments: String = messages("home.overdue.date.payment.count", "2")


  object TestHomeController extends HomeController(
    app.injector.instanceOf[views.html.Home],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockNextUpdatesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    mockFinancialDetailsService,
    mockDateService,
    mockWhatYouOweService,
    app.injector.instanceOf[NavBarPredicate],
    mockAuditingService)(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )
  when(mockDateService.getCurrentDate) thenReturn LocalDate.now()


  "navigating to the home page" should {
    "return ok (200)" which {
      "there is a next payment due date to display" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()
        when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(DocumentDetail(nextPaymentYear, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
            financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
              transactionId = Some("testId"),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))),
            codingDetails = None
          ))))
        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
      }

      "there is a next payment due date to display when getWhatYouOweChargesList contains overdue payment" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()
        when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
      }

      "display number of payments due when there are multiple payment due and dunning locks" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()

        when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear2, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString), dunningLock = Some("Stand over order"))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
        document.select("#overdue-warning").text shouldBe s"! Warning ${messages("home.overdue.message.dunningLock.true")}"
      }

      "display number of payments due when there are multiple payment due without dunning lock and filter out payments" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()

        when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29),
                paymentLotItem = Some("123"), paymentLot = Some("456")
              )),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear2, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
        document.select("#overdue-warning").text shouldBe s"! Warning ${messages("home.overdue.message.dunningLock.false")}"
      }

      "Not display the next payment due date" when {
        "there is a problem getting financial details" in new Setup {
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
          when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"

        }

        "There are no financial detail" in new Setup {
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None), None, List(), List()))))
          when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }

        "All financial detail bill are paid" in new Setup {
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              None,
              documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, None, Some(0), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
            ))))
          when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }
      }
    }
    "return OK (200)" when {
      "there is a update date to display" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()
        when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            None,
            documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
            financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
          ))))
        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2018"
      }
    }
  }

  "navigate to homepage as Agent" should {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestHomeController.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = TestHomeController.showAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }

    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = TestHomeController.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the call to retrieve income sources for the client returns an error" should {
      "return an internal server exception" in new Setup {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockErrorIncomeSource()

        val result = TestHomeController.showAgent()(fakeRequestConfirmedClient())

        result.failed.futureValue shouldBe an[InternalServerException]
        result.failed.futureValue.getMessage shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
      }
    }

    "the call to retrieve income sources for the client is successful" when {
      "retrieving their obligation due date details had a failure" should {
        "return an internal server exception" in new Setup {

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
          mockSingleBusinessIncomeSource()
          when(mockNextUpdatesService.getNextDeadlineDueDateAndOverDueObligations()(any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
          setupMockGetWhatYouOweChargesListEmpty()

          val result = TestHomeController.showAgent()(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "retrieving their obligation due date details was successful" when {
        "retrieving their charge due date details had a failure" should {
          "return an internal server exception" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
            setupMockGetWhatYouOweChargesListEmpty()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        "retrieving their charge due date details was successful" should {
          "display the home page with right details and without dunning lock warning and one overdue payment" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(dueDateValue = Some(LocalDate.of(2021, 5, 15).toString)))))
            setupMockGetWhatYouOweChargesListEmpty()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe s"${messages("home.overdue.date")} 15 June 2018"
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockFalse"
          }
          "display the home page with right details and without dunning lock warning and one overdue payment from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
          }
          "display the home page with right details and with dunning lock warning and two overdue payments" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and two overdue payments from FinancialDetailsService and one from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")),
                financialDetailsModel(testTaxYear))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe messages("home.overdue.date.payment.count", "3")
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and one overdue payments from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order"), dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and without dunning lock warning and one overdue payments from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = None, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOne()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! $overdueWarningMessageDunningLockFalse"
          }
        }
      }
    }
  }
}