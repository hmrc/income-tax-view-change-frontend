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

package controllers.agent

import assets.BaseTestConstants.testAgentAuthRetrievalSuccess
import assets.FinancialDetailsTestConstants.{fullDocumentDetailModel, fullFinancialDetailModel, testFinancialDetailsErrorModelParsing}
import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi}
import implicits.ImplicitDateFormatterImpl
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService}
import mocks.views.MockChargeSummary
import models.financialDetails.FinancialDetailsModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import play.twirl.api.Html
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockFinancialDetailsService
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with MockChargeSummary {

  class Setup(agentViewerEnabled: Boolean, newFinDetailsApiEnabled: Boolean) {

    if (agentViewerEnabled) enable(AgentViewer)
    else disable(AgentViewer)

    if (newFinDetailsApiEnabled) enable(NewFinancialDetailsApi)
    else disable(NewFinancialDetailsApi)

    mockChargeSummary()(Html("<html><head><title>Test title</title></head></html>"))

    val chargeSummaryController: ChargeSummaryController = new ChargeSummaryController(
      chargeSummary,
      mockAuthService,
      mockFinancialDetailsService,
      mockIncomeSourceDetailsService,
      mockAuditingService
    )(appConfig,
      app.injector.instanceOf[LanguageUtils],
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ImplicitDateFormatterImpl],
      app.injector.instanceOf[ExecutionContext],
      app.injector.instanceOf[ItvcErrorHandler]
    )
  }

  val currentYear: Int = LocalDate.now().getYear
  val errorHeading = "Sorry, there is a problem with the service"

  ".showChargeSummary" when {
    "the agent viewer feature switch is enabled" when {
      "the NewFinancialDetailsApi feature switch is enabled" when {
        "getFinancialDetails returns a FinancialDetailsModel" when {
          "the model contains a transaction for the charge provided" should {
            "show a charge summary with the correct title" in new Setup(true, true) {

              setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

              setupMockGetFinancialDetails(currentYear)(FinancialDetailsModel(
                documentDetails = List(fullDocumentDetailModel.copy(taxYear = currentYear.toString, transactionId = "testid")),
                financialDetails = List(fullFinancialDetailModel.copy(taxYear = currentYear.toString))
              ))

              mockSingleBusinessIncomeSource()

              val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, "testid")
                .apply(fakeRequestConfirmedClient("AB123456C"))

              status(await(result)) shouldBe OK
              JsoupParse(result).toHtmlDocument.title() shouldBe "Test title"

            }
          }

          "the model does not contains a transaction for the charge provided" should {
            "redirect to home page" in new Setup(true, true) {

              setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

              setupMockGetFinancialDetails(currentYear)(FinancialDetailsModel(
                documentDetails = List(fullDocumentDetailModel.copy(taxYear = currentYear.toString, transactionId = "NotTestid")),
                financialDetails = List(fullFinancialDetailModel.copy(taxYear = currentYear.toString))
              ))

              val result: Result = await(chargeSummaryController.showChargeSummary(currentYear, "testid")
                .apply(fakeRequestConfirmedClient("AB123456C")))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/income-tax-account")

            }
          }
        }

        "getFinancialDetails does not returns a successful FinancialDetailsModel" when {

          "show an internal server error with the correct title" in new Setup(true, true) {

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

            setupMockGetFinancialDetails(currentYear)(testFinancialDetailsErrorModelParsing)

            val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, "testid")
              .apply(fakeRequestConfirmedClient("AB123456C"))

            status(await(result)) shouldBe INTERNAL_SERVER_ERROR
            JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading

          }

        }

      }

      "the NewFinancialDetailsApi feature switch is not enabled" should {
        "redirect to home page with the correct title" in new Setup(true, false) {

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

          val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, "testid")
            .apply(fakeRequestConfirmedClient("AB123456C"))

          status(await(result)) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/income-tax-account")

        }
      }


    }

    "the agent viewer feature switch is not enabled" should {

      "return a not found" in new Setup(false, true) {

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = chargeSummaryController.showChargeSummary(currentYear, "testid")
          .apply(fakeRequestConfirmedClient("AB123456C"))

        status(await(result)) shouldBe NOT_FOUND
      }
    }
  }


  ".backUrl" when {

    "the user comes from what you owe page" should {
      "return what you owe page url string result" in new Setup(true, true) {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("paymentDue"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"

      }
    }

    "the user comes from tax overview page payment tab" should {
      "return payment tab url string result" in new Setup(true, true) {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("taxYearOverview"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2021#payments"

      }
    }

    "the user comes is not from tax overview payment tab and is not from what you owe page" should {
      "return agent home page url string result" in new Setup(true, true) {

        val result: String = chargeSummaryController.backUrl(backLocation = Some("_"), currentYear)

        result shouldBe "/report-quarterly/income-and-expenses/view/agents/income-tax-account"

      }
    }

  }
}
