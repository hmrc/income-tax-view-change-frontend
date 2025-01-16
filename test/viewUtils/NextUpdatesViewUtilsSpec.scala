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
import models.optout.OptOutOneYearViewModel
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import services.optout.OneYearOptOutFollowedByAnnual
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

    ".whatTheUserCanDo() - " when {

      "Reporting Frequency feature switch is ON" when {

        "is optOut single year" should {

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
                    link = reportingFrequencyLink,
                    messageKey = "view and change your reporting frequency."
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