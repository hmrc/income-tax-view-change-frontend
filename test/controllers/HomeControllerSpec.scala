/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import assets.Messages
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.calculation.{CalculationErrorModel, CalculationModel, CalculationResponseModelWithYear}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, SubItemModel, TransactionModel}
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{any, eq => matches}
import play.api.http.Status
import play.api.i18n.MessagesApi
import services.{CalculationService, FinancialTransactionsService, ReportDeadlinesService}

import scala.concurrent.Future

class HomeControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {



  trait Setup {
    val reportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]
    val calculationService: CalculationService = mock[CalculationService]
    val financialTransactionsService: FinancialTransactionsService = mock[FinancialTransactionsService]
    val controller = new HomeController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      reportDeadlinesService,
      calculationService,
      app.injector.instanceOf[ItvcErrorHandler],
      financialTransactionsService,
      app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesApi]
    )
  }
  val updateDate: LocalDate = LocalDate.of(2018, 1, 1)
  val nextPaymentDate: LocalDate =LocalDate.of(2019,1,31)
  val nextPaymentDate2: LocalDate =LocalDate.of(2018,1,31)

  "navigating to the home page" should {
    "return ok (200)" which {
      "there is a next payment due date to display" in new Setup {
        when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
        mockSingleBusinessIncomeSource()
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
        when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
          .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

        val result = controller.home(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document = Jsoup.parse(bodyOf(result))
        document.title shouldBe Messages.HomePage.title
        document.getElementById("income-tax-payment-card-body-date").text() shouldBe "31 January 2019"
      }

      "display the oldest next payment due day when there multiple payment due" in new Setup {
        when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
        mockSingleBusinessIncomeSource()
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2018), CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
        when(financialTransactionsService.getFinancialTransactions(any(), matches(2018))(any()))
          .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2018, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate2))))))))))
        when(financialTransactionsService.getFinancialTransactions(any(), matches(2019))(any()))
          .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

        val result = controller.home(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document = Jsoup.parse(bodyOf(result))
        document.title shouldBe Messages.HomePage.title
        document.getElementById("income-tax-payment-card-body-date").text() shouldBe "31 January 2018"
      }

      "Not display the next payment due date" when {
        "there is a problem getting financial transaction" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
          when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsErrorModel(1, "testString")))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."

        }

        "There are no financial transaction" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
          when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), None)))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."
        }

        "All financial transaction bill are paid" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
          when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(0), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."
        }

        "There is no calculation data" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List()))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."
        }

        s"All calculation error models from the service have a status of ${Status.NOT_FOUND}" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationErrorModel(Status.NOT_FOUND, "Not Found"), 2019))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."
        }

        "There are no crystallised calculation data" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(false), None, None, None), 2019))))
          when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
            .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK
          val document = Jsoup.parse(bodyOf(result))
          document.title shouldBe Messages.HomePage.title
          document.getElementById("income-tax-payment-card-body-date").text() shouldBe "No payments due."
        }
      }

      "return ISE (500)" when {
        "A calculation error model has returned from the service" in new Setup {
          when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
          mockSingleBusinessIncomeSource()
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationErrorModel(500,"errorMsg"), 2019))))

          val result = controller.home(fakeRequestWithActiveSession)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

    }
    "return OK (200)" when {
      "there is a update date to display" in new Setup {
        when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
        mockSingleBusinessIncomeSource()
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(List(CalculationResponseModelWithYear(CalculationModel("calcId", None, None, Some(true), None, None, None), 2019))))
        when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
          .thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

        val result = controller.home(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        val document = Jsoup.parse(bodyOf(result))
        document.title shouldBe Messages.HomePage.title
        document.getElementById("updates-card-body-date").text() shouldBe "1 January 2018"
      }
    }

  }

}
