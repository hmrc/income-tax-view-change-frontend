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

import audit.AuditingService
import audit.models.OptOutAuditModel
import auth.MtdItUser
import cats.data.OptionT
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums._
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{ITSAStatus, Mandated, Voluntary}
import models.optout._
import models.optout.newJourney.OptOutTaxYearQuestionViewModel
import play.api.Logger
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutProposition.createOptOutProposition
import services.reportingfreq.ReportingFrequency.{QuarterlyUpdatesCountForTaxYearModel, noQuarterlyUpdates}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutService @Inject()(
                               itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                               itsaStatusService: ITSAStatusService,
                               calculationListService: CalculationListService,
                               nextUpdatesService: NextUpdatesService,
                               dateService: DateServiceInterface,
                               optOutRepository: OptOutSessionDataRepository,
                               auditingService: AuditingService
                             ) {

  def fetchOptOutProposition()(implicit user: MtdItUser[_],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): Future[OptOutProposition] = {

    val currentYear = dateService.getCurrentTaxYear

    val finalisedStatusFuture: Future[Boolean] =
      calculationListService
        .isTaxYearCrystallised(currentYear.previousYear)

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
        statusMap(currentYear.nextYear)
      )
  }

  def recallOptOutPropositionWithIntent()(implicit hc: HeaderCarrier,
                                          ec: ExecutionContext): Future[(OptOutProposition, Option[TaxYear])] = {

    OptionT(optOutRepository.recallOptOutPropositionWithIntent()).
      getOrElseF(Future.failed(new RuntimeException("Failed to recall Opt Out journey initial state")))
  }

  def recallSavedIntent()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TaxYear]] = {

    optOutRepository.fetchSavedIntent().flatMap {
      case Some(chosenTaxYear) => Future.successful(Some(chosenTaxYear))
      case _ => Future.successful(None)
    }
  }

  private def getITSAStatusesFrom(previousYear: TaxYear)(implicit user: MtdItUser[_],
                                                         hc: HeaderCarrier,
                                                         ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] =
    itsaStatusService
      .getStatusTillAvailableFutureYears(previousYear)
      .map(_.view.mapValues(_.status)
        .toMap
        .withDefaultValue(ITSAStatus.NoStatus)
      )

  def makeOptOutUpdateRequest()(implicit user: MtdItUser[_],
                                hc: HeaderCarrier,
                                ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    recallOptOutPropositionWithIntent().flatMap { case (proposition, intentTaxYear) =>
      proposition
        .optOutPropositionType
        .map {
          case _: OneYearOptOutProposition =>
            makeOptOutUpdateRequest(proposition, proposition.availableTaxYearsForOptOut.head)
          case _: MultiYearOptOutProposition =>
            intentTaxYear match {
              case Some(taxYear) => makeOptOutUpdateRequest(proposition, taxYear)
              case _ => Future(ITSAStatusUpdateResponseFailure.defaultFailure())
            }
          case _ => Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())

        }.getOrElse {
          Future(ITSAStatusUpdateResponseFailure.defaultFailure())
        }
    }
  }

  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition, intentTaxYear: TaxYear)
                             (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {

    val yearsToUpdate = optOutProposition.optOutYearsToUpdate(intentTaxYear)
    val responsesSeqOfFutures = makeUpdateCalls(yearsToUpdate)
    Future.sequence(responsesSeqOfFutures)
      .map(responsesSeq => findAnyFailOrFirstSuccess(responsesSeq))
      .map { res =>
        val auditModel = OptOutAuditModel.generateOptOutAudit(optOutProposition, intentTaxYear, res)
        auditingService.extendedAudit(auditModel)
        res
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

  def reportingFrequencyViewModels()(implicit user: MtdItUser[_],
                                     hc: HeaderCarrier,
                                     ec: ExecutionContext): Future[(OptOutProposition, Option[OptOutViewModel])] = {
    for {
      proposition <- fetchOptOutProposition()
      _ <- optOutRepository.initialiseOptOutJourney(proposition)
    } yield (proposition, nextUpdatesOptOutViewModel(proposition))
  }

  def nextUpdatesPageChecksAndProposition()(implicit user: MtdItUser[_],
                                            hc: HeaderCarrier,
                                            ec: ExecutionContext): Future[(NextUpdatesQuarterlyReportingContentChecks, Option[OptOutViewModel], OptOutProposition)] = {
    for {
      proposition <- fetchOptOutProposition()
      _ <- optOutRepository.initialiseOptOutJourney(proposition)
    } yield (nextUpdatesQuarterlyReportingContentChecks(proposition), nextUpdatesOptOutViewModel(proposition), proposition)
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

  def recallNextUpdatesPageOptOutViewModel()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutViewModel]] = {
    recallOptOutPropositionWithIntent().map { case (proposition, _) =>
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

    for {
      (proposition, intent) <- recallOptOutPropositionWithIntent()
      propositionType = proposition.optOutPropositionType
      quarterlyUpdatesCount <- getQuarterlyUpdatesCount(propositionType)
    } yield processPropositionType(propositionType.get, intent, quarterlyUpdatesCount)
  }

  def getQuarterlyUpdatesCount(propositionType: Option[OptOutPropositionTypes])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    propositionType match {
      case Some(p: OneYearOptOutProposition) =>
        getQuarterlyUpdatesCountForOfferedYears(p.proposition).map(_.getCountFor(p.intent.taxYear))
      case _ =>
        Future.successful(noQuarterlyUpdates)
    }
  }

  def optOutConfirmedPageViewModel()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ConfirmedOptOutViewModel]] = {
    recallOptOutPropositionWithIntent().flatMap { case (proposition, intent) =>
      proposition.optOutPropositionType match {
        case Some(p: OneYearOptOutProposition) =>
          Future.successful(Some(ConfirmedOptOutViewModel(optOutTaxYear = p.intent.taxYear, state = p.state())))
        case Some(p: MultiYearOptOutProposition) =>
          Future.successful(intent.map(ConfirmedOptOutViewModel(_, p.state())))
        case _ =>
          Future.successful(None)
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


  def getTaxYearForOptOutCancelled()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TaxYear]] = {
    for {
      proposition: OptOutProposition <- fetchOptOutProposition()
      chosenTaxYear: Option[TaxYear] <- optOutRepository.fetchSavedIntent()
      availableOptOutYears: Seq[OptOutTaxYear] = proposition.availableOptOutYears
      singleTaxYear: Option[TaxYear] = availableOptOutYears.headOption.map(_.taxYear)
      isOneYearOptOut: Boolean = proposition.isOneYearOptOut
      isMultiYearOptOut: Boolean = proposition.isMultiYearOptOut
    } yield {
      (isOneYearOptOut, isMultiYearOptOut) match {
        case (false, true) => chosenTaxYear
        case _ => singleTaxYear
      }
    }
  }


  def determineOptOutIntentYear()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ChosenTaxYear] = {
    optOutRepository.fetchSavedIntent().map {
      case Some(chosenYear) if chosenYear == dateService.getCurrentTaxYear.previousYear =>
        PreviousTaxYear
      case Some(chosenYear) if chosenYear == dateService.getCurrentTaxYear =>
        CurrentTaxYear
      case Some(chosenYear) if chosenYear == dateService.getCurrentTaxYear.nextYear =>
        NextTaxYear
      case _ =>
        NoChosenTaxYear
    }
  }

  def isOptOutTaxYearValid(taxYear: Option[String])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutTaxYearQuestionViewModel]] = {
    taxYear match {
      case Some(year) =>
        for {
          proposition <- fetchOptOutProposition()
          numberOfQuarterlyUpdates <- getQuarterlyUpdatesCount(proposition.optOutPropositionType)
        } yield {
          val checkOptOutStatus = year match {
            case ty if ty == proposition.previousTaxYear.taxYear.startYear.toString => Some((proposition.previousTaxYear.canOptOut, proposition.previousTaxYear))
            case ty if ty == proposition.currentTaxYear.taxYear.startYear.toString => Some((proposition.currentTaxYear.canOptOut, proposition.currentTaxYear))
            case ty if ty == proposition.nextTaxYear.taxYear.startYear.toString => Some((proposition.nextTaxYear.canOptOut, proposition.nextTaxYear))
            case _ => None
          }

          val currentYearStatus = proposition.currentTaxYear.status
          val nextYearStatus = proposition.nextTaxYear.status

          (checkOptOutStatus, proposition.optOutPropositionType) match {
            case (Some((true, propositionTaxYear)), Some(propositionType)) if propositionType.state().isDefined =>
              Some(OptOutTaxYearQuestionViewModel(propositionTaxYear, propositionType.state(), numberOfQuarterlyUpdates, currentYearStatus, nextYearStatus))
            case (Some((true, _)), Some(_)) =>
              Logger("application").warn("[OptOutService] Unknown scenario for opt out tax year, redirecting to Reporting Obligations Page")
              None
            case _ =>
              Logger("application").warn("[OptOutService] Invalid tax year provided, redirecting to Reporting Obligations Page")
              None
          }
        }
      case _ =>
        Logger("application").warn("[OptOutService] No tax year provided, redirecting to Reporting Obligations Page")
        Future.successful(None)
    }
  }

  def saveIntent(taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    optOutRepository.saveIntent(taxYear).recover {
      case ex: Exception =>
        Logger("application").error(s"[OptOutService.saveIntent] - Could not save intent tax year to session: $ex")
        false
    }
  }


}