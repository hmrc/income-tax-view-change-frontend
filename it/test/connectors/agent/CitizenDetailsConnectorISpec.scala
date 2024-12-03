package connectors.agent

import com.github.tomakehurst.wiremock.client.WireMock
import helpers.{ComponentSpecBase, WiremockHelper}
import helpers.servicemocks.AuditStub
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.OK
import play.api.test.Injecting

class CitizenDetailsConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  val saUtr = "testUtr"

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  "CitizenDetailsConnector" when {
    "sending a request" should {
      "return a successful response" in {
        WiremockHelper.stubGet(s"/citizen-details/sautr/$saUtr", OK, "{}")

        val result = connector.getCitizenDetailsBySaUtr(saUtr).futureValue

        result shouldBe ""
      }
      "return an error when the request fails" in {

      }
    }
  }
}
