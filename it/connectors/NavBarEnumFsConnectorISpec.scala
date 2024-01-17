
package connectors

import _root_.helpers.ComponentSpecBase
import _root_.helpers.servicemocks.BtaNavBarPartialConnectorStub
import _root_.helpers.servicemocks.BtaNavBarPartialConnectorStub.testNavLinkJson
import models.btaNavBar.{NavContent, NavLinks}
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}


import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.Future

class NavBarEnumFsConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  val timeout: PatienceConfig = PatienceConfig(5.seconds)
  lazy val connector: BtaNavBarPartialConnector = inject[BtaNavBarPartialConnector]

  "ServiceInfoPartialConnector" when {

    "Requesting NavLinks Content" should {
      "Return the correct json for Navlinks" in {

        val expectedNavlinks = Some(NavContent(
          home = NavLinks("Home", "Hafan", "http://localhost:9020/business-account"),
          account = NavLinks("Manage account", "Rheoli'r cyfrif", "http://localhost:9020/business-account/manage-account"),
          messages = NavLinks("Messages", "Negeseuon", "http://localhost:9020/business-account/messages", Some(5)),
          help = NavLinks("Help and contact", "Cymorth a chysylltu", "http://localhost:9733/business-account/help"),
          forms = NavLinks("Track your forms{0}", "Gwirio cynnydd eich ffurflenni{0}", "/track/bta", Some(0))))

        BtaNavBarPartialConnectorStub.withResponseForNavLinks()(200, Some(testNavLinkJson))

        val result: Future[Option[NavContent]] = connector.getNavLinks()

        await(result) shouldBe expectedNavlinks


        BtaNavBarPartialConnectorStub.verifyNavlinksContent(1)

      }

      "Return None with failed status" in {

        BtaNavBarPartialConnectorStub.withResponseForNavLinks()(500, None)

        val result: Future[Option[NavContent]] = connector.getNavLinks()(hc, ec)

        await(result) shouldBe None

        BtaNavBarPartialConnectorStub.verifyNavlinksContent(1)

      }
    }
  }

}