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

package views.incomeSources.cease

import auth.MtdItUser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testRetrievedUserName, testSaUtr, testUserTypeAgent, testUserTypeIndividual}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessAndPropertyAligned, businessesAndPropertyIncome}
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.retrieve.Name
import views.html.incomeSources.cease.CheckCeaseUKPropertyDetails

class CheckCeaseUKPropertyDetailsViewSpec extends TestSupport with ViewSpec{
  val checkCeaseUKPropertyDetailsView = app.injector.instanceOf[CheckCeaseUKPropertyDetails]
  val businessEndDate = "2022-04-23"
  val businessEndShortLongDate = "23 Apr 2022"
  val individualUseWithSession: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(FakeRequest().withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> businessEndDate))

 def agentUserConfirmedClientWithSession(clientNino: String = "AA111111A"): MtdItUser[_] = MtdItUser(
    mtditid = "XAIT00000000015",
    nino = clientNino,
    userName = Some(Name(Some("Test"), Some("User"))),
    incomeSources = businessesAndPropertyIncome,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some(testCredId),
    userType = Some(testUserTypeAgent),
    arn = Some(testArn)
  )(FakeRequest().withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> businessEndDate))

  lazy val viewAgent: HtmlFormat.Appendable = checkCeaseUKPropertyDetailsView(true)(agentUserConfirmedClientWithSession(), implicitly)
  lazy val view: HtmlFormat.Appendable = checkCeaseUKPropertyDetailsView(false)(individualUseWithSession, implicitly)

  val caption = messages("incomeSources.ceaseUKProperty.checkDetails.paragraph")
  val heading = messages("incomeSources.ceaseUKProperty.checkDetails.heading")
  val businessStopDateLabel = messages("incomeSources.ceaseUKProperty.checkDetails.content")
  val buttonLabel = messages("incomeSources.ceaseUKProperty.checkDetails.confirm")
  val changeUrl = controllers.incomeSources.cease.routes.UKPropertyEndDateController.show().url
  val changeUrlAgent = controllers.incomeSources.cease.routes.UKPropertyEndDateController.showAgent().url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submit().url
  val formActionAgent = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submitAgent().url

  "CheckCeaseUKPropertyDetailsView - Individual" should {

    "render the page " in new Setup(view) {
        document.hasPageHeading(heading)
        document.getElementById("caption").text() shouldBe  caption
        document.getElementById("businessStopDateLabel").text() shouldBe businessStopDateLabel
        document.getElementById("businessStopDate").text() shouldBe businessEndShortLongDate
        document.getElementById("change").attr("href") shouldBe changeUrl
        document.getElementById("continue-button").text() shouldBe buttonLabel
        document.form.attr("action") shouldBe formAction

    }
  }

  "CheckCeaseUKPropertyDetailsView - Agent" should {

    "render the page " in new Setup(viewAgent) {
        document.hasPageHeading(heading)
        document.getElementById("caption").text() shouldBe  caption
        document.getElementById("businessStopDateLabel").text() shouldBe businessStopDateLabel
        document.getElementById("businessStopDate").text() shouldBe businessEndShortLongDate
        document.getElementById("change").attr("href") shouldBe changeUrlAgent
        document.getElementById("continue-button").text() shouldBe buttonLabel
        document.form.attr("action") shouldBe formActionAgent

    }
  }
}
