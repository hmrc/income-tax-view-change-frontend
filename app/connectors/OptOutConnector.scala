package connectors

import config.FrontendAppConfig
import models.optOut.OptOutApiCallResponse
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutConnector @Inject()(val http: HttpClient,
                                val appConfig: FrontendAppConfig
                               )(implicit val ec: ExecutionContext) extends RawResponseReads {

  //todo appConfig.itvcProtectedService?
  def getUrl(taxableEntityId: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax/itsa-status/update/$taxableEntityId"
  }

  def makeOptOutCall(taxableEntityId: String)(implicit headerCarrier: HeaderCarrier): Future[OptOutApiCallResponse] = {

    val body = ""

    http.PUT[UpdateIncomeSourceRequestModel, HttpResponse](
      getUrl(taxableEntityId),
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[OptOutApiCallResponse].fold(
          invalid => {
            Logger("application").error("" +
              s"Json validation error parsing update income source response, error $invalid")
            UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
          },
          valid => valid
        )
        case _ =>
          response.json.validate[OptOutApiCallResponse].fold(
            invalid => {
              Logger("application").error("" +
                s"Json validation error parsing update income source response, error $invalid")
              //UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
            },
            valid => valid
          )
      }
    }
  }

}
