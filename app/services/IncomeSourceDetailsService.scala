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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class IncomeSourceDetailsService @Inject()( val businessDetailsConnector: BusinessDetailsConnector,
                                            val propertyDetailsConnector: PropertyDetailsConnector) {

  def getIncomeSourceDetails(nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesResponseModel] = {
    for {
      businessDetails <- businessDetailsConnector.getBusinessList(nino)
      propertyDetails <- propertyDetailsConnector.getPropertyDetails(nino)
    } yield (businessDetails, propertyDetails) match {
      case (x: BusinessDetailsErrorModel, _) =>
        Logger.debug(s"[IncomeSourceDetailsService][getIncomeSourceDetails] Error Model from BusinessDetailsConnector: $x")
        IncomeSourcesError
      case (_, x:PropertyDetailsErrorModel) =>
        Logger.debug(s"[IncomeSourceDetailsService][getIncomeSourceDetails] Error Model from PropertyDetailsConnector: $x")
        IncomeSourcesError
      case (x, y) => createIncomeSourcesModel(x, y)
    }
  }

  private def createIncomeSourcesModel(business: BusinessListResponseModel, property: PropertyDetailsResponseModel) = {
    val businessModel = business match {
      case x: BusinessDetailsModel =>
        // TODO: In future, this will need to cater for multiple SE Trade (business) income sources
        val seTrade = x.business.head
        Some(BusinessIncomeModel(seTrade.id, seTrade.accountingPeriod, seTrade.tradingName))
      case _ => None
    }
    val propertyModel = property match {
      case x: PropertyDetailsModel => Some(PropertyIncomeModel(x.accountingPeriod))
      case _ => None
    }
    IncomeSourcesModel(businessModel, propertyModel)
  }
}
