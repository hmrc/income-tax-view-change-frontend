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

import enums.MTDPrimaryAgent
import models.admin.{CreditsRefundsRepay, IncomeSourcesFs, IncomeSourcesNewJourney, ReviewAndReconcilePoa}
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.Injecting
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, businessesAndPropertyIncomeCeased}

import java.time.LocalDate
import scala.concurrent.Future

class HomeControllerPrimaryAgentSpec extends HomeControllerHelperSpec with Injecting {

  lazy val testHomeController = app.injector.instanceOf[HomeController]

  trait Setup {
    val controller = testHomeController
    when(mockDateService.getCurrentDate) thenReturn fixedDate
    when(mockDateService.getCurrentTaxYearEnd) thenReturn fixedDate.getYear + 1

    lazy val homePageTitle = s"${messages("htmlTitle.agent", messages("home.agent.heading"))}"
    lazy val homePageCaption = "You are signed in as a main agent"
    lazy val homePageHeading = s"${messages("home.agent.headingWithClientName", "Test User")}"

    val overdueWarningMessageDunningLockTrue: String = messages("home.agent.overdue.message.dunningLock.true")
    val overdueWarningMessageDunningLockFalse: String = messages("home.agent.overdue.message.dunningLock.false")
    val expectedOverDuePaymentsText = s"${messages("home.overdue.date")} 31 January 2019"
    lazy val expectedAvailableCreditText: String => String = (amount: String) => messages("home.paymentHistoryRefund.availableCredit", amount)
    val threeOverduePayments: String = messages("home.overdue.date.payment.count", "3")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }


  "show()" when {
    val agentType = MTDPrimaryAgent
    val isSupportingAgent = false
    val fakeRequest = fakeRequestConfirmedClient()

    s"the user is authenticated $agentType" should {
      "render the home page with a Next Payments due tile" that {
        "has payments due" when {
          "the user has overdue payments and does not owe any charges" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            val financialDetailsModels = List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
              financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"), mainTransaction = Some("4920"),
                transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(financialDetailsModels))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(financialDetailsModels.flatMap(_.asChargeItems))
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("h1").text() shouldBe homePageHeading
            document.getElementsByClass("govuk-caption-xl").text() shouldBe homePageCaption
            document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
          }

          "the user has payments due and has overdue payments" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
            when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesList))
            setupMockGetFilteredChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList.chargesList)
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe expectedOverDuePaymentsText
          }
        }

        "has the number of payments due" when {
          "the user has multiple overdue payments with dunning locks and does not owe any charges" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            val financialDetailsModels = List(
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"), mainTransaction = Some("4910"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"), mainTransaction = Some("4920"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString), dunningLock = Some("Stand over order"))))))),
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"), mainTransaction = Some("4930"),
                  transactionId = Some("testId3"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
            )
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(financialDetailsModels))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(financialDetailsModels.flatMap(_.asChargeItems))
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe threeOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockTrue"
          }

          "the user has multiple overdue payments without dunning locks and does not owe any charges" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            val financialDetailsModels = List(
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"), mainTransaction = Some("4910"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId1", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29),
                  paymentLotItem = Some("123"), paymentLot = Some("456"), documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"), mainTransaction = Some("4910"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"), mainTransaction = Some("4920"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
              FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("ITSA - POA 2"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"), mainTransaction = Some("4930"),
                  transactionId = Some("testId3"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
            )
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(financialDetailsModels))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(financialDetailsModels.flatMap(_.asChargeItems).take(3))
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#payments-tile p:nth-child(2)").text shouldBe threeOverduePayments
            document.select("#overdue-warning").text shouldBe s"! Warning $overdueWarningMessageDunningLockFalse"
          }
        }

        "shows the daily interest accruing warning and tag" when {
          "the user has payments accruing interest" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            enable(ReviewAndReconcilePoa)

            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(
                FinancialDetailsModel(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                  documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                    documentDueDate = Some(futureDueDates.head), interestOutstandingAmount = Some(400))),
                  financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
                    items = Some(Seq(SubItem(dueDate = Some(futureDueDates.head))))))),
                FinancialDetailsModel(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                  documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                    documentDueDate = Some(futureDueDates.head), interestOutstandingAmount = Some(400))),
                  financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA POA 2 Reconciliation Debit"),
                    transactionId = Some("testId3"),
                    items = Some(Seq(SubItem(dueDate = Some(futureDueDates.head)))))))
              )))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#accrues-interest-tag").text shouldBe messages("home.payments.daily-interest-charges")
            document.select("#accrues-interest-warning").text shouldBe s"! Warning ${messages("home.interest-accruing")}"
          }
        }


        "does not show the daily interest accruing warning and tag" when {
          "the user has overdue payments accruing interest" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockSingleBusinessIncomeSource()
            mockGetDueDates(Right(futureDueDates))
            enable(ReviewAndReconcilePoa)

            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(
                FinancialDetailsModel(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                  documentDetails = List(DocumentDetail(nextPaymentYear2.toInt, "testId2", Some("SA POA 1 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                    documentDueDate = Some(nextPaymentDate2.toString), interestOutstandingAmount = Some(400))),
                  financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA POA 1 Reconciliation Debit"), transactionId = Some("testId2"),
                    items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
                FinancialDetailsModel(
                  balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                  documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId3", Some("SA POA 2 Reconciliation Debit"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                    documentDueDate = Some(nextPaymentDate2.toString), interestOutstandingAmount = Some(400))),
                  financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA POA 2 Reconciliation Debit"),
                    transactionId = Some("testId3"),
                    items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString)))))))
              )))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#accrues-interest-tag").text shouldBe ""
            document.select("#accrues-interest-warning").text shouldBe ""
          }
        }
      }

      "render the home page without a Next Payments due tile" when {
        "there is a problem getting financial details" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(futureDueDates))
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))

          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"

        }

        "There are no financial detail" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(futureDueDates))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List(), List(), List()))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }

        "All financial detail bill are paid" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(futureDueDates))
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 0, 0, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
            ))))
          when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyWhatYouOweChargesList))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#payments-tile p:nth-child(2)").text shouldBe "No payments due"
          document.select("#overdue-warning").text shouldBe ""
        }
      }

      "render the home page controller with the next updates tile" when {
        "there is a future update date to display" in new Setup {
          setupNextUpdatesTests(futureDueDates, agentType)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "1 January 2100"
        }

        "there is an overdue update date to display" in new Setup {
          setupNextUpdatesTests(overdueDueDates, agentType)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile p:nth-child(2)").text() shouldBe "Overdue 1 January 2018"
        }

        "there are no updates to display" in new Setup {
          setupNextUpdatesTests(Seq(), agentType)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text() shouldBe messages("home.updates.heading")
        }
      }

      "render the home without the Next Updates tile" when {
        "the user has no updates due" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          mockSingleBusinessIncomeSource()
          mockGetDueDates(Right(Seq()))
          mockGetAllUnpaidFinancialDetails()
          setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK

          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#updates-tile").text shouldBe messages("home.updates.heading")
        }
      }

      "render the home page with the Income Sources tile" that {
        "has `Cease an income source`" when {
          "the user has non-ceased businesses or property and income sources is enabled" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSourcesFs)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
                financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
              ))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
            document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url
            document.select("#income-sources-tile > div > p:nth-child(4) > a").text() shouldBe messages("home.incomeSources.ceaseIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
          }
        }

        "does not have a `Cease an income source`" when {
          "the user has ceased businesses or property and income sources is enabled" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(IncomeSourcesFs)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncomeCeased)
            mockGetDueDates(Right(futureDueDates))
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
                financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
              ))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)
            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.title shouldBe homePageTitle
            document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.heading")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.addIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
            document.select("#income-sources-tile > div > p:nth-child(3) > a").text() shouldBe messages("home.incomeSources.manageIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(3) > a").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url
            document.select("#income-sources-tile > div > p:nth-child(4) > a").text() should not be messages("home.incomeSources.ceaseIncomeSource.view")
            document.select("#income-sources-tile > div > p:nth-child(4) > a").attr("href") should not be controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
          }
        }
      }

      "render the home page with the Your Businesses tile with link" when {
        "the IncomeSourcesNewJourney is enabled" in new Setup {
          setupMockAgentWithClientAuth(isSupportingAgent)
          enable(IncomeSourcesFs, IncomeSourcesNewJourney)
          mockGetDueDates(Right(futureDueDates))
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
            .thenReturn(Future.successful(List(FinancialDetailsModel(
              balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
              documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", None, None, 1000.00, 0, LocalDate.of(2018, 3, 29))),
              financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
                items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
            ))))
          setupMockGetWhatYouOweChargesListFromFinancialDetails(oneOverdueBCDPaymentInWhatYouOweChargesList)
          setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
          setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
          setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
          val result: Future[Result] = controller.showAgent()(fakeRequest)
          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title shouldBe homePageTitle
          document.select("#income-sources-tile h2:nth-child(1)").text() shouldBe messages("home.incomeSources.newJourneyHeading")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").text() shouldBe messages("home.incomeSources.newJourney.view")
          document.select("#income-sources-tile > div > p:nth-child(2) > a").attr("href") shouldBe controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
        }
      }

      "render the home page with Payment history and refunds tile" that {
        "contains the available credit" when {
          "CreditsAndRefundsRepay FS is enabled and credit is available" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(CreditsRefundsRepay)
            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
                  transactionId = Some("testId"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
              ))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£786.00")
          }

          "CreditsAndRefundsRepay FS is enabled and credit is not available" in new Setup {
            setupMockAgentWithClientAuth(isSupportingAgent)
            enable(CreditsRefundsRepay)
            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
                  transactionId = Some("testId"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
              ))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            document.getElementById("available-credit").text shouldBe expectedAvailableCreditText("£0.00")
          }
        }

        "does not contain available credit" when {
          "CreditsAndRefundsRepay FS is disabled" in new Setup {
            disable(CreditsRefundsRepay)
            setupMockAgentWithClientAuth(isSupportingAgent)
            mockGetDueDates(Right(Seq.empty))
            mockSingleBusinessIncomeSource()
            when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
              .thenReturn(Future.successful(List(FinancialDetailsModel(
                balanceDetails = BalanceDetails(1.00, 2.00, 3.00, Some(786), None, None, None, None),
                documentDetails = List(DocumentDetail(nextPaymentYear.toInt, "testId", Some("ITSA- POA 1"), Some("documentText"), 1000.00, 0, LocalDate.of(2018, 3, 29),
                  documentDueDate = Some(LocalDate.of(2019, 1, 31)))),
                financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
                  transactionId = Some("testId"),
                  items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
              ))))
            setupMockGetWhatYouOweChargesListFromFinancialDetails(emptyWhatYouOweChargesList)
            setupMockGetFilteredChargesListFromFinancialDetails(emptyWhatYouOweChargesList.chargesList)
            setupMockGetStatusTillAvailableFutureYears(staticTaxYear)(Future.successful(Map(staticTaxYear -> baseStatusDetail)))
            setupMockHasMandatedOrVoluntaryStatusCurrentYear(true)
            val result: Future[Result] = controller.showAgent()(fakeRequest)

            status(result) shouldBe Status.OK
            val document: Document = Jsoup.parse(contentAsString(result))
            Option(document.getElementById("available-credit")).isDefined shouldBe false
          }
        }
      }
    }

    testMTDAgentAuthFailures(testHomeController.showAgent(), false)
    testMTDObligationsDueFailures(testHomeController.showAgent(), agentType)(fakeRequest)
  }
}
