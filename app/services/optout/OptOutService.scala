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
import cats.data.OptionT
import connectors.optout.ITSAStatusUpdateConnector
import connectors.optout.OptOutUpdateRequestModel.{OptOutUpdateResponse, OptOutUpdateResponseFailure, optOutUpdateReason}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optout._
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutService._
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.OptOutJourney

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutService @Inject()(itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusService: ITSAStatusService,
                              calculationListService: CalculationListService,
                              nextUpdatesService: NextUpdatesService,
                              dateService: DateServiceInterface,
                              repository: UIJourneySessionDataRepository) {

  def fetchOptOutProposition()(implicit user: MtdItUser[_],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): Future[OptOutProposition] = {

    val currentYear = dateService.getCurrentTaxYear
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    val finalisedStatusFuture: Future[Boolean] = calculationListService.isTaxYearCrystallised(previousYear)
    val statusMapFuture: Future[Map[TaxYear, ITSAStatus]] = getITSAStatusesFrom(previousYear)

    for {
      finalisedStatus <- finalisedStatusFuture
      statusMap <- statusMapFuture
    }
    yield createOptOutProposition(previousYear, currentYear, nextYear, finalisedStatus, statusMap)
  }

  private def createOptOutProposition(previousYear: TaxYear,
                                      currentYear: TaxYear,
                                      nextYear: TaxYear,
                                      finalisedStatus: Boolean,
                                      statusMap: Map[TaxYear, ITSAStatus]
                                     ): OptOutProposition = {

    val previousYearOptOut = PreviousOptOutTaxYear(
      status = statusMap(previousYear),
      taxYear = previousYear,
      crystallised = finalisedStatus
    )

    val currentYearOptOut = CurrentOptOutTaxYear(
      status = statusMap(currentYear),
      taxYear = currentYear
    )

    val nextYearOptOut = NextOptOutTaxYear(
      status = statusMap(nextYear),
      taxYear = nextYear,
      currentTaxYear = currentYearOptOut
    )

    OptOutProposition(previousYearOptOut, currentYearOptOut, nextYearOptOut)
  }

  def getNextUpdatesQuarterlyReportingContentChecks(implicit user: MtdItUser[_],
                                                    hc: HeaderCarrier,
                                                    ec: ExecutionContext): Future[NextUpdatesQuarterlyReportingContentChecks] = {
    val yearEnd = dateService.getCurrentTaxYearEnd
    val currentYear = TaxYear.forYearEnd(yearEnd)
    val previousYear = currentYear.previousYear

    val statusMapFuture: Future[Map[TaxYear, ITSAStatus]] = getITSAStatusesFrom(previousYear)
    val finalisedStatusFuture: Future[Boolean] = calculationListService.isTaxYearCrystallised(previousYear.endYear)

    for {
      statusMap <- statusMapFuture
      currentYearStatus = statusMap(currentYear)
      previousYearStatus = statusMap(previousYear)
      finalisedStatus <- finalisedStatusFuture
    } yield NextUpdatesQuarterlyReportingContentChecks(
      currentYearStatus == Mandated || currentYearStatus == Voluntary,
      previousYearStatus == Mandated || previousYearStatus == Voluntary,
      finalisedStatus)
  }

  private def getITSAStatusesFrom(previousYear: TaxYear)(implicit user: MtdItUser[_],
                                                         hc: HeaderCarrier,
                                                         ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] =
    itsaStatusService.getStatusTillAvailableFutureYears(previousYear).map(_.view.mapValues(_.status).toMap.withDefaultValue(ITSAStatus.NoStatus))

  def makeOptOutUpdateRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    fetchOptOutProposition().flatMap { proposition =>
      proposition.optOutPropositionType.map {
        case _: OneYearOptOutProposition =>
          makeOptOutUpdateRequest(proposition, Future.successful(proposition.availableTaxYearsForOptOut.headOption))
        case _: MultiYearOptOutProposition => makeOptOutUpdateRequest(proposition, fetchSavedIntent())
      } getOrElse Future.successful(OptOutUpdateResponseFailure.defaultFailure())
    }
  }

  def saveIntent(intent: TaxYear)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val data = UIJourneySessionData(
      sessionId = hc.sessionId.get.value,
      journeyType = OptOutJourney.Name,
      optOutSessionData = Some(OptOutSessionData(selectedOptOutYear = Some(intent.toString)))
    )
    repository.set(data)
  }

  def fetchSavedIntent()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TaxYear]] = {
    repository.get(hc.sessionId.get.value, OptOutJourney.Name) map { sessionData =>
      for {
        data <- sessionData
        optOutData <- data.optOutSessionData
        selected <- optOutData.selectedOptOutYear
        parsed <- TaxYear.getTaxYearModel(selected)
      } yield parsed
    }
  }

  def resetSavedIntent()(implicit hc: HeaderCarrier): Future[Boolean] = {
    val data = UIJourneySessionData(
      sessionId = hc.sessionId.get.value,
      journeyType = OptOutJourney.Name,
      optOutSessionData = Some(OptOutSessionData(selectedOptOutYear = None))
    )
    repository.set(data)
  }

  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition, intentFuture: Future[Option[TaxYear]])
                             (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    def makeUpdateCalls(optOutYearsToUpdate: Seq[TaxYear]): Seq[Future[OptOutUpdateResponse]] = {
      optOutYearsToUpdate.map(optOutYear => itsaStatusUpdateConnector.requestOptOutForTaxYear(optOutYear, user.nino, optOutUpdateReason))
    }

    def findAnyFailOrFirstSuccess(responses: Seq[OptOutUpdateResponse]): OptOutUpdateResponse = {
      responses.find {
          case _: OptOutUpdateResponseFailure => true
          case _ => false
      }.getOrElse(responses.head)
    }

    val result = for {
      intentTaxYear <- OptionT(intentFuture)
      yearsToUpdate = optOutProposition.optOutYearsToUpdate(intentTaxYear)
      responsesSeqOfFutures = makeUpdateCalls(yearsToUpdate)
      responsesSeq <- OptionT(Future.sequence(responsesSeqOfFutures).map(v => Option(v)))
    } yield findAnyFailOrFirstSuccess(responsesSeq)

    result.getOrElse(OptOutUpdateResponseFailure.defaultFailure())
  }

  def nextUpdatesPageOptOutViewModel()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutViewModel]] = {
    fetchOptOutProposition().map { proposition =>
      proposition.optOutPropositionType.flatMap {
        case p: OneYearOptOutProposition => Some(OptOutOneYearViewModel(oneYearOptOutTaxYear = p.intent.taxYear, state = p.state()))
        case _: MultiYearOptOutProposition => Some(OptOutMultiYearViewModel())
      }
    }
  }

  def optOutCheckPointPageViewModel()
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutCheckpointViewModel]] = {

    def processPropositionType(propositionType: OptOutPropositionTypes, intent: Option[TaxYear], quarterlyUpdatesCount: Int):
      Option[OptOutCheckpointViewModel] = propositionType match {
        case p: OneYearOptOutProposition => Some(OneYearOptOutCheckpointViewModel(intent = p.intent.taxYear, state = p.state(), Some(quarterlyUpdatesCount)))
        case _: MultiYearOptOutProposition => intent.map(i => MultiYearOptOutCheckpointViewModel(intent = i))
    }

    def getQuarterlyUpdatesCount(propositionType: Option[OptOutPropositionTypes]): Future[Int] = {
      propositionType match {
        case Some(p:OneYearOptOutProposition) =>
          getQuarterlyUpdatesCountForOfferedYears(p.proposition).map(_.getCountFor(p.intent.taxYear))
        case _ => Future.successful(noQuarterlyUpdates)
      }
    }

    for {
      proposition <- fetchOptOutProposition()
      intent <- fetchSavedIntent()
      propositionType = proposition.optOutPropositionType
      quarterlyUpdatesCount <- getQuarterlyUpdatesCount(propositionType)
    } yield processPropositionType(propositionType.get, intent, quarterlyUpdatesCount)
  }

  def optOutConfirmedPageViewModel()(implicit user: MtdItUser[_],
                                     hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ConfirmedOptOutViewModel]] = {
    fetchOptOutProposition().flatMap { proposition =>
        proposition.optOutPropositionType match {
          case Some(p: OneYearOptOutProposition) => Future.successful(Some(ConfirmedOptOutViewModel(optOutTaxYear = p.intent.taxYear, state = p.state())))
          case Some(p: MultiYearOptOutProposition) => fetchSavedIntent().map(_.map(taxYear => ConfirmedOptOutViewModel(taxYear, p.state())))
          case _ =>  Future.successful(None)
      }
    }
  }

  def getQuarterlyUpdatesCountForOfferedYears(proposition: OptOutProposition)
                                             (implicit user: MtdItUser[_],
                                              hc: HeaderCarrier,
                                              ec: ExecutionContext): Future[QuarterlyUpdatesCountForTaxYearModel] = {

    def cumulativeQuarterlyUpdateCounts(taxYearToCount: Seq[QuarterlyUpdatesCountForTaxYear]): Seq[QuarterlyUpdatesCountForTaxYear] =
      if (taxYearToCount.isEmpty)
        Seq()
      else
        Seq(cumulativeCount(taxYearToCount)) ++ cumulativeQuarterlyUpdateCounts(taxYearToCount.tail)

    def cumulativeCount(taxYearToCount: Seq[QuarterlyUpdatesCountForTaxYear]): QuarterlyUpdatesCountForTaxYear =
      QuarterlyUpdatesCountForTaxYear(taxYearToCount.head.taxYear, taxYearToCount.map(_.count).sum)

    val annualQuarterlyUpdateCounts = Future.sequence(
      proposition.availableOptOutYears.map {
        case next: NextOptOutTaxYear => Future.successful(QuarterlyUpdatesCountForTaxYear(next.taxYear, 0))
        case previousOrCurrent => nextUpdatesService.getQuarterlyUpdatesCounts(previousOrCurrent.taxYear)
      })

    annualQuarterlyUpdateCounts.
      map(cumulativeQuarterlyUpdateCounts).
      map(QuarterlyUpdatesCountForTaxYearModel)
  }

}


object OptOutService {

  private val noQuarterlyUpdates = 0

  case class QuarterlyUpdatesCountForTaxYearModel(counts: Seq[QuarterlyUpdatesCountForTaxYear]) {

    def getCountFor(offeredOptOutYear: TaxYear): Int = counts
      .filter(taxYearCounts => taxYearCounts.taxYear == offeredOptOutYear)
      .map(_.count).sum

    val isQuarterlyUpdatesMade: Boolean = counts.map(_.count).sum > noQuarterlyUpdates
  }


}