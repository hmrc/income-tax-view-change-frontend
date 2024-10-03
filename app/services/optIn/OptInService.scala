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

import auth.MtdItUser
import cats.data.OptionT
import connectors.ITSAStatusConnector
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import controllers.routes
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{ITSAStatus, NoStatus}
import models.itsaStatus.{ITSAStatus, ITSAStatusError, ITSAStatusResponseModel}
import models.optin.{ConfirmTaxYearViewModel, MultiYearCheckYourAnswersViewModel, OptInContextData, OptInSessionData}
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import services.optIn.core.OptInProposition.createOptInProposition
import services.optIn.core.{OptInInitialState, OptInProposition}
import services.{DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.OptInJourney

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex


// Note to self wish there was a better way to get and save the session data.
// Note: Conversion between itsa status and string could be nicer.
@Singleton
class OptInService @Inject()(
                              itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                              itsaStatusConnector: ITSAStatusConnector,
                              itsaStatusService: ITSAStatusService,
                              dateService: DateServiceInterface,
                              repository: UIJourneySessionDataRepository
                            ) {

  implicit class TaxYearHelper(taxYear: TaxYear) {
    def toApiFormat: String = {
      s"${taxYear.startYear}-${taxYear.endYear.toString.toSeq.drop(2)}"
    }
  }

  def getUIJourneySessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[UIJourneySessionData] = {
    val emptyUIJourneySessionData = UIJourneySessionData(sessionId = hc.sessionId.get.value, journeyType = OptInJourney.Name)
    repository.get(sessionId = hc.sessionId.get.value, journeyType = OptInJourney.Name).map(_.fold(emptyUIJourneySessionData)(identity))
  }

  def getOptInSessionData()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[OptInSessionData] = {
    getUIJourneySessionData().map(
      sessionData =>
        sessionData.optInSessionData.getOrElse(OptInSessionData(None, None))
    )
  }

  def setOptInSessionData(optInSessionData: OptInSessionData)(
    implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext
  ): Future[OptInSessionData] = {

    for {
      sessionData <- getUIJourneySessionData()
      _ <- repository.cleanSet(sessionData.copy(optInSessionData = Some(optInSessionData)))
      optInSessionData <- getOptInSessionData()
    } yield {
      optInSessionData
    }
  }


  def retrieveAndSetOptInSessionData(taxYear: TaxYear)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[UIJourneySessionData] = {
    println("********** " + taxYear)
    for {
      uiJourneySessionData: UIJourneySessionData <- getUIJourneySessionData()
      optInSessionData: OptInSessionData <- getOptInSessionData()
      itsaStatusDataCurrentYear: Either[ITSAStatusError, List[ITSAStatusResponseModel]] <-
        itsaStatusConnector.getITSAStatusDetail(user.nino, taxYear.formatTaxYearRange, futureYears = false, history = false)
      itsaStatusDataNextYear: Either[ITSAStatusError, List[ITSAStatusResponseModel]] <-
        itsaStatusConnector.getITSAStatusDetail(user.nino, taxYear.nextYear.formatTaxYearRange, futureYears = false, history = false)
      currentYearITSAStatus <-
        itsaStatusDataCurrentYear match {
          case Right(apiData) =>
            println("++++++++++++ " + apiData)
            val aa: Option[List[ITSAStatus]] = apiData.find(_.taxYear == taxYear.toApiFormat).flatMap(_.itsaStatusDetails.map(_.map(_.status)))
            val apiStatusForCurrentTaxYear: ITSAStatus = aa.flatMap(_.headOption).getOrElse(NoStatus)
            Future(apiStatusForCurrentTaxYear)
          case Left(_) =>
            Future(NoStatus)
        }
      nextYearITSAStatus <-
        itsaStatusDataNextYear match {
          case Right(apiData) =>
            println("++++++++++++ " + apiData)
            val aa: Option[List[ITSAStatus]] = apiData.find(_.taxYear == taxYear.nextYear.toApiFormat).flatMap(_.itsaStatusDetails.map(_.map(_.status)))
            val apiStatusForCurrentTaxYear: ITSAStatus = aa.flatMap(_.headOption).getOrElse(NoStatus)
            Future(apiStatusForCurrentTaxYear)
          case Left(_) =>
            Future(NoStatus)
        }
      updatedUISessionData <- {
        println(currentYearITSAStatus)
        println(nextYearITSAStatus)
        saveOptInSessionData(taxYear.formatTaxYearRange, currentYearITSAStatus, nextYearITSAStatus)
      }
    } yield {
      updatedUISessionData
    }
  }

  private def fetchExistingUIJourneySessionDataOrInit(attempt: Int = 1)(
    implicit user: MtdItUser[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[UIJourneySessionData]] = {
    repository.get(hc.sessionId.get.value, OptInJourney.Name).flatMap {
      case Some(jsd) => Future.successful(Some(jsd))
      case None if attempt < 2 => setupSessionData().filter(b => b).flatMap(_ => fetchExistingUIJourneySessionDataOrInit(2))
      case _ => Future.successful(None)
    }
  }

  def saveOptInSessionData(
                            currentTaxYear: String,
                            currentYearITSAStatus: ITSAStatus,
                            nextYearITSAStatus: ITSAStatus
                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UIJourneySessionData] = {

    for {
      sessionData: Option[UIJourneySessionData] <- repository.get(sessionId = hc.sessionId.get.value, journeyType = OptInJourney.Name)
      defaultedData: UIJourneySessionData = {
        val handledOptSessionData = sessionData.getOrElse(UIJourneySessionData(sessionId = hc.sessionId.get.value, journeyType = OptInJourney.Name))
        val selectedOptInYear: Option[String] = handledOptSessionData.optInSessionData.flatMap(_.selectedOptInYear)
        handledOptSessionData
          .copy(optInSessionData =
            Some(
              OptInSessionData(
                optInContextData = Some(OptInContextData(
                  currentTaxYear,
                  statusToString(currentYearITSAStatus),
                  statusToString(nextYearITSAStatus)
                )),
                selectedOptInYear = selectedOptInYear
              )
            ))
      }
      savedData <- repository.cleanSet(defaultedData)
    } yield {
      savedData
    }
  }

  def saveIntent(intent: TaxYear)(implicit user: MtdItUser[_],
                                  hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[Boolean] = {
    OptionT(fetchExistingUIJourneySessionDataOrInit())
      .map(journeySd => journeySd.copy(optInSessionData = journeySd.optInSessionData.map(_.copy(selectedOptInYear = Some(intent.toString)))))
      .flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(false)
  }

  def getSelectedOptInTaxYear()(implicit user: MtdItUser[_],
                                hc: HeaderCarrier,
                                ec: ExecutionContext): Future[Option[TaxYear]] = {
    fetchExistingUIJourneySessionDataOrInit()
      .map(_.flatMap(_.optInSessionData.flatMap(_.selectedOptInYear.flatMap { optInYear =>

        val yearPattern: Regex = """(\d{4})-(\d{4})""".r

        optInYear match {
          case yearPattern(startYear, endYear) =>
            Some(TaxYear(startYear.toInt, endYear.toInt))
          case _ =>
            None
        }
      }
      )))
  }

  def availableOptInTaxYear()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[TaxYear]] =
    fetchOptInProposition().map(_.availableOptInYears.map(_.taxYear))

  def setupSessionData()(implicit hc: HeaderCarrier): Future[Boolean] = {
    repository.set(
      UIJourneySessionData(hc.sessionId.get.value,
        OptInJourney.Name,
        optInSessionData =
          Some(OptInSessionData(None, None))))
  }


  def makeOptInCall()(implicit user: MtdItUser[_],
                      hc: HeaderCarrier,
                      ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {

    fetchSavedChosenTaxYear() flatMap {
      case Some(intentTaxYear) => itsaStatusUpdateConnector.optIn(taxYear = intentTaxYear, user.nino)
      case None => Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }

  }

  private def fetchSavedOptInSessionData()(implicit user: MtdItUser[_],
                                           hc: HeaderCarrier,
                                           ec: ExecutionContext): Future[Option[OptInSessionData]] = {

    val savedOptInSessionData = for {
      sessionData <- OptionT(fetchExistingUIJourneySessionDataOrInit())
      optInSessionData <- OptionT.fromOption[Future](sessionData.optInSessionData)
    } yield optInSessionData

    savedOptInSessionData.value
  }

  private def fetchSavedOptInProposition()(implicit user: MtdItUser[_],
                                           hc: HeaderCarrier,
                                           ec: ExecutionContext): Future[Option[OptInProposition]] = {

    val savedOptInProposition = for {

      optInSessionData <- OptionT(fetchSavedOptInSessionData())
      contextData <- OptionT.fromOption[Future](optInSessionData.optInContextData)
      currentYearAsTaxYear <- OptionT.fromOption[Future](contextData.currentYearAsTaxYear())
      nextTaxYearAsTaxYear <- OptionT.fromOption[Future](contextData.nextTaxYearAsTaxYear())

      currentYearITSAStatus = stringToStatus(contextData.currentYearITSAStatus)
      nextYearITSAStatus = stringToStatus(contextData.nextYearITSAStatus)

      optInInitialState = OptInInitialState(currentYearITSAStatus, nextYearITSAStatus)
      proposition = createOptInProposition(currentYearAsTaxYear, nextTaxYearAsTaxYear, optInInitialState)

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
          .map(initialState => createOptInProposition(currentYear, nextYear, initialState))
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

  private def getITSAStatusesFrom(currentYear: TaxYear)(
    implicit user: MtdItUser[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[TaxYear, ITSAStatus]] = {
    itsaStatusService.getStatusTillAvailableFutureYears(currentYear.previousYear)
      .map(_.view.mapValues(_.status).toMap.withDefaultValue(ITSAStatus.NoStatus))
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

  def getMultiYearCheckYourAnswersViewModel(isAgent: Boolean)(
    implicit user: MtdItUser[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[MultiYearCheckYourAnswersViewModel]] = {
    val result =
      for {
        intentTaxYear <- OptionT(fetchSavedChosenTaxYear())
        cancelURL = routes.ReportingFrequencyPageController.show(isAgent).url
        intentIsNextYear = isNextTaxYear(dateService.getCurrentTaxYear, intentTaxYear)
      } yield MultiYearCheckYourAnswersViewModel(intentTaxYear, isAgent, cancelURL, intentIsNextYear)

    result.value
  }

  private def isNextTaxYear(currentTaxYear: TaxYear, candidate: TaxYear): Boolean =
    currentTaxYear.nextYear == candidate

  def updateOptInPropositionYearStatuses(
                                          desiredCurrentYearItsaStatus: Option[ITSAStatus],
                                          desiredNextYearItsaStatus: Option[ITSAStatus]
                                        )(
                                          implicit user: MtdItUser[_],
                                          hc: HeaderCarrier,
                                          ec: ExecutionContext
                                        ): Future[OptInProposition] = {

    for {
      optInProposition: OptInProposition <- {
        val currentYear = dateService.getCurrentTaxYear
        val nextYear = currentYear.nextYear
        (desiredCurrentYearItsaStatus, desiredNextYearItsaStatus) match {
          case (Some(currentYearStatus), Some(nextYearStatus)) =>
            fetchOptInInitialState(currentYear, nextYear)
              .map(initialState => createOptInProposition(currentYear, nextYear, initialState.copy(currentYearItsaStatus = currentYearStatus, nextYearItsaStatus = nextYearStatus)))
          case (Some(currentYearStatus), None) =>
            fetchOptInInitialState(currentYear, nextYear)
              .map(initialState => createOptInProposition(currentYear, nextYear, initialState.copy(currentYearItsaStatus = currentYearStatus)))
          case (_, Some(nextYearStatus)) =>
            fetchOptInInitialState(currentYear, nextYear)
              .map(initialState => createOptInProposition(currentYear, nextYear, initialState.copy(nextYearItsaStatus = nextYearStatus)))
          case _ =>
            fetchOptInInitialState(currentYear, nextYear)
              .map(initialState => createOptInProposition(currentYear, nextYear, initialState))
        }
      }
    } yield {
      optInProposition
    }
  }

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

}