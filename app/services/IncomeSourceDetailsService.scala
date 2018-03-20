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

import connectors.IncomeSourceDetailsConnector
import models._
import models.core.ErrorModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines._
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IncomeSourceDetailsService @Inject()(val incomeSourceDetailsConnector: IncomeSourceDetailsConnector,
                                           val reportDeadlinesService: ReportDeadlinesService) {


  def getBusinessDetails(mtditid: String, id: Int)(implicit hc:HeaderCarrier): Future[Either[ErrorModel, Option[(BusinessDetailsModel, Int)]]] = {
    incomeSourceDetailsConnector.getIncomeSources(mtditid).map {
      case sources: IncomeSourceDetailsModel => Right(sources.sortedBusinesses.find(_._2 == id))
      case error: IncomeSourceDetailsError => Left(ErrorModel(error.status, error.reason))
    }
  }

  def getIncomeSourceDetails(mtditid: String, nino: String)(implicit hc: HeaderCarrier): Future[IncomeSourcesWithDeadlinesResponse] = {
    for {
      sources <- incomeSourceDetailsConnector.getIncomeSources(mtditid)
      incomeSources <- createIncomeSourcesModel(nino, sources)
    } yield incomeSources
  }

  def createIncomeSourcesModel(nino: String, incomeSourceResponse: IncomeSourceDetailsResponse)
                              (implicit hc: HeaderCarrier): Future[IncomeSourcesWithDeadlinesResponse] = {
    incomeSourceResponse match {
      case sources: IncomeSourceDetailsModel =>
        val businessIncomeModelFList: Future[List[BusinessIncomeWithDeadlinesModel]] =
          Future.sequence(sources.businesses.map { seTrade =>
            reportDeadlinesService.getBusinessReportDeadlines(nino, seTrade.incomeSourceId).map { obs =>
              BusinessIncomeWithDeadlinesModel(
                seTrade,
                obs
              )
            }
          })

        val propertyIncomeModelFOpt: Future[Option[PropertyIncomeWithDeadlinesModel]] =
          Future.sequence(Option.option2Iterable(sources.property.map { propertyIncome =>
            reportDeadlinesService.getPropertyReportDeadlines(nino).map { obs =>
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
