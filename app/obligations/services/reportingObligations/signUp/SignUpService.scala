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

package obligations.services.reportingObligations.signUp

import cats.data.OptionT
import cats.implicits.*
import common.auth.MtdItUser
import common.models.UIJourneySessionData
import common.models.itsaStatus.ITSAStatus
import common.services.{DateServiceInterface, ITSAStatusService}
import ITSAStatus.ITSAStatus
import common.models.incomeSourceDetails.TaxYear
import obligations.models.reportingObligations.signUp.*
import obligations.repositories.SignUpSessionDataRepository
import obligations.services.reportingObligations.signUp.core.SignUpProposition.*
import obligations.services.reportingObligations.signUp.core.{SignUpInitialState, SignUpProposition}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignUpService @Inject()(
                               itsaStatusService: ITSAStatusService,
                               dateService: DateServiceInterface,
                               repository: SignUpSessionDataRepository,
                            ) {

  def saveIntent(intent: TaxYear)(implicit user: MtdItUser[_],
                                  hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[Boolean] = {
    repository.saveIntent(intent)
  }

  def updateJourneyStatusInSessionData(journeyComplete: Boolean)(implicit user: MtdItUser[_],
                                                                 hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[Boolean] = {
    repository.setJourneyCompleteStatus(journeyComplete)
  }

  def availableSignUpTaxYear()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] =
    fetchSignUpProposition().map(_.availableSignUpYears.map(_.taxYear))


  
  def fetchSavedSignUpSessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpSessionData]] = {

    val savedOptInSessionData = for {
      sessionData <- OptionT(repository.fetchExistingUIJourneySessionDataOrInit())
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

    repository.fetchExistingUIJourneySessionDataOrInit().flatMap {

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

              repository.setUpdatedSessionDataStatus(updatedJourneySessionData)
            }
          case None =>
            repository.setupSessionData().flatMap {
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