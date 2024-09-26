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

package controllers.optIn

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockDateService, MockOptInService}
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.BeforeYouStart

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptInService with MockDateService {

  val beforeYouStartController = new BeforeYouStartController(
    beforeYouStart = app.injector.instanceOf[BeforeYouStart],
    optInService = mockOptInService
  )(
    appConfig = appConfig,
    ec = ec,
    auth = testAuthenticator,
    authorisedFunctions = mockAuthService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents]
  )

  val endTaxYear = 2025
  val taxYear2024: TaxYear = TaxYear.forYearEnd(endTaxYear - 1)
  val taxYear2025: TaxYear = TaxYear.forYearEnd(endTaxYear)

  def show(isAgent: Boolean, redirectPageURL: String, availableOptInTaxYear: Seq[TaxYear]): Unit = {
    "show page" should {

      s"return result with $OK status" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
          .thenReturn(Future.successful(availableOptInTaxYear))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = beforeYouStartController.show(isAgent).apply(requestGET)
        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.getElementById("start-button").attr("href") shouldBe redirectPageURL
        status(result) shouldBe Status.OK
      }

      s"return result with $INTERNAL_SERVER_ERROR status when exception is thrown" in {
        setupMockAuthorisationSuccess(isAgent)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
          .thenReturn(Future.failed(new Exception("Some error")))

        val requestGET = if (isAgent) fakeRequestConfirmedClient() else fakeRequestWithNinoAndOrigin("PTA")
        val result = beforeYouStartController.show(isAgent).apply(requestGET)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "BeforeYouStartController with start button redirects to choose tax year page- Individual" when {
    val chooseTaxYearPageURL = controllers.optIn.routes.ChooseYearController.show(false).url
    val availableOptInTaxYear = Seq(taxYear2024, taxYear2025)
    show(isAgent = false, redirectPageURL = chooseTaxYearPageURL, availableOptInTaxYear = availableOptInTaxYear)
  }

  "BeforeYouStartController with start button redirects to confirm tax year page- Individual" when {
    val confirmTaxYearPageURL = controllers.optIn.routes.ConfirmTaxYearController.show(false).url
    val availableOptInTaxYear = Seq(taxYear2024)
    show(isAgent = false, redirectPageURL = confirmTaxYearPageURL, availableOptInTaxYear = availableOptInTaxYear)
  }

  "BeforeYouStartController with start button redirects to choose tax year page- Agent" when {
    val chooseTaxYearPageURL = controllers.optIn.routes.ChooseYearController.show(true).url
    val availableOptInTaxYear = Seq(taxYear2024, taxYear2025)
    show(isAgent = true, redirectPageURL = chooseTaxYearPageURL, availableOptInTaxYear = availableOptInTaxYear)
  }

  "BeforeYouStartController with start button redirects to confirm tax year page- Agent" when {
    val confirmTaxYearPageURL = controllers.optIn.routes.ConfirmTaxYearController.show(true).url
    val availableOptInTaxYear = Seq(taxYear2024)
    show(isAgent = true, redirectPageURL = confirmTaxYearPageURL, availableOptInTaxYear = availableOptInTaxYear)
  }
}
