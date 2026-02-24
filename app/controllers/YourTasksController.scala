/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import audit.AuditingService
import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.*
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.{DateServiceInterface, ITSAStatusService, NextUpdatesService}
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewUtils.NextUpdatesViewUtils

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourTasksController @Inject()(
                                       newHomeYourTasksView: views.html.NewHomeYourTasksView,
                                       auditingService: AuditingService,
                                       nextUpdatesService: NextUpdatesService,
                                       val itvcErrorHandler: ItvcErrorHandler,
                                       val agentItvcErrorHandler: AgentItvcErrorHandler,
                                       optOutService: OptOutService,
                                       nextUpdatesViewUtils: NextUpdatesViewUtils,
                                       val appConfig: FrontendAppConfig,
                                       val authActions: AuthActions,
                                       val dateService: DateServiceInterface,
                                       val ITSAStatusService: ITSAStatusService,
                                     )
                                   (
                                       implicit mcc: MessagesControllerComponents,
                                       val ec: ExecutionContext
                                     )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  val getCurrentTaxYearEnd: Int = dateService.getCurrentTaxYearEnd
  val getCurrentDate: LocalDate = dateService.getCurrentDate
  val currentTaxYear = TaxYear(getCurrentTaxYearEnd - 1, getCurrentTaxYearEnd)

  private def hasAnyIncomeSource(action: => Future[Result])(implicit user: MtdItUser[_], origin: Option[String]): Future[Result] = {

    if (user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      action
    } else {
      Future.successful(Ok("TODO What should we render if there is no next updates"))
    }
  }

  private def getNextUpdates(backUrl: Call, isAgent: Boolean, errorHandler: ShowInternalServerError, origin: Option[String] = None)
                            (implicit user: MtdItUser[_]): Future[Result] = {

    hasAnyIncomeSource {
      for {
        nextUpdates <- nextUpdatesService.getOpenObligations().map {
          case obligations: ObligationsModel => obligations
          case _ => ObligationsModel(Nil)
        }
        isR17ContentEnabled = isEnabled(OptInOptOutContentUpdateR17)
        isOptOutEnabled = isEnabled(OptOutFs)

        result <- nextUpdates.obligations match {
          case Nil =>
            Logger("application").warn(s"${if (isAgent) "[Agent]"} No open obligations found for user.")
            Future.successful(errorHandler.showInternalServerError())
          case _ =>
            auditNextUpdates(user, isAgent, origin)

            val optOutSetup = {
              for {
                (checks, optOutOneYearViewModel, optOutProposition) <- optOutService.nextUpdatesPageChecksAndProposition()
                currentITSAStatus <- getCurrentITSAStatus(currentTaxYear)
                (nextQuarterlyUpdateDueDate, nextTaxReturnDueDate) <- getNextDueDatesIfEnabled()
                nextUpdatesDueDates <- getNextUpdatesDueDates()
                viewModel = nextUpdatesService.getNextUpdatesViewModel(nextUpdates, isR17ContentEnabled)
              } yield {
                val whatTheUserCanDoContent = if (isOptOutEnabled) nextUpdatesViewUtils.whatTheUserCanDo(optOutOneYearViewModel, isAgent) else None
              //TODO check what would we need from the nex Updates for Your tasks section
               /* Ok(
                  nextUpdatesOptOutView(
                    viewModel = viewModel,
                    checks = checks,
                    optOutProposition = optOutProposition,
                    backUrl = backUrl.url,
                    isAgent = isAgent,
                    isSupportingAgent = user.isSupportingAgent,
                    origin = origin,
                    whatTheUserCanDo = whatTheUserCanDoContent,
                    optInOptOutContentR17Enabled = isR17ContentEnabled,
                    taxYearStatusesCyNy = (optOutProposition.currentTaxYear.status, optOutProposition.nextTaxYear.status))
                )*/

                val nextUpdatesTileViewModel: NextUpdatesTileViewModel =
                  NextUpdatesTileViewModel(
                    dueDates = nextUpdatesDueDates,
                    currentDate = dateService.getCurrentDate,
                    isReportingFrequencyEnabled = isEnabled(ReportingFrequencyPage),
                    showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
                    currentYearITSAStatus = currentITSAStatus,
                    nextQuarterlyUpdateDueDate = nextQuarterlyUpdateDueDate,
                    nextTaxReturnDueDate = nextTaxReturnDueDate
                  )

                Ok(
                  newHomeYourTasksView(
                    nextUpdatesTileViewModel = nextUpdatesTileViewModel,
                    viewModel = viewModel,
                    isAgent = isAgent,
                    origin = origin,
                    yourTasksUrl = yourTasksUrl(origin, isAgent),
                    recentActivityUrl = recentActivityUrl(origin, isAgent),
                    overViewUrl = overviewUrl(origin, isAgent),
                    helpUrl = helpUrl(origin, isAgent)
                  )
                )
              }
            }.recoverWith {
              case ex =>
                Logger("application").error(s"Failed to retrieve quarterly reporting content checks: ${ex.getMessage}")
                Future.successful(errorHandler.showInternalServerError())
            }
            optOutSetup
        }
      } yield result
    }(user, origin)
  }


  def show(origin: Option[String] = None, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    getNextUpdates(
      backUrl = controllers.routes.HomeController.show(origin),
      isAgent = false,
      errorHandler = itvcErrorHandler,
      origin = origin
    )
  }

  private def auditNextUpdates[A](user: MtdItUser[A], isAgent: Boolean, origin: Option[String])(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    if (isAgent) {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.showAgent().url))
    } else {
      auditingService.extendedAudit(NextUpdatesAuditModel(user), Some(controllers.routes.NextUpdatesController.show(origin).url))
    }

  def yourTasksUrl(origin: Option[String] = None, isAgent: Boolean): String = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show(origin).url

  def recentActivityUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleRecentActivity(origin, isAgent).url

  def overviewUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleOverview(origin, isAgent).url

  def helpUrl(origin: Option[String] = None, isAgent: Boolean): String = controllers.routes.HomeController.handleHelp(origin, isAgent).url

  private def getCurrentITSAStatus(taxYear: TaxYear)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[ITSAStatus.ITSAStatus] = {
    ITSAStatusService.getStatusTillAvailableFutureYears(taxYear.previousYear).map(_.view.mapValues(_.status)
      .toMap
      .withDefaultValue(ITSAStatus.NoStatus)
    ).map(detail => detail(taxYear))
  }

  private def getNextDueDatesIfEnabled()
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[(Option[LocalDate], Option[LocalDate])] = {
    if (isEnabled(OptInOptOutContentUpdateR17)) {
      nextUpdatesService.getNextDueDates()
    } else {
      Future.successful((None, None))
    }
  }

  private def getNextUpdatesDueDates()
                                    (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Seq[LocalDate]] = {
    nextUpdatesService.getDueDates().flatMap {
      case Right(nextUpdatesDueDates: Seq[LocalDate] )  => Future.successful(nextUpdatesDueDates)
      case Left(ex) =>
        Logger("application").error(s"Unable to get next updates ${ex.getMessage} - ${ex.getCause}")
        Future.successful(Seq.empty[LocalDate])
    }
  }

}