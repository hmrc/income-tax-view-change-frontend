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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import services.optout.{OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated}
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.UnitSpec
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.components.link

class NextUpdatesViewUtilsSpec extends UnitSpec with FeatureSwitching with ImplicitDateFormatter with GuiceOneAppPerSuite {

  override implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  override implicit val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  val linkComponent: link = app.injector.instanceOf[link]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  implicit val tsTestUser: MtdItUser[AnyContentAsEmpty.type] =
    MtdItUser(
      mtditid = testMtditid, nino = testNino, userName = None, incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty), btaNavPartial = None,
      saUtr = Some("1234567890"), credId = Some("12345-credId"), userType = Some(Individual), arn = None
    )(FakeRequest())


  val nextUpdatesViewUtils = new NextUpdatesViewUtils(linkComponent)

  "NextUpdatesViewUtils" when {

    "Method whatTheUserCanDo() - " when {

      "Reporting Frequency feature switch is ON" when {

        "Single year optout model is used" should {

          "return the correct content" in {

            enable(ReportingFrequencyPage)

            val isAgent = false

            val optOutSingleYear = OptOutOneYearViewModel(oneYearOptOutTaxYear = TaxYear(2025, 2026), state = Some(OneYearOptOutFollowedByAnnual))

            val reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(isAgent).url

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(Some(optOutSingleYear), isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("Depending on your circumstances, you may be able to")),
                  linkComponent(
                    id = Some("reporting-frequency-link"),
                    link = reportingFrequencyLink,
                    messageKey = "view and change your reporting frequency."
                  )
                )
              )

            actual shouldBe Some(expected)
          }
        }

        "multi-year optout model is used" should {

          "return the correct content" in {

            enable(ReportingFrequencyPage)

            val isAgent = false

            val optOutMultiYear = OptOutMultiYearViewModel()

            val reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(isAgent).url

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(Some(optOutMultiYear), isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("nextUpdates.optOutMultiYear.reportingFrequency.p.message")),
                  linkComponent(
                    id = Some("reporting-frequency-link"),
                    link = reportingFrequencyLink,
                    messageKey = "nextUpdates.optOutMultiYear.reportingFrequency.p.link"
                  )
                )
              )

            actual shouldBe Some(expected)
          }
        }

      }

      "Reporting Frequency feature switch is OFF" when {

        "Single year optout model is used and warning shown" should {

          "return the correct content" in {

            disable(ReportingFrequencyPage)

            val isAgent = false

            val taxYear = TaxYear(2025, 2026)

            val optOutSingleYear = OptOutOneYearViewModel(oneYearOptOutTaxYear = taxYear, state = Some(OneYearOptOutFollowedByMandated))

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(Some(optOutSingleYear), isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("nextUpdates.optOutOneYear.p.message", taxYear.startYear.toString, taxYear.endYear.toString)),
                  linkComponent(
                    id = Some("single-year-opt-out-warning-link"),
                    link = controllers.optOut.routes.SingleYearOptOutWarningController.show(isAgent).url,
                    messageKey = "nextUpdates.optOutOneYear.p.link"
                  )
                )
              )

            actual shouldBe Some(expected)
          }
        }

        "Single year optout model is used and warning NOT shown" should {

          "return the correct content" in {

            disable(ReportingFrequencyPage)

            val isAgent = false

            val taxYear = TaxYear(2025, 2026)

            val optOutSingleYear = OptOutOneYearViewModel(oneYearOptOutTaxYear = taxYear, state = Some(OneYearOptOutFollowedByAnnual))

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(Some(optOutSingleYear), isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("nextUpdates.optOutOneYear.p.message", taxYear.startYear.toString, taxYear.endYear.toString)),
                  linkComponent(
                    id = Some("confirm-opt-out-link"),
                    link = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url,
                    messageKey = "nextUpdates.optOutOneYear.p.link"
                  )
                )
              )

            actual shouldBe Some(expected)
          }
        }

        "multi-year optout model is used" should {

          "return the correct content" in {

            disable(ReportingFrequencyPage)

            val isAgent = false

            val optOutMultiYear = OptOutMultiYearViewModel()

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(Some(optOutMultiYear), isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("nextUpdates.optOutMultiYear.p.message")),
                  linkComponent(
                    id = Some("opt-out-link"),
                    link = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url,
                    messageKey = "nextUpdates.optOutMultiYear.p.link"
                  )
                )
              )

            actual shouldBe Some(expected)
          }
        }


      }

    }
  }
}