package helpers

object ITSAStatusUpdateConnectorStub {
  def stubPUTItsaStatusUpdate(taxableEntityId: String, status: Int, responseBody: String, headers: Map[String, String] = Map()): Unit =
    WiremockHelper.stubPutWithHeaders(s"/income-tax/itsa-status/update/$taxableEntityId", status = status, responseBody = responseBody, headers)
}