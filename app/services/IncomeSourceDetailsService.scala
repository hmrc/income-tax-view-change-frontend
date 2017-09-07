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
class IncomeSourceDetailsService @Inject()( val businessDetailsConnector: BusinessDetailsConnector,
                                            val propertyDetailsConnector: PropertyDetailsConnector) {

  def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] = {
    for {
      businessDetails <- getBusinessDetails(nino).map {
        case businessList: BusinessDetailsModel => businessList.business.headOption match {
          case Some(business) => Some(BusinessIncomeModel(business.id, business.accountingPeriod, business.tradingName))
          case None => {
            Logger.debug("[IncomeSourceDetailsService][getIncomeSourceDetails] No Businesses retrieved from business details")
            None
          }
        }
        case _: BusinessDetailsErrorModel => None
      }
      propertyDetails <- getPropertyDetails(nino).map {
        case property: PropertyDetailsModel => Some(PropertyIncomeModel(property.accountingPeriod))
        case _: PropertyDetailsErrorModel => None
      }
    } yield IncomeSourcesModel(businessDetails, propertyDetails)
  }

  private[IncomeSourceDetailsService] def getBusinessDetails(nino: String)(implicit hc: HeaderCarrier): Future[BusinessListResponseModel] = {
    Logger.debug(s"[IncomeSourceDetailsService][getBusinessDetails] - Requesting Business Details from connector for user with NINO: $nino")
    businessDetailsConnector.getBusinessList(nino).flatMap {
      case success: BusinessDetailsModel => Future.successful(success)
      case error: BusinessDetailsErrorModel =>
        Logger.debug(s"[IncomeSourceDetailsService][getBusinessDetails] - Error Response Status: ${error.code}, Message: ${error.message}")
        Future.successful(error)
    }
  }

  private[IncomeSourceDetailsService]def getPropertyDetails(nino: String)(implicit hc: HeaderCarrier): Future[PropertyDetailsResponseModel] = {
    Logger.debug(s"[IncomeSourceDetailsService][getPropertyDetails] - Requesting Property Details from connector for user with NINO: $nino")
    propertyDetailsConnector.getPropertyDetails(nino).flatMap {
      case success: PropertyDetailsModel => Future.successful(success)
      case error: PropertyDetailsErrorModel =>
        Logger.debug(s"[IncomeSourceDetailsService][getPropertyDetails] - Error Response Status: ${error.code}, Message: ${error.message}")
        Future.successful(error)
    }
  }
}
