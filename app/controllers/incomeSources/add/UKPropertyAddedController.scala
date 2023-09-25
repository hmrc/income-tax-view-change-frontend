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

package controllers.incomeSources.add

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.UkProperty
import exceptions.MissingSessionKey
import forms.utils.SessionKeys
import forms.utils.SessionKeys.incomeSourceId
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateService, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceAddedObligations

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UKPropertyAddedController @Inject()(val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val dateService: DateService,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val nextUpdatesService: NextUpdatesService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val sessionService: SessionService,
                                          val view: IncomeSourceAddedObligations)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def getBackUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent().url else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show().url
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true)
        }
  }

  def getUKPropertyStartDate(incomeSourceId: String)(implicit user: MtdItUser[_]): Option[LocalDate] = {
    for {
      newlyAddedUKProperty <- user.incomeSources.properties.find(incomeSource =>
        incomeSource.incomeSourceId.equals(incomeSourceId) && incomeSource.isUkProperty
      )
      startDate <- newlyAddedUKProperty.tradingStartDate
    } yield startDate
  }

  def handleRequest(isAgent: Boolean)(implicit messages: Messages, user: MtdItUser[_]): Future[Result] = {
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    sessionService.get(SessionKeys.incomeSourceId).flatMap {
      case Right(incomeSourceIdMayBe) =>
        incomeSourceIdMayBe match {
          case Some(incomeSourceId) =>
            withIncomeSourcesFS {
              val backUrl = getBackUrl(isAgent)
              val UKPropertyStartDate = getUKPropertyStartDate(incomeSourceId)

              UKPropertyStartDate match {
                case Some(startDate) =>
                  val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
                  nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears).map { viewModel =>
                    Ok(view(sources = viewModel, isAgent = isAgent, backUrl = backUrl, incomeSourceType = UkProperty)(messages, user))
                  }
                case None =>
                  Logger("application").error(
                    s"[UKPropertyAddedController][handleRequest] - unable to find incomeSource by id: $incomeSourceId")
                  Future.successful(errorHandler.showInternalServerError())
              }
            }
          case None => Future.failed(MissingSessionKey(incomeSourceId))
        }
      case Left(exception) => Future.failed(exception)
    }.recover {
      case ex: Exception =>
        Logger("application").error(
          s"[UKPropertyReportingMethodController][handleRequest]: Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }
}
