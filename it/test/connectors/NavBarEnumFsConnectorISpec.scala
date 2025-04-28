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

import _root_.helpers.ComponentSpecBase
import _root_.helpers.servicemocks.BtaNavBarPartialConnectorStub
import _root_.helpers.servicemocks.BtaNavBarPartialConnectorStub.testNavLinkJson
import models.btaNavBar.{NavContent, NavLinks}
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.Injecting

import scala.concurrent.Future
import scala.concurrent.duration._

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