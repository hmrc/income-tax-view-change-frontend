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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.{AddressLookupStub, IncomeTaxViewChangeStub}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ComponentSpecBase {

  val changeBusinessAddressShowUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = true).url
  val businessAddressShowUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url
  val continueUrlChange = "/report-quarterly/income-and-expenses/view/income-sources/add/change-business-address/id/"
  val individualFeedbackUrlChange = "/report-quarterly/income-and-expenses/view/feedback"
  val individualEnglishBannerChange = "Manage your Income Tax updates"
  val individualWelshBannerChange = "Rheoli’ch diweddariadau Treth Incwm"

//  val payloadChange = addressJson(continueUrlChange, individualFeedbackUrlChange, individualEnglishBannerChange, individualWelshBannerChange)

  val addressLookupInitializeUrl = "http://localhost:9028/api/v2/init"

//  s"calling GET $businessAddressShowUrl" should {
//    "render the add business address page" when {
//      "User is authorised" in {
//        Given("I wiremock stub a successful Income Source Details response")
//        enable(IncomeSources)
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
//
//        IncomeTaxViewChangeStub.stubGetAddressLookupServiceResponse(SEE_OTHER)
//
//        When(s"I call GET $businessAddressShowUrl")
//        val result = IncomeTaxViewChangeFrontend.getAddBusinessAddress
//
//        result should have(
//          httpStatus(SEE_OTHER),
//        )
//      }
//    }
//  }

  s"calling GET $changeBusinessAddressShowUrl" should {
    "render the change business address page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        AddressLookupStub.stubPostInitialiseAddressLookup

        When(s"I call GET $changeBusinessAddressShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddChangeBusinessAddress

        result should have(
          httpStatus(OK),
        )
      }
    }
  }

//  def addressJson(continueUrl: String, feedbackUrl: String, headerEnglish: String, headerWelsh: String): JsValue = {
//    JsObject(
//      Seq(
//        "version" -> JsNumber(2),
//        "options" -> JsObject(
//          Seq(
//            "continueUrl" -> JsString(appConfig.itvcFrontendEnvironment + continueUrl),
//            "timeoutConfig" -> JsObject(
//              Seq(
//                "timeoutAmount" -> JsNumber(3600),
//                "timeoutUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.timeout.url),
//                "timeoutKeepAliveUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.keepAlive.url)
//              )
//            ),
//            "signOutHref" -> JsString(appConfig.itvcFrontendEnvironment + controllers.routes.SignOutController.signOut.url),
//            "accessibilityFooterUrl" -> JsString(appConfig.itvcFrontendEnvironment + "/accessibility-statement/income-tax-view-change?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview"),
//            "selectPageConfig" -> JsObject(
//              Seq(
//                "proposalListLimit" -> JsNumber(15)
//              )
//            ),
//            "confirmPageConfig" -> JsObject(
//              Seq(
//                "showChangeLink" -> JsBoolean(true),
//                "showSearchAgainLink" -> JsBoolean(true),
//                "showConfirmChangeText" -> JsBoolean(true)
//              )
//            ),
//            "phaseFeedbackLink" -> JsString(appConfig.itvcFrontendEnvironment + feedbackUrl),
//            "deskProServiceName" -> JsString("cds-reimbursement-claim"),
//            "showPhaseBanner" -> JsBoolean(true),
//            "ukMode" -> JsBoolean(true)
//          )
//        ),
//        "labels" -> JsObject(
//          Seq(
//            "en" -> JsObject(
//              Seq(
//                "selectPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.select.heading"))
//                  )
//                ),
//                "lookupPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.lookup.heading"))
//                  )
//                ),
//                "confirmPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.confirm.heading"))
//                  )
//                ),
//                "editPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.edit.heading"))
//                  )
//                ),
//                "appLevelLabels" -> JsObject(
//                  Seq(
//                    "navTitle" -> JsString(headerEnglish)
//                  )
//                )
//              )
//            ),
//            "cy" -> JsObject(
//              Seq(
//                "selectPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.select.heading"))
//                  )
//                ),
//                "lookupPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.lookup.heading"))
//                  )
//                ),
//                "confirmPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.confirm.heading"))
//                  )
//                ),
//                "editPageLabels" -> JsObject(
//                  Seq(
//                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.edit.heading"))
//                  )
//                ),
//                "appLevelLabels" -> JsObject(
//                  Seq(
//                    "navTitle" -> JsString(headerWelsh)
//                  )
//                )
//              )
//            )
//          )
//        )
//      )
//    )
//  }
}
