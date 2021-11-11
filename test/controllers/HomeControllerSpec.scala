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

import testConstants.MessagesLookUp
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.calculation.Calculation
import models.financialDetails._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.test.Helpers._
import play.api.mvc.{MessagesControllerComponents, Result}
import services.{FinancialDetailsService, NextUpdatesService}
import utils.CurrentDateProvider

import java.time.LocalDate
import scala.concurrent.Future

class HomeControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

	val updateDateAndOverdueObligations: (LocalDate, Seq[LocalDate]) = (LocalDate.of(2018, 1, 1), Seq.empty[LocalDate])
	val nextPaymentDate: LocalDate = LocalDate.of(2019, 1, 31)
	val nextPaymentDate2: LocalDate = LocalDate.of(2018, 1, 31)
	val updateYear: String = "2018"
	val nextPaymentYear: String = "2019"
	val nextPaymentYear2: String = "2018"
	val emptyEstimateCalculation: Calculation = Calculation(crystallised = false)
	val emptyCrystallisedCalculation: Calculation = Calculation(crystallised = true)

	trait Setup {
		val NextUpdatesService: NextUpdatesService = mock[NextUpdatesService]
		val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
		val currentDateProvider: CurrentDateProvider = mock[CurrentDateProvider]
		val controller = new HomeController(
			app.injector.instanceOf[views.html.Home],
			app.injector.instanceOf[SessionTimeoutPredicate],
			MockAuthenticationPredicate,
			app.injector.instanceOf[NinoPredicate],
			MockIncomeSourceDetailsPredicate,
			NextUpdatesService,
			app.injector.instanceOf[ItvcErrorHandler],
			financialDetailsService,currentDateProvider,
			mockAuditingService)(
      ec,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig]
			)
		when(currentDateProvider.getCurrentDate()) thenReturn LocalDate.now()
	}

	"navigating to the home page" should {
			"return ok (200)" which {
				"there is a next payment due date to display" in new Setup {
					when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()
					when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
						.thenReturn(Future.successful(List(FinancialDetailsModel(
							balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
							documentDetails = List(DocumentDetail(nextPaymentYear, "testId", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
							financialDetails = List(FinancialDetail(taxYear = nextPaymentYear, mainType = Some("SA Payment on Account 1"),
								transactionId = Some("testId"),
								items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
						))))


					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(contentAsString(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "OVERDUE 31 January 2019"
				}

				"display number of payments due when there are multiple payment due" in new Setup {
					when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()

					when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
						.thenReturn(Future.successful(List(
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear2, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
								FinancialDetailsModel(
									balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
									documentDetails = List(DocumentDetail(nextPaymentYear, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
									financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
										transactionId = Some("testId3"),
										items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
						)))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(contentAsString(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "2 OVERDUE PAYMENTS"
				}

				"display number of payments due when there are multiple payment due and filter out payments" in new Setup {
					when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()

					when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
						.thenReturn(Future.successful(List(
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear2, "testId1", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29),
									paymentLotItem = Some("123"), paymentLot = Some("456")
								)),
								financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, transactionId = Some("testId1"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear2, "testId2", Some("ITSA- POA 1"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(taxYear = nextPaymentYear2, mainType = Some("SA Payment on Account 1"), transactionId = Some("testId2"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate2.toString))))))),
							FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear, "testId3", Some("ITSA - POA 2"), Some("documentText"), Some(1000.00), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(nextPaymentYear, mainType = Some("SA Payment on Account 2"),
									transactionId = Some("testId3"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString)))))))
						)))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(contentAsString(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "2 OVERDUE PAYMENTS"
				}

				"Not display the next payment due date" when {
					"there is a problem getting financial detalis" in new Setup {
						when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
							.thenReturn(Future.successful(List(FinancialDetailsErrorModel(1, "testString"))))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(contentAsString(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"

					}

					"There are no financial detail" in new Setup {
						when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
							.thenReturn(Future.successful(List(FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00),List(), List()))))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(contentAsString(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
					}

					"All financial detail bill are paid" in new Setup {
						when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
						mockSingleBusinessIncomeSource()
						when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
							.thenReturn(Future.successful(List(FinancialDetailsModel(
								balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
								documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, None, Some(0), None, LocalDate.of(2018, 3, 29))),
								financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
									items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
							))))

						val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

						status(result) shouldBe Status.OK
						val document: Document = Jsoup.parse(contentAsString(result))
						document.title shouldBe MessagesLookUp.HomePage.title
						document.select("#payments-tile > div > p:nth-child(2)").text shouldBe "No payments due"
					}
				}
			}
			"return OK (200)" when {
				"there is a update date to display" in new Setup {
					when(NextUpdatesService.getNextDeadlineDueDateAndOverDueObligations(any())(any(), any(), any())) thenReturn Future.successful(updateDateAndOverdueObligations)
					mockSingleBusinessIncomeSource()
					when(financialDetailsService.getAllUnpaidFinancialDetails(any(), any(), any()))
						.thenReturn(Future.successful(List(FinancialDetailsModel(
							balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
							documentDetails = List(DocumentDetail(nextPaymentYear, "testId", None, None, Some(1000.00), None, LocalDate.of(2018, 3, 29))),
							financialDetails = List(FinancialDetail(nextPaymentYear, transactionId = Some("testId"),
								items = Some(Seq(SubItem(dueDate = Some(nextPaymentDate.toString))))))
						))))

					val result: Future[Result] = controller.home(fakeRequestWithActiveSession)

					status(result) shouldBe Status.OK
					val document: Document = Jsoup.parse(contentAsString(result))
					document.title shouldBe MessagesLookUp.HomePage.title
					document.select("#updates-tile > div > p:nth-child(2)").text() shouldBe "1 January 2018"
				}
			}
	}
}
