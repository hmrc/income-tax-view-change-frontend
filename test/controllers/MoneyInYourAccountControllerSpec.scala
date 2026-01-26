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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockCreditService, MockRepaymentService}
import models.admin.CreditsRefundsRepay
import models.financialDetails.FinancialDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{CreditService, DateServiceInterface, RepaymentService}
import testConstants.ANewCreditAndRefundModel
import testConstants.BaseTestConstants.testTaxYearTo
import testConstants.FinancialDetailsTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear

import java.time.LocalDate
import scala.concurrent.Future

class MoneyInYourAccountControllerSpec extends MockAuthActions with MockCreditService with MockRepaymentService {

  override lazy val mockBusinessDetailsConnector: BusinessDetailsConnector = mock[BusinessDetailsConnector]

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[CreditService].toInstance(mockCreditService),
        api.inject.bind[RepaymentService].toInstance(mockRepaymentService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[MoneyInYourAccountController]

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  mtdAllRoles.foreach { mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual

    s"show${if (isAgent) "Agent"}" when {

      val action = if (isAgent) testController.showAgent() else testController.show()
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)

      s"the $mtdUserRole is authenticated" should {

        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {

          "render the credit and refund page" when {

            "MFACreditsAndDebits disabled: credit charges are returned" in {

              disableAllSwitches()
              enable(CreditsRefundsRepay)
              setupMockSuccess(mtdUserRole)
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
  //TODO: Re-enable these tests as part of MISUV-10631
//            "credit charges are returned" in {
//
//              disableAllSwitches()
//              enable(CreditsRefundsRepay)
//              setupMockSuccess(mtdUserRole)
//              mockItsaStatusRetrievalAction()
//              mockSingleBISWithCurrentYearAsMigrationYear()
//
//              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
//                ANewCreditAndRefundModel()
//                  .withBalancingChargeCredit(LocalDate.parse("2022-08-16"), 100.0)
//                  .get()))
//
//              val result = action(fakeRequest)
//              status(result) shouldBe Status.OK
//            }
//
//            "credit charges are returned in sorted order of credits" in {
//
//              enable(CreditsRefundsRepay)
//              setupMockSuccess(mtdUserRole)
//              mockItsaStatusRetrievalAction()
//              mockSingleBISWithCurrentYearAsMigrationYear()
//
//              when(mockCreditService.getAllCredits(any(), any())).thenReturn(Future.successful(
//                ANewCreditAndRefundModel()
//                  .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 250.0)
//                  .withBalancingChargeCredit(LocalDate.of(2019, 5, 15), 125.0)
//                  .withPayment(LocalDate.parse("2022-08-16"), 100.0)
//                  .withPayment(LocalDate.parse("2022-08-16"), 500.0)
//                  .withPayment(LocalDate.parse("2022-08-16"), 300.0)
//                  .withMfaCredit(LocalDate.of(2019, 5, 15), 100.0)
//                  .withMfaCredit(LocalDate.of(2019, 5, 15), 1000.0)
//                  .withMfaCredit(LocalDate.of(2019, 5, 15), 800.0)
//                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 200.0)
//                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 2000.0)
//                  .withCutoverCredit(LocalDate.of(2019, 5, 15), 700.0)
//                  .withFirstRefund(4.0)
//                  .withSecondRefund(2.0)
//                  .get()))
//
//              val result = action(fakeRequest)
//              status(result) shouldBe Status.OK
//
//              val doc: Document = Jsoup.parse(contentAsString(result))
//              doc.select("#main-content").select("li:nth-child(1)")
//                .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
//              doc.select("#main-content").select("li:nth-child(2)")
//                .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
//              doc.select("#main-content").select("li:nth-child(3)")
//                .select("p").first().text().contains(messages("credit-and-refund.payment") + " 15 June 2018")
//              doc.select("#main-content").select("li:nth-child(4)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(5)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(6)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(7)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-earlier-tax-year") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(8)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(9)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-balancing-charge-prt-1") + " " +
//                  messages("credit-and-refund.credit-from-balancing-charge-prt-2") + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(10)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-balancing-charge-prt-1") + " " +
//                  messages("credit-and-refund.credit-from-balancing-charge-prt-2") + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(11)")
//                .select("p").first().text().contains(messages("credit-and-refund.credit-from-adjustment-prt-1") + " " + s"$testTaxYearTo")
//              doc.select("#main-content").select("li:nth-child(12)")
//                .select("p").first().text() shouldBe "£4.00 " + messages("credit-and-refund.refundProgress-prt-2")
//              doc.select("#main-content").select("li:nth-child(13)")
//                .select("p").first().text() shouldBe "£2.00 " + messages("credit-and-refund.refundProgress-prt-2")
//            }
          }

          "render the custom not found error page" when {

            "CreditsRefundsRepay feature is disabled" in {

              disableAllSwitches()
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

              disableAllSwitches()
              enable(CreditsRefundsRepay)
              setupMockSuccess(mtdUserRole)
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
              disableAllSwitches()
              enable(CreditsRefundsRepay)
              setupMockSuccess(mtdUserRole)
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
              disableAllSwitches()
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
