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

import audit.mocks.MockAuditingService
import config.featureswitch.{CreditsRefundsRepay, FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService, MockWhatYouOweService}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.i18n.Lang
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import play.i18n
import play.i18n.MessagesApi
import services.{CreditService, DateService, FinancialDetailsService, NextUpdatesService, WhatYouOweService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment, testTaxYear}
import testConstants.FinancialDetailsTestConstants.financialDetailsModel
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, businessesAndPropertyIncomeCeased}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class HomeControllerSpec extends TestSupport with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with FeatureSwitching
  with MockAuditingService with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with BeforeAndAfterEach
  with MockItvcErrorHandler with MockNextUpdatesService with MockFinancialDetailsService with MockWhatYouOweService with Injecting {

  val updateYear: String = "2018"
  val nextPaymentYear: String = "2019"
  val nextPaymentYear2: String = "2018"
  val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(updateYear.toInt, Month.JANUARY, 1), Seq.empty[LocalDate])
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val nextPaymentDate2: LocalDate = LocalDate.of(nextPaymentYear2.toInt, Month.JANUARY, 31)
  val emptyWhatYouOweChargesListIndividual: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.0, 0.0, 0.0, None, None, None, None))
  val oneOverdueBCDPaymentInWhatYouOweChargesListIndividual: WhatYouOweChargesList =
    emptyWhatYouOweChargesListIndividual.copy(
      outstandingChargesModel = Some(OutstandingChargesModel(List(OutstandingChargeModel("BCD", Some("2019-01-31"), 1.67, 2345))))
    )
  val homePageTitle = s"${messages("htmlTitle", messages("home.heading"))}"
  val agentTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
  val mockDateService: DateService = mock(classOf[DateService])

  trait Setup {
    val NextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
    val financialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])
    val whatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])
    val creditService: CreditService = mock(classOf[CreditService])

    val controller = new HomeController(
      app.injector.instanceOf[views.html.Home],
      mockAuthService,
      NextUpdatesService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      mockIncomeSourceDetailsService,
      financialDetailsService,
      mockDateService,
      whatYouOweService,
      mockAuditingService,
      testAuthenticator)(
      ec,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig]
    )
    when(mockDateService.getCurrentDate(any())) thenReturn LocalDate.now()

    val overdueWarningMessageDunningLockTrue: String = messages("home.overdue.message.dunningLock.true")
    val overdueWarningMessageDunningLockFalse: String = messages("home.overdue.message.dunningLock.false")
    val expectedOverDuePaymentsText1 = s"${messages("home.overdue.date")} 31 January 2019"
    lazy val expectedAvailableCreditText: String => String = (amount: String) => messages("home.paymentHistoryRefund.availableCredit", amount)
    val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  }

  //new setup for agent
  implicit val lang: Lang = Lang("en-US")
  val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  val javaMessagesApi: MessagesApi = inject[play.i18n.MessagesApi]
  val overdueWarningMessageDunningLockTrue: String = javaMessagesApi.get(new i18n.Lang(lang), "home.agent.overdue.message.dunningLock.true")
  val overdueWarningMessageDunningLockFalse: String = javaMessagesApi.get(new i18n.Lang(lang), "home.agent.overdue.message.dunningLock.false")
  val expectedOverDuePaymentsText = s"${messages("home.overdue.date")} 31 January 2019"
  val twoOverduePayments: String = messages("home.overdue.date.payment.count", "2")


  object TestHomeController extends HomeController(
    app.injector.instanceOf[views.html.Home],
    mockAuthService,
    mockNextUpdatesService,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    mockFinancialDetailsService,
    mockDateService,
    mockWhatYouOweService,
    mockAuditingService,
    testAuthenticator)(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  when(mockDateService.getCurrentDate(any())) thenReturn LocalDate.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  "navigating to the home page" should {
    "return ok (200)" when {
      "there is a next payment due date to display" in new Setup {
        disableAllSwitches()
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()
        when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
              documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
            financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
              transactionId = Some("testId"),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
          ))))
        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText1
      }

      "there is a next payment due date to display when getWhatYouOweChargesList contains overdue payment" in new Setup {
        disableAllSwitches()
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()
        when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText1
      }

      "display number of payments due when there are multiple payment due and dunning locks" in new Setup {
        disableAllSwitches()
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()

        when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString), dunningLock = Some("Stand over order"))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
        document.select("#overdue-warning").text shouldBe s"! Warning ${messages("home.overdue.message.dunningLock.true")}"
      }

      "display number of payments due when there are multiple payment due without dunning lock and filter out payments" in new Setup {
        when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
        mockSingleBusinessIncomeSource()

        when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29),
                paymentLotItem = Some("123"), paymentLot = Some("456"), documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
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
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
          when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"

        }

        "There are no financial detail" in new Setup {
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None), List(), List()))))
          when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }

        "All financial detail bill are paid" in new Setup {
          when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, Some(0), None, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
            ))))
          when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
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
    "there is a update date to display" in new Setup {
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      mockSingleBusinessIncomeSource()
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2018"
    }
    "display the Income Sources tile with `Cease an income source` when user has non-ceased businesses or property" in new Setup {
      enable(IncomeSources)
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))
      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
      document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url
      document.select("#income-sources-tile > div > p:nth-child(4) > a").text() shouldBe messages("home.incomeSources.ceaseIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    }
    "display the Income Sources tile without `Cease an income source` when user has ceased businesses or property" in new Setup {
      enable(IncomeSources)
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncomeCeased)
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))
      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
      document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url
      document.select("#income-sources-tile > div > p:nth-child(4) > a").text() should not be messages("home.incomeSources.ceaseIncomeSource.view")
      document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") should not be controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
    }
    "display the available credit when CreditsAndRefundsRepay FS is enabled" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      mockSingleBusinessIncomeSource()
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£786.00")
    }
    "display £0.00 available credit when available credit is None" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      mockSingleBusinessIncomeSource()
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£0.00")
    }
    "not display the available credit when CreditsAndRefundsRepay FS is disabled" in new Setup {
      disable(CreditsRefundsRepay)
      when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
      mockSingleBusinessIncomeSource()
      when(financialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      when(whatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyWhatYouOweChargesListIndividual))

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      Option(document.getElementById("available-credit")).isDefined shouldBe false
    }
  }

  "navigate to homepage as Agent" should {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = TestHomeController.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
      }
    }

    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = TestHomeController.showAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
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
          when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
          mockSingleBusinessIncomeSource()
          when(mockNextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any(), any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
          setupMockGetWhatYouOweChargesListEmpty()

          val result = TestHomeController.showAgent()(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "retrieving their obligation due date details was successful" when {
        "retrieving their charge due date details had a failure" should {
          "return an internal server exception" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
            setupMockGetWhatYouOweChargesListEmpty()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        "retrieving their charge due date details was successful" should {
          "display the home page with right details and without dunning lock warning and one overdue payment" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(dueDateValue = Some(LocalDate.of(2021, 5, 15).toString)))))
            setupMockGetWhatYouOweChargesListEmptyFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe s"${messages("home.overdue.date")} 15 June 2018"
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockFalse"
          }
          "display the home page with right details and without dunning lock warning and one overdue payment from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
          }
          "display the home page with right details and with dunning lock warning and two overdue payments" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")))))
            setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and two overdue payments from FinancialDetailsService and one from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")),
                financialDetailsModel(testTaxYear))))
            setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe messages("home.overdue.date.payment.count", "3")
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and with dunning lock warning and one overdue payments from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order"), dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockTrue"
          }
          "display the home page with right details and without dunning lock warning and one overdue payments from CESA" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate(any())).thenReturn(LocalDate.now())
            mockSingleBusinessIncomeSource()
            mockNextDeadlineDueDateAndOverDueObligations()(updateDateAndOverdueObligationsLPI)
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = None, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails()

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockFalse"
          }
        }
      }
    }
  }
}