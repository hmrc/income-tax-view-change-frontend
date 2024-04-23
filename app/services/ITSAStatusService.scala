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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus
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
        Logger("application").error(s"[ITSAStatusService][hasMandatedOrVoluntaryStatusCurrentYear] $error")
        Future.failed(new Exception("[ITSAStatusService][hasMandatedOrVoluntaryStatusCurrentYear] - Failed to retrieve ITSAStatus"))
    }
  }

  private def getStatus(itsaStatusResponseModel: ITSAStatusResponseModel): Option[ITSAStatus] = {
    itsaStatusResponseModel
      .itsaStatusDetails
      .flatMap(statusDetail => statusDetail.headOption.map(detail => detail.status))
  }

  def hasMandatedOrVoluntaryStatusCurrentYear(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val yearEnd = dateService.getCurrentTaxYearEnd
    val taxYear = TaxYear(yearEnd)

    getITSAStatusDetail(taxYear, futureYears = false, history = false)
      .map(statusDetail =>
        statusDetail.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))))
  }


  def getStatusTillAvailableFutureYears(taxYear: TaxYear)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] = {
    getITSAStatusDetail(taxYear, futureYears = true, history = false).map {
      _.map(item => TaxYear(item.taxYear.split("-")(0).toInt + 1) -> getStatus(item)).flatMap {
        case (taxYear, Some(status)) => Some(taxYear -> status)
        case _ => None
      }.toMap
    }
  }

}

