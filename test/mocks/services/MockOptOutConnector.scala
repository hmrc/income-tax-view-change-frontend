package mocks.services

import connectors.OptOutConnector
import models.incomeSourceDetails.TaxYear
import models.optOut.OptOutUpdateRequestModel.OptOutUpdateResponse
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptOutConnector extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutConnector: OptOutConnector = mock(classOf[OptOutConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutConnector)
  }

  def setupMockRequestOptOutForTaxYear(taxYear: TaxYear, taxableEntityId: String)(out: OptOutUpdateResponse): Unit = {
    when(mockOptOutConnector.requestOptOutForTaxYear(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(taxableEntityId))
      (any())).thenReturn(Future.successful(out))
  }

}