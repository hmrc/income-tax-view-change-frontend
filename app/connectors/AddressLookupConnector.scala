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
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import controllers.routes
import play.api.Logger


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


  def addressJson(continueUrl: String): JsValue = {
    play.api.libs.json.Json.parse(
      s"""
         |{
         |  "version" : 2,
         |  "options" : {
         |    "continueUrl" : "http://localhost:9081placeholder",
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
      """.stripMargin.replace("placeholder", continueUrl)
    )
  }

  /*
  "labels": {
    "en": {
      "appLevelLabels": {
        "navTitle": "",
        "phaseBannerHtml": ""
      },
      "countryPickerLabels": {
        "title": "Custom title",
        "heading": "Custom heading",
        "countryLabel": "Custom country label",
        "submitLabel": "Custom submit label"
      },
      "selectPageLabels": {
        "title": "Select business address",
        "heading": "Select business address",
        "headingWithPostcode": "foo",
        "proposalListLabel": "Please select one of the following addresses",
        "submitLabel": "Continue",
        "searchAgainLinkText": "Search again",
        "editAddressLinkText": "Enter address manually"
      },
      "lookupPageLabels": {
        "title": "What is your business address?",
        "heading": "What is your business address?",
        "filterLabel": "Property name or number (optional)",
        "postcodeLabel": "Postcode",
        "submitLabel": "Find address",
        "noResultsFoundMessage": "Sorry, we couldn't find anything for that postcode.",
        "resultLimitExceededMessage": "There were too many results. Please add additional details to limit the number of results.",
        "manualAddressLinkText": "Enter the address manually"
      },
      "confirmPageLabels": {
        "title": "Confirm business address",
        "heading": "Confirm business address",
        "infoSubheading": "Your selected address",
        "infoMessage": "This is how your address will look. Please double-check it and, if accurate, click on the <kbd>Confirm</kbd> button.",
        "submitLabel": "Confirm Address",
        "searchAgainLinkText": "Search again",
        "changeLinkText": "Edit address",
        "confirmChangeText": "By confirming this, you agree that the information you have given is complete and correct."
      },
      "editPageLabels": {
        "title": "Enter address",
        "heading": "Enter address",
        "organisationLabel": "Organisation (optional)",
        "line1Label": "Address line 1",
        "line2Label": "Address line 2 (optional)",
        "line3Label": "Address line 3 (optional)",
        "townLabel": "Town/City",
        "postcodeLabel": "Postcode (optional)",
        "countryLabel": "Country",
        "submitLabel": "Continue"
      }
    },
    "cy": {
      "appLevelLabels": {
        "navTitle": "",
        "phaseBannerHtml": ""
      },
      "countryPickerLabels": {
        "title": "Custom title - Welsh",
        "heading": "Custom heading - Welsh",
        "countryLabel": "Custom country label - Welsh",
        "submitLabel": "Custom submit label - Welsh"
      },
      "selectPageLabels": {
        "title": "Choose address welsh",
        "heading": "Choose address welsh",
        "headingWithPostcode": "foo",
        "proposalListLabel": "Please select one of the following addresses welsh",
        "submitLabel": "Continue welsh",
        "searchAgainLinkText": "Search again welsh",
        "editAddressLinkText": "Enter address manually welsh"
      },
      "lookupPageLabels": {
        "title": "Find address welsh",
        "heading": "Find address welsh",
        "afterHeadingText": "We will use this address to send letters welsh",
        "filterLabel": "Property name or number welsh (optional)",
        "postcodeLabel": "Postcode welsh",
        "submitLabel": "Find address welsh",
        "noResultsFoundMessage": "Sorry, we couldn't find anything for that postcode. welsh",
        "resultLimitExceededMessage": "There were too many results. Please add additional details to limit the number of results. welsh",
        "manualAddressLinkText": "Enter the address manually welsh"
      },
      "confirmPageLabels": {
        "title": "Confirm address welsh",
        "heading": "Review and confirm welsh",
        "infoSubheading": "Your selected address welsh",
        "infoMessage": "This is how your address will look. Please double-check it and, if accurate, click on the <kbd>Confirm</kbd> button. welsh",
        "submitLabel": "Confirm Address welsh",
        "searchAgainLinkText": "Search again welsh",
        "changeLinkText": "Edit address welsh",
        "confirmChangeText": "By confirming this change, you agree that the information you have given is complete and correct. welsh"
      },
      "editPageLabels": {
        "title": "Enter address welsh",
        "heading": "Enter address welsh",
        "organisationLabel": "Organisation (optional) welsh",
        "line1Label": "Address line 1 welsh",
        "line2Label": "Address line 2 (optional) welsh",
        "line3Label": "Address line 3 (optional) welsh",
        "townLabel": "Town/City welsh",
        "postcodeLabel": "Postcode (optional) welsh",
        "countryLabel": "Country welsh",
        "submitLabel": "Continue welsh"
      }
    }
  }

   */

  def initialiseAddressLookup(isAgent: Boolean)(implicit hc: HeaderCarrier, request: RequestHeader): Future[PostAddressLookupResponse] = {
    Logger("application").info(s"URL: $addressLookupInitializeUrl")
    val payload = if (isAgent) addressJson(agentContinueUrl) else addressJson(individualContinueUrl)
    http.POST[JsValue, PostAddressLookupResponse](
      url = addressLookupInitializeUrl,
      body = payload
    )
  }

  def getAddressDetails(id: String)(implicit hc: HeaderCarrier): Future[GetAddressLookupDetailsResponse] = {
    http.GET[GetAddressLookupDetailsResponse](getAddressDetailsUrl(id))
  }
}
