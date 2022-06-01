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
    val heading: String = messages("agent.client_relationship_failure.heading")
    val title: String = messages("agent.titlePattern.serviceName.govUk", heading)
    val info: String = messages("agent.client_relationship_failure.info", s"${messages("agent.client_relationship_failure.info.link")}${messages("pagehelp.opensInNewTabText")}")
    val clientAuthorisationLink: String = s"${messages("agent.client_relationship_failure.info.link")}${messages("pagehelp.opensInNewTabText")}"
    val enterDifferentDetails: String = messages("agent.client_relationship_failure.enter_different_details")
  }

  "The Client Relationship Failure page" should {

    s"have the title ${ClientRelationshipMessages.title}" in new ClientRelationshipFailureSetup {
      document.title shouldBe ClientRelationshipMessages.title
    }

    s"have the heading ${ClientRelationshipMessages.heading}" in new ClientRelationshipFailureSetup {
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
