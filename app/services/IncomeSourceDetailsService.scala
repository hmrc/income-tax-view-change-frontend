/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.{BusinessDetailsConnector, PropertyDetailsConnector}
import models._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsService @Inject()(val businessDetailsConnector: BusinessDetailsConnector) {

  def getBusinessDetails(nino: String)(implicit hc: HeaderCarrier): Future[BusinessListResponseModel] = {
    Logger.debug(s"[IncomeSourceDetailsService][getBusinessDetails] - Requesting Business Details from connector for user with NINO: $nino")
    businessDetailsConnector.getBusinessList(nino).flatMap {
      case success: BusinessDetailsModel =>
        Future.successful(success)
      case error: BusinessDetailsErrorModel =>
        Logger.debug(s"[IncomeSourceDetailsService][getBusinessDetails] - Error Response Status: ${error.code}, Message: ${error.message}")
        Future.successful(error)
    }
  }

  def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSources] = ???
}
