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

package services.optIn

import audit.AuditingService
import audit.models.OptInAuditModel
import auth.MtdItUser
import cats.data.OptionT
import cats.implicits._
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import controllers.routes
import enums.JourneyType.{Opt, OptInJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optin.newJourney.SignUpTaxYearQuestionViewModel
import models.optin.{ConfirmTaxYearViewModel, MultiYearCheckYourAnswersViewModel, OptInSessionData}
import play.api.Logger
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import services.optIn.core.OptInProposition._
import services.optIn.core.{OptInInitialState, OptInProposition}
import services.{DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptInService @Inject()(
                              itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusService: ITSAStatusService,
                              dateService: DateServiceInterface,
                              repository: UIJourneySessionDataRepository,
                              auditingService: AuditingService
                            ) {

  def saveIntent(intent: TaxYear)(implicit user: MtdItUser[_],
                                  hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[Boolean] = {
    OptionT(fetchExistingUIJourneySessionDataOrInit()).
      map(journeySd => journeySd.copy(optInSessionData = journeySd.optInSessionData.map(_.copy(selectedOptInYear = Some(intent.toString))))).
      flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(false)
  }

  def availableOptInTaxYear()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] =
    fetchOptInProposition().map(_.availableOptInYears.map(_.taxYear))

  def setupSessionData()(implicit hc: HeaderCarrier): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(hc.sessionId.get.value,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(None, None))))
  }

  private def fetchExistingUIJourneySessionDataOrInit(attempt: Int = 1)(implicit user: MtdItUser[_],
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext): Future[Option[UIJourneySessionData]] = {
    repository.get(hc.sessionId.get.value, Opt(OptInJourney)).flatMap {
      case Some(jsd) => Future.successful(Some(jsd))
      case None if attempt < 2 => setupSessionData().filter(b => b).flatMap(_ => fetchExistingUIJourneySessionDataOrInit(2))
      case _ => Future.successful(None)
    }
  }

  def makeOptInCall()(implicit user: MtdItUser[_],
                      hc: HeaderCarrier,
                      ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {

    fetchSavedChosenTaxYear() flatMap {
      case Some(intentTaxYear) => itsaStatusUpdateConnector.optIn(taxYear = intentTaxYear, user.nino)
        .map { res =>
          fetchOptInProposition().map { proposition =>
            auditingService.extendedAudit(OptInAuditModel(proposition, intentTaxYear, res))
          }
          res
        }
      case None =>
        Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }
  }

  private def fetchSavedOptInSessionData()
                                        (implicit user: MtdItUser[_],
                                         hc: HeaderCarrier,
                                         ec: ExecutionContext): Future[Option[OptInSessionData]] = {

    val savedOptInSessionData = for {
      sessionData <- OptionT(fetchExistingUIJourneySessionDataOrInit())
      optInSessionData <- OptionT(Future(sessionData.optInSessionData))
    } yield optInSessionData

    savedOptInSessionData.value
  }

  def fetchSavedOptInProposition()(implicit user: MtdItUser[_],
                                           hc: HeaderCarrier,
                                           ec: ExecutionContext): Future[Option[OptInProposition]] = {

    val savedOptInProposition = for {

      optInSessionData <- OptionT(fetchSavedOptInSessionData())
      contextData <- OptionT(Future(optInSessionData.optInContextData))
      currentYearAsTaxYear <- OptionT(Future(contextData.currentYearAsTaxYear()))

      currentYearITSAStatus = ITSAStatus.fromString(contextData.currentYearITSAStatus)
      nextYearITSAStatus = ITSAStatus.fromString(contextData.nextYearITSAStatus)

      proposition = createOptInProposition(
        currentYear = currentYearAsTaxYear,
        currentYearItsaStatus = currentYearITSAStatus,
        nextYearItsaStatus = nextYearITSAStatus
      )

    } yield proposition

    savedOptInProposition.value
  }


  def fetchOptInProposition()(implicit user: MtdItUser[_],
                              hc: HeaderCarrier,
                              ec: ExecutionContext): Future[OptInProposition] = {

    fetchSavedOptInProposition().flatMap { savedProposition =>
      savedProposition.map(Future.successful).getOrElse {
        val currentYear = dateService.getCurrentTaxYear
        val nextYear = currentYear.nextYear
        fetchOptInInitialState(currentYear, nextYear)
          .map(initialState => createOptInProposition(currentYear,
            initialState.currentYearItsaStatus,
            initialState.nextYearItsaStatus))
      }
    }
  }

  private def fetchOptInInitialState(currentYear: TaxYear,
                                     nextYear: TaxYear)
                                    (implicit user: MtdItUser[_],
                                     hc: HeaderCarrier,
                                     ec: ExecutionContext): Future[OptInInitialState] = {

    val statusMapFuture: Future[Map[TaxYear, ITSAStatus]] = getITSAStatusesFrom(currentYear)

    for {
      statusMap <- statusMapFuture
    } yield OptInInitialState(statusMap(currentYear), statusMap(nextYear))
  }

  private def getITSAStatusesFrom(currentYear: TaxYear)(implicit user: MtdItUser[_],
                                                        hc: HeaderCarrier,
                                                        ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] = {
    itsaStatusService.getStatusTillAvailableFutureYears(currentYear.previousYear).map(_.view.mapValues(_.status).toMap.withDefaultValue(ITSAStatus.NoStatus))
    //todo is passing currentYear.previousYear correct here?
  }

  def fetchSavedChosenTaxYear()(implicit user: MtdItUser[_],
                                hc: HeaderCarrier,
                                ec: ExecutionContext): Future[Option[TaxYear]] = {

    fetchSavedOptInSessionData().map {
      case None => None
      case Some(s) => s.selectedOptInYear.flatMap(TaxYear.getTaxYearModel)
    }
  }

  def getMultiYearCheckYourAnswersViewModel(isAgent: Boolean)(implicit user: MtdItUser[_],
                                                              hc: HeaderCarrier,
                                                              ec: ExecutionContext): Future[Option[MultiYearCheckYourAnswersViewModel]] = {
    val result = for {
      intentTaxYear <- OptionT(fetchSavedChosenTaxYear())
      cancelURL = routes.ReportingFrequencyPageController.show(isAgent).url
      intentIsNextYear = isNextTaxYear(dateService.getCurrentTaxYear, intentTaxYear)
    } yield MultiYearCheckYourAnswersViewModel(intentTaxYear, isAgent, cancelURL, intentIsNextYear)

    result.value
  }

  private def isNextTaxYear(currentTaxYear: TaxYear, candidate: TaxYear): Boolean = currentTaxYear.nextYear == candidate

  def getConfirmTaxYearViewModel(isAgent: Boolean)(implicit user: MtdItUser[_],
                                                   hc: HeaderCarrier,
                                                   ec: ExecutionContext): Future[Option[ConfirmTaxYearViewModel]] = {
    val result = for {
      intentTaxYear <- OptionT(fetchSavedChosenTaxYear())
      cancelURL = routes.ReportingFrequencyPageController.show(isAgent).url
      intentIsNextYear = isNextTaxYear(dateService.getCurrentTaxYear, intentTaxYear)
    } yield ConfirmTaxYearViewModel(intentTaxYear, cancelURL, intentIsNextYear, isAgent)

    result.value
  }

  def isSignUpTaxYearValid(taxYear: Option[String])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpTaxYearQuestionViewModel]] = {
    taxYear match {
      case Some(year) =>
        fetchOptInProposition().flatMap { proposition =>

          val isSignUpForCY = year match {
            case ty if ty == proposition.currentTaxYear.taxYear.startYear.toString => Some(true)
            case ty if ty == proposition.nextTaxYear.taxYear.startYear.toString => Some(false)
            case _ => None
          }

          (isSignUpForCY, proposition.currentTaxYear.canOptIn, proposition.nextTaxYear.canOptIn) match {
            case (Some(true), true, _) => Future.successful(Some(SignUpTaxYearQuestionViewModel(proposition.currentTaxYear)))
            case (Some(false), _, true) => Future.successful(Some(SignUpTaxYearQuestionViewModel(proposition.nextTaxYear)))
            case _ =>
              Logger("application").warn(s"[OptInService] invalid tax year provided, redirecting to Reporting Obligations Page")
              Future.successful(None)
          }
        }
      case _ =>
        Logger("application").warn("[OptInService] No tax year provided, redirecting to Reporting Obligations Page")
        Future.successful(None)
    }
  }

}