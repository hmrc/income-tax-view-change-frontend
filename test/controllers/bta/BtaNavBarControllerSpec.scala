
package controllers.bta


import connectors.BtaNavBarPartialConnector
import mocks.controllers.predicates.MockAuthenticationPredicate
import models.btaNavBar._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import play.test.Helpers.fakeRequest
import services.BtaNavBarService
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.bta.BtaNavBar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BtaNavBarControllerSpec extends MockAuthenticationPredicate with UnitSpec with TestSupport with MockitoSugar with ScalaFutures {

  val mockNavBarService: BtaNavBarService = mock[BtaNavBarService]
  val mockBtaNavBarPartialConnector: BtaNavBarPartialConnector = mock[BtaNavBarPartialConnector]
  val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val saUtr = "1234567800"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testController = new BtaNavBarController(mockBtaNavBarPartialConnector, testView, mockMcc, mockNavBarService)


  "ServiceInfoController" should {
//    "retrieve the correct Model and return HTML" in {
//      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))
//
//      val navContent = NavContent(
//        NavLinks("testEnHome", "testCyHome", "testUrl"),
//        NavLinks("testEnAccount", "testCyAccount", "testUrl"),
//        NavLinks("testEnMessages", "testCyMessages", "testUrl"),
//        NavLinks("testEnHelp", "testCyHelp", "testUrl"),
//        NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
//      )
//
//      val listLinks: Seq[ListLinks] = Seq(
//        ListLinks("testEnHome", "testUrl"),
//        ListLinks("testEnAccount", "testUrl"),
//        ListLinks("testEnMessages", "testUrl", Some("0")),
//        ListLinks("testEnForm", "testUrl", Some("1")),
//        ListLinks("testEnHelp", "testUrl"),
//      )
//
//      when(mockBtaNavBarPartialConnector.getNavLinks()(any(), any()))
//        .thenReturn(Future.successful(Some(navContent)))
//
//      when(mockNavBarService.partialList(any())(any())).thenReturn(listLinks)
//
//      val result = testController.btaNavBarPartial(AuthenticatedRequest(fakeRequest, Some(saUtr), None))
//
//      whenReady(result) { response =>
//        response.toString must include(testView.apply(listLinks).toString())
//      }
//    }
//
//    "retrieve the empty Model and empty HTML" in {
//
//      implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))
//
//      when(mockBtaNavBarPartialConnector.getNavLinks()(any(), any()))
//        .thenReturn(Future.successful(None))
//
//      when(mockNavBarService.partialList(any())(any())).thenReturn(Seq())
//
//      val result = testController.btaNavBarPartial(AuthenticatedRequest(fakeRequest, Some(saUtr), None))
//
//
//      whenReady(result) { response =>
//        response.toString must include(testView.apply(Seq()).toString())
//      }
//    }
  }
}
