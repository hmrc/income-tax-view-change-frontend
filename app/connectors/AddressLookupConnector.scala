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
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.GetAddressLookupDetailsResponse
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.PostAddressLookupResponse
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import controllers.routes
import play.api.Logger
import play.api.libs.json._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClient)(implicit ec: ExecutionContext) extends FeatureSwitching {

  val baseUrl: String = appConfig.addressLookupService

  def addressLookupInitializeUrl: String = {
    s"${baseUrl}/api/v2/init"
  }

  def getAddressDetailsUrl(id: String): String = {
    s"${baseUrl}/api/v2/confirmed?id=$id"
  }

  lazy val individualContinueUrl: String = routes.AddBusinessAddressController.submit(None).url
  lazy val agentContinueUrl: String = routes.AddBusinessAddressController.agentSubmit(None).url

  lazy val individualFeedbackUrl: String = controllers.feedback.routes.FeedbackController.show.url
  lazy val agentFeedbackUrl: String = controllers.feedback.routes.FeedbackController.showAgent.url


  def addressJson(continueUrl: String, feedbackUrl: String): JsValue = {
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
            "accessibilityFooterUrl" -> JsString(appConfig.itvcFrontendEnvironment +  "/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview"),
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
                "selectPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Select business address")
                  )
                ),
                "lookupPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("What is your business address?")
                  )
                ),
                "confirmPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Confirm business address")
                  )
                ),
                "editPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Enter your business address")
                  )
                )
              )
            ),
            "cy" -> JsObject(
              Seq(
                "selectPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Dewiswch gyfeiriad y busnes")
                  )
                ),
                "lookupPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Beth yw cyfeiriad eich busnes?")
                  )
                ),
                "confirmPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Cadarnhewch gyfeiriad y busnes")
                  )
                ),
                "editPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString("Nodwch gyfeiriad eich busnes")
                  )
                )
              )
            )
          )
        )
      )
    )
  }
  def initialiseAddressLookup(isAgent: Boolean)(implicit hc: HeaderCarrier, request: RequestHeader): Future[PostAddressLookupResponse] = {
    Logger("application").info(s"URL: $addressLookupInitializeUrl")
    val payload = if (isAgent) addressJson(agentContinueUrl, agentFeedbackUrl) else addressJson(individualContinueUrl, individualFeedbackUrl)
    http.POST[JsValue, PostAddressLookupResponse](
      url = addressLookupInitializeUrl,
      body = payload
    )
  }

  def getAddressDetails(id: String)(implicit hc: HeaderCarrier): Future[GetAddressLookupDetailsResponse] = {
    http.GET[GetAddressLookupDetailsResponse](getAddressDetailsUrl(id))
  }
}
