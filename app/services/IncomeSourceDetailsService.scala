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

import audit.AuditingService
import connectors.IncomeSourceDetailsConnector
import models.core.ErrorModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeSourceDetailsConnector: IncomeSourceDetailsConnector,
                                           val reportDeadlinesService: ReportDeadlinesService,
                                           val auditingService: AuditingService
                                          ) {


  def getBusinessDetails(mtditid: String, nino: String, id: Int)(implicit hc:HeaderCarrier): Future[Either[ErrorModel, Option[(BusinessDetailsModel, Int)]]] = {
    incomeSourceDetailsConnector.getIncomeSources(mtditid, nino).map {
      case sources: IncomeSourceDetailsModel => Right(sources.sortedBusinesses.find(_._2 == id))
      case error: IncomeSourceDetailsError => Left(ErrorModel(error.status, error.reason))
    }
  }

  def getIncomeSourceDetails(mtditid: String, nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesResponseModel] = {
    for {
      sources <- incomeSourceDetailsConnector.getIncomeSources(mtditid, nino)
      incomeSources <- createIncomeSourcesModel(nino, sources)
    } yield incomeSources
  }

  def createIncomeSourcesModel(nino: String, incomeSourceResponse: IncomeSourceDetailsResponse)
                              (implicit hc: HeaderCarrier): Future[IncomeSourcesResponseModel] = {
    incomeSourceResponse match {
      case sources: IncomeSourceDetailsModel =>
        val businessIncomeModelFList: Future[List[BusinessIncomeModel]] =
          Future.sequence(sources.businesses.map { seTrade =>
            reportDeadlinesService.getBusinessReportDeadlines(nino, seTrade.incomeSourceId).map { obs =>
              BusinessIncomeModel(
                seTrade.incomeSourceId,
                seTrade.tradingName.getOrElse("No Trading Name Found"), //TODO: What should this do if no Trading Name is supplied...?
                seTrade.cessation.flatMap(_.date),
                seTrade.accountingPeriod,
                obs
              )
            }
          })

        val propertyIncomeModelFOpt: Future[Option[PropertyIncomeModel]] =
          Future.sequence(Option.option2Iterable(sources.property.map { propertyIncome =>
            reportDeadlinesService.getPropertyReportDeadlines(nino).map { obs =>
              PropertyIncomeModel(propertyIncome.accountingPeriod, obs)
            }
          })).map(_.headOption)

        for {
          businessList <- businessIncomeModelFList
          property <- propertyIncomeModelFOpt
        } yield {
          IncomeSourcesModel(businessList, property)
        }
      case _: IncomeSourceDetailsError => Future.successful(IncomeSourcesError)
    }
  }
}
