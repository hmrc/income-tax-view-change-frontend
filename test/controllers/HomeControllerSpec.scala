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

import java.time.{LocalDate, ZonedDateTime}

import assets.MessagesLookUp
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.calculation.Calculation
import models.financialDetails._
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, SubItemModel, TransactionModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import services.{FinancialDetailsService, FinancialTransactionsService, ReportDeadlinesService}
import utils.CurrentDateProvider

import scala.concurrent.Future

class HomeControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

	val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2018, 1, 1), Seq.empty[LocalDate])
	val nextPaymentDate: LocalDate = LocalDate.of(2019, 1, 31)
	val nextPaymentDate2: LocalDate = LocalDate.of(2018, 1, 31)
	val updateYear: String = "2018"
	val nextPaymentYear: String = "2019"
	val nextPaymentYear2: String = "2018"
	val emptyEstimateCalculation: Calculation = Calculation(crystallised = false)
	val emptyCrystallisedCalculation: Calculation = Calculation(crystallised = true)

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
		when(currentDateProvider.getCurrentDate()) thenReturn LocalDate.of(2018, 1, 20)
	}

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

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2019"
				}

				"display the oldest next payment due day when there multiple payment due" in new Setup {
					when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()
					when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
						.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2018, 4, 5)), outstandingAmount = Some(0), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))
					when(financialTransactionsService.getFinancialTransactions(any(), matches(2018))(any()))
						.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2018, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate2))))))))))
					when(financialTransactionsService.getFinancialTransactions(any(), matches(2019))(any()))
						.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(1000), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2018"
				}

				"Not display the next payment due date" when {
					"there is a problem getting financial transaction" in new Setup {
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
							.thenReturn(Future.successful(FinancialTransactionsErrorModel(1, "testString")))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"

					}

					"There are no financial transaction" in new Setup {
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
							.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), None)))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
					}

					"All financial transaction bill are paid" in new Setup {
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialTransactionsService.getFinancialTransactions(any(), any())(any()))
							.thenReturn(Future.successful(FinancialTransactionsModel(None, None, None, ZonedDateTime.now(), Some(Seq(TransactionModel(taxPeriodTo = Some(LocalDate.of(2019, 4, 5)), outstandingAmount = Some(0), items = Some(Seq(SubItemModel(dueDate = Some(nextPaymentDate))))))))))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
					}
				}

				"return ISE (500)" when {
					"A calculation error model has returned from the service" in new Setup {
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

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

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
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
						.thenReturn(Future.successful(FinancialDetailsModel(
							documentDetails = List(DocumentDetail(nextPaymentYear, "testId", Some("ITSA- POA 1"), Some(1000.00), None, "2018-03-29")),
							financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
								items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
						)))


					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "31 January 2019"
				}

				"display the oldest next payment due day when there multiple payment due" in new Setup {
					enable(NewFinancialDetailsApi)
					when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()

					when(financialDetailsService.getFinancialDetails(any(), any())(any()))
						.thenReturn(Future.successful(FinancialDetailsModel(
							documentDetails = List(DocumentDetail(nextPaymentYear2, "testId", None, Some(1000.00), None, "2018-03-29")),
							financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))
						)))

					when(financialDetailsService.getFinancialDetails(matches(2018), any())(any()))
						.thenReturn(Future.successful(FinancialDetailsModel(
							documentDetails = List(DocumentDetail(nextPaymentYear2, "testId", Some("ITSA- POA 1"), Some(1000.00), None, "2018-03-29")),
							financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"),
								items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))
						)))

					when(financialDetailsService.getFinancialDetails(matches(2019), any())(any()))
						.thenReturn(Future.successful(FinancialDetailsModel(
							documentDetails = List(DocumentDetail(nextPaymentYear, "id", Some("ITSA - POA 2"), Some(1000.00), None, "2018-03-29")),
							financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
								items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
						)))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
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

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"

					}

					"There are no financial detail" in new Setup {
						enable(NewFinancialDetailsApi)
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialDetailsService.getFinancialDetails(any(), any())(any()))
							.thenReturn(Future.successful(FinancialDetailsModel(List(), List())))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
					}

					"All financial detail bill are paid" in new Setup {
						enable(NewFinancialDetailsApi)
						when(reportDeadlinesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialDetailsService.getFinancialDetails(any(), any())(any()))
							.thenReturn(Future.successful(FinancialDetailsModel(
								documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, Some(0), None, "2018-03-29")),
								financialDetails = List(FinancialDetail(nextPaymentYear, items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
							)))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(bodyOf(result))
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
						.thenReturn(Future.successful(FinancialDetailsModel(
							documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, Some(1000.00), None, "2018-03-29")),
							financialDetails = List(FinancialDetail(nextPaymentYear, items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
						)))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(bodyOf(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#updates-tile > div > p:nth-child(2)").text() shouldBe "1 January 2018"
				}
			}
			disable(NewFinancialDetailsApi)
		}
	}

}
