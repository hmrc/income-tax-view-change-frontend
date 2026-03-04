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

package services.reportingObligations.signUp

import audit.AuditingService
import audit.models.OptInAuditModel
import auth.MtdItUser
import cats.data.OptionT
import cats.implicits.*
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums.JourneyType.{Opt, SignUpJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.reportingObligations.signUp.*
import play.api.Logger
import repositories.UIJourneySessionDataRepository
import services.reportingObligations.signUp.core.SignUpProposition.*
import services.reportingObligations.signUp.core.{SignUpInitialState, SignUpProposition}
import services.{DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignUpService @Inject()(
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
      map(journeySd => journeySd.copy(signUpSessionData = journeySd.signUpSessionData.map(_.copy(selectedSignUpYear = Some(intent.toString))))).
      flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(false)
  }

  def updateJourneyStatusInSessionData(journeyComplete: Boolean)(implicit user: MtdItUser[_],
                                                                 hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[Boolean] = {
    OptionT(fetchExistingUIJourneySessionDataOrInit())
      .map(journeySd => journeySd.copy(signUpSessionData = journeySd.signUpSessionData.map(_.copy(journeyIsComplete = Some(journeyComplete)))))
      .flatMap(journeySd => OptionT.liftF(repository.set(journeySd)))
      .getOrElse(false)
      .map {
        case false =>
          Logger("application").error(s"[OptInService][updateJourneyStatusInSessionData] Failed to set journeyIsComplete flag")
          false
        case x => x
      }
  }

  def availableSignUpTaxYear()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] =
    fetchSignUpProposition().map(_.availableSignUpYears.map(_.taxYear))

  def setupSessionData()(implicit hc: HeaderCarrier): Future[Boolean] =
    repository.set(UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString, signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))))

  private def fetchExistingUIJourneySessionDataOrInit(attempt: Int = 1)(implicit user: MtdItUser[_],
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext): Future[Option[UIJourneySessionData]] = {
    repository.get(hc.sessionId.get.value, Opt(SignUpJourney)).flatMap {
      case Some(jsd) => Future.successful(Some(jsd))
      case None if attempt < 2 => setupSessionData().filter(b => b).flatMap(_ => fetchExistingUIJourneySessionDataOrInit(2))
      case _ => Future.successful(None)
    }
  }

  def makeOptInCall()(implicit user: MtdItUser[_],
                      hc: HeaderCarrier,
                      ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {

    fetchSavedChosenTaxYear() flatMap {
      case Some(intentTaxYear) => itsaStatusUpdateConnector.signUp(taxYear = intentTaxYear, user.nino)
        .map { res =>
          fetchSignUpProposition().map { proposition =>
            auditingService.extendedAudit(OptInAuditModel(proposition, intentTaxYear, res))
          }
          res
        }
      case None =>
        Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }
  }

  def fetchSavedSignUpSessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpSessionData]] = {

    val savedOptInSessionData = for {
      sessionData <- OptionT(fetchExistingUIJourneySessionDataOrInit())
      optInSessionData <- OptionT(Future(sessionData.signUpSessionData))
    } yield optInSessionData

    savedOptInSessionData.value
  }

  private def fetchSavedSignUpProposition()(implicit user: MtdItUser[_],
                                           hc: HeaderCarrier,
                                           ec: ExecutionContext): Future[Option[SignUpProposition]] = {

    val savedOptInProposition = for {

      optInSessionData <- OptionT(fetchSavedSignUpSessionData())
      contextData <- OptionT(Future(optInSessionData.signUpContextData))
      currentYearAsTaxYear <- OptionT(Future(contextData.currentYearAsTaxYear()))

      currentYearITSAStatus = ITSAStatus.fromString(contextData.currentYearITSAStatus)
      nextYearITSAStatus = ITSAStatus.fromString(contextData.nextYearITSAStatus)

      proposition = createSignUpProposition(
        currentYear = currentYearAsTaxYear,
        currentYearItsaStatus = currentYearITSAStatus,
        nextYearItsaStatus = nextYearITSAStatus
      )

    } yield proposition

    savedOptInProposition.value
  }


  def fetchSignUpProposition()(implicit user: MtdItUser[_],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): Future[SignUpProposition] = {

    fetchSavedSignUpProposition().flatMap { savedProposition =>
      savedProposition.map(Future.successful).getOrElse {
        val currentYear = dateService.getCurrentTaxYear
        val nextYear = currentYear.nextYear
        fetchOptInInitialState(currentYear, nextYear)
          .map(initialState => createSignUpProposition(currentYear,
            initialState.currentYearItsaStatus,
            initialState.nextYearItsaStatus))
      }
    }
  }

  private def fetchOptInInitialState(currentYear: TaxYear,
                                     nextYear: TaxYear)
                                    (implicit user: MtdItUser[_],
                                     hc: HeaderCarrier,
                                     ec: ExecutionContext): Future[SignUpInitialState] = {

    val statusMapFuture: Future[Map[TaxYear, ITSAStatus]] = getITSAStatusesFrom(currentYear)

    for {
      statusMap <- statusMapFuture
    } yield SignUpInitialState(statusMap(currentYear), statusMap(nextYear))
  }

  private def getITSAStatusesFrom(taxYear: TaxYear)(implicit user: MtdItUser[_],
                                                    hc: HeaderCarrier,
                                                    ec: ExecutionContext): Future[Map[TaxYear, ITSAStatus]] = {
    itsaStatusService.getStatusTillAvailableFutureYears(taxYear.previousYear).map(_.view.mapValues(_.status).toMap.withDefaultValue(ITSAStatus.NoStatus))
  }

  def fetchSavedChosenTaxYear()(implicit user: MtdItUser[_],
                                hc: HeaderCarrier,
                                ec: ExecutionContext): Future[Option[TaxYear]] = {

    fetchSavedSignUpSessionData().map {
      case None => None
      case Some(s) => s.selectedSignUpYear.flatMap(TaxYear.getTaxYearModel)
    }
  }

  private def isNextTaxYear(currentTaxYear: TaxYear, candidate: TaxYear): Boolean = currentTaxYear.nextYear == candidate

  def isSignUpTaxYearValid(taxYear: Option[String])(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpTaxYearQuestionViewModel]] = {
    taxYear match {
      case Some(year) =>
        fetchSignUpProposition().flatMap { proposition =>

          val isSignUpForCY = year match {
            case ty if ty == proposition.currentTaxYear.taxYear.startYear.toString => Some(true)
            case ty if ty == proposition.nextTaxYear.taxYear.startYear.toString => Some(false)
            case _ => None
          }

          (isSignUpForCY, proposition.currentTaxYear.canSignUp, proposition.nextTaxYear.canSignUp) match {
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

  def initialiseOptInContextData()(implicit user: MtdItUser[_],
                                   hc: HeaderCarrier,
                                   ec: ExecutionContext): Future[Boolean] = {

    fetchExistingUIJourneySessionDataOrInit().flatMap {

      case Some(journeySessionData) =>
        journeySessionData.signUpSessionData match {

          case Some(optInSd) if optInSd.signUpContextData.isDefined =>
            Future.successful(true)

          case Some(optInSd) =>
            val currentYear = dateService.getCurrentTaxYear
            val nextYear = currentYear.nextYear

            fetchOptInInitialState(currentYear, nextYear).flatMap { initialState =>

              val contextData = SignUpContextData(
                currentTaxYear = currentYear.toString,
                currentYearITSAStatus = initialState.currentYearItsaStatus.toString,
                nextYearITSAStatus = initialState.nextYearItsaStatus.toString
              )

              val updatedJourneySessionData = journeySessionData.copy(
                signUpSessionData = Some(
                  optInSd.copy(signUpContextData = Some(contextData))
                )
              )

              repository.set(updatedJourneySessionData)
            }
          case None =>
            setupSessionData().flatMap {
              case true  => initialiseOptInContextData()
              case false => Future.successful(false)
            }

        }

      case None =>
        Logger("application").error(
          "[OptInService][initialiseOptInContextData] Could not initialise session"
        )
        Future.successful(false)
    }
  }
}