package testConstants

import models.incomeSourceDetails.TaxYear
import models.optOut.{OptOutApiCallRequest, OptOutApiCallResponse, OptOutApiCallSuccessfulResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse

object OptOutStatusUpdateTestConstants {

  def request: OptOutApiCallRequest = OptOutApiCallRequest(TaxYear.forYearEnd(2023).toString)
  def successHttpResponse: HttpResponse = HttpResponse(Status.OK, Json.toJson(""), Map.empty)

  val expectedSuccessResponse: OptOutApiCallSuccessfulResponse = OptOutApiCallSuccessfulResponse("")

}
