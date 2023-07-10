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
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, ObligationsRetrievalService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.BusinessAddedObligations

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.Inject
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

class BusinessAddedObligationsController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val obligationsView: BusinessAddedObligations,
                                                   nextUpdatesService: NextUpdatesService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, id)
  }

  def showAgent(id: String): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true, id)
          }
    }

  def handleRequest(isAgent: Boolean, id: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    lazy val backUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(id).url
    lazy val agentBackUrl = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(id).url

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      val businessDetailsParams = for {
        addedBusiness <- user.incomeSources.businesses.find(x => x.incomeSourceId.contains(id))
        businessName <- addedBusiness.tradingName
        startDate <- addedBusiness.tradingStartDate
      } yield (addedBusiness, businessName, startDate)
      businessDetailsParams match {
        case Some((_, businessName, startDate)) =>
          val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
          for {
            maybeViewModel <- nextUpdatesService.getObligationsViewModel(id, businessName, startDate.toString, showPreviousTaxYears)
          } yield maybeViewModel match {
            case  Right(viewModel) =>
              if (isAgent) Ok(obligationsView(businessName, viewModel,
                controllers.incomeSources.add.routes.BusinessAddedObligationsController.agentSubmit(), agentBackUrl, isAgent = true))
              else Ok(obligationsView(businessName, viewModel,
                controllers.incomeSources.add.routes.BusinessAddedObligationsController.submit(), backUrl, isAgent = false))
            case Left(ex) =>
              Logger("application").error(
                s"[BusinessAddedObligationsController][handleRequest] - unable to construct viewModel $ex ")
              // TODO: call correct handler individual/agent
              itvcErrorHandler.showInternalServerError()
          }
        case _ =>
          Logger("application").error(
            s"[BusinessAddedObligationsController][handleRequest] - unable to find incomeSource by id: $id ")
          // TODO: call correct handler individual/agent
          Future(itvcErrorHandler.showInternalServerError())
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

  def agentSubmit: Action[AnyContent] = Authenticated.async {
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
