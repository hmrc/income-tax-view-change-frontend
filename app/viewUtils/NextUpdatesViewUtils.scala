/*
 * Copyright 2025 HM Revenue & Customs
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

package viewUtils

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.ReportingFrequencyPage
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel, OptOutViewModel}
import play.api.i18n.Messages
import play.twirl.api.Html
import views.html.components.link

import javax.inject.Inject

case class EntryPointLink(message: String, linkComponent: Html)

class NextUpdatesViewUtils @Inject()(
                                      link: link
                                    )(
                                      implicit val appConfig: FrontendAppConfig
                                    ) extends FeatureSwitching {


  def entryPointLink(optOutViewModel: Option[OptOutViewModel], isAgent: Boolean)(implicit user: MtdItUser[_], messages: Messages): Option[EntryPointLink] = {
    val reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(isAgent).url
    optOutViewModel.map {
      case m: OptOutOneYearViewModel if isEnabled(ReportingFrequencyPage) =>
        EntryPointLink(
          message = messages("nextUpdates.withReportingFrequencyContent.optOutOneYear-1", m.startYear, m.endYear),
          linkComponent = link(
            link = reportingFrequencyLink,
            messageKey = "nextUpdates.optOutOneYear-2"
          )
        )
      case _: OptOutMultiYearViewModel if isEnabled(ReportingFrequencyPage) =>
        EntryPointLink(
          message = messages("nextUpdates.withReportingFrequencyContent.optOutMultiYear-1"),
          linkComponent = link(
            link = reportingFrequencyLink,
            messageKey = "nextUpdates.optOutMultiYear-2"
          )
        )
      case m: OptOutOneYearViewModel if m.showWarning =>
        EntryPointLink(
          message = messages("nextUpdates.optOutOneYear-1", m.startYear, m.endYear),
          linkComponent = link(
            link = controllers.optOut.routes.SingleYearOptOutWarningController.show(isAgent).url,
            messageKey = "nextUpdates.optOutOneYear-2"
          )
        )

      case m: OptOutOneYearViewModel =>
        EntryPointLink(
          message = messages("nextUpdates.optOutOneYear-1", m.startYear, m.endYear),
          linkComponent = link(
            link = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url,
            messageKey = "nextUpdates.optOutOneYear-2"
          )
        )
      case _: OptOutMultiYearViewModel =>
        EntryPointLink(
          message = messages("nextUpdates.optOutMultiYear-1"),
          linkComponent = link(
            link = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url,
            messageKey = "nextUpdates.optOutMultiYear-2"
          )
        )
    }
  }
}