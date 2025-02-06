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

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import connectors.BtaNavBarPartialConnector
import models.btaNavBar._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.i18n.{Lang, Messages}
import play.api.inject.guice.GuiceApplicationBuilder
import services.BtaNavBarService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.navBar.BtaNavBar

import scala.concurrent.Future

class NavBarEnumFsControllerSpec extends TestSupport {

  lazy val mockNavBarService:             BtaNavBarService          = mock(classOf[BtaNavBarService])
  lazy val mockBtaNavBarPartialConnector: BtaNavBarPartialConnector = mock(classOf[BtaNavBarPartialConnector])

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      api.inject.bind[BtaNavBarService].toInstance(mockNavBarService),
      api.inject.bind[BtaNavBarPartialConnector].toInstance(mockBtaNavBarPartialConnector)
    )
    .build()

  lazy val testController = app.injector.instanceOf[BtaNavBarController]

  lazy val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]
  val saUtr = "1234567800"

  lazy val successResponse: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncome)

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

      val result = testController.btaNavBarPartial(successResponse)

      whenReady(result) { response =>
        response.toString shouldBe (testView.apply(listLinks).toString())
      }
    }

    "retrieve the empty Model and empty HTML" in {

      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

      when(mockBtaNavBarPartialConnector.getNavLinks()(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockNavBarService.partialList(any())(any())).thenReturn(Seq())

      val result = testController.btaNavBarPartial(successResponse)

      whenReady(result) { response =>
        response.toString shouldBe (testView.apply(Seq()).toString())
      }
    }
  }
}
