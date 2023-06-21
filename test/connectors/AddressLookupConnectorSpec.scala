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

package connectors

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import org.mockito.Mockito.mock
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient

class AddressLookupConnectorSpec extends TestSupport with FeatureSwitching {

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val mockHttpGet: HttpClient = mock(classOf[HttpClient])
  val baseUrl: String = appConfig.addressLookupService

  object TestAddressLookupConnector extends AddressLookupConnector(appConfig, mockHttpGet)

  "AddressLookupConnector" should {
    "addressLookupInitializeUrl" should {
      "return the initialising address" in {
        disableAllSwitches()
        enable(IncomeSources)

        val result = TestAddressLookupConnector.addressLookupInitializeUrl
        result shouldBe s"${baseUrl}/api/v2/init"
      }
    }

    "getAddressDetailsUrl" should {
      "return the get url" in {
        disableAllSwitches()
        enable(IncomeSources)

        val result = TestAddressLookupConnector.getAddressDetailsUrl("123")
        result shouldBe s"${baseUrl}/api/v2/confirmed?id=123"
      }
    }
  }


}
