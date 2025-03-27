/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testOnly.services

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.itsaStatus.{ITSAStatus, ITSAStatusResponseModel, StatusDetail}
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Json
import services.DateServiceInterface
import testOnly.models.{DataModel, Nino}
import testOnly.utils.OptOutCustomDataUploadHelper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutCustomDataService @Inject()(implicit val appConfig: FrontendAppConfig,
                                        implicit val dateService: DateServiceInterface,
                                        implicit val dynamicStubService: DynamicStubService) extends OptOutCustomDataUploadHelper {

  def uploadCalculationListData(nino: Nino, taxYear: TaxYear, status: String)(implicit hc: HeaderCarrier)
  : Future[Unit] = {
    handleDefaultValues(status = status) {
      Logger("application").info(s" Attempting to overwrite data for < NINO $nino >, < taxYearRange: ${taxYear.formatAsShortYearRange} > and < status: $status >")
      dynamicStubService.overwriteCalculationList(nino = nino, taxYearRange = taxYear.formatAsShortYearRange, crystallisationStatus = status)
    }
  }

  def uploadITSAStatusData(nino: Nino, taxYear: TaxYear, status: String)(implicit hc: HeaderCarrier)
  : Future[Unit] = {
    handleDefaultValues(status = status) {
      Logger("application").info(s" Attempting to overwrite data for < NINO $nino >, < taxYearRange: ${taxYear.formatAsShortYearRange} > and < status: $status >")
      dynamicStubService.overwriteItsaStatus(nino = nino, taxYearRange = taxYear.formatAsShortYearRange, ITSAStatus = status)
    }
  }

  def stubITSAStatusFutureYearData(nino: String,
                                   taxYear: TaxYear,
                                   cyMinusOneItsaStatus: String,
                                   cyItsaStatus: String,
                                   cyPlusOneItsaStatus: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    Logger("application").info(s"cyMinusOneItsaStatus :: $cyMinusOneItsaStatus, cyItsaStatus:: $cyItsaStatus, cyPlusOneItsaStatus:: $cyPlusOneItsaStatus")

    def commonStatusDetails(taxYear: TaxYear, status: ITSAStatus) =
      StatusDetail(
        submittedOn = s"${taxYear.endYear}-01-10T06:14:00Z",
        status = status,
        statusReason = "Sign up - no return available",
        businessIncomePriorTo2Years = None)

    def taxYearFormatForITSA(taxYear: TaxYear): String = s"${taxYear.startYear}-${taxYear.endYear.toString.takeRight(2)}"

    val taxYearMinus1 = taxYear.addYears(-1)
    val taxYearPlus1 = taxYear.addYears(1)


    val cyItsaStatusResponse =
      ITSAStatusResponseModel(
        taxYearFormatForITSA(taxYear),
        Some(List(commonStatusDetails(taxYear, ITSAStatus.withName(cyItsaStatus)))))

    val cyMinusOneItsaStatusResponse =
      ITSAStatusResponseModel(
        taxYearFormatForITSA(taxYearMinus1),
        Some(List(commonStatusDetails(taxYearMinus1, ITSAStatus.withName(cyMinusOneItsaStatus)))))

    val cyPlusOneItsaStatusResponse =
      ITSAStatusResponseModel(
        taxYearFormatForITSA(taxYearPlus1),
        Some(List(commonStatusDetails(taxYearPlus1, ITSAStatus.withName(cyPlusOneItsaStatus)))))

    val itsaStatusCombined: List[ITSAStatusResponseModel] =
      List(cyPlusOneItsaStatusResponse,
        cyItsaStatusResponse,
        cyMinusOneItsaStatusResponse)

    val payload: DataModel = DataModel(
      _id = s"/income-tax/$nino/person-itd/itsa-status/${taxYear.addYears(-1).formatAsShortYearRange}?futureYears=true&history=false",
      schemaId = "getITSAStatusSuccess", method = "GET", status = OK, response = Some(Json.toJson(itsaStatusCombined))
    )

    dynamicStubService.addData(dataModel = payload)

  }
}
