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

package hub.views.agent

import common.testUtils.ViewSpec
import hub.views.html.agent.ConfirmClientUTRView
import play.api.mvc.Call


class ConfirmClientViewSpec extends ViewSpec {

  lazy val postAction: Call = hub.controllers.agent.routes.ConfirmClientUTRController.submit()
  lazy val backUrl: String = hub.controllers.agent.routes.EnterClientsUTRController.show().url
  lazy val testClientName: Option[String] = Some("Test Name")
  lazy val testClientUTR: Option[String] = Some("1234567890")

  val confirmClient: ConfirmClientUTRView = app.injector.instanceOf[ConfirmClientUTRView]


  def confirmClientView(isSupportingAgent: Boolean = false) = confirmClient(
    clientName = testClientName,
    clientUtr = testClientUTR,
    postAction = postAction,
    backUrl = backUrl,
    isSupportingAgent = isSupportingAgent
  )

  object confirmClientMessages {
    val heading: String = "Confirm your client’s details"
    val title: String = "Confirm your client’s details - Manage your Self Assessment - GOV.UK"
    val backLink: String = "Back"
    val clientNameHeading: String = "Client’s name"
    val clientUTRHeading: String = "Client’s Unique Taxpayer Reference (UTR)"
    val changeClient: String = "Change client"
    val confirmContinue: String = "Confirm and continue"
    
    val supportingAgentText = "Supporting agents cannot access a client’s tax account information that shows:"
    val supportingAgentBullets = "payments, credits and refunds returns next charges due penalties and appeals"
    
  }

  "the Confirm Client page" should {

    s"have the title ${confirmClientMessages.title}" in new Setup(confirmClientView()) {
      document.title shouldBe confirmClientMessages.title
    }

    s"have the heading ${confirmClientMessages.heading}" in new Setup(confirmClientView()) {
      layoutContent hasPageHeading confirmClientMessages.heading
    }

    s"have a back link" in new Setup(confirmClientView()) {
      document.hasFallbackBacklink
    }

    s"have the sub heading ${confirmClientMessages.clientNameHeading}" in new Setup(confirmClientView()) {
      layoutContent.selectHead("dl > div:nth-child(1) > dt:nth-child(1)").text shouldBe confirmClientMessages.clientNameHeading
    }

    s"display the client name as ${testClientName.get}" in new Setup(confirmClientView()) {
      layoutContent.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text shouldBe testClientName.get
    }

    s"have the sub heading ${confirmClientMessages.clientUTRHeading}" in new Setup(confirmClientView()) {
      layoutContent.selectHead("dl > div:nth-child(2) > dt:nth-child(1)").text shouldBe confirmClientMessages.clientUTRHeading
    }

    s"display the client UTR as ${testClientUTR.get}" in new Setup(confirmClientView()) {
      layoutContent.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text shouldBe testClientUTR.get
    }

    s"have a ${confirmClientMessages.changeClient} link" in new Setup(confirmClientView()) {
      layoutContent.hasCorrectHref(hub.controllers.agent.routes.EnterClientsUTRController.show().url)
    }

    s"have a ${confirmClientMessages.confirmContinue} button" in new Setup(confirmClientView()) {
      val button = layoutContent.selectById("continue-button")
      button.attr("type") shouldBe "submit"
      button.text shouldBe confirmClientMessages.confirmContinue
    }
    s"have the black banner empty" in new Setup(confirmClientView()) {
      document.select(".govuk-header__content")
        .select(".hmrc-header__service-name hmrc-header__service-name--linked").text shouldBe ("")
    }
    
    "show supporting agent text when isSupportingAgent is true" in new Setup(confirmClientView(true)) {
      layoutContent.getElementById("supporting-agent-access-text").text() shouldBe confirmClientMessages.supportingAgentText
    }
    
    "show supporting agent bullets when isSupportingAgent is true" in new Setup(confirmClientView(true)) {
      layoutContent.getElementById("supporting-agent-access-bullets").text() shouldBe confirmClientMessages.supportingAgentBullets
    }
    
     "not show supporting agent text when isSupportingAgent is false" in new Setup(confirmClientView()) {
      Option(layoutContent.getElementById("supporting-agent-access-text")) shouldBe None
    }
  }
}
