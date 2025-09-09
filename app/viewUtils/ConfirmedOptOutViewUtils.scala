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

package viewUtils

import config.FrontendAppConfig
import enums.{ChosenTaxYear, CurrentTaxYear, NextTaxYear}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

import javax.inject.Inject

class ConfirmedOptOutViewUtils @Inject()(
                                          link: views.html.components.link,
                                          h2: views.html.components.h2,
                                          p: views.html.components.p
                                        ) {


  val selfAssessmentTaxReturnLink = "https://www.gov.uk/log-in-file-self-assessment-tax-return"
  val compatibleSoftwareLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  private def submitYourTaxReturnHeading()(implicit messages: Messages): HtmlFormat.Appendable =
    h2(msg = "optout.confirmedOptOut.submitTax", optId = Some("submit-tax-heading"))

  private def firstParagraph()(implicit messages: Messages): HtmlFormat.Appendable =
    p(id = Some("now-you-have-opted-out")) {
      HtmlFormat.fill(
        Seq(
          Html(messages("optout.confirmedOptOut.submitTax.confirmed.p1")),
          link(link = selfAssessmentTaxReturnLink, id = Some("fill-your-tax-return-link"), messageKey = "optout.confirmedOptOut.submitTax.confirmed.p1.link", target = Some("_blank"), additionalOpenTabMessage = Some("."))
        )
      )
    }

  private def secondParagraph()(implicit messages: Messages): HtmlFormat.Appendable =
    p(id = Some("tax-year-reporting-quarterly")) {
      HtmlFormat.fill(
        Seq(
          Html(messages("optout.confirmedOptOut.submitTax.confirmed.p2")),
          link(link = compatibleSoftwareLink, id = Some("compatible-software-link"), messageKey = "optout.confirmedOptOut.submitTax.confirmed.p2.link", target = Some("_blank"), additionalOpenTabMessage = Some("."))
        )
      )
    }

  def submitYourTaxReturnContent(
                                  `itsaStatusCY-1`: ITSAStatus,
                                  itsaStatusCY: ITSAStatus,
                                  `itsaStatusCY+1`: ITSAStatus,
                                  chosenTaxYear: ChosenTaxYear,
                                  isMultiYear: Boolean,
                                  isPreviousYearCrystallised: Boolean
                                )(implicit messages: Messages): Option[Html] = {

    (`itsaStatusCY-1`, itsaStatusCY, `itsaStatusCY+1`, chosenTaxYear) match {
      case (Voluntary | Mandated, Voluntary | Mandated, Annual, CurrentTaxYear) if isMultiYear =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph(), secondParagraph())
          )
        )
      case (Voluntary | Mandated, Voluntary | Mandated, Voluntary | Mandated, CurrentTaxYear) if isMultiYear && isPreviousYearCrystallised =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph())
          )
        )
      case (Voluntary | Mandated, Voluntary | Mandated, Voluntary | Mandated, CurrentTaxYear | NextTaxYear) if isMultiYear =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph(), secondParagraph())
          )
        )
      case (Voluntary | Mandated, Annual, Voluntary | Mandated, NextTaxYear) if isMultiYear =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph(), secondParagraph())
          )
        )
      case (Annual, Voluntary | Mandated, Voluntary | Mandated, NextTaxYear) if isMultiYear =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph(), secondParagraph())
          )
        )
      case (Voluntary | Mandated, Voluntary | Mandated, Annual, CurrentTaxYear) if isMultiYear =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph(), secondParagraph())
          )
        )
      case _ =>
        Some(
          HtmlFormat.fill(
            Seq(submitYourTaxReturnHeading(), firstParagraph())
          )
        )
    }
  }
}