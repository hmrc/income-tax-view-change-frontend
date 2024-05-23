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
import connectors.optout.ITSAStatusUpdateConnector
import connectors.optout.OptOutUpdateRequestModel.{ErrorItem, OptOutUpdateResponse, OptOutUpdateResponseFailure, optOutUpdateReason}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Mandated}
import models.itsaStatus.StatusDetail
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutMultiYearViewModel, OptOutOneYearCheckpointViewModel, OptOutOneYearViewModel}
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

  sealed trait OptOutCheckpointViewModel

  case class OptOutOneYearCheckpointViewModel(taxYear: TaxYear, showFutureChangeInfo: Boolean) extends OptOutCheckpointViewModel

  case class OptOutMultiYearViewModel(intent: Option[TaxYear]) extends OptOutCheckpointViewModel

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

  private def optOutViewModel[T](function: OptOutProposition => Option[T])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]] = {
    setupOptOutProposition()
      .map(optOutData => function(optOutData))
      .recover({
        case e =>
          Logger("application").error(s"trying to get opt-out status but failed with message: ${e.getMessage}")
          None
      })
  }

  private def optOutOneYearViewModel[T](function: (OptOutProposition, OptOutTaxYear) => T)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]] = {
    setupOptOutProposition()
      .map(optOutData => optOutData.optOutForSingleYear(function))
      .recover({
        case e =>
          Logger("application").error(s"trying to get opt-out status but failed with message: ${e.getMessage}")
          None
      })
  }

  def nextUpdatesPageOneYearOptOutViewModel()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutOneYearViewModel]] = {
    optOutOneYearViewModel((optOutData, optOutYear) => {
      val showWarning = optOutData match {
        case OptOutProposition(previousTaxYear, currentTaxYear, _) if previousTaxYear == optOutYear && currentTaxYear.status == Mandated => true
        case OptOutProposition(_, currentTaxYear, nextTaxYear) if currentTaxYear == optOutYear && nextTaxYear.status == Mandated => true
        case _ => false
      }
      OptOutOneYearViewModel(optOutYear.taxYear, showWarning)
    })
  }

  def optOutCheckPointPageViewModel(intent: Option[TaxYear])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutCheckpointViewModel]] = {
    optOutViewModel {
      optOutData =>
        if (optOutData.isOneYearOptOut) {
          Some {
            val optOutYear = optOutData.availableOptOutYears.head
            val showFutureChangeInfo = optOutData match {
              case OptOutProposition(previousTaxYear, currentTaxYear, _) if previousTaxYear == optOutYear && currentTaxYear.status == Annual => true
              case OptOutProposition(_, currentTaxYear, nextTaxYear) if currentTaxYear == optOutYear && nextTaxYear.status == Annual => true
              case OptOutProposition(_, _, nextTaxYear) if nextTaxYear == optOutYear => true
              case _ => false
            }
            OptOutOneYearCheckpointViewModel(optOutYear.taxYear, showFutureChangeInfo)
          }
        } else
          Some {
            logAndReturn(OptOutMultiYearViewModel(intent))
          }
    }
  }

  //todo remove after testing
  def logAndReturn[T](viewModel: T): T = {
    Logger("application").info(s"Returning view model in multiYear: $viewModel")
    viewModel
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

  def makeOptOutUpdateRequest(taxPayerIntent: Option[TaxYear] = None)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    setupOptOutProposition().flatMap { proposition =>
      if (proposition.isOneYearOptOut)
        makeOptOutUpdateRequestForOneYear(proposition)

      else {
        taxPayerIntent.flatMap(intent => proposition.optOutTaxYearFor(intent))
          .map(optOutTaxYearIntent => makeOptOutUpdateRequest(proposition, optOutTaxYearIntent))
          .getOrElse(Future.successful(OptOutUpdateResponseFailure.defaultFailure()))
      }
    }
  }

  private def makeOptOutUpdateRequestForOneYear(optOutProposition: OptOutProposition)(implicit
                                                                                      user: MtdItUser[_],
                                                                                      shc: HeaderCarrier,
                                                                                      ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    val intent = optOutProposition.availableOptOutYears.head
    makeOptOutUpdateRequest(optOutProposition, intent)
  }

  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition, intent: OptOutTaxYear)(implicit user: MtdItUser[_],
                                                                                           shc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    val optOutYearsToUpdate = optOutProposition.optOutYearsToUpdate(intent)

    val responses: Seq[Future[OptOutUpdateResponse]] = optOutYearsToUpdate.map(optOutYear =>
      itsaStatusUpdateConnector.requestOptOutForTaxYear(optOutYear.taxYear, user.nino, optOutUpdateReason))
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
    Future.sequence(items).map { responses => responses.find(_.isInstanceOf[OptOutUpdateResponseFailure]).getOrElse(responses.head) }
  }
}