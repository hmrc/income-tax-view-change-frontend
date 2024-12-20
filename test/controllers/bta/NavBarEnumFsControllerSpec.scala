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

package controllers.bta


import auth.{MtdItUser, MtdItUserWithNino}
import connectors.BtaNavBarPartialConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.btaNavBar._
import org.mockito.ArgumentMatchers.{any, contains}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito.mock
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import services.BtaNavBarService
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier
import views.html.navBar.BtaNavBar

import scala.concurrent.Future

class NavBarEnumFsControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with UnitSpec with TestSupport with ScalaFutures {

  val mockNavBarService: BtaNavBarService = mock(classOf[BtaNavBarService])
  val mockBtaNavBarPartialConnector: BtaNavBarPartialConnector = mock(classOf[BtaNavBarPartialConnector])
  val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val saUtr = "1234567800"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val testController = new BtaNavBarController(mockBtaNavBarPartialConnector, testView, mockMcc, mockNavBarService)

  lazy val userWithNino: MtdItUserWithNino[Any] = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName),
    None, Some("testUtr"), Some("testCredId"), Some(Individual), None)
  lazy val successResponse: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some(Individual), None)

  "ServiceInfoController" should {
    "retrieve the correct Model and return HTML" in {
      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

      val navContent = NavContent(
        NavLinks("testEnHome", "testCyHome", "testUrl"),
        NavLinks("testEnAccount", "testCyAccount", "testUrl"),
        NavLinks("testEnMessages", "testCyMessages", "testUrl"),
        NavLinks("testEnHelp", "testCyHelp", "testUrl"),
        NavLinks("testEnForm", "testCyForm", "testUrl", Some(1))
      )

      val listLinks: Seq[ListLinks] = Seq(
        ListLinks("testEnHome", appConfig.homePageUrl),
        ListLinks("testEnAccount", "testUrl"),
        ListLinks("testEnMessages", "testUrl", Some("0")),
        ListLinks("testEnForm", "testUrl", Some("1")),
        ListLinks("testEnHelp", "testUrl")
      )

      when(mockBtaNavBarPartialConnector.getNavLinks()(any(), any()))
        .thenReturn(Future.successful(Some(navContent)))

      when(mockNavBarService.partialList(any())(any())).thenReturn(listLinks)

      val result = testController.btaNavBarPartial(userWithNino)

      whenReady(result) { response =>
        response.get.toString shouldBe (testView.apply(listLinks).toString())
      }
    }

    "retrieve the empty Model and empty HTML" in {

      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

      when(mockBtaNavBarPartialConnector.getNavLinks()(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockNavBarService.partialList(any())(any())).thenReturn(Seq())

      val result = testController.btaNavBarPartial(successResponse)


      whenReady(result) { response =>
        response.get.toString shouldBe (testView.apply(Seq()).toString())
      }
    }
  }
}
