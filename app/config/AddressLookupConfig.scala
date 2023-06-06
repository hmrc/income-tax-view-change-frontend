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

package config

import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.hmrcfrontend.config.AccessibilityStatementConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AddressLookupConfig @Inject()(appConfig: FrontendAppConfig, messagesApi: MessagesApi, accessibilityStatementConfig: AccessibilityStatementConfig){

  def config(continueUrl: String)(implicit request: RequestHeader): JsObject = {

    Json.obj(
      "version" -> 2,
      "options" -> Json.obj(
        "continueUrl" -> continueUrl,
        "showBackButtons" -> false,
        "includeHMRCBranding" -> true,
        "ukMode"-> true,
        "selectPageConfig" -> Json.obj(
          "proposalListLimit" -> 50,
          "showSearchLinkAgain" -> true
        ),
        "confirmPageConfig" -> Json.obj(
          "showChangeLink" -> true,
          "showSubHeadingAndInfo" -> true,
          "showSearchAgainLink" -> false,
          "showConfirmChangeText" -> true
        ),
        "timeoutConfig" -> Json.obj(
          "timeoutAmount" -> 900,
          "timeoutUrl" -> s"${appConfig.baseUrl}/session-timeout"
        )
        ,
        "accessibilityFooterUrl" -> accessibilityStatementConfig.url
      )
      ,
      "labels"-> Json.obj (
        "en" -> Json.obj (
          "appLevelLabels"-> Json.obj (
          "navTitle"-> "",
          "phaseBannerHtml"-> ""
          ),
          "countryPickerLabels"-> Json.obj (
          "title"-> "Custom title",
          "heading"-> "Custom heading",
          "countryLabel"-> "Custom country label",
          "submitLabel"-> "Custom submit label"
          ),
          "selectPageLabels"-> Json.obj (
          "title"-> "Choose address",
          "heading"-> "Choose address",
          "headingWithPostcode"-> "foo",
          "proposalListLabel"-> "Please select one of the following addresses",
          "submitLabel"-> "Continue",
          "searchAgainLinkText"-> "Search again",
          "editAddressLinkText"-> "Enter address manually"
          ),
          "lookupPageLabels"-> Json.obj (
          "title"-> "Find address",
          "heading"-> "Find address",
          "afterHeadingText"-> "We will use this address to send letters",
          "filterLabel"-> "Property name or number (optional)",
          "postcodeLabel"-> "Postcode",
          "submitLabel"-> "Find address",
          "noResultsFoundMessage"-> "Sorry, we couldn't find anything for that postcode.",
          "resultLimitExceededMessage"-> "There were too many results. Please add additional details to limit the number of results.",
          "manualAddressLinkText"-> "Enter the address manually"
          ),
          "confirmPageLabels"-> Json.obj (
          "title"-> "Confirm address",
          "heading"-> "Review and confirm",
          "infoSubheading"-> "Your selected address",
          "infoMessage"-> "This is how your address will look. Please double-check it and, if accurate, click on the <kbd>Confirm</kbd> button.",
          "submitLabel"-> "Confirm Address",
          "searchAgainLinkText"-> "Search again",
          "changeLinkText"-> "Edit address",
          "confirmChangeText"-> "By confirming this change, you agree that the information you have given is complete and correct."
          ),
          "editPageLabels"-> Json.obj (
          "title"-> "Enter address",
          "heading"-> "Enter address",
          "organisationLabel"-> "Organisation (optional)",
          "line1Label"-> "Address line 1",
          "line2Label"-> "Address line 2 (optional)",
          "line3Label"-> "Address line 3 (optional)",
          "townLabel"-> "Town/City",
          "postcodeLabel"-> "Postcode (optional)",
          "countryLabel"-> "Country",
          "submitLabel"-> "Continue"
          )
        ),
        "cy"-> Json.obj(
          "appLevelLabels"-> Json.obj(
          "navTitle"-> "",
          "phaseBannerHtml"-> ""
            ),
          "countryPickerLabels"-> Json.obj(
          "title"-> "Custom title - Welsh",
          "heading"-> "Custom heading - Welsh",
          "countryLabel"-> "Custom country label - Welsh",
          "submitLabel"-> "Custom submit label - Welsh"
            ),
          "selectPageLabels"-> Json.obj(
          "title"-> "Choose address welsh",
          "heading"-> "Choose address welsh",
          "headingWithPostcode"-> "foo",
          "proposalListLabel"-> "Please select one of the following addresses welsh",
          "submitLabel"-> "Continue welsh",
          "searchAgainLinkText"-> "Search again welsh",
          "editAddressLinkText"-> "Enter address manually welsh"
            ),
          "lookupPageLabels"-> Json.obj(
          "title"-> "Find address welsh",
          "heading"-> "Find address welsh",
          "afterHeadingText"-> "We will use this address to send letters welsh",
          "filterLabel"-> "Property name or number welsh (optional)",
          "postcodeLabel"-> "Postcode welsh",
          "submitLabel"-> "Find address welsh",
          "noResultsFoundMessage"-> "Sorry, we couldn't find anything for that postcode. welsh",
          "resultLimitExceededMessage"-> "There were too many results. Please add additional details to limit the number of results. welsh",
          "manualAddressLinkText"-> "Enter the address manually welsh"
            ),
          "confirmPageLabels"-> Json.obj(
          "title"-> "Confirm address welsh",
          "heading"-> "Review and confirm welsh",
          "infoSubheading"-> "Your selected address welsh",
          "infoMessage"-> "This is how your address will look. Please double-check it and, if accurate, click on the <kbd>Confirm</kbd> button. welsh",
          "submitLabel"-> "Confirm Address welsh",
          "searchAgainLinkText"-> "Search again welsh",
          "changeLinkText"-> "Edit address welsh",
          "confirmChangeText"-> "By confirming this change, you agree that the information you have given is complete and correct. welsh"
            ),
          "editPageLabels"-> Json.obj(
          "title"-> "Enter address welsh",
          "heading"-> "Enter address welsh",
          "organisationLabel"-> "Organisation (optional) welsh",
          "line1Label"-> "Address line 1 welsh",
          "line2Label"-> "Address line 2 (optional) welsh",
          "line3Label"-> "Address line 3 (optional) welsh",
          "townLabel"-> "Town/City welsh",
          "postcodeLabel"-> "Postcode (optional) welsh",
          "countryLabel"-> "Country welsh",
          "submitLabel"-> "Continue welsh"
          )
        )
      )
    )
  }
}
