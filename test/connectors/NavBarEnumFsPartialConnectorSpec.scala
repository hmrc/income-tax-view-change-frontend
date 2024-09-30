/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.http.HttpReads

import scala.concurrent.Future

class NavBarEnumFsPartialConnectorSpec extends BaseConnectorSpec {

  object TestBtaNavBarPartialConnector extends BtaNavBarPartialConnector(mockHttpClientV2, appConfig)

  val successResponseNavLinks = NavContent(
    NavLinks("Home", "Hafan", appConfig.homePageUrl),
    NavLinks("Manage account", "Rheoli'r cyfrif", "http://localhost:9020/business-account/manage-account"),
    NavLinks("Messages", "Negeseuon", "http://localhost:9020/business-account/messages", Some(5)),
    NavLinks("Help and contact", "Cymorth a chysylltu", "http://localhost:9733/business-account/help"),
    NavLinks("Track your forms{0}", "Gwirio cynnydd eich ffurflenni{0}", "/track/bta", Some(0))
  )

  "The BtaNavBarPartialConnector.getNavLinks() method" when {

    def result: Future[Option[NavContent]] = TestBtaNavBarPartialConnector.getNavLinks()

    "a valid NavLink Content is received" should {
      "retrieve the correct Model" in {

        val response = Some(successResponseNavLinks)

        when(mockHttpClientV2.get(any())(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[Option[NavContent]]], any()))
          .thenReturn(Future.successful(response))


        whenReady(result) { response =>
          response mustBe Some(successResponseNavLinks)
        }
      }
    }

    "a BadRequest(400) exception occurs" should {
      "fail and return empty content" in {

        when(mockHttpClientV2.get(any())(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[Option[NavContent]]], any()))
          .thenReturn(Future.failed(new Exception))

        whenReady(result) { response =>
          response mustBe None
        }
      }
    }
  }

}
