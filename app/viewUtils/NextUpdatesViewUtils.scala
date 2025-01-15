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
import play.twirl.api.{Html, HtmlFormat}
import views.html.components.link

import javax.inject.Inject

class NextUpdatesViewUtils @Inject()(
                                      link: link
                                    )(
                                      implicit val appConfig: FrontendAppConfig
                                    ) extends FeatureSwitching {


  def whatTheUserCanDo(optOutViewModel: Option[OptOutViewModel], isAgent: Boolean)(implicit user: MtdItUser[_], messages: Messages): Option[Html] = {
    val reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(isAgent).url
    optOutViewModel.map {
      case m: OptOutOneYearViewModel if isEnabled(ReportingFrequencyPage) =>
        HtmlFormat.fill(
          Seq(
            Html(messages("nextUpdates.optOutOneYear.reportingFrequency.p.message", m.startYear, m.endYear)),
            link(
              link = reportingFrequencyLink,
              messageKey = "nextUpdates.optOutOneYear.reportingFrequency.p.link"
            )
          )
        )
      case _: OptOutMultiYearViewModel if isEnabled(ReportingFrequencyPage) =>
        HtmlFormat.fill(
          Seq(
            Html(messages("nextUpdates.optOutMultiYear.reportingFrequency.p.message")),
            link(
              link = reportingFrequencyLink,
              messageKey = "nextUpdates.optOutMultiYear.reportingFrequency.p.link"
            )
          )
        )
      case m: OptOutOneYearViewModel if m.showWarning =>
        HtmlFormat.fill(
          Seq(
            Html(messages("nextUpdates.optOutOneYear.p.message", m.startYear, m.endYear)),
            link(
              link = controllers.optOut.routes.SingleYearOptOutWarningController.show(isAgent).url,
              messageKey = "nextUpdates.optOutOneYear.p.link"
            )
          )
        )
      case m: OptOutOneYearViewModel =>
        HtmlFormat.fill(
          Seq(
            Html(messages("nextUpdates.optOutOneYear.p.message", m.startYear, m.endYear)),
            link(
              link = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url,
              messageKey = "nextUpdates.optOutOneYear.p.link"
            )
          )
        )
      case _: OptOutMultiYearViewModel =>
        HtmlFormat.fill(
          Seq(
            Html(messages("nextUpdates.optOutMultiYear.p.message")),
            link(
              link = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url,
              messageKey = "nextUpdates.optOutMultiYear.p.link"
            )
          )
        )
    }
  }
}