/*
 * Copyright 2023 HM Revenue & Customs
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

package businessDetails.services

import common.auth.MtdItUser
import common.models.core.IncomeSourceId.mkIncomeSourceId
import common.services.DateServiceInterface
import models.incomeSourceDetails.viewmodels.*
import play.api.Logger
import shared.connectors.ObligationsConnector
import shared.models.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesService @Inject()(
                                    val obligationsConnector: ObligationsConnector
                                  )(implicit val dateService: DateServiceInterface) {

  def getOpenObligations()(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {
    Logger("application").debug(s"Requesting current Next Updates for nino: ${mtdUser.nino}")
    obligationsConnector.getOpenObligations()
  }


  def getObligationDates(id: String)
                        (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[DatesModel]] = {
    getOpenObligations() map {
      case ObligationsErrorModel(code, message) =>
        Logger("application").error(
          s"Error: $message, code $code")
        Seq.empty
      case model: ObligationsModel =>
        Seq(model.obligations.flatMap(nextUpdatesModel => nextUpdatesModel.currentCrystDeadlines) map {
          source =>
            DatesModel(source.start, source.end, source.due, source.periodKey, isFinalDec = true, obligationType = source.obligationType)
        },
          model.obligations
            .filter(nextUpdatesModel => mkIncomeSourceId(nextUpdatesModel.identification) == mkIncomeSourceId(id))
            .flatMap(obligation => obligation.obligations.map(obligation =>
              DatesModel(obligation.start, obligation.end, obligation.due, obligation.periodKey, isFinalDec = false, obligationType = obligation.obligationType)))
        ).flatten
    }
  }

  def getObligationsViewModel(id: String, showPreviousTaxYears: Boolean)
                             (implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[ObligationsViewModel] = {
    val processingRes =
      for {
        datesList <- getObligationDates(id)
      } yield {
        val (finalDeclarationDates, otherObligationDates) = datesList.partition(datesModel => datesModel.isFinalDec)

        val quarterlyDates: Seq[DatesModel] =
          otherObligationDates
            .filter(datesModel => datesModel.obligationType == "Quarterly")
            .sortBy(_.inboundCorrespondenceFrom)

        val obligationsGroupedByYear =
          quarterlyDates
            .groupBy(datesModel => dateService.getAccountingPeriodEndDate(datesModel.inboundCorrespondenceTo).getYear)

        val sortedObligationsByYear =
          obligationsGroupedByYear
            .toSeq
            .sortBy(_._1)
            .map(_._2.distinct.sortBy(_.periodKey))

        val finalDecDates: Seq[DatesModel] = finalDeclarationDates.distinct.sortBy(_.inboundCorrespondenceFrom)

        ObligationsViewModel(sortedObligationsByYear, finalDecDates, dateService.getCurrentTaxYearEnd, showPrevTaxYears = showPreviousTaxYears)
      }

    processingRes
  }
}


