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
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeSourceDetailsConnector: IncomeSourceDetailsConnector,
                                           val reportDeadlinesService: ReportDeadlinesService,
                                           val auditingService: AuditingService
                                          ) {

  def getIncomeSourceDetails(mtditid: String, nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesWithDeadlinesResponse] = {
    for {
      sources <- incomeSourceDetailsConnector.getIncomeSources(mtditid, nino)
      incomeSources <- createIncomeSourcesModel(nino, sources)
    } yield incomeSources
  }

  def createIncomeSourcesModel(nino: String, incomeSourceResponse: IncomeSourceDetailsResponse)
                              (implicit hc: HeaderCarrier): Future[IncomeSourcesWithDeadlinesResponse] = {
    incomeSourceResponse match {
      case sources: IncomeSourceDetailsModel =>
        val businessIncomeModelFList: Future[List[BusinessIncomeWithDeadlinesModel]] =
          Future.sequence(sources.businesses.map { seTrade =>
            reportDeadlinesService.getReportDeadlines(seTrade.incomeSourceId).map { obs =>
              BusinessIncomeWithDeadlinesModel(
                seTrade,
                obs
              )
            }
          })

        val propertyIncomeModelFOpt: Future[Option[PropertyIncomeWithDeadlinesModel]] =
          Future.sequence(Option.option2Iterable(sources.property.map { propertyIncome =>
            reportDeadlinesService.getReportDeadlines(propertyIncome.incomeSourceId).map { obs =>
              PropertyIncomeWithDeadlinesModel(propertyIncome, obs)
            }
          })).map(_.headOption)

        for {
          businessList <- businessIncomeModelFList
          property <- propertyIncomeModelFOpt
        } yield {
          IncomeSourcesWithDeadlinesModel(businessList, property)
        }
      case _: IncomeSourceDetailsError => Future.successful(IncomeSourcesWithDeadlinesError)
    }
  }
}
