package helpers.servicemocks

import helpers.{ComponentSpecBase, WiremockHelper}

object AddressLookupStub extends ComponentSpecBase{

  val addressLookupServiceChangeUrl = "/income-tax-view-change/income-sources/add/change-business-address-lookup"

//  def stubGetAddressLookupServiceResponse(status: Int): Unit =
//    WiremockHelper.stubGetNoBody(addressLookupServiceChangeUrl, status)

  def stubPostInitialiseAddressLookup: Unit =
    WiremockHelper.stubPost("http://localhost:9028/api/v2/init", 200, "Success")
}
