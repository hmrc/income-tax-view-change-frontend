/*
 * Copyright 2019 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import connectors._
import javax.inject.{Inject, Singleton}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines._
import models.reportDeadlines.ReportDeadlinesResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesService @Inject()(val reportDeadlinesConnector: ReportDeadlinesConnector) {

  def getReportDeadlines(incomeSourceId: String)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {
    Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting Report Deadlines for incomeSourceID: $incomeSourceId")
    reportDeadlinesConnector.getReportDeadlines(incomeSourceId)
  }

  def createIncomeSourcesWithDeadlinesModel(incomeSourceResponse: IncomeSourceDetailsResponse)
                                           (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdUser: MtdItUser[_])
  : Future[IncomeSourcesWithDeadlinesResponse] = {
    incomeSourceResponse match {
      case sources: IncomeSourceDetailsModel =>
        val businessIncomeModelFList: Future[List[BusinessIncomeWithDeadlinesModel]] =
          Future.sequence(sources.businesses.map { seTrade =>
            getReportDeadlines(seTrade.incomeSourceId).map { obs =>
              BusinessIncomeWithDeadlinesModel(
                seTrade,
                obs
              )
            }
          })

        val propertyIncomeModelFOpt: Future[Option[PropertyIncomeWithDeadlinesModel]] =
          Future.sequence(Option.option2Iterable(sources.property.map { propertyIncome =>
            getReportDeadlines(propertyIncome.incomeSourceId).map { obs =>
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
