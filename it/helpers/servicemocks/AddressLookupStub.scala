package helpers.servicemocks

import helpers.{ComponentSpecBase, WiremockHelper}

object AddressLookupStub extends ComponentSpecBase{

  val addressLookupServiceChangeUrl = "/income-tax-view-change/income-sources/add/change-business-address-lookup"

  def stubGetAddressLookupServiceResponse(status: Int): Unit =
    WiremockHelper.stubGetNoBody(addressLookupServiceChangeUrl, status)

//  def stubPost
}
