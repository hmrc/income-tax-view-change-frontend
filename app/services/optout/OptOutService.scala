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
import models.itsaStatus.StatusDetail
import models.optout._
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService.SubmissionsCountForTaxYear
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
      yearsToUpdate <- OptionT(Future.successful(Option(optOutProposition.optOutYearsToUpdate(intentOptOutTaxYear))))
      responsesSeqOfFutures  <- OptionT(Future.successful(Option(makeUpdateCalls(yearsToUpdate))))
      responsesSeq <- OptionT(Future.sequence(responsesSeqOfFutures).map(v => Option(v)))
      finalResponse <- OptionT(Future.successful(Option(findAnyFailOrFirstSuccess(responsesSeq))))
    } yield finalResponse

    result.getOrElse(OptOutUpdateResponseFailure.defaultFailure())
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
    fetchOptOutProposition().flatMap { proposition =>
      fetchSavedIntent().map { intent =>
        proposition.optOutPropositionType.flatMap {
          case p: OneYearOptOutProposition => Some(OneYearOptOutCheckpointViewModel(intent = p.intent.taxYear, state = p.state()))
          case p: MultiYearOptOutProposition =>
            intent.map(i => MultiYearOptOutCheckpointViewModel(intent = i))//todo: add test code
        }
      }
    }
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

  def getSubmissionCountForTaxYear(offeredOptOutYears: Seq[TaxYear])
                                  (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[SubmissionsCountForTaxYearModel] = {
    val futureCounts: Seq[Future[SubmissionsCountForTaxYear]] = offeredOptOutYears.map { optOutTaxYear =>
      nextUpdatesService.getSubmissionCounts(optOutTaxYear)
    }

    Future.sequence(futureCounts).map(SubmissionsCountForTaxYearModel)
  }

}


object OptOutService {

  private val noSubmissions = 0

  private def includeTaxYearCount(optOutYear: TaxYear)(countsYear: TaxYear): Boolean = {
    (optOutYear.startYear == countsYear.startYear) || (optOutYear.endYear == countsYear.startYear)
  }

  case class SubmissionsCountForTaxYearModel(counts: Seq[SubmissionsCountForTaxYear]) {

    def getCountFor(optOutYear: TaxYear): Int = counts.find(v => includeTaxYearCount(optOutYear)(v.taxYear))
      .map(_.submissions)
      .getOrElse(noSubmissions)

    val isSubmissionsMade: Boolean = counts.map(_.submissions).sum > noSubmissions
  }


}