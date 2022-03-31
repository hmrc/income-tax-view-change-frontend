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
import models.navBar.{NavContent, NavLinks}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BtaNavBarPartialConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  val mockHttpGet: HttpClient = mock[HttpClient]
  val frontendAppConfig = mock[FrontendAppConfig]

  object TestBtaNavBarPartialConnector extends BtaNavBarPartialConnector(mockHttpGet, frontendAppConfig)

  val successResponseNavLinks = NavContent(
    NavLinks("Home", "Hafan", frontendAppConfig.homePageUrl),
    NavLinks("Manage account", "Rheoli'r cyfrif", "http://localhost:9020/business-account/manage-account"),
    NavLinks("Messages", "Negeseuon", "http://localhost:9020/business-account/messages", Some(5)),
    NavLinks("Help and contact", "Cymorth a chysylltu", "http://localhost:9733/business-account/help"),
    NavLinks("Track your forms{0}", "Gwirio cynnydd eich ffurflenni{0}", "/track/bta", Some(0))
  )

  "The BtaNavBarPartialConnector.getNavLinks() method" when {
    lazy val btaNavLinkUrl: String = TestBtaNavBarPartialConnector.btaNavLinksUrl
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def result: Future[Option[NavContent]] = TestBtaNavBarPartialConnector.getNavLinks()

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

}
