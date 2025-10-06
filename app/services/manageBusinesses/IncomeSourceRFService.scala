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
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyState
import enums.JourneyType.IncomeSourceJourneyType
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, LatencyDetails, TaxYear, UIJourneySessionData}
import models.itsaStatus.StatusDetail
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{DateService, ITSAStatusService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.JourneyCheckerManageBusinesses

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceRFService @Inject()(val sessionService: SessionService,
                                      val itsaStatusService: ITSAStatusService,
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

  private def onlyOneYearInLatencyPeriod(latencyDetails: Option[LatencyDetails]): Boolean = {
     latencyDetails.exists(_.taxYear2.toInt == currentTy.endYear)
  }

  private def updateChooseTaxYearsModelInSessionData(
                                                      sessionData: UIJourneySessionData,
                                                      incomeSourceReportingFrequencySourceData: IncomeSourceReportingFrequencySourceData
                                                    ): Future[UIJourneySessionData] = {
    val updatedSessionData: UIJourneySessionData = sessionData.copy(incomeSourceReportingFrequencyData = Some(incomeSourceReportingFrequencySourceData))
    sessionService.setMongoData(updatedSessionData).flatMap {
      case true =>
        Future.successful(updatedSessionData)
      case _ =>
        Logger("application").error("incomeSourceReportingFrequencyData status update failed")
        throw new Exception("incomeSourceReportingFrequencyData status update failed")
    }
  }

  private def latencyDetailsCheckToRedirectToCompletionPage(latencyDetails: Option[LatencyDetails]): Boolean = {
    latencyDetails.isEmpty ||
      !latencyDetails.exists(_.isBusinessOrPropertyInLatency(currentTy.endYear)) ||
      onlyOneYearInLatencyPeriod(latencyDetails)
  }

  private def itsaStatusChecksToRedirectToCompletionPage(currentTyStatus: Option[StatusDetail],
                                                         nextTyStatus: Option[StatusDetail]): Boolean = {
    !currentTyStatus.exists(_.isMandatedOrVoluntary) ||
      (nextTyStatus.isEmpty && currentTyStatus.exists(_.isMandatedOrVoluntary) && !currentTyStatus.exists(_.statusReasonRollover)) ||
      (nextTyStatus.isEmpty && !currentTyStatus.exists(_.isMandatedOrVoluntary)) ||
      (nextTyStatus.nonEmpty && !nextTyStatus.exists(_.isMandatedOrVoluntary))
  }

  private def redirectToCompletionPageChecks(
                                        currentTyStatus: Option[StatusDetail],
                                        nextTyStatus: Option[StatusDetail],
                                        latencyDetails: Option[LatencyDetails]
                                      ): Boolean = {
    latencyDetailsCheckToRedirectToCompletionPage(latencyDetails) || itsaStatusChecksToRedirectToCompletionPage(currentTyStatus, nextTyStatus)
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
    withSessionData(incomeSourceJourneyType, journeyState) { sessionData =>

      if (isChange) {
        codeBlock(sessionData)
      } else {
        for {
          statusTaxYearMap <- itsaStatusService.getStatusTillAvailableFutureYears(TaxYear(currentTaxYearEnd - 1, currentTaxYearEnd))
          accountLevelITSAStatusCurrentTaxYear = statusTaxYearMap.get(currentTy)
          accountLevelITSAStatusNextTaxYear = statusTaxYearMap.get(nextTy)
          someIncomeSourceId <- Future(sessionData.addIncomeSourceData.flatMap(_.incomeSourceId))
          result <-
            someIncomeSourceId match {
              case Some(id) =>
                val latencyDetails = user.incomeSources.getLatencyDetails(incomeSourceType, id)
                if (redirectToCompletionPageChecks(accountLevelITSAStatusCurrentTaxYear, accountLevelITSAStatusNextTaxYear, latencyDetails)) {
                  redirectToIncomeSourceAddedPage(isAgent, incomeSourceType)
                } else {
                  updateChooseTaxYearsModelInSessionData(sessionData, IncomeSourceReportingFrequencySourceData(true, true, false, false))
                    .flatMap(codeBlock(_))
                }
              case None =>
                Logger("application").error(
                  agentPrefix(isAgent) + s"Unable to retrieve incomeSourceId from session data for $incomeSourceType on IncomeSourceReportingFrequency page")
                Future.successful(errorHandler(isAgent).showInternalServerError())
            }
        } yield {
          result
        }
      }
    }
  }
}