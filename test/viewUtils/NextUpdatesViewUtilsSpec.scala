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
import authV2.AuthActionsTestData.defaultMTDITUser
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.admin.{FeatureSwitch, ReportingFrequencyPage}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import services.reportingObligations.optOut.{OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated}
import testConstants.BaseTestConstants.{testNino, testUserTypeIndividual}
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.components.link

class NextUpdatesViewUtilsSpec extends UnitSpec with TestSupport with ImplicitDateFormatter with GuiceOneAppPerSuite {

  override implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  override implicit val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  val linkComponent: link = app.injector.instanceOf[link]

  override implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  override implicit val individualUser: MtdItUser[_] = defaultMTDITUser(Some(testUserTypeIndividual),
    IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))
    .addFeatureSwitches(List(FeatureSwitch(ReportingFrequencyPage, true)))

  val nextUpdatesViewUtils = new NextUpdatesViewUtils(linkComponent)

  "NextUpdatesViewUtils" when {

    "Method whatTheUserCanDo() - " when {

      "Reporting Frequency feature switch is ON" when {

        "any optout model is used" should {

          "return the correct content" in {

            enable(ReportingFrequencyPage)

            val isAgent = false
            
            val reportingFrequencyLink = controllers.reportingObligations.routes.ReportingFrequencyPageController.show(isAgent).url

            val actual = nextUpdatesViewUtils.whatTheUserCanDo(isAgent)
            val expected =
              HtmlFormat.fill(
                Seq(
                  Html(messages("Depending on your circumstances,")),
                  linkComponent(
                    id = Some("reporting-frequency-link"),
                    link = reportingFrequencyLink,
                    messageKey = "you may be able to view and change your reporting obligations",
                    outerMessage = "."
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