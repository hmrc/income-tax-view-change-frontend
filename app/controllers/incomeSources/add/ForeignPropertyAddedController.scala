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

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.ForeignPropertyAddedObligations

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.Inject
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

class ForeignPropertyAddedController @Inject()(val foreignPropertyObligationsView: ForeignPropertyAddedObligations,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               val retrieveNino: NinoPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val nextUpdatesService: NextUpdatesService)
                                              (implicit val appConfig: FrontendAppConfig,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  def show(incomeSourceId: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceId)
  }

  def showAgent(incomeSourceId: String): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true, incomeSourceId)
          }
    }

  def handleRequest(isAgent: Boolean, incomeSourceId: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    lazy val backUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(incomeSourceId).url
    lazy val agentBackUrl = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(incomeSourceId).url

    if (isDisabled(IncomeSources)) {
      if (isAgent) {
        Future.successful(Redirect(controllers.routes.HomeController.showAgent))
      } else {
        Future.successful(Redirect(controllers.routes.HomeController.show()))
      }
    } else {
      val foreignPropertyDetailsParams = for {
        addedForeignProperty <- user.incomeSources.properties.filter(_.isForeignProperty).find(x => x.incomeSourceId.contains(incomeSourceId))
        startDate <- addedForeignProperty.tradingStartDate
      } yield (addedForeignProperty, startDate)
      foreignPropertyDetailsParams match {
        case Some((_, startDate)) =>
          val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
          nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
            if (isAgent) Ok(foreignPropertyObligationsView(viewModel,
              controllers.incomeSources.add.routes.ForeignPropertyAddedController.submitAgent(), agentBackUrl, isAgent = true))
            else Ok(foreignPropertyObligationsView(viewModel,
              controllers.incomeSources.add.routes.ForeignPropertyAddedController.submit(), backUrl, isAgent = false))
          }
        case _ =>
          Logger("application").error(
            s"[ForeignPropertyAddedObligationsController][handleRequest] - unable to find incomeSource by id: $incomeSourceId")
          if (isAgent) Future(itvcErrorHandlerAgent.showInternalServerError())
          else Future(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future.successful {
        Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future.successful {
              Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
            }
        }
  }

}
