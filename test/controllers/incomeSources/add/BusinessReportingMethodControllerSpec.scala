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

package controllers.incomeSources.add

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.incomeSourceDetails.viewmodels.BusinessReportingMethodViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.BusinessReportingMethodService
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessReportingMethod

import scala.concurrent.Future

class BusinessReportingMethodControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockBusinessReportingMethod: BusinessReportingMethod = app.injector.instanceOf[BusinessReportingMethod]
  val mockBusinessReportingMethodService: BusinessReportingMethodService = mock(classOf[BusinessReportingMethodService])

  val TestBusinessReportingMethodController = new BusinessReportingMethodController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockBusinessReportingMethod,
    mockBusinessReportingMethodService,
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.add.businessReportingMethod.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.businessReportingMethod.heading"))}"
    val heading: String = messages("incomeSources.add.businessReportingMethod.heading")
  }


  "Individual - BusinessReportingMethodController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()
        when(mockBusinessReportingMethodService.getBusinessReportingMethodDetails())
          .thenReturn(BusinessReportingMethodViewModel(Some(2022), Some(2023)))

        val result: Future[Result] = TestBusinessReportingMethodController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestBusinessReportingMethodController.title
        document.select("legend:nth-child(1)").text shouldBe TestBusinessReportingMethodController.heading
      }
    }
  }

}
