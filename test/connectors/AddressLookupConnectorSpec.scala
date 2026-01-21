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

import config.featureswitch.FeatureSwitching
import mocks.MockHttpV2
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import org.scalactic.Fail
import play.api.http.Status.{ACCEPTED, OK}
import play.api.libs.json._
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

class AddressLookupConnectorSpec extends TestSupport with FeatureSwitching with MockHttpV2 {

  val baseUrl: String = appConfig.addressLookupService

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))

  object TestAddressLookupConnector extends AddressLookupConnector(appConfig, mockHttpClientV2, messagesApi)

  "AddressLookupConnector" should {
    "addressLookupInitializeUrl" should {
      "return the initialising address" in {
        disableAllSwitches()

        val result = TestAddressLookupConnector.addressLookupInitializeUrl
        result shouldBe s"$baseUrl/api/v2/init"
      }
    }

    "getAddressDetailsUrl" should {
      "return the get url" in {
        disableAllSwitches()

        val result = TestAddressLookupConnector.getAddressDetailsUrl("123")
        result shouldBe s"$baseUrl/api/v2/confirmed?id=123"
      }
    }

    "initialiseAddressLookup" should {
      "return the redirect location" when {
        "location returned from the lookup-service (individual)" in {
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false, mode = NormalMode, isTriggeredMigration = false)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
        "location returned from lookup-service (agent)" in { //this is the only specific agent test, just to test that everything works with both possible json payloads
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = true, mode = NormalMode, isTriggeredMigration = false)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
      }
      "return the redirect location when on the Check page" when {
        "location returned from the lookup-service (individual) and mode = CheckMode" in {
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false, mode = CheckMode, isTriggeredMigration = false)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
        "location returned from lookup-service (agent) when mode = CheckMode" in { //this is the only specific agent test, just to test that everything works with both possible json payloads
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = true, mode = CheckMode, isTriggeredMigration = false)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
      }

      "return an error" when {
        "non-standard status returned from lookup-service" in {
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = OK,
            json = JsString(""), headers = Map.empty))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false, mode = NormalMode, isTriggeredMigration = false)
          result map {
            case Left(UnexpectedPostStatusFailure(status)) => status shouldBe OK
            case Right(_) => Fail("error should be returned")
          }
        }
        "non-standard status returned from lookup-service on Check page" in {
          disableAllSwitches()
          beforeEach()

          setupMockHttpV2Post(TestAddressLookupConnector.addressLookupInitializeUrl)(HttpResponse(status = OK,
            json = JsString(""), headers = Map.empty))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false, mode = CheckMode, isTriggeredMigration = false)
          result map {
            case Left(UnexpectedPostStatusFailure(status)) => status shouldBe OK
            case Right(_) => Fail("error should be returned")
          }
        }
      }
    }
  }
}