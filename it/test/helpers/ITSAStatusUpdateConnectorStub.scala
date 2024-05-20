package helpers

object ITSAStatusUpdateConnectorStub {
  def stubPUTItsaStatusUpdate(taxableEntityId: String, status: Int, responseBody: String): Unit =
    WiremockHelper.stubPut(s"/income-tax/itsa-status/update/$taxableEntityId", status = status, responseBody = responseBody)
}