/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.constants

import connectors.BaseConnectorSpec
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import play.api.i18n.Lang
import play.api.libs.json._

object AddressLookupConnectorConstants extends BaseConnectorSpec {

  val baseUrl: String = appConfig.addressLookupService

  lazy val individualContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.submit(None, isChange = false).url
  lazy val agentContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.agentSubmit(None, isChange = false).url

  lazy val changeIndividualContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.submit(None, isChange = true).url
  lazy val changeAgentContinueUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.agentSubmit(None, isChange = true).url

  lazy val individualFeedbackUrl: String = controllers.feedback.routes.FeedbackController.show().url
  lazy val agentFeedbackUrl: String = controllers.feedback.routes.FeedbackController.show().url

  lazy val individualEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("header.serviceName")
  lazy val agentEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("agent.header.serviceName")

  lazy val individualWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("header.serviceName")
  lazy val agentWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("agent.header.serviceName")


  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))

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
                "timeoutUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.timeout().url),
                "timeoutKeepAliveUrl" -> JsString(appConfig.itvcFrontendEnvironment + controllers.timeout.routes.SessionTimeoutController.keepAlive().url)
              )
            ),
            "signOutHref" -> JsString(appConfig.itvcFrontendEnvironment + controllers.routes.SignOutController.signOut().url),
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
