package helpers.servicemocks

import helpers.{ComponentSpecBase, WiremockHelper}

object AddressLookupStub extends ComponentSpecBase {

  def stubPostInitialiseAddressLookup: Unit =
    WiremockHelper.stubPostWithHeader("/api/v2/init", 202, "location", "TestRedirect")

}
