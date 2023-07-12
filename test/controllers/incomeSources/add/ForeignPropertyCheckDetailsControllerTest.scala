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

package controllers.incomeSources.add

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.CreateBusinessDetailsService
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.ForeignPropertyCheckDetails

import java.time.LocalDate

class ForeignPropertyCheckDetailsControllerTest extends TestSupport with MockAuthenticationPredicate
with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching {

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  val testForeignPropertyStartDate: String = LocalDate.of(2023, 1, 2).toString
  val testForeignPropertyAccountingMethod: String = "cash"

  object TestForeignPropertyCheckDetailsController extends ForeignPropertyCheckDetailsController (
    app.injector.instanceOf[ForeignPropertyCheckDetails],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    businessDetailsService = mockBusinessDetailsService)(
    ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val htmlTitle: String = s"${messages("htmlTitle", messages("incomeSources.add.foreign-property-check-details.title"))}"
    val title: String = messages("incomeSources.add.foreign-property-check-details.title")
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.foreign-property-check-details.title"))}"
    val heading: String = messages("incomeSources.add.foreign-property-check-details.heading")
    val link: String = s"${messages("incomeSources.add.foreign-property-check-details.change")}"
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "ForeignPropertyCheckDetailsController" should {
    "return 200 OK" when {
      "the session contains full business details and FS enabled" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        val result = TestForeignPropertyCheckDetailsController.show()(
          fakeRequestWithActiveSession
            .withSession(
              SessionKeys.foreignPropertyStartDate -> testForeignPropertyStartDate,
              SessionKeys.addForeignPropertyAccountingMethod -> testForeignPropertyAccountingMethod
            ))

        val document: Document = Jsoup.parse(contentAsString(result))
        val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")

        status(result) shouldBe OK
        document.title shouldBe TestForeignPropertyCheckDetailsController.htmlTitle
        document.select("h1:nth-child(1)").text shouldBe TestForeignPropertyCheckDetailsController.title
        changeDetailsLinks.first().text shouldBe TestForeignPropertyCheckDetailsController.link

      }
    }
  }


  }
