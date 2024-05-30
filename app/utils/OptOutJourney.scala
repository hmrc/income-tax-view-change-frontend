package utils

import auth.MtdItUser
import models.incomeSourceDetails.UIJourneySessionData
import play.api.mvc.Result
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait OptOutJourney {
  self =>
  val sessionService: SessionService

  implicit val ec: ExecutionContext

  def withSessionData(codeBlock: UIJourneySessionData => Future[Result])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val journeyType = "OPTOUT"
    sessionService.getMongo(journeyType).flatMap {
      case Right(Some(data: UIJourneySessionData)) => codeBlock(data)
      case Right(None) =>
        sessionService.createSession(journeyType).flatMap { _ =>
          val data = UIJourneySessionData(hc.sessionId.get.value, journeyType)
          codeBlock(data)
        }
      case Left(ex) => ???
    }
  }
}
