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

package hub.services

import common.auth.MtdItUser
import common.models.obligations.{ObligationsErrorModel, ObligationsModel, ObligationsResponseModel}
import common.services.DateServiceInterface
import play.api.Logger
import shared.connectors.ObligationsConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesService @Inject()(
                                    val obligationsConnector: ObligationsConnector
                                  )(implicit ec: ExecutionContext, val dateService: DateServiceInterface) {

  def getDueDates(openObligations: Option[Future[ObligationsResponseModel]] = None)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[Either[Exception, Seq[LocalDate]]] = {
    openObligations.getOrElse(getOpenObligations()).map {
      case deadlines: ObligationsModel if !deadlines.obligations.forall(_.obligations.isEmpty) =>
        Right(deadlines.obligations.flatMap(_.obligations.map(_.due)))
      case ObligationsModel(obligations) if obligations.isEmpty || obligations.forall(_.obligations.isEmpty) =>
        Right(Seq.empty)
      case error: ObligationsErrorModel =>
        Left(new Exception(s"${error.message}"))
      case _ =>
        Left(new Exception("Unexpected Exception getting next deadline due and Overdue Obligations"))
    }
  }

  def getOpenObligations()(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[ObligationsResponseModel] = {
    Logger("application").debug(s"Requesting current Next Updates for nino: ${mtdUser.nino}")
    obligationsConnector.getOpenObligations()
  }

  def getNextDueDates(openObligations: Option[Future[ObligationsResponseModel]] = None)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    openObligations.getOrElse(getOpenObligations()).map {
      case ObligationsModel(obligations) =>
        val openObligations = obligations.flatMap(_.obligations)

        val nextQuarterlyDueDate = openObligations
          .filter(_.obligationType == "Quarterly")
          .map(_.due)
          .filter(dueDate => !dueDate.isBefore(dateService.getCurrentDate))
          .sorted
          .headOption

        val nextCrystallisationDueDate = openObligations
          .filter(_.obligationType == "Crystallisation")
          .map(_.due)
          .filter(dueDate => !dueDate.isBefore(dateService.getCurrentDate))
          .sorted
          .headOption

        val fallbackNextTaxReturnDate: LocalDate = LocalDate.of(dateService.getCurrentTaxYear.endYear + 1, 1, 31)

        val nextTaxReturnDate: Option[LocalDate] = nextCrystallisationDueDate match {
          case Some(date) => Some(date)
          case None =>
            Logger("application").info("[getNextDueDates] No upcoming crystallisation obligation found - falling back to static next tax return due date")
            Some(fallbackNextTaxReturnDate)
        }

        (nextQuarterlyDueDate, nextTaxReturnDate)

      case error: ObligationsErrorModel =>
        Logger("application").warn(s"[getNextDueDates] Failed to fetch obligations: ${error.message}")
        (None, Some(LocalDate.of(dateService.getCurrentTaxYear.endYear + 1, 1, 31)))
    }
  }
}


