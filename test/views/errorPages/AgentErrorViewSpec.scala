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

package views.errorPages

import assets.MessagesLookUp.{AgentErrorMessages => pageMessages}
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.errorPages.AgentError

class AgentErrorViewSpec extends ViewSpec {

  def agentErrorView: Html = app.injector.instanceOf[AgentError].apply()

  "The Agent Error page" should {

    s"have the title: ${pageMessages.title}" in new Setup(agentErrorView) {
      document.title shouldBe pageMessages.title
    }

    s"have the heading: ${pageMessages.heading}" in new Setup(agentErrorView) {
      document hasPageHeading pageMessages.heading
    }

    "not have a back link" in new Setup(agentErrorView) {
      document doesNotHave Selectors.backLink
    }

    s"have a paragraph stating: ${pageMessages.notAnAgentNote}" in new Setup(agentErrorView) {
      content.select(Selectors.p).text shouldBe pageMessages.notAnAgentNote
    }

    s"have a link in the paragraph: ${pageMessages.setupAccountLink}" in new Setup(agentErrorView) {
      content.selectFirst(Selectors.p)
        .hasCorrectLink(pageMessages.setupAccountLink, "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account")
    }

    s"have a sign out button stating: ${pageMessages.signOutButton}" in new Setup(agentErrorView) {
      val signoutLinkButton = content.select("a[class=button]")
      signoutLinkButton.text shouldBe pageMessages.signOutButton
      signoutLinkButton.attr("href") shouldBe controllers.routes.SignOutController.signOut().url
    }

  }
}
