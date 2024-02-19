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

package testOnly.services

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.ITSAStatusConnector
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import services.DateServiceInterface
import testOnly.connectors.DynamicStubConnector
import testOnly.models.{ItsaStatus, Nino}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DynamicStubService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                   dynamicStubConnector: DynamicStubConnector,
                                   implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def overwriteCalculationList(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("[CalculationService][overwriteCalculationList] - " +
      s"Overwriting calculation list (1896) data via the dynamic stub with nino / taxYearRange: ${nino.value} - $taxYearRange")
    dynamicStubConnector.overwriteCalculationList(nino, taxYearRange, crystallisationStatus)
  }

  def getITSAStatusDetail(itsaStatus: ItsaStatus, nino: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext, dateService: DateServiceInterface): Future[ITSAStatusResponseModel] = {

    itsaStatusConnector.getITSAStatusDetail(
      nino = nino,
      taxYear = itsaStatus.taxYearRange,
      futureYears = false,
      history = false
    ) map {
      case Right(itsaStatus: List[ITSAStatusResponseModel]) =>
        itsaStatus.head
      case Left(error) =>
        Logger("application").error(s"[ITSAStatusService][getITSAStatusDetail] $error")
        throw new Exception("[ITSAStatusService][getITSAStatusDetail] - Failed to retrieve ITSAStatus")
    }
  }

  def overwriteItsaStatus(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                         (implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("[ITSAStatusService][overwriteItsaStatus] - " +
      s"Overwriting ITSA Status (1878) data via the dynamic stub with nino / taxYearRange: ${nino.value} - $taxYearRange")
    dynamicStubConnector.overwriteItsaStatus(nino, taxYearRange, crystallisationStatus)
  }

}
