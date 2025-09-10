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
import config.featureswitch.FeatureSwitching
import enums.{CurrentTaxYear, NextTaxYear, NoChosenTaxYear}
import implicits.ImplicitDateFormatter
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import testUtils.UnitSpec
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.components.{h2, link, p}

class ConfirmedOptOutViewUtilsSpec extends UnitSpec with FeatureSwitching with ImplicitDateFormatter with GuiceOneAppPerSuite {

  override implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  override implicit val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  val linkComponent: link = app.injector.instanceOf[link]
  val paragraphComponent: p = app.injector.instanceOf[p]
  val h2Component: h2 = app.injector.instanceOf[h2]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val taxReturnLink = "https://www.gov.uk/log-in-file-self-assessment-tax-return"
  val compatibleSoftwareLink = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  val confirmedOptOutViewUtils =
    new ConfirmedOptOutViewUtils(linkComponent, h2Component, paragraphComponent)

  val submitYourTaxReturnHeading: HtmlFormat.Appendable =
    h2Component(msg = "optout.confirmedOptOut.submitTax", optId = Some("submit-tax-heading"))

  val firstParagraph =
    paragraphComponent(id = Some("now-you-have-opted-out")) {
      HtmlFormat.fill(
        Seq(
          Html(messages("Now you have opted out, you will need to go back to the way you have previously")),
          linkComponent(link = taxReturnLink, id = Some("fill-your-tax-return-link"), messageKey = "optout.confirmedOptOut.submitTax.confirmed.p1.link", target = Some("_blank"), additionalOpenTabMessage = Some("."))
        )
      )
    }

  val secondParagraph =
    paragraphComponent(id = Some("tax-year-reporting-quarterly")) {
      HtmlFormat.fill(
        Seq(
          Html(messages("For any tax year you are reporting quarterly, you will need")),
          linkComponent(link = compatibleSoftwareLink, id = Some("compatible-software-link"), messageKey = "optout.confirmedOptOut.submitTax.confirmed.p2.link", target = Some("_blank"), additionalOpenTabMessage = Some("."))
        )
      )
    }

  "ConfirmOptOutViewUtils" when {

    ".submitYourTaxReturnContent()" when {

      "CY-1 == Quarterly, CY == Quarterly, CY+1 == Quarterly - chosen intent is CurrentTaxYear, is a multi year scenario & CY-1 is Crystallised" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Voluntary,
              `itsaStatusCY+1` = Voluntary,
              chosenTaxYear = CurrentTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = true
            )

          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph)
            )

          actual shouldBe Some(expected)
        }
      }

      "CY-1 == Quarterly, CY == Quarterly, CY+1 == Quarterly - chosen intent is CurrentTaxYear and is a multi year scenario" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Voluntary,
              `itsaStatusCY+1` = Voluntary,
              chosenTaxYear = CurrentTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )

          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph, secondParagraph)
            )

          actual shouldBe Some(expected)
        }
      }

      "CY-1 == Quarterly, CY == Quarterly, CY+1 == Quarterly - chosen intent is NextTaxYear and is a multi year scenario" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Voluntary,
              `itsaStatusCY+1` = Voluntary,
              chosenTaxYear = NextTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )

          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph, secondParagraph)
            )

          actual shouldBe Some(expected)
        }
      }

      "CY-1 == Quarterly, CY == Quarterly, CY+1 == Annual - chosen intent is CurrentTaxYear and is a multi year scenario" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Voluntary,
              `itsaStatusCY+1` = Annual,
              chosenTaxYear = CurrentTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )

          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph, secondParagraph)
            )

          actual shouldBe Some(expected)
        }
      }

      "CY-1 == Quarterly, CY == Annual, CY+1 == Quarterly - chosen intent is NextTaxYear and is a multi year scenario" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Annual,
              `itsaStatusCY+1` = Voluntary,
              chosenTaxYear = NextTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )
          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph, secondParagraph)
            )


          actual shouldBe Some(expected)
        }
      }

      "CY-1 == Quarterly - no chosen intent since it is a single year scenario and tax year is deduced" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Voluntary,
              itsaStatusCY = Annual,
              `itsaStatusCY+1` = Annual,
              chosenTaxYear = NoChosenTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )
          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph)
            )


          actual shouldBe Some(expected)
        }
      }

      "CY == Quarterly - no chosen intent since it is a single year scenario and tax year is deduced" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Annual,
              itsaStatusCY = Voluntary,
              `itsaStatusCY+1` = Annual,
              chosenTaxYear = NoChosenTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )
          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph)
            )


          actual shouldBe Some(expected)
        }
      }

      "CY+1 == Quarterly - no chosen intent since it is a single year scenario and tax year is deduced" should {

        "return the correct content" in {

          val actual: Option[Html] =
            confirmedOptOutViewUtils.submitYourTaxReturnContent(
              `itsaStatusCY-1` = Annual,
              itsaStatusCY = Annual,
              `itsaStatusCY+1` = Voluntary,
              chosenTaxYear = NoChosenTaxYear,
              isMultiYear = true,
              isPreviousYearCrystallised = false
            )
          val expected =
            HtmlFormat.fill(
              Seq(submitYourTaxReturnHeading, firstParagraph)
            )


          actual shouldBe Some(expected)
        }
      }
    }
  }
}