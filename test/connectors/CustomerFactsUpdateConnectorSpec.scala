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

package connectors

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttpV2
import play.api.Configuration
import play.api.http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class CustomerFactsUpdateConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {

  trait Setup {
    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new CustomerFactsUpdateConnector(mockHttpClientV2, getAppConfig())
    val mtdId = "XAIT0000123456"
    val url = connector.getCustomerFactsUpdateUrl(mtdId)
  }

  "updateCustomerFacts" should {

    "return OK when the backend returns 200" in new Setup {
      setupMockHttpV2Put(url)(HttpResponse(Status.OK, ""))
      val result: Future[HttpResponse] = connector.updateCustomerFacts(mtdId)
      result.futureValue.status shouldBe Status.OK
    }

    "return Unprocessable Entity when the backend returns 422" in new Setup {
      setupMockHttpV2Put(url)(HttpResponse(Status.UNPROCESSABLE_ENTITY, ""))
      val result: Future[HttpResponse] = connector.updateCustomerFacts(mtdId)
      result.futureValue.status shouldBe Status.UNPROCESSABLE_ENTITY
    }

    "return Internal Server Error when the backend returns 500" in new Setup {
      setupMockHttpV2Put(url)(HttpResponse(Status.INTERNAL_SERVER_ERROR, ""))
      val result: Future[HttpResponse] = connector.updateCustomerFacts(mtdId)
      result.futureValue.status shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}
