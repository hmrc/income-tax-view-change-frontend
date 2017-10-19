/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import assets.TestConstants.ServiceInfoPartial._
import mocks.MockHttp
import play.mvc.Http.Status
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.HtmlPartial.{Failure, Success}
import utils.TestSupport

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse


class ServiceInfoPartialConnectorSpec extends TestSupport with MockHttp {

  val successResponse = Success(None, serviceInfoPartialSuccess)
  val badGatewayResponse = Failure(Some(Status.BAD_REQUEST))
  val gatewayTimeoutResponse = Failure(Some(Status.GATEWAY_TIMEOUT))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestServiceInfoPartialConnector extends ServiceInfoPartialConnector(mockHttpGet, mockItvcHeaderCarrierForPartialsConverter)

  "The ServiceInfoPartialConnector.getServiceInfoPartial() method" when {
    lazy val testUrl: String = TestServiceInfoPartialConnector.btaUrl
    def result: Future[Html] = TestServiceInfoPartialConnector.getServiceInfoPartial()

    "a valid HtmlPartial is received" should {
      "retrieve the correct HTML" in {
        setupMockHttpGetPartial(testUrl)(successResponse)
        await(result) shouldBe serviceInfoPartialSuccess
      }
    }

    "a BadGateway(400) exception occurs" should {
      "fail and return empty content" in {
        setupMockHttpGetPartial(testUrl)(badGatewayResponse)
        await(result) shouldBe Html("")
      }
    }

    "a GatewayTimeout(504) exception occurs" should {
      "fail and return empty content" in {
        setupMockHttpGetPartial(testUrl)(gatewayTimeoutResponse)
        await(result) shouldBe Html("")
      }
    }

    "an unexpected future failed occurs" should {
      "return empty" in {
        setupMockFailedHttpGet(testUrl)(badResponse)
        await(result) shouldBe Html("")
      }
    }
  }
}
