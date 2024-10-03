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
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import models.optout._
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutProposition.createOptOutProposition
import services.reportingfreq.ReportingFrequency.{QuarterlyUpdatesCountForTaxYearModel, noQuarterlyUpdates}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import audit.AuditingService
import audit.models.{CheckYourAnswersAuditModel, Outcome}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class OptOutService @Inject()(itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusService: ITSAStatusService,
                              calculationListService: CalculationListService,
                              nextUpdatesService: NextUpdatesService,
                              dateService: DateServiceInterface,
                              repository: OptOutSessionDataRepository,
                              auditingService: AuditingService) {

  def fetchOptOutProposition()(implicit user: MtdItUser[_],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): Future[OptOutProposition] = {

    val currentYear = dateService.getCurrentTaxYear

    val finalisedStatusFuture: Future[Boolean] = calculationListService.isTaxYearCrystallised(currentYear.previousYear)
    val statusMapFuture: Future[Map[TaxYear, ITSAStatus]] = getITSAStatusesFrom(currentYear.previousYear)

    for {
      finalisedStatus <- finalisedStatusFuture
      statusMap <- statusMapFuture
    } yield
      createOptOutProposition(
        currentYear,
        finalisedStatus,
        statusMap(currentYear.previousYear),
        statusMap(currentYear),
        statusMap(currentYear.nextYear))
  }

  def recallOptOutProposition()(implicit hc: HeaderCarrier,
                                ec: ExecutionContext): Future[OptOutProposition] = {

    OptionT(repository.recallOptOutProposition()).
      getOrElseF(Future.failed(new RuntimeException("Failed to recall Opt Out journey initial state")))
  }

  private def getITSAStatusesFrom(previousYear: TaxYear)(implicit user: MtdItUser[_],
                                                         hc: HeaderCarrier,
                                                         ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] =
    itsaStatusService.getStatusTillAvailableFutureYears(previousYear).map(_.view.mapValues(_.status).toMap.withDefaultValue(ITSAStatus.NoStatus))

  def makeOptOutUpdateRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    recallOptOutProposition().flatMap { proposition =>
      proposition.optOutPropositionType.map {
        case _: OneYearOptOutProposition =>
          makeOptOutUpdateRequest(proposition, proposition.availableTaxYearsForOptOut.head)
        case _: MultiYearOptOutProposition =>
          OptionT(repository.fetchSavedIntent())
            .map(intentTaxYear => makeOptOutUpdateRequest(proposition, intentTaxYear))
            .flatMap(responsesFuture => OptionT.liftF(responsesFuture))
            .getOrElse(ITSAStatusUpdateResponseFailure.defaultFailure())
      } getOrElse Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }
  }

  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition, intentTaxYear: TaxYear)
                             (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    val yearsToUpdate = optOutProposition.optOutYearsToUpdate(intentTaxYear)
    val responsesSeqOfFutures = makeUpdateCalls(yearsToUpdate)
    val result = Future.sequence(responsesSeqOfFutures).
      map(responsesSeq => findAnyFailOrFirstSuccess(responsesSeq))

    def checkVoluntaryElseReturnCurrent(optOutTaxYear: OptOutTaxYear): String = {

      val isYearUserWantsToUpdate = intentTaxYear == optOutTaxYear.taxYear

      if (isYearUserWantsToUpdate){
        optOutTaxYear.status match {
          case Voluntary => s"Voluntary change :: Successful"
          case Annual => s"Annual change :: Failure"
          case Mandated => s"Mandated change :: Failure"
          case _ => s"Other non-changeable status :: Failure"
        }
      }else{
        s"Status of ${optOutTaxYear.taxYear} : ${optOutTaxYear.status} remained the same"
      }
    }

    auditingService.extendedAudit(CheckYourAnswersAuditModel(
      nino = user.nino,
      optOutRequestedFromTaxYear = intentTaxYear.formatTaxYearRange,
      currentYear = optOutProposition.currentTaxYear.taxYear.toString,
      beforeITSAStatusCurrentYearMinusOne = optOutProposition.previousTaxYear.taxYear.toString,
      beforeITSAStatusCurrentYear = optOutProposition.currentTaxYear.taxYear.toString,
      beforeITSAStatusCurrentYearPlusOne = optOutProposition.nextTaxYear.taxYear.toString,

      outcome = createOutcome(result.value.getOrElse(ITSAStatusUpdateResponseFailure) //TODO needs to be changed to resolve into a Success(ITSAResponse)
    ),
      afterAssumedITSAStatusCurrentYearMinusOne = checkVoluntaryElseReturnCurrent(optOutProposition.previousTaxYear),
      afterAssumedITSAStatusCurrentYear = checkVoluntaryElseReturnCurrent(optOutProposition.currentTaxYear),
      afterAssumedITSAStatusCurrentYearPlusOne = checkVoluntaryElseReturnCurrent(optOutProposition.nextTaxYear),
      currentYearMinusOneCrystallised = optOutProposition.previousTaxYear.crystallised
    ))

    result
  }
  private def createOutcome(resolvedResponse: Object): Outcome = {

    resolvedResponse match {
      case ITSAStatusUpdateResponseFailure => new Outcome {
        override val isSuccessful: Boolean = false
      }
      case ITSAStatusUpdateResponseSuccess => new Outcome {
        override val isSuccessful: Boolean = true
        override val failureReason: String = ""
        override val failureCategory: String = ""
      }
      case _ => new Outcome {
        override val isSuccessful: Boolean = false
        override val failureReason: String = "Unknown reason"
        override val failureCategory: String = "Unknown"
      }
    }
  }

  private def makeUpdateCalls(optOutYearsToUpdate: Seq[TaxYear])
                             (implicit user: MtdItUser[_], hc: HeaderCarrier): Seq[Future[ITSAStatusUpdateResponse]] = {
    optOutYearsToUpdate.map(optOutYear => itsaStatusUpdateConnector.optOut(optOutYear, user.nino))
  }

  private def findAnyFailOrFirstSuccess(responses: Seq[ITSAStatusUpdateResponse]): ITSAStatusUpdateResponse = {
    responses.find {
      case _: ITSAStatusUpdateResponseFailure => true
      case _ => false
    }.getOrElse(responses.head)
  }

  def nextUpdatesPageOptOutViewModels()(implicit user: MtdItUser[_],
                                        hc: HeaderCarrier,
                                        ec: ExecutionContext): Future[(NextUpdatesQuarterlyReportingContentChecks, Option[OptOutViewModel])] = {
    for {
      proposition <- fetchOptOutProposition()
      _ <- repository.initialiseOptOutJourney(proposition)
    } yield (nextUpdatesQuarterlyReportingContentChecks(proposition), nextUpdatesOptOutViewModel(proposition))
  }

  private def nextUpdatesQuarterlyReportingContentChecks(oop: OptOutProposition) = {
    val currentYearStatus = oop.currentTaxYear.status
    val previousYearStatus = oop.previousTaxYear.status
    NextUpdatesQuarterlyReportingContentChecks(
      currentYearStatus == Mandated || currentYearStatus == Voluntary,
      previousYearStatus == Mandated || previousYearStatus == Voluntary,
      oop.previousTaxYear.crystallised)
  }

  private def nextUpdatesOptOutViewModel(proposition: OptOutProposition): Option[OptOutViewModel] = {
    proposition.optOutPropositionType.map {
      case p: OneYearOptOutProposition => OptOutOneYearViewModel(oneYearOptOutTaxYear = p.intent.taxYear, state = p.state())
      case _: MultiYearOptOutProposition => OptOutMultiYearViewModel()
    }
  }

  def recallNextUpdatesPageOptOutViewModel()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutViewModel]] = {
    recallOptOutProposition().map { proposition =>
      proposition.optOutPropositionType.map {
        case p: OneYearOptOutProposition => OptOutOneYearViewModel(oneYearOptOutTaxYear = p.intent.taxYear, state = p.state())
        case _: MultiYearOptOutProposition => OptOutMultiYearViewModel()
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
      proposition <- recallOptOutProposition()
      intent <- repository.fetchSavedIntent()
      propositionType = proposition.optOutPropositionType
      quarterlyUpdatesCount <- getQuarterlyUpdatesCount(propositionType)
    } yield processPropositionType(propositionType.get, intent, quarterlyUpdatesCount)
  }

  def optOutConfirmedPageViewModel()(implicit user: MtdItUser[_],
                                     hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ConfirmedOptOutViewModel]] = {
    recallOptOutProposition().flatMap { proposition =>
        proposition.optOutPropositionType match {
          case Some(p: OneYearOptOutProposition) => Future.successful(Some(ConfirmedOptOutViewModel(optOutTaxYear = p.intent.taxYear, state = p.state())))
          case Some(p: MultiYearOptOutProposition) => repository.fetchSavedIntent().map(_.map(taxYear => ConfirmedOptOutViewModel(taxYear, p.state())))
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
        case anyOtherOptOutTaxYear => nextUpdatesService.getQuarterlyUpdatesCounts(anyOtherOptOutTaxYear.taxYear)
      })

    annualQuarterlyUpdateCounts.
      map(cumulativeQuarterlyUpdateCounts).
      map(QuarterlyUpdatesCountForTaxYearModel)
  }

}