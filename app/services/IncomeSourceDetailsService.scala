/*
 * Copyright 2018 HM Revenue & Customs
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
        Future.successful(IncomeSourcesError)
      case (_, x:PropertyDetailsErrorModel) =>
        Logger.debug(s"[IncomeSourceDetailsService][getIncomeSourceDetails] Error Model from PropertyDetailsConnector: $x")
        Future.successful(IncomeSourcesError)
      case (x: BusinessDetailsModel, y) =>
        createIncomeSourcesModel(nino, x, y)
    }
  }.flatMap(z => z)

  def createIncomeSourcesModel(nino: String,
                               businessDetails: BusinessDetailsModel,
                               property: PropertyDetailsResponseModel
                              )(implicit hc: HeaderCarrier): Future[IncomeSourcesModel] = {

    val businessIncomeModelListF: Future[List[BusinessIncomeModel]] =
      Future.sequence(businessDetails.businesses.map { seTrade =>
        reportDeadlinesService.getBusinessReportDeadlines(nino, seTrade.id).map { obs =>
          BusinessIncomeModel(seTrade.id, seTrade.tradingName, seTrade.cessationDate, seTrade.accountingPeriod, obs)
        }
      })

    val propertyIncomeModelF: Future[Option[PropertyIncomeModel]] = property match {
      case x: PropertyDetailsModel =>
        for {
          obs <- reportDeadlinesService.getPropertyReportDeadlines(nino)
        } yield {
          Some(PropertyIncomeModel(x.accountingPeriod, obs))
        }
      case _ => Future.successful(None)
    }

    for {
      businessList <- businessIncomeModelListF
      property <- propertyIncomeModelF
    } yield {
      IncomeSourcesModel(businessList, property)
    }
  }

  def getBusinessDetails(nino: String, selfEmploymentId: String)(implicit hc:HeaderCarrier): Future[Either[BusinessDetailsErrorModel,Option[BusinessModel]]] = {
    for {
      businesses <- businessDetailsConnector.getBusinessList(nino)
    } yield businesses match {
      case bizDeets: BusinessDetailsModel => Right(bizDeets.businesses.find(_.id == selfEmploymentId))
      case error: BusinessDetailsErrorModel => Left(error)
    }
  }
}
