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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.routes
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.UnexpectedGetStatusFailure
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, JsString, JsValue}
import org.scalactic.{Fail, Pass}
import play.api.i18n.{Lang, MessagesApi}
import mocks.MockHttp
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import play.api.Logger
import play.api.http.Status.{ACCEPTED, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import play.api.libs.json._

class AddressLookupConnectorGetSpec extends TestSupport with FeatureSwitching with MockHttp{

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }
  override def messagesApi: MessagesApi = inject[MessagesApi]

  object TestAddressLookupConnector extends AddressLookupConnector(appConfig, mockHttpGet, messagesApi)

  "getAddressDetails" should {
    "return the address details" when {
      "called by service" in {
        disableAllSwitches()
        enable(IncomeSources)
        beforeEach()

        val businessAddressModel: BusinessAddressModel = BusinessAddressModel(auditRef = "1",
          Address(lines = Seq("line1", "line2", "line3"), postcode = Some("TF3 4NT")))

        val testResponse = HttpResponse(status = OK, json = Json.toJson(businessAddressModel), headers = Map.empty)

        lazy val url = TestAddressLookupConnector.getAddressDetailsUrl("123")
        Logger("application").info("test call url: " + url)

        setupMockHttpGet(url)(testResponse)

        val result = TestAddressLookupConnector.getAddressDetails("123") //result set to null
        result map {
          case Left(_) => Fail("Error returned from lookup service")
          case Right(None) => Fail("No address details with that id")
          case Right(Some(model)) => model shouldBe businessAddressModel
        }
      }
    }
    /*"return None" when {
      "no address details with specified id exist" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockHttpGet(TestAddressLookupConnector.getAddressDetailsUrl("123"))(HttpResponse(status = NOT_FOUND,
          json = JsString(""), headers = Map.empty))

        val result = TestAddressLookupConnector.getAddressDetails("123") //result set to null
        result map {
          case Left(_) => Fail("Error returned from lookup service")
          case Right(None) => Pass
          case Right(Some(_)) => Fail("Model found where model should not exist")
        }
      }
    }


    "return an error" when {
      "non-standard status returned from lookup-service" in {
        disableAllSwitches()
        enable(IncomeSources)


        setupMockHttpGet(TestAddressLookupConnector.getAddressDetailsUrl("123"))(HttpResponse(status = IM_A_TEAPOT,
          json = JsString(""), headers = Map.empty))

        val result = TestAddressLookupConnector.getAddressDetails("123") //result set to null
        result map {
          case Left(UnexpectedGetStatusFailure(status)) => status shouldBe IM_A_TEAPOT
          case Right(None) => Fail("Tried to check for model")
          case Right(Some(_)) => Fail("Model found where model should not exist")
        }
      }
    }*/
  }

}
