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

package services

import mocks.connectors.MockServiceInfoPartialConnector
import utils.TestSupport
import play.twirl.api.Html
import assets.TestConstants.ServiceInfoPartial._

class ServiceInfoPartialServiceSpec extends TestSupport with MockServiceInfoPartialConnector{

  object TestServiceInfoPartialService extends ServiceInfoPartialService(mockServiceInfoPartialConnector)

  "The ServiceInfoPartialService.serviceInfoPartial" when {
    "valid HTML is retrieved from the connector" should {
      "return the expected HMTL" in {
        setupMockGetServiceInfoPartial()(serviceInfoPartialSuccess)
        await(TestServiceInfoPartialService.serviceInfoPartial()) shouldBe serviceInfoPartialSuccess
      }
    }
    "no HTML is retrieved from the connector" should {
      "return empty HTML" in {
        setupMockGetServiceInfoPartial()(Html(""))
        await(TestServiceInfoPartialService.serviceInfoPartial()) shouldBe Html("")
      }
    }
  }
}
