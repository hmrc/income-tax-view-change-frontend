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

import audit.mocks.MockAuditingService
import audit.models.IvOutcomeFailureAuditModel
import controllers.errors.UpliftFailedController
import org.jsoup.Jsoup
import play.api.http.{HttpEntity, Status}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.expectedJourneyId
import testUtils.TestSupport
import views.html.errorPages.UpliftFailedView

import scala.concurrent.ExecutionContext

class UpliftFailedControllerSpec extends TestSupport with MockAuditingService {

  val upliftFailureTitle: String = messages("upliftFailure.title")

  object TestUpliftFailedController extends UpliftFailedController(
    app.injector.instanceOf[UpliftFailedView],
    app.injector.instanceOf[MessagesControllerComponents],
    mockAuditingService
  )(app.injector.instanceOf[ExecutionContext])

  "the UpliftFailedController.show() action" should {

    "audited V-uplift-failure-outcome when ivJourneyId is defined" in {
      lazy val result = TestUpliftFailedController.show()(FakeRequest("GET", s"/test-path?journeyId=$expectedJourneyId"))

      val expectedIvOutcomeFailureAuditModel = IvOutcomeFailureAuditModel(expectedJourneyId)

      whenReady(result) { response =>
        verifyAudit(expectedIvOutcomeFailureAuditModel)

        response.header.status shouldBe Status.FORBIDDEN
        Jsoup.parse(response.body.asInstanceOf[HttpEntity.Strict].data.utf8String).getElementsByTag("h1").text() shouldBe upliftFailureTitle
      }
    }

    "show the upliftFailedView page with forbidden status" in {
      lazy val result = TestUpliftFailedController.show()(fakeRequestWithActiveSession)

      whenReady(result) { response =>
        response.header.status shouldBe Status.FORBIDDEN
        Jsoup.parse(response.body.asInstanceOf[HttpEntity.Strict].data.utf8String).getElementsByTag("h1").text() shouldBe upliftFailureTitle
      }
    }
  }

}
