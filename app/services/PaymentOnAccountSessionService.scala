package services

import config.FrontendAppConfig
import models.paymentOnAccount.PoAAmmendmentData
import repositories.PoAAmmendmentDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentOnAccountSessionService @Inject()(
                                                poAAmmendmentDataRepository: PoAAmmendmentDataRepository,
                                                config: FrontendAppConfig
                                              ) {

  def createSession(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    setMongoData(PoAAmmendmentData(hc.sessionId.get.value))
  }

  def getMongo(journeyType: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, Option[PoAAmmendmentData]]] = {
    poAAmmendmentDataRepository.get(hc.sessionId.get.value) map {
      case Some(data: PoAAmmendmentData) =>
        Right(Some(data))
      case None => Right(None)
    }
  }

  private def setMongoData(poAAmmendmentData: PoAAmmendmentData)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    poAAmmendmentDataRepository.set(poAAmmendmentData)
  }

}
