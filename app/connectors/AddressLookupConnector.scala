package connectors

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.GetAddressLookupDetailsHttpParser.GetAddressLookupDetailsResponse
import connectors.PostAddressLookupHttpParser.PostAddressLookupResponse
import models.incomeSourceDetails.BusinessAddressModel
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{AnyContent, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClient)(implicit ec: ExecutionContext) extends FeatureSwitching {

  val baseUrl: String = appConfig.addressLookupService

  def addressLookupInitializeUrl: String = {
    s"${appConfig.addressLookupService}/api/v2/init"
  }

  def getAddressDetailsUrl(mtditid: String): String = {
    s"${appConfig.addressLookupService}/api/v2/confirmed?id=$mtditid"
  }

  def initialiseAddressLookup: Future[PostAddressLookupResponse] = ???

  def getAddressDetails: Future[GetAddressLookupDetailsResponse] = ???


}


object PostAddressLookupHttpParser {

  type PostAddressLookupResponse = Either[PostAddressLookupFailure, PostAddressLookupSuccess]

  implicit def postAddressLookupHttpReads: HttpReads[PostAddressLookupResponse] =
    new HttpReads[PostAddressLookupResponse] {
      override def read(method: String, url: String, response: HttpResponse): PostAddressLookupResponse = {
        response.status match {
          case ACCEPTED => Right(
            PostAddressLookupSuccessResponse(response.header(key = "Location"))
          )
          case status => Left(UnexpectedStatusFailure(status))
        }
      }
    }

  sealed trait PostAddressLookupSuccess

  case class PostAddressLookupSuccessResponse(location: Option[String]) extends PostAddressLookupSuccess

  sealed trait PostAddressLookupFailure

  case class UnexpectedStatusFailure(status: Int) extends PostAddressLookupFailure

}

object GetAddressLookupDetailsHttpParser {

  type GetAddressLookupDetailsResponse = Either[GetAddressLookupDetailsFailure, Option[BusinessAddressModel]]

  implicit def getAddressLookupDetailsHttpReads: HttpReads[GetAddressLookupDetailsResponse] = HttpReads { (_, _, response) =>
    response.status match {
      case OK => response.json.validate[BusinessAddressModel] match {
        case JsSuccess(value, _) => Right(Some(value))
        case _ => Left(InvalidJson)
      }
      case NOT_FOUND => Right(None)
      case status => Left(UnexpectedStatusFailure(status))
    }
  }

  sealed trait GetAddressLookupDetailsFailure

  case object InvalidJson extends GetAddressLookupDetailsFailure

  case class UnexpectedStatusFailure(status: Int) extends GetAddressLookupDetailsFailure

}
