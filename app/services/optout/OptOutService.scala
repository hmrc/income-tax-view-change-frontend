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
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optout._
import play.mvc.Http
import services.optout.OptOutService._
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutService @Inject()(itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusService: ITSAStatusService,
                              calculationListService: CalculationListService,
                              nextUpdatesService: NextUpdatesService,
                              dateService: DateServiceInterface) {

  //TODO: Remove the default intent when Multi year OptOut intent is implemented
  private val defaultOptOutMultiYearIntent = Some(CurrentOptOutTaxYear(ITSAStatus.Voluntary, TaxYear.forYearEnd(2023)))

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

  private def makeOptOutUpdateRequestForOneYear(optOutProposition: OptOutProposition)(implicit
                                                                                      user: MtdItUser[_],
                                                                                      shc: HeaderCarrier,
                                                                                      ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    val intent = optOutProposition.availableOptOutYears.head
    makeOptOutUpdateRequest(optOutProposition, intent)
  }

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


  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition, intent: OptOutTaxYear)(implicit user: MtdItUser[_],
                                                                                           hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {


    val optOutYearsToUpdate = optOutProposition.optOutYearsToUpdate(intent)

    val responses: Seq[Future[OptOutUpdateResponse]] = optOutYearsToUpdate.map(optOutYear =>
      itsaStatusUpdateConnector.requestOptOutForTaxYear(optOutYear.taxYear, user.nino, optOutUpdateReason))
    combineByReturningAnyFailureFirstOrAnySuccess(responses)
  }


  def nextUpdatesPageOptOutViewModel()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutViewModel]] = {
    setupOptOutProposition().map { proposition =>
      proposition.optOutPropositionType.flatMap {
        case p: OneYearOptOutProposition => Some(OptOutOneYearViewModel(oneYearOptOutTaxYear = p.intent.taxYear, state = p.state()))
        case _: MultiYearOptOutProposition => Some(OptOutMultiYearViewModel())
        case _ => None
      }
    }
  }

  //TODO: Remove the default intent when Multi year OptOut intent is implemented
  def optOutCheckPointPageViewModel(intent: Option[OptOutTaxYear] = defaultOptOutMultiYearIntent)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutCheckpointViewModel]] = {
    setupOptOutProposition().map { proposition =>
      proposition.optOutPropositionType.flatMap {
        case p: OneYearOptOutProposition => Some(OptOutCheckpointViewModel(optOutTaxYear = p.intent, state = p.state()))
        case p: MultiYearOptOutProposition => intent.map(i => OptOutCheckpointViewModel(optOutTaxYear = i, state = p.state()))
      }
    }
  }

  //TODO: Remove the default intent when Multi year OptOut intent is implemented
  def optOutConfirmedPageViewModel(intent: Option[OptOutTaxYear] = defaultOptOutMultiYearIntent)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ConfirmedOptOutViewModel]] = {
    setupOptOutProposition().map { proposition =>
      proposition.optOutPropositionType.flatMap {
        case p: OneYearOptOutProposition => Some(ConfirmedOptOutViewModel(optOutTaxYear = p.intent, state = p.state()))
        case p: MultiYearOptOutProposition => intent.map(i => ConfirmedOptOutViewModel(optOutTaxYear = i, state = p.state()))
      }
    }
  }


  def getAvailableOptOutYear()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] = {
    setupOptOutProposition().map(proposition => proposition.availableTaxYearsForOptOut)
  }

  def getSubmissionCountForTaxYear(offeredOptOutYears: Seq[TaxYear])
                                  (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Map[Int, Int]] = {
    val futureCounts: Seq[Future[(Int, Int)]] = offeredOptOutYears.map { optOutTaxYear =>
      nextUpdatesService.getSubmissionCounts(optOutTaxYear)
    }

    Future.sequence(futureCounts).map(_.toMap)
  }

}


object OptOutService {

  private def combineByReturningAnyFailureFirstOrAnySuccess(responses: Seq[Future[OptOutUpdateResponse]])(implicit ec: ExecutionContext): Future[OptOutUpdateResponse] = {
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