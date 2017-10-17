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

import java.io
import javax.inject.{Inject, Singleton}

import connectors.{BusinessDetailsConnector, BusinessReportDeadlinesConnector, PropertyDetailsConnector, PropertyReportDeadlineDataConnector}
import models._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsService @Inject()(val businessDetailsConnector: BusinessDetailsConnector,
                                           val propertyDetailsConnector: PropertyDetailsConnector,
                                           val reportDeadlinesService: ReportDeadlinesService) {

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
      case (x, y) => createIncomeSourcesModel(nino, x, y)
    }
  }

  private def createIncomeSourcesModel(nino: String,
                                       business: BusinessListResponseModel,
                                       property: PropertyDetailsResponseModel
                                      )(implicit hc: HeaderCarrier) = {
    val businessIncomeModelListF: Future[Option[List[BusinessIncomeModel]]] = business match {
      case x: BusinessDetailsModel =>
       Future.sequence(
          for {
            seTrade <- x.business
            obs <- reportDeadlinesService.getBusinessReportDeadlines(nino, seTrade.id)
          } yield {
            BusinessIncomeModel(seTrade.id, seTrade.tradingName, seTrade.cessationDate, seTrade.accountingPeriod, obligations = Some(obs))
          }
       )
      case _ => Future.successful(None)
    }

    val propertyIncomeModelF: Future[Option[PropertyIncomeModel]] = property match {
      case x: PropertyDetailsModel =>
        for {
          obs <- reportDeadlinesService.getPropertyReportDeadlines(nino)
        } yield {
          Some(PropertyIncomeModel(x.accountingPeriod, obligations = Some(obs)))
        }
      case _ => Future.successful(None)
    }

    for {
      businessList <- businessIncomeModelListF
      property <- propertyIncomeModelF
    } yield {
      IncomeSourcesModel(List(businessList ++ property).flatten.flatten)
    }
  }
}
