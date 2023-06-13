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

import config.AddressLookupConfig
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.GetAddressLookupDetailsResponse
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.PostAddressLookupResponse
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import controllers.routes
import play.api.Logger
import play.libs.Json


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(val appConfig: FrontendAppConfig,
                                       val addressConfig: AddressLookupConfig,
                                       http: HttpClient)(implicit ec: ExecutionContext) extends FeatureSwitching {

  val baseUrl: String = appConfig.addressLookupService

  def addressLookupInitializeUrl: String = {
    s"${baseUrl}/api/v2/init"
  }

  def getAddressDetailsUrl(mtditid: String): String = {
    s"${appConfig.addressLookupService}/api/v2/confirmed?id=$mtditid"
  }

  lazy val individualContinueUrl: String = routes.AddBusinessAddressController.submit(None).url
  lazy val agentContinueUrl: String = routes.AddBusinessAddressController.agentSubmit(None).url


  //"http://localhost:9081/report-quarterly/income-and-expenses/view/income-sources/add/business-check-details"
  def addressJson(continueUrl: String): JsValue = {
    play.api.libs.json.Json.parse(
      s"""
         |{
         |  "version" : 2,
         |  "options" : {
         |    "continueUrl" : "placeholder",
         |    "timeoutConfig" : {
         |      "timeoutAmount" : 3600,
         |      "timeoutUrl" : "http://localhost:9081/report-quarterly/income-and-expenses/session-timeout",
         |      "timeoutKeepAliveUrl" : "http://localhost:9081/report-quarterly/income-and-expenses/keep-alive"
         |    },
         |    "signOutHref" : "http://localhost:9081/report-quarterly/income-and-expenses/view/sign-out",
         |    "accessibilityFooterUrl" : "http://localhost:12346/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview",
         |    "selectPageConfig" : {
         |      "proposalListLimit" : 15
         |    },
         |    "confirmPageConfig" : {
         |      "showChangeLink" : true,
         |      "showSearchAgainLink" : true,
         |      "showConfirmChangeText" : true
         |    },
         |    "phaseFeedbackLink" : "http://localhost:9081/report-quarterly/income-and-expenses/view/feedback",
         |    "deskProServiceName" : "cds-reimbursement-claim",
         |    "showPhaseBanner" : true,
         |    "ukMode" : true
         |  }
         |}
      """.stripMargin.replace("placeholder", "http://localhost:9081/report-quarterly" + continueUrl)
    )
  }

  def initialiseAddressLookup(isAgent: Boolean)(implicit hc: HeaderCarrier, request: RequestHeader): Future[PostAddressLookupResponse] = {
    Logger("application").info(s"URL: $addressLookupInitializeUrl")
    val payload = if (isAgent) addressJson(agentContinueUrl) else addressJson(individualContinueUrl)
    http.POST[JsValue, PostAddressLookupResponse](
      url = addressLookupInitializeUrl,
      body = payload
    )
    // if (isAgent) addressConfig.config(agentContinueUrl) else addressConfig.config(continueUrl)

  }

  def getAddressDetails(id: String)(implicit hc: HeaderCarrier): Future[GetAddressLookupDetailsResponse] = {
    http.GET[GetAddressLookupDetailsResponse](getAddressDetailsUrl(id))
    //http.GET(baseUrl+"/lookup-address/test-only/v2/test-setup")
  }
}
