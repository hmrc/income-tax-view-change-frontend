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
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                  implicit val appConfig: FrontendAppConfig) extends FeatureSwitching with Logging {

  def getITSAStatusDetail(taxYear: TaxYear, futureYears: Boolean, history: Boolean)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext, user: MtdItUser[_]): Future[List[ITSAStatusResponseModel]] = {
    itsaStatusConnector.getITSAStatusDetail(
      nino = user.nino,
      taxYear = taxYear.formatAsShortYearRange,
      futureYears = futureYears,
      history = history).flatMap {
      case Right(itsaStatus) => Future.successful(itsaStatus)
      case Left(error) =>
        logger.error(s"[getITSAStatusDetail] $error")
        Future.failed(new Exception("Failed to retrieve ITSAStatus"))
    }
  }
}