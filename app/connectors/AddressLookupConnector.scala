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

  lazy val continueUrl: String = routes.AddBusinessCheckDetailsController.show().url
  lazy val agentContinueUrl: String = routes.AddBusinessCheckDetailsController.showAgent().url

  def addressJson(): JsValue = {
    play.api.libs.json.Json.parse(
      s"""
         |{
         |  "version" : 2,
         |  "options" : {
         |    "continueUrl" : "http://localhost:7500/claim-for-reimbursement-of-import-duties/rejected-goods/single/claimant-details/update-address",
         |    "timeoutConfig" : {
         |      "timeoutAmount" : 3600,
         |      "timeoutUrl" : "/claim-for-reimbursement-of-import-duties/exit/we-signed-you-out",
         |      "timeoutKeepAliveUrl" : "http://localhost:7500/claim-for-reimbursement-of-import-duties/keep-alive"
         |    },
         |    "signOutHref" : "http://localhost:9949/auth-login-stub/session/logout",
         |    "accessibilityFooterUrl" : "http://localhost:12346/accessibility-statement/cds-reimbursement-claim",
         |    "selectPageConfig" : {
         |      "proposalListLimit" : 15
         |    },
         |    "confirmPageConfig" : {
         |      "showChangeLink" : true,
         |      "showSearchAgainLink" : true,
         |      "showConfirmChangeText" : true
         |    },
         |    "phaseFeedbackLink" : "http://localhost:9250/contact/contact-hmrc?service=play.cds-reimbursement-claim-frontend",
         |    "deskProServiceName" : "cds-reimbursement-claim",
         |    "showPhaseBanner" : true,
         |    "ukMode" : true
         |  }
         |}
      """.stripMargin
    )
  }

  def initialiseAddressLookup(isAgent: Boolean)(implicit hc: HeaderCarrier, request: RequestHeader): Future[PostAddressLookupResponse] = {
    Logger("application").info(s"URL: $addressLookupInitializeUrl")
    val payload = addressJson()
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
