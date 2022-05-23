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

import org.jsoup.nodes.Element
import play.api.mvc.Call
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.ClientRelationshipFailure


class ClientDetailsFailureViewSpec extends ViewSpec {

  lazy val postAction: Call = controllers.agent.routes.EnterClientsUTRController.show()

  val clientRelationshipFailure: ClientRelationshipFailure = app.injector.instanceOf[ClientRelationshipFailure]

  val confirmClientView: Html = clientRelationshipFailure(
    postAction = postAction
  )

  class ClientRelationshipFailureSetup extends Setup(
    clientRelationshipFailure(postAction = postAction)
  )

  object ClientRelationshipMessages {
    val title: String = "You need permission to view this client - Your client’s Income Tax details - GOV.UK"
    val heading: String = "You need permission to view this client"
    val info: String = "Your client needs to authorise you as their agent (opens in new tab) before you can log in to this service.You may want to check the Unique Taxpayer Reference (UTR) to make sure it’s the right one or look up another client."
    val clientAuthorisationLink: String = "authorise you as their agent (opens in new tab)"
    val enterDifferentDetails: String = "Enter another UTR"
  }

  "The Client Relationship Failure page" should {

    s"have the title '${ClientRelationshipMessages.title}'" in new ClientRelationshipFailureSetup {
      document.title shouldBe ClientRelationshipMessages.title
    }

    s"have the heading '${ClientRelationshipMessages.heading}'" in new ClientRelationshipFailureSetup {
      layoutContent hasPageHeading ClientRelationshipMessages.heading
    }

    s"have information about why the user is not authorised for the client" in new ClientRelationshipFailureSetup {
      layoutContent.getElementsByTag("p").get(0).text shouldBe ClientRelationshipMessages.info
    }

    "have a link to the govuk client authorisation overview" in new ClientRelationshipFailureSetup {
      val link: Element = layoutContent.selectHead(s"a[href=${appConfig.clientAuthorisationGuidance}]")
      link.text shouldBe ClientRelationshipMessages.clientAuthorisationLink
      link.attr("target") shouldBe "_blank"
    }

    s"have a ${ClientRelationshipMessages.enterDifferentDetails} button" in new ClientRelationshipFailureSetup {
      val button: Element = layoutContent.selectById("continue-button")
      button.attr("type") shouldBe "submit"
      button.text shouldBe ClientRelationshipMessages.enterDifferentDetails
    }

    s"have a form with ${postAction.method} and ${postAction.url}" in new ClientRelationshipFailureSetup {
      layoutContent.hasFormWith(postAction.method, postAction.url)
    }
  }


}
