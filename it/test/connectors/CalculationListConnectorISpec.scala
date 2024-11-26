package connectors

import _root_.helpers.ComponentSpecBase
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Injecting

class CalculationListConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: CalculationListConnector = app.injector.instanceOf[CalculationListConnector]

  "CalculationListConnector" when

}
