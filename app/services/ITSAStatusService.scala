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
import connectors.IncomeTaxViewChangeConnector
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                  dateService: DateService,
                                  implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def hasMandatedOrVoluntaryStatusCurrentYear(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val yearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)).toString.substring(2).toInt
    val yearStart = yearEnd - 1

    incomeTaxViewChangeConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = s"$yearStart-$yearEnd",
      futureYears = false,
      history = false).flatMap {
      case Right(itsaStatus) =>
        val isMandatedOrVoluntary = itsaStatus.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)))
        Future.successful(isMandatedOrVoluntary)
      case Left(x: ITSAStatusResponse) =>
        Logger("application").error(s"[ITSAStatusService][hasEligibleITSAStatusCurrentYear] $x")
        Future.failed(new InternalServerException("[ITSAStatusService][hasEligibleITSAStatusCurrentYear] - Failed to retrieve ITSAStatus"))
    }
  }

}

