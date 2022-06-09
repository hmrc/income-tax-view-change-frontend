/*
 * Copyright 2022 HM Revenue & Customs
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
    val heading: String = messages("agent.confirmClient.heading")
    val title: String = messages("agent.title_pattern.service_name.govuk", heading)
    val backLink: String = messages("base.back")
    val clientNameHeading: String = messages("agent.confirmClient.clientName")
    val clientUTRHeading: String = messages("agent.confirmClient.clientUtr")
    val changeClient: String = messages("agent.confirmClient.changeClient")
    val confirmContinue: String = messages("agent.confirmClient.confirmContinue")
  }

  "The Confirm Client page" should {

    s"have the title ${confirmClientMessages.title}" in new Setup(confirmClientView) {
      document.title shouldBe confirmClientMessages.title
    }

    s"have the heading ${confirmClientMessages.heading}" in new Setup(confirmClientView) {
      layoutContent hasPageHeading confirmClientMessages.heading
    }

    s"have a back link" in new Setup(confirmClientView) {
      document.hasFallbackBacklink()
    }

    s"have the sub heading ${confirmClientMessages.clientNameHeading}" in new Setup(confirmClientView) {
      layoutContent.selectHead("dl > div:nth-child(1) > dt:nth-child(1)").text shouldBe confirmClientMessages.clientNameHeading
    }

    s"display the client name as ${testClientName.get}" in new Setup(confirmClientView) {
      layoutContent.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe testClientName.get
    }

    s"have the sub heading ${confirmClientMessages.clientUTRHeading}" in new Setup(confirmClientView) {
      layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe confirmClientMessages.clientUTRHeading
    }

    s"display the client UTR as ${testClientUTR.get}" in new Setup(confirmClientView) {
      layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe testClientUTR.get
    }

    s"have a ${confirmClientMessages.changeClient} link" in new Setup(confirmClientView) {
      layoutContent.hasCorrectHref(controllers.agent.routes.EnterClientsUTRController.show().url)
    }

    s"have a ${confirmClientMessages.confirmContinue} button" in new Setup(confirmClientView) {
      val button = layoutContent.selectById("continue-button")
      button.attr("type") shouldBe "submit"
      button.text shouldBe confirmClientMessages.confirmContinue
    }
  }


}
