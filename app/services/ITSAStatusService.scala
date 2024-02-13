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
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import connectors.ITSAStatusConnector
import models.core.Nino
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                  dateService: DateService,
                                  implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def hasMandatedOrVoluntaryStatusCurrentYear(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val yearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)).toString.substring(2).toInt
    val yearStart = yearEnd - 1

    itsaStatusConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = s"$yearStart-$yearEnd",
      futureYears = false,
      history = false).flatMap {
      case Right(itsaStatus: List[ITSAStatusResponseModel]) =>
        val isMandatedOrVoluntary = itsaStatus.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)))
        Future.successful(isMandatedOrVoluntary)
      case Left(error) =>
        Logger("application").error(s"[ITSAStatusService][hasMandatedOrVoluntaryStatusCurrentYear] $error")
        Future.failed(new Exception("[ITSAStatusService][hasMandatedOrVoluntaryStatusCurrentYear] - Failed to retrieve ITSAStatus"))
    }
  }

  def overwriteItsaStatus(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Either[Throwable, Result]] = {
    Logger("application").debug("[ITSAStatusService][overwriteItsaStatus] - " +
      s"Overwriting ITSA Status (1878) data via the dynamic stub with nino / taxYearRange: ${nino.value} - $taxYearRange")
    itsaStatusConnector.overwriteItsaStatus(nino, taxYearRange, crystallisationStatus)
  }

}

