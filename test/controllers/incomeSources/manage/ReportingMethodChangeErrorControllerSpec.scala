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

import audit.AuditingService
import config.featureswitch.IncomeSources
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import views.html.incomeSources.manage.{ManageIncomeSources, ReportingMethodChangeError}

import scala.concurrent.Future

class ReportingMethodChangeErrorControllerSpec
  extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockNavBarEnumFsPredicate {

  val mockSessionService: SessionService = mock(classOf[SessionService])

  object TestReportingMethodChangeErrorController
    extends ReportingMethodChangeErrorController(
      manageIncomeSources = app.injector.instanceOf[ManageIncomeSources],
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      retrieveNinoWithIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      sessionService = mockSessionService,
      reportingMethodChangeError = app.injector.instanceOf[ReportingMethodChangeError]
    )(
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  "ReportingMethodChangeErrorController.show" should {
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "the IncomeSources FS is disabled for an Individual" in {

        val result = runTest(isAgent = false, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }

      "the IncomeSources FS is disabled for an Agent" in {

        val result = runTest(isAgent = true, disableIncomeSources = true)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
    }

    s"return ${Status.OK}: render Reporting Method Change Error Page" when {
      s"Calling .show with income source type: $UkProperty when Individual has a UK Property" in {

        val result = runTest(isAgent = false, incomeSourceType = UkProperty)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showUkProperty().url
        status(result) shouldBe Status.OK
      }

      s"Calling .show with income source type: $UkProperty when Agent has a UK Property" in {

        val result = runTest(isAgent = true, incomeSourceType = UkProperty)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showUkPropertyAgent().url
        status(result) shouldBe Status.OK
      }

      s"Calling .show with income source type: $ForeignProperty when Individual has a Foreign Property" in {

        val result = runTest(isAgent = false, incomeSourceType = ForeignProperty)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showForeignProperty().url
        status(result) shouldBe Status.OK
      }

      s"Calling .show with income source type: $ForeignProperty when Agent has a Foreign Property" in {

        val result = runTest(isAgent = true, incomeSourceType = ForeignProperty)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showForeignPropertyAgent().url
        status(result) shouldBe Status.OK
      }

      s"Calling .show with income source type: $SelfEmployment when Individual has a Sole Trader Business for the given incomeSourceId" in {

        val result = runTest(isAgent = false, incomeSourceType = SelfEmployment)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showSoleTraderBusiness(testSelfEmploymentId).url
        status(result) shouldBe Status.OK
      }

      s"Calling .show with income source type: $SelfEmployment when Agent has a Sole Trader Business for the given incomeSourceId" in {

        val result = runTest(isAgent = true, incomeSourceType = SelfEmployment)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          controllers.incomeSources.manage.routes
            .ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testSelfEmploymentId).url
        status(result) shouldBe Status.OK
      }
    }
  }

  def runTest(isAgent: Boolean,
              disableIncomeSources: Boolean = false,
              incomeSourceType: IncomeSourceType = SelfEmployment,
             ): Future[Result] = {

    if (disableIncomeSources)
      disable(IncomeSources)

    mockBothPropertyBothBusiness()

    if (isAgent)
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
    else
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())

    if (incomeSourceType == SelfEmployment)
      when(mockSessionService.getMongoKey(any(), any())(any(), any()))
        .thenReturn(
          Future(
            Right(Some(testSelfEmploymentId))
          )
        )

    TestReportingMethodChangeErrorController
      .show(isAgent, incomeSourceType)(
        if (isAgent)
          fakeRequestConfirmedClient()
        else
          fakeRequestWithActiveSession
      )
  }

  override def beforeEach(): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
  }
}
