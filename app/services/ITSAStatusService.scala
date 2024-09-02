/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.ITSAStatusConnector
import models.incomeSourceDetails.{LatencyDetails, TaxYear}
import models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                  dateService: DateService,
                                  implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {
  private def getITSAStatusDetail(taxYear: TaxYear, futureYears: Boolean, history: Boolean)
                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[ITSAStatusResponseModel]] = {
    itsaStatusConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = taxYear.formatTaxYearRange,
      futureYears = futureYears,
      history = history).flatMap {
      case Right(itsaStatus) => Future.successful(itsaStatus)
      case Left(error) =>
        Logger("application").error(s"$error")
        Future.failed(new Exception("Failed to retrieve ITSAStatus"))
    }
  }

  private def getStatusDetail(itsaStatusResponseModel: ITSAStatusResponseModel): Option[StatusDetail] = {
    itsaStatusResponseModel.itsaStatusDetails.flatMap(statusDetail => statusDetail.headOption)
  }

  def hasMandatedOrVoluntaryStatusCurrentYear(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val yearEnd = dateService.getCurrentTaxYearEnd
    val taxYear = TaxYear.forYearEnd(yearEnd)

    getITSAStatusDetail(taxYear, futureYears = false, history = false)
      .map(statusDetail =>
        statusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))))
  }

  def hasMandatedOrVoluntaryStatusForLatencyYears(latencyDetails: Option[LatencyDetails])
  (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[(Boolean, Boolean)] = {

    latencyDetails match {
      case Some(details) =>
        val taxYear1 = TaxYear.forYearEnd(details.taxYear1.toInt)
        val taxYear2 = TaxYear.forYearEnd(details.taxYear2.toInt)

        val taxYear1StatusFuture = getITSAStatusDetail(taxYear1, futureYears = false, history = false)
        val taxYear2StatusFuture = getITSAStatusDetail(taxYear2, futureYears = false, history = false)

        for {
          taxYear1Status <- taxYear1StatusFuture.map(statusDetail =>
            statusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))))
          taxYear2Status <- taxYear2StatusFuture.map(statusDetail =>
            statusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))))
        } yield {
          (taxYear1Status, taxYear2Status)
        }

      case None =>
        Future.successful((false, false))
    }
  }



  def getStatusTillAvailableFutureYears(taxYear: TaxYear)(implicit user: MtdItUser[_],
                                                          hc: HeaderCarrier,
                                                          ec: ExecutionContext): Future[Map[TaxYear, StatusDetail]] = {
    getITSAStatusDetail(taxYear, futureYears = true, history = false).map {
      _.map(responseModel => parseTaxYear(responseModel.taxYear) -> getStatusDetail(responseModel)).flatMap {
        case (taxYear, Some(statusDetail)) => Some(taxYear -> statusDetail)
        case _ => None
      }.toMap
    }
  }

  private def parseTaxYear(taxYear: String) = {
    //item.taxYear has string format as 2021-22
    TaxYear.forYearEnd(taxYear.split("-")(0).toInt + 1)
  }
}

