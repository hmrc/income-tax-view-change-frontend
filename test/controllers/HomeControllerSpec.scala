/*
 * Copyright 2021 HM Revenue & Customs
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

import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, Payment}
import java.time.{LocalDate, ZonedDateTime}

import assets.{BaseTestConstants, MessagesLookUp}
import auth.MtdItUserWithNino
import config.featureswitch.{API5, Bills, FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.calculation.{Calculation, CalculationErrorModel, CalculationResponseModelWithYear}
import models.financialDetails.{Charge, FinancialDetailsErrorModel, FinancialDetailsModel, SubItem}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, SubItemModel, TransactionModel}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import services.{CalculationService, FinancialDetailsService, FinancialTransactionsService, ReportDeadlinesService}
import utils.CurrentDateProvider

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class HomeControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching{

  trait Setup {
    val reportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]
    val financialTransactionsService: FinancialTransactionsService = mock[FinancialTransactionsService]
    val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
    val currentDateProvider: CurrentDateProvider = mock[CurrentDateProvider]
    val controller = new HomeController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      reportDeadlinesService,
      app.injector.instanceOf[ItvcErrorHandler],
      financialTransactionsService,
      financialDetailsService,
      app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      currentDateProvider,
      app.injector.instanceOf[ImplicitDateFormatterImpl]
    )
    when(currentDateProvider.getCurrentDate()) thenReturn(LocalDate.of(2018, 1, 20))
  }

  val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2018, 1, 1), Seq.empty[LocalDate])
  val nextPaymentDate: LocalDate = LocalDate.of(2019, 1, 31)
  val nextPaymentDate2: LocalDate = LocalDate.of(2018, 1, 31)
  val updateYear: String = "2018"
  val nextPaymentYear: String = "2019"
  val nextPaymentYear2: String = "2018"
  val emptyEstimateCalculation: Calculation = Calculation(crystallised = false)
  val emptyCrystallisedCalculation: Calculation = Calculation(crystallised = true)

  "navigating to the home page" should {
    "NewFinancialDetailsApi FS is disabled" when {
      disable(NewFinancialDetailsApi)
      "return ok (200)" which {
        "there is a next payment due date to display" in new Setup {
          enable(Payment)
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
         	when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2019"
        }

        "display the oldest next payment due day when there multiple payment due" in new Setup {
					enable(API5)
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
					when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
						.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2018, 4, 5)), outstandingAmount = Some(0), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))
					when(financialTransactionsService.getFinancialTransactions(any(), matches(2018))(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2018, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate2))))))))))
          when(financialTransactionsService.getFinancialTransactions(any(), matches(2019))(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2018"
        }

        "Not display the next payment due date" when {
          "there is a problem getting financial transaction" in new Setup {
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
		when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
              .thenReturn(Future.successful(FinancialTransactionsErrorModel(1, "testString")))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"

          }

          "There are no financial transaction" in new Setup {
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
            when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
              .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), None)))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
          }

          "All financial transaction bill are paid" in new Setup {
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
            when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
              .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(0), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
          }
        }

        "return ISE (500)" when {
          "A calculation error model has returned from the service" in new Setup {
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

      }
      "return OK (200)" when {
        "there is a update date to display" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#updates-tile > div > p:nth-child(2)").text() shouldBe "1 January 2018"
        }
      }

    }
    "NewFinancialDetailsApi FS is enabled" when {
      "return ok (200)" which {
        "there is a next payment due date to display" in new Setup {
          enable(NewFinancialDetailsApi)
          enable(Payment)
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getFinancialDetails(any(), any())(any()))
            .thenReturn(Future.successful(FinancialDetailsModel(List(Charge(transactionId = "testId", taxYear = nextPaymentYear, outstandingAmount = Some(1000),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))))


          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2019"
        }

        "display the oldest next payment due day when there multiple payment due" in new Setup {
          enable(NewFinancialDetailsApi)
					enable(API5)
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()

					when(financialDetailsService.getFinancialDetails(any(), any())(any()))
						.thenReturn(Future.successful(FinancialDetailsModel(List(Charge(transactionId = "testId", taxYear = nextPaymentYear2, outstandingAmount = Some(1000),
							items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString)))))))))
          when(financialDetailsService.getFinancialDetails(matches(2018), any())(any()))
            .thenReturn(Future.successful(FinancialDetailsModel(List(Charge(transactionId = "testId", taxYear = nextPaymentYear2, outstandingAmount = Some(1000),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString)))))))))
          when(financialDetailsService.getFinancialDetails(matches(2019), any())(any()))
            .thenReturn(Future.successful(FinancialDetailsModel(List(Charge(nextPaymentYear, "id", outstandingAmount = Some(1000),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2018"
        }

        "Not display the next payment due date" when {
          "there is a problem getting financial detalis" in new Setup {
            enable(NewFinancialDetailsApi)
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
            when(financialDetailsService.getFinancialDetails(any(), any())(any()))
              .thenReturn(Future.successful(FinancialDetailsErrorModel(1, "testString")))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"

          }

          "There are no financial detail" in new Setup {
            enable(NewFinancialDetailsApi)
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
            when(financialDetailsService.getFinancialDetails(any(), any())(any()))
              .thenReturn(Future.successful(FinancialDetailsModel(List())))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
          }

          "All financial detail bill are paid" in new Setup {
            enable(NewFinancialDetailsApi)
            when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
            mockSingleBusinessIncomeSource()
            when(financialDetailsService.getFinancialDetails(any(), any())(any()))
              .thenReturn(Future.successful(FinancialDetailsModel(List(Charge(nextPaymentYear, transactionId = "testId", outstandingAmount = Some(0), items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))))

            val result = controller.home(fakeRequestWithActiveSession)

            status(result) shouldBe Status.OK
            val document = Jsoup.parse(bodyOf(result))
            document.title shouldBe MessagesLookUp.HomePage.title
            document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
          }
        }
      }
      "return OK (200)" when {
        "there is a update date to display" in new Setup {
          enable(NewFinancialDetailsApi)
          when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
          mockSingleBusinessIncomeSource()
          when(financialDetailsService.getFinancialDetails(any(), any())(any()))
            .thenReturn(Future.successful(FinancialDetailsModel(List(Charge(nextPaymentYear, transactionId = "testId", outstandingAmount = Some(1000),
              items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe MessagesLookUp.HomePage.title
          document.select("#updates-tile > div > p:nth-child(2)").text() shouldBe "1 January 2018"
        }
      }
      disable(NewFinancialDetailsApi)
    }
  }

}
