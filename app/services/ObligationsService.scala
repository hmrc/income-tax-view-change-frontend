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

import connectors.{BusinessReportDeadlinesConnector, PropertyReportDeadlineDataConnector}
import models._
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ObligationsService @Inject()(val businessObligationDataConnector: BusinessReportDeadlinesConnector,
                                   val propertyObligationDataConnector: PropertyReportDeadlineDataConnector
                                  ) {

  def getBusinessObligations(nino: String,
                             businessIncomeSource: Option[BusinessIncomeModel]
                            )(implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    businessIncomeSource match {
      case Some(incomeSource) =>
        Logger.debug(s"[ObligationsService][getBusinessObligations] - Requesting Business Obligation details from connector for user with NINO: $nino")
        businessObligationDataConnector.getBusinessObligationData(nino, incomeSource.selfEmploymentId)
      case _ => Future.successful(NoObligations)
    }
  }

  def getPropertyObligations(nino: String, propertyIncomeSource: Option[PropertyIncomeModel])(implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    propertyIncomeSource match {
      case Some(_) =>
        Logger.debug (s"[ObligationsService][getPropertyObligations] - Requesting Property Obligation details from connectors for user with NINO: $nino")
        propertyObligationDataConnector.getPropertyObligationData (nino)
      case _ => Future.successful(NoObligations)
    }
  }
}
