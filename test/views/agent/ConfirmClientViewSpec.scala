/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import play.api.mvc.Call
import testUtils.ViewSpec
import views.html.agent.confirmClient


class ConfirmClientViewSpec extends ViewSpec {

  lazy val postAction: Call = controllers.agent.routes.ConfirmClientUTRController.submit()
  lazy val backUrl: String = controllers.agent.routes.EnterClientsUTRController.show().url
  lazy val testClientName: Option[String] = Some("Test Name")
  lazy val testClientUTR: Option[String] = Some("1234567890")

  val confirmClient: confirmClient = app.injector.instanceOf[confirmClient]


    val confirmClientView = confirmClient(
      clientName = testClientName,
      clientUtr = testClientUTR,
      postAction = postAction,
      backUrl = backUrl
    )

  object confirmClientMessages {
    val title: String = "Confirm your client’s details - Your client’s Income Tax details - GOV.UK"
    val heading: String = "Confirm your client’s details"
    val backLink: String = "Back"
    val clientNameHeading: String = "Client’s name"
    val clientUTRHeading: String = "Client’s Unique Taxpayer Reference"
    val changeClient: String = "Change Client"
    val confirmContinue: String = "Confirm and continue"
  }

  "The Confirm Client page" should {

    s"have the title ${confirmClientMessages.title}" in new Setup(confirmClientView) {
      document.title shouldBe confirmClientMessages.title
    }

    s"have the heading ${confirmClientMessages.heading}" in new Setup(confirmClientView) {
      content hasPageHeading confirmClientMessages.heading
    }

    s"have a back link" in new Setup(confirmClientView) {
      content.doesNotHave(Selectors.backLink)
      document.backLink.text shouldBe confirmClientMessages.backLink
      document.hasBackLinkTo(controllers.agent.routes.EnterClientsUTRController.show().url)
    }

    s"have the sub heading ${confirmClientMessages.clientNameHeading}" in new Setup(confirmClientView) {
      content.selectNth("h2", 1).text shouldBe confirmClientMessages.clientNameHeading
    }

    s"display the client name as ${testClientName.get}" in new Setup(confirmClientView) {
      content.selectById("clientName").text shouldBe testClientName.get
    }

    s"have the sub heading ${confirmClientMessages.clientUTRHeading}" in new Setup(confirmClientView) {
      content.selectNth("h2", 2).text shouldBe confirmClientMessages.clientUTRHeading
    }

    s"display the client UTR as ${testClientUTR.get}" in new Setup(confirmClientView) {
      content.selectById("clientUTR").text shouldBe testClientUTR.get
    }

    s"have a ${confirmClientMessages.changeClient} link" in new Setup(confirmClientView) {
      val changeClient = content.selectById("changeClientLink")
      changeClient.text shouldBe confirmClientMessages.changeClient
      changeClient.attr("href") shouldBe controllers.agent.routes.EnterClientsUTRController.show().url
    }

    s"have a ${confirmClientMessages.confirmContinue} button" in new Setup(confirmClientView) {
      val button = content.selectById("continue-button")
      button.attr("type") shouldBe "submit"
      button.text shouldBe confirmClientMessages.confirmContinue
    }
  }


}
