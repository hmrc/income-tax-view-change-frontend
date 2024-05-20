/*
 * Copyright 2024 HM Revenue & Customs
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

package services.optout

import auth.MtdItUser
import connectors.ITSAStatusUpdateConnector
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Mandated
import models.itsaStatus.StatusDetail
import models.optOut.OptOutUpdateRequestModel.{ErrorItem, OptOutUpdateResponse, OptOutUpdateResponseFailure, itsaOptOutUpdateReason}
import models.optOut.{NextUpdatesQuarterlyReportingContentChecks, OptOutOneYearViewModel}
import play.api.Logger
import play.mvc.Http
import services.optout.OptOutService.combineByReturningAnyFailureFirstOrAnySuccess
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutService @Inject()(itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusService: ITSAStatusService,
                              calculationListService: CalculationListService,
                              dateService: DateServiceInterface) {

  def getNextUpdatesQuarterlyReportingContentChecks(implicit user: MtdItUser[_],
                                                    hc: HeaderCarrier,
                                                    ec: ExecutionContext): Future[NextUpdatesQuarterlyReportingContentChecks] = {
    val yearEnd = dateService.getCurrentTaxYearEnd
    val currentYear = TaxYear.forYearEnd(yearEnd)
    val previousYear = currentYear.previousYear

    val taxYearITSAStatus: Future[Map[TaxYear, StatusDetail]] = itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    val previousYearCalcStatus: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {
      statusMap <- taxYearITSAStatus
      isCurrentYearStatusMandatoryOrVoluntary = statusMap(currentYear).isMandatedOrVoluntary
      isPreviousYearStatusMandatoryOrVoluntary = statusMap(previousYear).isMandatedOrVoluntary
      calStatus <- previousYearCalcStatus
      optOutChecks = NextUpdatesQuarterlyReportingContentChecks(
        isCurrentYearStatusMandatoryOrVoluntary,
        isPreviousYearStatusMandatoryOrVoluntary,
        calStatus)
    } yield optOutChecks
  }

  def nextUpdatesPageOneYearOptOutViewModel()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutOneYearViewModel]] = {
    setupOptOutProposition()
      .map(optOutData => optOutData.optOutForSingleYear((optOutData, optOutYear) => {
        val showWarning = optOutData match {
          case OptOutProposition(previousTaxYear, currentTaxYear, _) if previousTaxYear == optOutYear && currentTaxYear.status == Mandated => true
          case OptOutProposition(_, currentTaxYear, nextTaxYear) if currentTaxYear == optOutYear && nextTaxYear.status == Mandated => true
          case _ => false
        }
        OptOutOneYearViewModel(optOutYear.taxYear, showWarning)
      }))
      .recover({
        case e =>
          Logger("application").error(s"trying to get opt-out status but failed with message: ${e.getMessage}")
          None
      })
  }

  private def setupOptOutProposition()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutProposition] = {

    val currentYear = dateService.getCurrentTaxYear
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    for {
      finalisedStatus <- calculationListService.isTaxYearCrystallised(previousYear)
      statusMap <- itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    }
    yield createOptOutProposition(previousYear, currentYear, nextYear, finalisedStatus, statusMap)
  }

  private def createOptOutProposition(previousYear: TaxYear,
                               currentYear: TaxYear,
                               nextYear: TaxYear,
                               finalisedStatus: Boolean,
                               statusMap: Map[TaxYear, StatusDetail]): OptOutProposition = {

    val previousYearOptOut = PreviousOptOutTaxYear(statusMap(previousYear).status, previousYear, finalisedStatus)
    val currentTaxYearOptOut = CurrentOptOutTaxYear(statusMap(currentYear).status, currentYear)
    val nextTaxYearOptOut = NextOptOutTaxYear(statusMap(nextYear).status, nextYear, currentTaxYearOptOut)

    OptOutProposition(previousYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)
  }

  def makeOptOutUpdateRequest(intent: Option[TaxYear] = None)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    setupOptOutProposition().flatMap { proposition =>
      if(proposition.isOneYearOptOut)
        makeOptOutUpdateRequestForOneYear(proposition)
      else {
        intent.map(ty => makeOptOutUpdateRequestFor(proposition, proposition.optOutTaxYearFor(ty)))
          .getOrElse(Future.successful(OptOutUpdateResponseFailure.defaultFailure()))
      }
    }
  }

  def makeOptOutUpdateRequestForOneYear(optOutProposition: OptOutProposition)(implicit
                                                                              user: MtdItUser[_],
                                                                              shc: HeaderCarrier,
                                                                              ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    val intent = optOutProposition.availableOptOutYears.head
    makeOptOutUpdateRequestFor(optOutProposition, intent)
  }

  def makeOptOutUpdateRequestFor(optOutProposition: OptOutProposition, intent: OptOutTaxYear)(implicit user: MtdItUser[_],
                                                                                              shc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    val optOutYearsToUpdate = optOutProposition.optOutYearsToUpdate(intent)

    val responses: Seq[Future[OptOutUpdateResponse]] = optOutYearsToUpdate.map(optOutYear =>
      itsaStatusUpdateConnector.requestOptOutForTaxYear(optOutYear.taxYear, user.nino, itsaOptOutUpdateReason))
    combineByReturningAnyFailureFirstOrAnySuccess(responses)
  }
}

object OptOutService {
  def combineByReturningAnyFailureFirstOrAnySuccess(responses: Seq[Future[OptOutUpdateResponse]])(implicit ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    Some(responses)
      .filter(isItsaStatusUpdateAttempted)
      .map(reduceByReturningAnyFailureFirstOrAnySuccess)
      .getOrElse(Future.successful(OptOutUpdateResponseFailure("default", Http.Status.INTERNAL_SERVER_ERROR,
        List(ErrorItem("Illegal State", "ITSA Status updated requested but no API call was made"))
      )))
  }

  private val isItsaStatusUpdateAttempted: Seq[Future[OptOutUpdateResponse]] => Boolean = items => items.nonEmpty

  private def reduceByReturningAnyFailureFirstOrAnySuccess(items: Seq[Future[OptOutUpdateResponse]])(implicit ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    items.reduce((f1, f2) => f1.flatMap(r1 => f2.map {
      case r2 if r2.isInstanceOf[OptOutUpdateResponseFailure] => r2
      case _ if r1.isInstanceOf[OptOutUpdateResponseFailure] => r1
      case r => r
    }))
  }
}