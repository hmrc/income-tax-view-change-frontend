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
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import views.html.components.link

import javax.inject.Inject

class NextUpdatesViewUtils @Inject()(link: link)(
                                      implicit val appConfig: FrontendAppConfig
                                    ) extends FeatureSwitching {
  def whatTheUserCanDo(isAgent: Boolean)(implicit user: MtdItUser[_], messages: Messages): Option[Html] = {

    val reportingFrequencyLink = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent).url

    val reportingFrequencyHtml: Html =
      HtmlFormat.fill(
        Seq(
          Html(messages("nextUpdates.reporting.obligations.p.message")),
          link(
            link = reportingFrequencyLink,
            messageKey = "nextUpdates.reporting.obligations.p.link",
            id = Some("reporting-frequency-link"),
            outerMessage = messages("base.fullstop")
          )
        )
      )
    if (isEnabled(ReportingFrequencyPage)) Some(reportingFrequencyHtml) else None
  }
}