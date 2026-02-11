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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.core.Mode
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.GetAddressLookupDetailsResponse
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.PostAddressLookupResponse
import play.api.Logger
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClientV2,
                                       val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends FeatureSwitching {

  val baseUrl: String = appConfig.addressLookupService

  def addressLookupInitializeUrl: String = {
    s"$baseUrl/api/v2/init"
  }

  def getAddressDetailsUrl(id: String): String = {
    s"$baseUrl/api/v2/confirmed?id=$id"
  }

  def continueUrl(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_]): String = {
    if(isAgent) controllers.manageBusinesses.add.routes.AddBusinessAddressController.agentSubmit(None, mode = mode, isTriggeredMigration).url
    else controllers.manageBusinesses.add.routes.AddBusinessAddressController.submit(None, mode = mode, isTriggeredMigration).url
  }

  lazy val individualFeedbackUrl: String = controllers.feedback.routes.FeedbackController.show().url
  lazy val agentFeedbackUrl: String = controllers.feedback.routes.FeedbackController.showAgent().url

  lazy val individualEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("header.serviceName")
  lazy val agentEnglishBanner: String = messagesApi.preferred(Seq(Lang("en")))("agent.header.serviceName")

  lazy val individualWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("header.serviceName")
  lazy val agentWelshBanner: String = messagesApi.preferred(Seq(Lang("cy")))("agent.header.serviceName")

  private def addressJson(continueUrl: String, feedbackUrl: String, headerEnglish: String, headerWelsh: String): JsValue = {
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
                ),
                "appLevelLabels" -> JsObject(
                  Seq(
                    "navTitle" -> JsString(headerEnglish)
                  )
                )
              )
            ),
            "cy" -> JsObject(
              Seq(
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
                ),
                "appLevelLabels" -> JsObject(
                  Seq(
                    "navTitle" -> JsString(headerWelsh)
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  private def internationalAddressJson(continueUrl: String, feedbackUrl: String, headerEnglish: String, headerWelsh: String): JsValue = {
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
            "manualAddressEntryConfig" -> JsObject(
              Seq(
                "mandatoryFields" -> JsObject(
                  Seq(
                    "addressLine1" -> JsBoolean(true),
                    "addressLine2" -> JsBoolean(true)
                  )
                )
              )
            ),
            "phaseFeedbackLink" -> JsString(appConfig.itvcFrontendEnvironment + feedbackUrl),
            "deskProServiceName" -> JsString("cds-reimbursement-claim"),
            "showPhaseBanner" -> JsBoolean(true),
            "ukMode" -> JsBoolean(false)
          )
        ),
        "labels" -> JsObject(
          Seq(
            "en" -> JsObject(
              Seq(
                "confirmPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.confirm.heading"))
                  )
                ),
                "lookupPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.lookup.heading"))
                  )
                ),
                "countryPickerLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-international-business-address.countryPicker.heading")),
                    "title" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-international-business-address.countryPicker.heading"))
                  )
                ),
                "editPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-business-address.edit.heading"))
                  )
                ),
                "international" -> JsObject(
                  Seq(
                    "editPageLabels" -> JsObject(
                      Seq(
                        "heading" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-international-business-address.heading")),
                        "title" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-international-business-address.heading")),
                        "postcodeLabel" -> JsString(messagesApi.preferred(Seq(Lang("en")))("add-international-business-address.postcode"))
                      )
                    )
                  )
                )
              )
            ),
            "cy" -> JsObject(
              Seq(
                "confirmPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.confirm.heading"))
                  )
                ),
                "lookupPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.lookup.heading"))
                  )
                ),
                "editPageLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-business-address.edit.heading"))
                  )
                ),
                "countryPickerLabels" -> JsObject(
                  Seq(
                    "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-international-business-address.countryPicker.heading")),
                    "title" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-international-business-address.countryPicker.heading"))
                  )
                ),
                "international" -> JsObject(
                  Seq(
                    "editPageLabels" -> JsObject(
                      Seq(
                        "heading" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-international-business-address.heading")),
                        "title" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-international-business-address.heading")),
                        "postcodeLabel" -> JsString(messagesApi.preferred(Seq(Lang("cy")))("add-international-business-address.postcode"))
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  def initialiseAddressLookup(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean, ukOnly: Boolean)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[PostAddressLookupResponse] = {
    Logger("application").info(s"[AddressLookupConnector] - URL: $addressLookupInitializeUrl")
    val payload = (isAgent, ukOnly) match {
      case (true, true)   => addressJson(continueUrl(isAgent, mode, isTriggeredMigration), agentFeedbackUrl, agentEnglishBanner, agentWelshBanner)
      case (false, true)  => addressJson(continueUrl(isAgent, mode, isTriggeredMigration), individualFeedbackUrl, individualEnglishBanner, individualWelshBanner)
      case (true, false)  => internationalAddressJson(continueUrl(isAgent, mode, isTriggeredMigration), agentFeedbackUrl, agentEnglishBanner, agentWelshBanner)
      case (false, false) => internationalAddressJson(continueUrl(isAgent, mode, isTriggeredMigration), individualFeedbackUrl, individualEnglishBanner, individualWelshBanner)
    }

    http.post(url"$addressLookupInitializeUrl")
      .withBody(payload)
      .execute[PostAddressLookupResponse]
  }

  def getAddressDetails(id: String)(implicit hc: HeaderCarrier): Future[GetAddressLookupDetailsResponse] = {
    http.get(url"${getAddressDetailsUrl(id)}").execute[GetAddressLookupDetailsResponse]
  }
}
