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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import exceptions.AgentException
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService, MockNextUpdatesService, MockWhatYouOweService}
import models.admin.{CreditsRefundsRepay, IncomeSources, IncomeSourcesNewJourney}
import models.financialDetails._
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
import services.DateService
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
  private val futureDueDates: Seq[LocalDate] = Seq(LocalDate.of(2100, 1, 1))
  private val overdueDueDates: Seq[LocalDate] = Seq(LocalDate.of(2018, 1, 1))
  val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(updateYear.toInt, Month.JANUARY, 1), futureDueDates)
  val nextPaymentDate: LocalDate = LocalDate.of(nextPaymentYear.toInt, Month.JANUARY, 31)
  val nextPaymentDate2: LocalDate = LocalDate.of(nextPaymentYear2.toInt, Month.JANUARY, 31)
  val homePageTitle = s"${messages("htmlTitle", messages("home.heading"))}"
  val agentTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
  val mockDateService: DateService = mock(classOf[DateService])

  trait Setup {
    val controller = new HomeController(
      app.injector.instanceOf[views.html.Home],
      mockAuthService,
      mockNextUpdatesService,
      mockIncomeSourceDetailsService,
      mockFinancialDetailsService,
      mockDateService,
      mockWhatYouOweService,
      mockAuditingService,
      testAuthenticator)(
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig]
    )
    when(mockDateService.getCurrentDate) thenReturn fixedDate

    val overdueWarningMessageDunningLockTrue: String = messages("home.overdue.message.dunningLock.true")
    val overdueWarningMessageDunningLockFalse: String = messages("home.overdue.message.dunningLock.false")
    val expectedOverDuePaymentsText1 = s"${messages("home.overdue.date")} 31 January 2019"
    lazy val expectedAvailableCreditText: String => String = (amount: String) => messages("home.paymentHistoryRefund.availableCredit", amount)
    val updateDateAndOverdueObligationsLPI: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2021, Month.MAY, 15), Seq.empty[LocalDate])
  }

  //new setup for agent
  implicit val lang: Lang = Lang("en-US")
  val javaMessagesApi: MessagesApi = inject[play.i18n.MessagesApi]
  val overdueWarningMessageDunningLockTrue: String = javaMessagesApi.get(new i18n.Lang(lang), "home.agent.overdue.message.dunningLock.true")
  val overdueWarningMessageDunningLockFalse: String = javaMessagesApi.get(new i18n.Lang(lang), "home.agent.overdue.message.dunningLock.false")
  val expectedOverDuePaymentsText = s"${messages("home.overdue.date")} 31 January 2019"
  val twoOverduePayments: String = messages("home.overdue.date.payment.count", "2")


  object TestHomeController extends HomeController(
    app.injector.instanceOf[views.html.Home],
    mockAuthService,
    mockNextUpdatesService,
    mockIncomeSourceDetailsService,
    mockFinancialDetailsService,
    mockDateService,
    mockWhatYouOweService,
    mockAuditingService,
    testAuthenticator)(
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  when(mockDateService.getCurrentDate) thenReturn fixedDate

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  "navigating to the home page" should {
    "return ok (200)" when {
      "there is a next payment due date to display" in new Setup {
        disableAllSwitches()
        mockGetDueDates(Right(futureDueDates))
        mockSingleBusinessIncomeSource()
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
              documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
            financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
              transactionId = Some("testId"),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
          ))))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText1
      }

      "there is a next payment due date to display when getWhatYouOweChargesList contains overdue payment" in new Setup {
        disableAllSwitches()
        mockGetDueDates(Right(futureDueDates))
        mockSingleBusinessIncomeSource()
        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
        when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesList))

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText1
      }

      "display number of payments due when there are multiple payment due and dunning locks" in new Setup {
        disableAllSwitches()
        mockGetDueDates(Right(futureDueDates))
        mockSingleBusinessIncomeSource()

        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString), dunningLock = Some("Stand over order"))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
        document.select("#overdue-warning").text shouldBe s"! Warning ${messages("home.overdue.message.dunningLock.true")}"
      }

      "display number of payments due when there are multiple payment due without dunning lock and filter out payments" in new Setup {
        mockGetDueDates(Right(futureDueDates))
        mockSingleBusinessIncomeSource()

        when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(List(
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                paymentLotItem = Some("123"), paymentLot = Some("456"), documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
            FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
                transactionId = Some("testId3"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
          )))
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
        document.select("#overdue-warning").text shouldBe s"! Warning ${messages("home.overdue.message.dunningLock.false")}"
      }

      "Not display the next payment due date" when {
        "there is a problem getting financial details" in new Setup {
          mockGetDueDates(Right(futureDueDates))
          mockSingleBusinessIncomeSource()
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"

        }

        "There are no financial detail" in new Setup {
          mockGetDueDates(Right(futureDueDates))
          mockSingleBusinessIncomeSource()
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List(), List()))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }

        "All financial detail bill are paid" in new Setup {
          mockGetDueDates(Right(futureDueDates))
          mockSingleBusinessIncomeSource()
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 0, 0, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
            ))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))

          val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }
      }
      "there is no update date to display - Individual" in new Setup {
        mockGetDueDates(Right(Seq.empty))
        mockSingleBusinessIncomeSource()
        mockGetAllUnpaidFinancialDetails()
        setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title shouldBe homePageTitle
        document.select("#updates-tile").text shouldBe messages("home.updates.heading")
      }
    }

    def setupNextUpdatesTests(dueDates: Seq[LocalDate]): Unit = {
      mockGetDueDates(Right(dueDates))
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
    }

    "there is a future update date to display" in new Setup {
      setupNextUpdatesTests(futureDueDates)

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2100"
    }
    "there is an overdue update date to display" in new Setup {
      setupNextUpdatesTests(overdueDueDates)

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#updates-tile p:nth-child(2)").text() shouldBe "OVERDUE 1 January 2018"
    }
    "there are no updates to display" in new Setup {
      setupNextUpdatesTests(Seq())

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#updates-tile").text() shouldBe messages("home.updates.heading")
    }

    "display the Income Sources tile with `Cease an income source` when user has non-ceased businesses or property" in new Setup {
      enable(IncomeSources)
      mockGetDueDates(Right(futureDueDates))
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
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
      mockGetDueDates(Right(futureDueDates))
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncomeCeased)
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
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
    "display the Income Sources tile with title Your Businesses and new link when new journey FS is enabled" in new Setup {
      enable(IncomeSources)
      enable(IncomeSourcesNewJourney)
      mockGetDueDates(Right(futureDueDates))
      setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
          financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.title shouldBe homePageTitle
      document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.newJourneyHeading")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.newJourney.view")
      document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.manageBusinesses.routes.ManageYourBusinessesController.show(false).url
    }

    "display the available credit when CreditsAndRefundsRepay FS is enabled" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      mockGetDueDates(Right(Seq.empty))
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£786.00")
    }
    "display £0.00 available credit when available credit is None" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      mockGetDueDates(Right(Seq.empty))
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

      val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£0.00")
    }
    "not display the available credit when CreditsAndRefundsRepay FS is disabled" in new Setup {
      disable(CreditsRefundsRepay)
      mockGetDueDates(Right(Seq.empty))
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

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

        result.failed.futureValue shouldBe an[AgentException]
        result.failed.futureValue.getMessage shouldBe "IncomeSourceDetailsModel not created"
      }
    }

    "the call to retrieve income sources for the client is successful" when {
      "retrieving their obligation due date details had a failure" should {
        "return an internal server exception" in new Setup {

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          when(mockDateService.getCurrentDate).thenReturn(fixedDate)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Left(new Exception("obligation test exception")))
          setupMockGetWhatYouOweChargesList(emptyWhatYouOweChargesList)

          val result = TestHomeController.showAgent()(fakeRequestConfirmedClient())

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "retrieving their obligation due date details was successful" when {
        "retrieving their charge due date details had a failure" should {
          "return an internal server exception" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any())) thenReturn Future.failed(new InternalServerException("obligation test exception"))
            setupMockGetWhatYouOweChargesList(emptyWhatYouOweChargesList)

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        "retrieving their charge due date details was successful" should {
          "display the home page with right details and without dunning lock warning and one overdue payment" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockGetDueDates(Right(futureDueDates))

            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(dueDateValue = Some(LocalDate.of(2021, 5, 15).toString)))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

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
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
          }
          "display the home page with right details and with dunning lock warning and two overdue payments" in {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)

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
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order")),
                financialDetailsModel(testTaxYear))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)

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
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = Some("Stand over order"), dueDateValue = None))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)

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
            when(mockDateService.getCurrentDate).thenReturn(fixedDate)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
              .thenReturn(Future.successful(List(financialDetailsModel(testTaxYear, dunningLock = None, dueDateValue = None))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)

            val result: Future[Result] = TestHomeController.showAgent()(fakeRequestConfirmedClient())

            status(result) shouldBe OK
            contentType(result) shouldBe Some(HTML)
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe agentTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe twoOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockFalse"
          }
        }
        "there is no update date to display - Agent" in new Setup {
          setupMockAuthorisationSuccess(true)
          mockGetDueDates(Right(Seq.empty))
          mockSingleBusinessIncomeSource()
          mockGetAllUnpaidFinancialDetails()
          setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

          val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
          status(result) shouldBe Status.OK

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe agentTitle
          document.select("#updates-tile").text shouldBe messages("home.updates.heading")
        }
      }
    }
    "display the available credit when CreditsAndRefundsRepay FS is enabled" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockGetDueDates(Right(Seq.empty))
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

      val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£786.00")
    }
    "display £0.00 available credit when available credit is None" in new Setup {
      disableAllSwitches()
      enable(CreditsRefundsRepay)
      mockGetDueDates(Right(Seq.empty))
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

      val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£0.00")
    }
    "not display the available credit when CreditsAndRefundsRepay FS is disabled" in new Setup {
      disable(CreditsRefundsRepay)
      mockGetDueDates(Right(Seq.empty))
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      mockSingleBusinessIncomeSource()
      when(mockFinancialDetailsService.getAllUnpaidFinancialDetails(any())(any(), any(), any()))
        .thenReturn(Future.successful(List(FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
          documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
            documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
          financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
            transactionId = Some("testId"),
            items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
        ))))
      setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)

      val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())

      status(result) shouldBe Status.OK
      val document: Document = Jsoup.parse(contentAsString(result))
      Option(document.getElementById("available-credit")).isDefined shouldBe false
    }
  }
}
