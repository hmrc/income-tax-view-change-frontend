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

package financials.controllers

import common.connectors.ITSAStatusConnector
import common.enums.{MTDIndividual, MTDSupportingAgent}
import common.mocks.auth.MockAuthActions
import common.models.admin.CreditsRefundsRepay
import common.services.DateServiceInterface
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import financials.mocks.services.{MockCreditService, MockRepaymentService}
import financials.models.FinancialDetailsModel
import financials.services.{CreditService, RepaymentService}
import financials.testConstants.ANewCreditAndRefundModel
import financials.testConstants.FinancialDetailsTestConstants.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.*

import java.time.LocalDate
import scala.concurrent.Future

class MoneyInYourAccountControllerSpec extends MockAuthActions with MockCreditService with MockRepaymentService {
  
  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[CreditService].toInstance(mockCreditService),
        api.inject.bind[RepaymentService].toInstance(mockRepaymentService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[MoneyInYourAccountController]

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  mtdAllRoles.foreach { mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual

    s"show${if (isAgent) "Agent" else ""}" when {

      val action = if (isAgent) testController.showAgent() else testController.show()
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)

      s"the $mtdUserRole is authenticated" should {

        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {

          "render the credit and refund page" when {

            "MFACreditsAndDebits disabled: credit charges are returned" in {
              setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(mockCreditService.getAllCredits(any(), any()))
                .thenReturn(
                  Future(
                    ANewCreditAndRefundModel()
                      .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
                      .get()
                  )
                )

              val result = action(fakeRequest)
              val body = contentAsString(result)
              val document = Jsoup.parse(body)
              val title = document.title()

              status(result) shouldBe Status.OK
              title shouldBe "Money in your account - Manage your Self Assessment - GOV.UK"
            }

            "credit charges are returned" in {

              setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
                ANewCreditAndRefundModel()
                  .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
                  .get()))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }

            "credit charges are returned in sorted order of credits" in {

              setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()

              val testPreviousTaxYear = 2019
              val testTaxYear = 2020
              val testTaxYear2023 = 2023

              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
                ANewCreditAndRefundModel()
                  .withAvailableCredit(5.0)
                  .withTotalCredit(5.0)
                  .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 250.0)
                  .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 125.0)
                  .withPayment(LocalDate.parse("2022-08-16"), 100.0, Some(LocalDate.parse("2021-06-15")))
                  .withPayment(LocalDate.parse("2022-08-16"), 500.0, Some(LocalDate.parse("2021-06-15")))
                  .withPayment(LocalDate.parse("2022-08-16"), 300.0, Some(LocalDate.parse("2021-06-15")))
                  .withMfaCredit(LocalDate.of(2019, 5, 15), 100.0)
                  .withMfaCredit(LocalDate.of(2019, 5, 15), 1000.0)
                  .withMfaCredit(LocalDate.of(2019, 5, 15), 800.0)
                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 200.0)
                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 2000.0)
                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 700.0)
                  .withFirstRefund(4.0)
                  .withSecondRefund(2.0)
                  .get()))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK

              val doc: Document = Jsoup.parse(contentAsString(result))
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(1)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£250.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(2)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£125.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(3)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£100.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(4)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£1,000.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(5)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£800.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(6)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£200.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(7)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£2,000.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(8)")
                .text() shouldBe s"15 May $testPreviousTaxYear " + messages("money-in-your-account.where-from.credit-row.description") + s" $testPreviousTaxYear to $testTaxYear " + "£700.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(9)")
                .text() shouldBe s"16 Aug ${testTaxYear2023 - 1} " + messages("money-in-your-account.where-from.payment-row.description", "15 Jun 2021") + s" ${testTaxYear2023 - 1} to $testTaxYear2023 " + "£100.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(10)")
                .text() shouldBe s"16 Aug ${testTaxYear2023 - 1} " + messages("money-in-your-account.where-from.payment-row.description", "15 Jun 2021") + s" ${testTaxYear2023 - 1} to $testTaxYear2023 " + "£500.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(11)")
                .text() shouldBe s"16 Aug ${testTaxYear2023 - 1} " + messages("money-in-your-account.where-from.payment-row.description", "15 Jun 2021") + s" ${testTaxYear2023 - 1} to $testTaxYear2023 " + "£300.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(12)")
                .text() shouldBe messages("chargeSummary.noData") + " " + messages("money-in-your-account.where-from.refund-row.description") + " " + messages("chargeSummary.noData") + " −£4.00"
              doc.select("#main-content").select("#where-the-money-came-from-table tbody tr:nth-child(13)")
                .text() shouldBe messages("chargeSummary.noData") + " " + messages("money-in-your-account.where-from.refund-row.description") + " " + messages("chargeSummary.noData") + " −£2.00"
            }
          }

          "render the custom not found error page" when {

            "CreditsRefundsRepay feature is disabled" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction()
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
                ANewCreditAndRefundModel()
                  .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
                  .get()))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              val doc: Document = Jsoup.parse(contentAsString(result))
              val title = messages("error.custom.heading")
              doc.title() shouldBe messages("htmlTitle", title)
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }


    if (mtdUserRole == MTDIndividual) {

      s"startRefund" when {

        val action = testController.startRefund()
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)

        s"the $mtdUserRole is authenticated" should {

          "render the start refund process" when {

            "RepaymentJourneyModel is returned" in {
              setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
              mockItsaStatusRetrievalAction(singleBusinessIncomeWithCurrentYear)
              mockSingleBISWithCurrentYearAsMigrationYear()

              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
                ANewCreditAndRefundModel()
                  .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
                  .get()))

              when(mockRepaymentService.start(any(), any())(any()))
                .thenReturn(Future.successful(Right("/test/url")))

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
            }
          }

          "not start refund process" when {

            "RepaymentJourneyErrorResponse is returned" in {
              setupMockSuccess(mtdUserRole, false, List(CreditsRefundsRepay))
              mockSingleBISWithCurrentYearAsMigrationYear()
              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
                ANewCreditAndRefundModel()
                  .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
                  .get()))
              when(mockRepaymentService.start(any(), any())(any()))
                .thenReturn(Future.successful(Left(new InternalError)))

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "CreditsRefundsRepay FS is disabled" in {
              setupMockSuccess(mtdUserRole)

              mockSingleBISWithCurrentYearAsMigrationYear()
              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }
        }
        testMTDIndividualAuthFailures(action)
      }
    }
  }
}
