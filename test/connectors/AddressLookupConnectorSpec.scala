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
import controllers.routes
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.UnexpectedGetStatusFailure
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
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

import scala.concurrent.Future

class AddressLookupConnectorSpec extends TestSupport with FeatureSwitching with MockHttp{

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val baseUrl: String = appConfig.addressLookupService
  override def messagesApi: MessagesApi = inject[MessagesApi]
  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))


  object TestAddressLookupConnector extends AddressLookupConnector(appConfig, mockHttpGet, messagesApi)

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

    "initialiseAddressLookup" should {
      "return the redirect location" when {
        "location returned from the lookup-service (individual)" in {
          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          setupMockHttpPost(TestAddressLookupConnector.addressLookupInitializeUrl, addressJson(individualContinueUrl, individualFeedbackUrl, individualEnglishBanner, individualWelshBanner))(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
        "location returned from lookup-service (agent)" in { //this is the only specific agent test, just to test that everything works with both possible json payloads
          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          setupMockHttpPost(TestAddressLookupConnector.addressLookupInitializeUrl, addressJson(agentContinueUrl, agentFeedbackUrl, agentEnglishBanner, agentWelshBanner))(HttpResponse(status = ACCEPTED,
            json = JsString(""), headers = Map("Location" -> Seq("Sample location"))))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = true)
          result map {
            case Left(_) => Fail("Error returned from lookup service")
            case Right(PostAddressLookupSuccessResponse(location)) => location shouldBe Some("Sample location")
          }
        }
      }

      "return an error" when {
        "non-standard status returned from lookup-service" in {
          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          setupMockHttpPost(TestAddressLookupConnector.addressLookupInitializeUrl, addressJson(individualContinueUrl, individualFeedbackUrl, individualEnglishBanner, individualWelshBanner))(HttpResponse(status = OK,
            json = JsString(""), headers = Map.empty))

          val result = TestAddressLookupConnector.initialiseAddressLookup(isAgent = false)
          result map {
            case Left(UnexpectedPostStatusFailure(status)) => status shouldBe OK
            case Right(_) => Fail("error should be returned")
          }
        }
      }
    }

    lazy val individualContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.submit(None).url
    lazy val agentContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.agentSubmit(None).url

    lazy val individualFeedbackUrl: String = controllers.feedback.routes.FeedbackController.show.url
    lazy val agentFeedbackUrl: String = controllers.feedback.routes.FeedbackController.showAgent.url

    lazy val individualEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("header.serviceName")
    lazy val agentEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("agent.header.serviceName")

    lazy val individualWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("header.serviceName")
    lazy val agentWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("agent.header.serviceName")

    def addressJson(continueUrl: String, feedbackUrl: String, headerEnglish: String, headerWelsh: String): JsValue = {
      JsObject(
        Seq(
          "version" -> JsNumber(2),
          "options" -> JsObject(
            Seq(
              "continueUrl" -> JsString(appConfig.itvcFrontendEnvironment + continueUrl),
              "timeoutConfig" -> JsObject(
                Seq(
                  "timeoutAmount" -> JsNumber(3600),
                  "timeoutUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.timeout.url),
                  "timeoutKeepAliveUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.keepAlive.url)
                )
              ),
              "signOutHref" -> JsString(appConfig.itvcFrontendEnvironment + controllers.routes.SignOutController.signOut.url),
              "accessibilityFooterUrl" -> JsString(appConfig.itvcFrontendEnvironment + "/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview"),
              "selectPageConfig" -> JsObject(
                Seq(
                  "proposalListLimit" -> JsNumber(15)
                )
              ),
              "confirmPageConfig" -> JsObject(
                Seq(
                  "showChangeLink" -> JsBoolean(true),
                  "showSearchAgainLink" -> JsBoolean(true),
                  "showConfirmChangeText" -> JsBoolean(true)
                )
              ),
              "phaseFeedbackLink" -> JsString(appConfig.itvcFrontendEnvironment + feedbackUrl),
              "deskProServiceName" -> JsString("cds-reimbursement-claim"),
              "showPhaseBanner" -> JsBoolean(true),
              "ukMode" -> JsBoolean(true)
            )
          ),
          "labels" -> JsObject(
            Seq(
              "en" -> JsObject(
                Seq(
                  "appLevelLabels" -> JsObject(
                    Seq(
                      "navTitle" -> JsString(headerEnglish),
                    )
                  ),
                  "selectPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.select.heading"))
                    )
                  ),
                  "lookupPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.lookup.heading"))
                    )
                  ),
                  "confirmPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.confirm.heading"))
                    )
                  ),
                  "editPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.edit.heading"))
                    )
                  )
                )
              ),
              "cy" -> JsObject(
                Seq(
                  "appLevelLabels" -> JsObject(
                    Seq(
                      "navTitle" -> JsString(headerWelsh)
                    )
                  ),
                  "selectPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.select.heading"))
                    )
                  ),
                  "lookupPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.lookup.heading"))
                    )
                  ),
                  "confirmPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.confirm.heading"))
                    )
                  ),
                  "editPageLabels" -> JsObject(
                    Seq(
                      "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.edit.heading"))
                    )
                  )
                )
              )
            )
          )
        )
      )
    }
  }
}