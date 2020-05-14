/*
 * Copyright 2020 HM Revenue & Customs
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

import auth.MtdItUser
import connectors._
import javax.inject.{Inject, Singleton}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import models.incomeSourcesWithDeadlines.{IncomeSourcesWithDeadlinesError, _}
import models.reportDeadlines._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector) {

  def getNextDeadlineDueDate(incomeSourceResponse: IncomeSourceDetailsModel)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdItUser: MtdItUser[_]): Future[LocalDate] = {
    getReportDeadlines().map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        deadlines.obligations.flatMap(_.obligations.map(_.due)).sortWith(_ isBefore _).head
      case error: ReportDeadlinesErrorModel => throw new Exception(s"${error.message}")
      case _ =>
        Logger.error("Unexpected Exception getting next deadline due")
        throw new Exception(s"Unexpected Exception getting next deadline due")
    }
  }

  def previousObligationsWithIncomeType(incomeSourceResponse: IncomeSourceDetailsModel)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext,
                                        mtdItUser: MtdItUser[_]): Future[List[ReportDeadlineModelWithIncomeType]] = {
    createIncomeSourcesWithDeadlinesModel(incomeSourceResponse, previousDeadlines = true) map {
      case allDeadlines: IncomeSourcesWithDeadlinesModel =>
        val previousBusinessDeadlines = allDeadlines.businessIncomeSources.flatMap{
          singleBusinessDeadlines => singleBusinessDeadlines.reportDeadlines.obligations.map {
            deadline => ReportDeadlineModelWithIncomeType(singleBusinessDeadlines.incomeSource.tradingName.getOrElse("Business"), deadline)
          }
        }
        val previousPropertyDeadlines = allDeadlines.propertyIncomeSource.map(propertyDeadlines => propertyDeadlines.reportDeadlines.obligations map {
          deadline => ReportDeadlineModelWithIncomeType("Property", deadline)
        })
        val previousCrystallisedDeadlines = allDeadlines.crystallisedDeadlinesModel.map {
          crystallisedDeadlines => crystallisedDeadlines.reportDeadlines.obligations map {
            deadline => ReportDeadlineModelWithIncomeType("Crystallised", deadline)
          }
        }
        (previousBusinessDeadlines ++ previousPropertyDeadlines.toList.flatten ++ previousCrystallisedDeadlines.toList.flatten)
          .sortBy(_.obligation.dateReceived.map(_.toEpochDay)).reverse
      case _ => List()
    }
  }

  def getReportDeadlines(previous: Boolean = false)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ReportDeadlinesResponseModel] = {
    if (previous) {
      Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting previous Report Deadlines for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getPreviousObligations()
    } else {
      Logger.debug(s"[ReportDeadlinesService][getReportDeadlines] - Requesting current Report Deadlines for nino: ${mtdUser.nino}")
      incomeTaxViewChangeConnector.getReportDeadlines()
    }
  }

  def createIncomeSourcesWithDeadlinesModel(incomeSourceResponse: IncomeSourceDetailsResponse, previousDeadlines: Boolean = false)
                                           (implicit hc: HeaderCarrier, ec: ExecutionContext, mtdUser: MtdItUser[_])
  : Future[IncomeSourcesWithDeadlinesResponse] = {
    incomeSourceResponse match {
      case sources: IncomeSourceDetailsModel =>
        getReportDeadlines(previousDeadlines).map {
          case obligations: ObligationsModel =>
            val propertyIncomeDeadlines: Option[PropertyIncomeWithDeadlinesModel] = {
              sources.property.flatMap { property =>
                obligations.obligations.find(_.identification == property.incomeSourceId) map { deadline =>
                  PropertyIncomeWithDeadlinesModel(property, deadline)
                }
              }
            }
            val businessIncomeDeadlines:  List[BusinessIncomeWithDeadlinesModel]  = {
              sources.businesses.flatMap { business =>
                val deadlines = obligations.obligations.find(_.identification == business.incomeSourceId)
                deadlines.map(deadline => BusinessIncomeWithDeadlinesModel(business, deadline))
              }
            }
            val crystallisedDeadlines: Option[CrystallisedDeadlinesModel] = {
                obligations.obligations.find(_.identification == mtdUser.mtditid) map { deadline =>
                  CrystallisedDeadlinesModel(deadline)
                }
            }

            IncomeSourcesWithDeadlinesModel(businessIncomeDeadlines, propertyIncomeDeadlines, crystallisedDeadlines)
          case _ => IncomeSourcesWithDeadlinesError
        }
      case _: IncomeSourceDetailsError => Future.successful(IncomeSourcesWithDeadlinesError)
    }
  }
}
