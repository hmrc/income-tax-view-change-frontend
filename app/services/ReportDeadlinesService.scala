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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import auth.MtdItUser
import connectors._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines._
import models.reportDeadlines.{ReportDeadlineModel, ReportDeadlinesErrorModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector) {

  def getNextDeadlineDueDate(incomeSourceResponse: IncomeSourceDetailsModel)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[LocalDate] = {
    getAllDeadlines(incomeSourceResponse).map { reportDeadlines =>
      reportDeadlines.map(_.due).sortWith(_ isBefore _).head
    }
  }

  def getAllDeadlines(incomeSourceResponse: IncomeSourceDetailsModel)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[List[ReportDeadlineModel]] = {

    val allRetrievalIds: List[String] = incomeSourceResponse.businesses.map(_.incomeSourceId) ++
      incomeSourceResponse.property.map(_.incomeSourceId) :+
      mtdItUser.nino

    Future.sequence(
      allRetrievalIds.map { incomeSource =>
        getReportDeadlines(incomeSource).map {
          case ReportDeadlinesModel(obligations) => obligations
          case _: ReportDeadlinesErrorModel => Nil
        }
      }
    ).map(_.flatten)
  }

  def getReportDeadlines(incomeSourceId: String)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {
    Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting Report Deadlines for incomeSourceID: $incomeSourceId")
    incomeTaxViewChangeConnector.getReportDeadlines(incomeSourceId)
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


        val crystallisedModelFList: Future[Option[CrystallisedDeadlinesModel]] =
          getReportDeadlines(mtdUser.nino).map{
            case deadlines: ReportDeadlinesModel => Some(CrystallisedDeadlinesModel(deadlines))
            case deadlines: ReportDeadlinesErrorModel => None
          }

        for {
          businessList <- businessIncomeModelFList
          property <- propertyIncomeModelFOpt
          crystallised <- crystallisedModelFList
        } yield {
          IncomeSourcesWithDeadlinesModel(businessList, property, crystallised)
        }
      case _: IncomeSourceDetailsError => Future.successful(IncomeSourcesWithDeadlinesError)
    }
  }
}
