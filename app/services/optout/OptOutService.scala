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

  private def fetchOptOutProposition()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutProposition] = {

    val currentYear = dateService.getCurrentTaxYear
    val previousYear = currentYear.previousYear
    val nextYear = currentYear.nextYear

    for {
      finalisedStatus <- calculationListService.isTaxYearCrystallised(previousYear)
      statusMap <- itsaStatusService.getStatusTillAvailableFutureYears(previousYear)
    }
    yield createOptOutProposition(previousYear, currentYear, nextYear, finalisedStatus, statusMap)
  }

  def getStatusDetail(year: TaxYear, statusMap: Map[TaxYear, StatusDetail]): StatusDetail = {
    val defaultStatusDetail = StatusDetail("Unknown", ITSAStatus.NoStatus, "Unknown")
    statusMap.getOrElse(year, defaultStatusDetail)
  }

  private def createOptOutProposition(previousYear: TaxYear,
                                      currentYear: TaxYear,
                                      nextYear: TaxYear,
                                      finalisedStatus: Boolean,
                                      statusMap: Map[TaxYear, StatusDetail]
                                     ): OptOutProposition = {

    val previousYearOptOut = PreviousOptOutTaxYear(
      status = getStatusDetail(previousYear, statusMap).status,
      taxYear = previousYear,
      crystallised = finalisedStatus
    )

    val currentYearOptOut = CurrentOptOutTaxYear(
      status = getStatusDetail(currentYear, statusMap).status,
      taxYear = currentYear
    )

    val nextYearOptOut = NextOptOutTaxYear(
      status = getStatusDetail(nextYear, statusMap).status,
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

  def makeOptOutUpdateRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {
    fetchOptOutProposition().flatMap { proposition =>
      proposition.optOutPropositionType.map {
        case _: OneYearOptOutProposition =>
          saveIntent(intent = proposition.availableTaxYearsForOptOut.head)
          makeOptOutUpdateRequest(proposition)
        case _: MultiYearOptOutProposition => makeOptOutUpdateRequest(proposition)
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

  def makeOptOutUpdateRequest(optOutProposition: OptOutProposition)
                             (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptOutUpdateResponse] = {

    def makeUpdateCalls(optOutYearsToUpdate: Seq[OptOutTaxYear]): Seq[Future[OptOutUpdateResponse]] = {
      optOutYearsToUpdate.map(optOutYear => itsaStatusUpdateConnector.requestOptOutForTaxYear(optOutYear.taxYear, user.nino, optOutUpdateReason))
    }

    def findAnyFailOrFirstSuccess(responses: Seq[OptOutUpdateResponse]): OptOutUpdateResponse = {
      responses.find {
          case _: OptOutUpdateResponseFailure => true
          case _ => false
      }.getOrElse(responses.head)
    }

    val result = for {
      intentTaxYear <- OptionT(fetchSavedIntent())
      intentOptOutTaxYear <- OptionT(Future.successful(optOutProposition.optOutTaxYearFor(intentTaxYear)))
      yearsToUpdate <- OptionT(Future.successful(Option(optOutProposition.optOutYearsToUpdate(intentOptOutTaxYear.taxYear))))
      responsesSeqOfFutures  <- OptionT(Future.successful(Option(makeUpdateCalls(yearsToUpdate))))
      responsesSeq <- OptionT(Future.sequence(responsesSeqOfFutures).map(v => Option(v)))
      finalResponse <- OptionT(Future.successful(Option(findAnyFailOrFirstSuccess(responsesSeq))))
    } yield finalResponse

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
        case Some(p:OneYearOptOutProposition) => getQuarterlyUpdatesCountForTaxYear(Seq(p.intent.taxYear)).map(_.getCountFor(p.intent.taxYear))
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
      fetchSavedIntent().map { intent =>
        proposition.optOutPropositionType.flatMap {
          case p: OneYearOptOutProposition => Some(ConfirmedOptOutViewModel(optOutTaxYear = p.intent.taxYear, state = p.state()))
          case p: MultiYearOptOutProposition => intent.map(i => ConfirmedOptOutViewModel(optOutTaxYear = i, state = p.state())) //todo: add test code
        }
      }
    }
  }


  def getTaxYearsAvailableForOptOut()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] = {
    fetchOptOutProposition().map(proposition => proposition.availableTaxYearsForOptOut)
  }

  def getQuarterlyUpdatesCountForTaxYear(offeredOptOutYears: Seq[TaxYear])
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[QuarterlyUpdatesCountForTaxYearModel] = {
    val futureCounts: Seq[Future[QuarterlyUpdatesCountForTaxYear]] = offeredOptOutYears.map { optOutTaxYear =>
      nextUpdatesService.getQuarterlyUpdatesCounts(optOutTaxYear)
    }

    Future.sequence(futureCounts).map(QuarterlyUpdatesCountForTaxYearModel)
  }

}


object OptOutService {

  private val noQuarterlyUpdates = 0

  private def includeTaxYearCount(optOutYear: TaxYear, countsYear: TaxYear): Boolean = {
    (optOutYear.startYear == countsYear.startYear) || (optOutYear.endYear == countsYear.startYear)
  }

  case class QuarterlyUpdatesCountForTaxYearModel(counts: Seq[QuarterlyUpdatesCountForTaxYear]) {

    def getCountFor(optOutYear: TaxYear): Int = counts.find(taxYearCounts => includeTaxYearCount(optOutYear, taxYearCounts.taxYear))
      .map(_.count)
      .getOrElse(noQuarterlyUpdates)

    val isQuarterlyUpdatesMade: Boolean = counts.map(_.count).sum > noQuarterlyUpdates
  }


}