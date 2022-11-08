package auth

import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import javax.inject.Singleton


@Singleton
class TestHeaderExtractor extends HeaderExtractor {

  override def extractHeader(request: play.api.mvc.Request[_], session: play.api.mvc.Session): HeaderCarrier = {
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
      .copy(authorization = Some(Authorization("Bearer")))
  }
  
}
