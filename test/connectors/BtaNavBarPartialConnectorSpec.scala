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

package connectors

import config.FrontendAppConfig
import models.btaNavBar.{NavContent, NavLinks}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BtaNavBarPartialConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  "The ServiceInfoPartialConnector.getNavLinks() method" when {
    lazy val btaNavLinkUrl: String = TestServiceInfoPartialConnector.btaNavLinksUrl
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def result: Future[Option[NavContent]] = TestServiceInfoPartialConnector.getNavLinks()

    "a valid NavLink Content is received" should {
      "retrieve the correct Model" in {

        when(mockHttpGet.GET[Option[NavContent]](eqTo(btaNavLinkUrl), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(successResponseNavLinks)))

        whenReady(result) { response =>
          response mustBe Some(successResponseNavLinks)
        }
      }
    }

    "a BadRequest(400) exception occurs" should {
      "fail and return empty content" in {
        when(mockHttpGet.GET[Option[NavContent]](eqTo(btaNavLinkUrl), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new Exception))

        whenReady(result) { response =>
          response mustBe None
        }
      }
    }
  }

  val mockHttpGet: HttpClient = mock[HttpClient]
  val frontendAppConfig = mock[FrontendAppConfig]

  object TestServiceInfoPartialConnector extends BtaNavBarPartialConnector(mockHttpGet, frontendAppConfig)

  val serviceInfoPartialSuccess =
    Html(
      """
    <a id="service-info-home-link"
       class="service-info__item service-info__left font-xsmall button button--link button--link-table button--small soft-half--sides"
       data-journey-click="link - click:Service info:Business tax home"
       href="/business-account">
      Business tax home
    </a>
    <ul id="service-info-list"
      class="service-info__item service-info__right list--collapse">
      <li class="list__item">
        <span id="service-info-user-name" class="bold-xsmall">Test User</span>
      </li>
      <li class="list__item soft--left">
        <a id="service-info-manage-account-link"
           href="/business-account/manage-account"
          data-journey-click="link - click:Service info:Manage account">
          Manage account
        </a>
      </li>
      <li class="list__item soft--left">
        <a id="service-info-messages-link"
           href="/business-account/messages"
          data-journey-click="link - click:Service info:Messages">
          Messages
        </a>
      </li>
    </ul>
  """.stripMargin.trim
    )

  val successResponseNavLinks = NavContent(
    NavLinks("Home", "Hafan", "http://localhost:9020/business-account"),
    NavLinks("Manage account", "Rheoli'r cyfrif", "http://localhost:9020/business-account/manage-account"),
    NavLinks("Messages", "Negeseuon", "http://localhost:9020/business-account/messages", Some(5)),
    NavLinks("Help and contact", "Cymorth a chysylltu", "http://localhost:9733/business-account/help"),
    NavLinks("Track your forms{0}", "Gwirio cynnydd eich ffurflenni{0}", "/track/bta", Some(0))
  )

}
