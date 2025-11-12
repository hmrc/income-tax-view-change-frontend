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
import models.incomeSourceDetails._
import models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                  dateService: DateService,
                                  implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def getITSAStatusDetail(taxYear: TaxYear, futureYears: Boolean, history: Boolean)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_]): Future[List[ITSAStatusResponseModel]] = {
    itsaStatusConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = taxYear.formatAsShortYearRange,
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

  def hasMandatedOrVoluntaryStatusCurrentYear(selector: StatusDetail => Boolean = _.isMandatedOrVoluntary)(
    implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_]
  ): Future[Boolean] = {

    val yearEnd = dateService.getCurrentTaxYearEnd
    val taxYear = TaxYear.forYearEnd(yearEnd)

    getITSAStatusDetail(taxYear, futureYears = false, history = false)
      .map(statusDetail =>
        statusDetail.exists(_.itsaStatusDetails.exists(_.exists(selector))))
  }

  def latencyYearsQuarterlyAndAnnualStatus(latencyDetails: Option[LatencyDetails])
                                          (implicit hc: HeaderCarrier,
                                           ec: ExecutionContext,
                                           user: MtdItUser[_]): Future[LatencyYearsQuarterlyAndAnnualStatus] = {

    latencyDetails match {
      case Some(details) =>
        val taxYear1 = TaxYear.forYearEnd(details.taxYear1.toInt)
        val taxYear2 = TaxYear.forYearEnd(details.taxYear2.toInt)
        for {
          taxYear1StatusDetail <- getITSAStatusDetail(taxYear1, futureYears = false, history = false)
          taxYear2StatusDetail <- getITSAStatusDetail(taxYear2, futureYears = false, history = false)
          latencyYearOneQuarterlyStatus = taxYear1StatusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)))
          isTaxYear1StatusReasonRollover = taxYear1StatusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.statusReasonRollover)))
          latencyYearTwoQuarterlyStatus =
            if (taxYear2StatusDetail.exists(_.itsaStatusDetails.exists(sd => sd.exists(_.isUnknown)
              && isTaxYear1StatusReasonRollover))) latencyYearOneQuarterlyStatus
            else taxYear2StatusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)))
          latencyYearOneAnnualStatus = taxYear1StatusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isAnnual)))
          latencyYearTwoAnnualStatus = if (taxYear2StatusDetail.exists(_.itsaStatusDetails.exists(sd => sd.exists(_.isUnknown)
            && isTaxYear1StatusReasonRollover))) latencyYearOneAnnualStatus
          else taxYear2StatusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isAnnual)))
        } yield {
          LatencyYearsQuarterlyAndAnnualStatus(LatencyYearsQuarterly(Some(latencyYearOneQuarterlyStatus), Some(latencyYearTwoQuarterlyStatus)),
            LatencyYearsAnnual(Some(latencyYearOneAnnualStatus), Some(latencyYearTwoAnnualStatus)))
        }

      case None =>
        Future.successful(LatencyYearsQuarterlyAndAnnualStatus(LatencyYearsQuarterly(Some(false), Some(false)), LatencyYearsAnnual(Some(false), Some(false))))
    }
  }


  def getStatusTillAvailableFutureYears(taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_]): Future[Map[TaxYear, StatusDetail]] = {
    getITSAStatusDetail(taxYear, futureYears = true, history = false)
      .map {
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

