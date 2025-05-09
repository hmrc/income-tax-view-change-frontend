/*
 * Copyright 2023 HM Revenue & Customs
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

package services.manageBusinesses

import auth.MtdItUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.manageBusinesses.add.routes
import enums.IncomeSourceJourney.{IncomeSourceType, JourneyState}
import enums.JourneyType.IncomeSourceJourneyType
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, LatencyDetails, TaxYear, UIJourneySessionData}
import models.itsaStatus.{ITSAStatus, StatusDetail, StatusReason}
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{CalculationListService, DateService, ITSAStatusService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.JourneyCheckerManageBusinesses

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceRFService @Inject()(val sessionService: SessionService,
                                      val itsaStatusService: ITSAStatusService,
                                      val calculationListService: CalculationListService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      val dateService: DateService,
                                      val appConfig: FrontendAppConfig)
                                     (implicit val ec: ExecutionContext) extends JourneyCheckerManageBusinesses {

  def agentPrefix(isAgent: Boolean): String = if (isAgent) "[Agent]" else ""

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private val currentTy: TaxYear = dateService.getCurrentTaxYear
  private val nextTy: TaxYear = dateService.getNextTaxYear

  private def redirectToIncomeSourceAddedPage(isAgent: Boolean, incomeSourceType: IncomeSourceType): Future[Result] = {
    if (isAgent) Future.successful(Redirect(routes.IncomeSourceAddedController.showAgent(incomeSourceType)))
    else Future.successful(Redirect(routes.IncomeSourceAddedController.show(incomeSourceType)))
  }

  private def isItsaStatusMandatedOrVoluntary(itsaStatusDetail: Option[StatusDetail]): Boolean = {
    itsaStatusDetail.exists(_.isMandatedOrVoluntary)
  }

  private def isItsaStatusAnnual(itsaStatusDetail: Option[StatusDetail]): Boolean = {
    itsaStatusDetail.exists(_.isAnnual)
  }

  private def businessIsInLatency(latencyDetails: Option[LatencyDetails]): Boolean = {
    !latencyDetails.exists(_.taxYear2.toInt < currentTy.endYear)
  }

  private case class CrystallisationStatus(cyTaxYearIsCrystallised: Boolean, `cy+1TaxYearIsCrystallised`: Boolean)

  private def isCrystallisedForCurrTyAndNextTy(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[CrystallisationStatus] = {
    for {
      currTyIsCrystallised <- calculationListService.determineTaxYearCrystallised(currentTy.endYear)
      nextTyIsCrystallised <- calculationListService.determineTaxYearCrystallised(nextTy.endYear)
    } yield {
      CrystallisationStatus(currTyIsCrystallised, nextTyIsCrystallised)
    }
  }

  private def updateChooseTaxYearsModelInSessionData(
                                                      sessionData: UIJourneySessionData,
                                                      incomeSourceReportingFrequencySourceData: Option[IncomeSourceReportingFrequencySourceData]
                                                    ): Future[UIJourneySessionData] = {
    val updatedSessionData: UIJourneySessionData = sessionData.copy(incomeSourceReportingFrequencyData = incomeSourceReportingFrequencySourceData)
    sessionService.setMongoData(updatedSessionData).flatMap {
      case true =>
        Future.successful(updatedSessionData)
      case _ =>
        Logger("application").error("incomeSourceReportingFrequencyData status update failed")
        throw new Exception("incomeSourceReportingFrequencyData status update failed")
    }
  }

  private def redirectToCompletionPage(
                                        currentTyStatus: Option[StatusDetail],
                                        nextTyStatus: Option[StatusDetail],
                                        isCrystallisedForCurrTy: Boolean,
                                        isCrystallisedForNextTy: Boolean,
                                        latencyDetails: Option[LatencyDetails]
                                      ): Boolean = {
    (!businessIsInLatency(latencyDetails)) ||
      (isCrystallisedForCurrTy && isCrystallisedForNextTy) ||
      (nextTyStatus.exists(_.status == ITSAStatus.NoStatus)
        && currentTyStatus.exists(cys => cys.status == ITSAStatus.Annual && cys.statusReason == StatusReason.Rollover)) ||
      (isItsaStatusAnnual(currentTyStatus) && isItsaStatusAnnual(nextTyStatus))
  }

  private def determineIncomeSourceReportingFrequencySourceData(
                                                                 currentTyStatusDetailOpt: Option[StatusDetail],
                                                                 nextTyStatusDetailOpt: Option[StatusDetail],
                                                                 isCrystallisedForCurrTy: Boolean,
                                                                 isCrystallisedForNextTy: Boolean
                                                               ): Option[IncomeSourceReportingFrequencySourceData] = {

    val incomeSourceReportingFrequencySourceData: Option[IncomeSourceReportingFrequencySourceData] = {
      (
        isCrystallisedForCurrTy,
        isCrystallisedForNextTy,
        currentTyStatusDetailOpt.map(_.status),
        nextTyStatusDetailOpt.map(_.status)
      ) match {
        case (true, false, Some(ITSAStatus.Annual), Some(ITSAStatus.NoStatus)) if currentTyStatusDetailOpt.exists(_.statusReason != StatusReason.Rollover) =>
          Some(IncomeSourceReportingFrequencySourceData(false, true, false, false))
        case (true, false, _, _) if isItsaStatusMandatedOrVoluntary(currentTyStatusDetailOpt) =>
          Some(IncomeSourceReportingFrequencySourceData(false, true, false, false))
        case (_, false, Some(ITSAStatus.Annual), _) if isItsaStatusMandatedOrVoluntary(nextTyStatusDetailOpt) =>
          Some(IncomeSourceReportingFrequencySourceData(false, true, false, false))
        case (false, _, _, Some(ITSAStatus.Annual)) if isItsaStatusMandatedOrVoluntary(currentTyStatusDetailOpt) =>
          Some(IncomeSourceReportingFrequencySourceData(true, false, false, false))
        case (false, _, _, _) if isItsaStatusMandatedOrVoluntary(currentTyStatusDetailOpt) =>
          Some(IncomeSourceReportingFrequencySourceData(true, true, false, false))
        case _ =>
          None
      }
    }
    incomeSourceReportingFrequencySourceData
  }

  private def redirectToRFPageChecks(latencyDetails: Option[LatencyDetails],
                                     currentTyStatusDetailOpt: Option[StatusDetail],
                                     nextTyStatusDetailOpt: Option[StatusDetail],
                                     isCrystallisedForCurrTy: Boolean,
                                     isCrystallisedForNextTy: Boolean,
                                     incomeSourceType: IncomeSourceType,
                                     isAgent: Boolean,
                                     sessionData: UIJourneySessionData
                                    )(codeBlock: UIJourneySessionData => Future[Result]): Future[Result] = {
    val data =
      determineIncomeSourceReportingFrequencySourceData(
        currentTyStatusDetailOpt,
        nextTyStatusDetailOpt,
        isCrystallisedForCurrTy,
        isCrystallisedForNextTy
      )

    data match {
      case data: Option[IncomeSourceReportingFrequencySourceData] if businessIsInLatency(latencyDetails) =>
        updateChooseTaxYearsModelInSessionData(sessionData, data)
          .flatMap(codeBlock(_))
      case _ =>
        redirectToIncomeSourceAddedPage(isAgent, incomeSourceType)
    }
  }

  def redirectChecksForIncomeSourceRF(incomeSourceJourneyType: IncomeSourceJourneyType,
                                      journeyState: JourneyState,
                                      incomeSourceType: IncomeSourceType,
                                      currentTaxYearEnd: Int,
                                      isAgent: Boolean,
                                      isChange: Boolean
                                     )(codeBlock: UIJourneySessionData => Future[Result])
                                     (implicit user: MtdItUser[_],
                                      hc: HeaderCarrier): Future[Result] = {
    withSessionDataAndNewIncomeSourcesFS(incomeSourceJourneyType, journeyState) { sessionData =>

      if (isChange) {
        codeBlock(sessionData)
      } else {
        for {
          crystallisationStatus <- isCrystallisedForCurrTyAndNextTy
          statusTaxYearMap <- itsaStatusService.getStatusTillAvailableFutureYears(TaxYear(currentTaxYearEnd - 1, currentTaxYearEnd))
          accountLevelITSAStatusCurrentTaxYear = statusTaxYearMap.get(currentTy)
          accountLevelITSAStatusNextTaxYear = statusTaxYearMap.get(nextTy)
          someIncomeSourceId <- Future(sessionData.addIncomeSourceData.flatMap(_.incomeSourceId))
          result <-
            someIncomeSourceId match {
              case Some(id) =>
                val latencyDetails = user.incomeSources.getLatencyDetails(incomeSourceType, id)
                if (redirectToCompletionPage(accountLevelITSAStatusCurrentTaxYear, accountLevelITSAStatusNextTaxYear, crystallisationStatus.cyTaxYearIsCrystallised, crystallisationStatus.`cy+1TaxYearIsCrystallised`, latencyDetails)) {
                  redirectToIncomeSourceAddedPage(isAgent, incomeSourceType)
                } else {
                  redirectToRFPageChecks(latencyDetails, accountLevelITSAStatusCurrentTaxYear, accountLevelITSAStatusNextTaxYear, crystallisationStatus.cyTaxYearIsCrystallised, crystallisationStatus.`cy+1TaxYearIsCrystallised`, incomeSourceType, isAgent, sessionData)(codeBlock)
                }
              case None =>
                Logger("application").error(agentPrefix(isAgent) + s"Unable to retrieve incomeSourceId from session data for $incomeSourceType on IncomeSourceReportingFrequency page")
                Future.successful(errorHandler(isAgent).showInternalServerError())
            }
        } yield {
          result
        }
      }
    }
  }
}