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

package controllers.incomeSources.manage

import config.featureswitch.IncomeSources
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.mockito.Mockito.mock
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.UpdateIncomeSourceService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import views.html.incomeSources.manage.{ManageIncomeSources, ReportingMethodChangeError}

import scala.concurrent.Future

class ReportingMethodChangeErrorControllerSpec
  extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockNavBarEnumFsPredicate {

  object TestReportingMethodChangeErrorController
    extends ReportingMethodChangeErrorController(
      manageIncomeSources = app.injector.instanceOf[ManageIncomeSources],
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      reportingMethodChangeError = app.injector.instanceOf[ReportingMethodChangeError]
    )(
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  val testSoleTraderBusinessIncomeSourceId = "XAIS00000099004"

  "Individual: ReportingMethodChangeErrorController.show" should {
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          Some(testSoleTraderBusinessIncomeSourceId), incomeSourceType = SelfEmployment, isAgent = false)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
    }
    s"return ${Status.OK}: render Reporting Method Change Error Page" when {
      s"Calling .show with income source type: ${UkProperty} when user has a UK Property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          None, incomeSourceType = UkProperty, isAgent = false)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkProperty().url
        status(result) shouldBe Status.OK
      }
      s"Calling .show with income source type: ${ForeignProperty} when user has a Foreign Property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          None, incomeSourceType = ForeignProperty, isAgent = false)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showForeignProperty().url
        status(result) shouldBe Status.OK
      }
      s"Calling .show with income source type: ${SelfEmployment} when user has a Sole Trader Business for the given incomeSourceId" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          Some(testSelfEmploymentId), incomeSourceType = SelfEmployment, isAgent = false)(fakeRequestWithActiveSession)

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController
            .showSoleTraderBusiness(testSelfEmploymentId).url
        status(result) shouldBe Status.OK
      }
    }
  }

  "Agent: ReportingMethodChangeErrorController.show" should {
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "the IncomeSources FS is disabled" in {
        disableAllSwitches()
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          Some(testSoleTraderBusinessIncomeSourceId), incomeSourceType = SelfEmployment, isAgent = true)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }
    s"return ${Status.OK}: render Reporting Method Change Error Page" when {
      s"Calling .show with key: ${UkProperty} when user has a UK Property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          None, incomeSourceType = UkProperty, isAgent = true)(fakeRequestConfirmedClient())

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController
            .showUkPropertyAgent().url
        status(result) shouldBe Status.OK
      }
      s"Calling .show with income source type: ${ForeignProperty} when user has a Foreign Property" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockForeignPropertyIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          None, incomeSourceType = ForeignProperty, isAgent = true)(fakeRequestConfirmedClient())

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController
            .showForeignPropertyAgent().url
        status(result) shouldBe Status.OK
      }
      s"Calling .show with income source type: ${SelfEmployment} when user has a Sole Trader Business for the given incomeSourceId" in {
        disableAllSwitches()
        enable(IncomeSources)
        mockSingleBusinessIncomeSource()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestReportingMethodChangeErrorController.show(
          Some(testSelfEmploymentId), incomeSourceType = SelfEmployment, isAgent = true)(fakeRequestConfirmedClient())

        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController
            .showSoleTraderBusinessAgent(testSelfEmploymentId).url
        status(result) shouldBe Status.OK
      }
    }
  }
}
