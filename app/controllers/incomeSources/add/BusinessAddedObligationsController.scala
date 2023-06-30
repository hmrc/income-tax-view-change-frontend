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
import controllers.routes
import forms.utils.SessionKeys
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, NextUpdatesResponseModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.BusinessAddedObligations

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, CanAwait, ExecutionContext, Future}

class BusinessAddedObligationsController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   nextUpdatesService: NextUpdatesService,
                                                   val obligationsView: BusinessAddedObligations)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching{

  lazy val backUrl = controllers.incomeSources.add.routes.AddBusinessReportingMethod.show().url
  lazy val agentBackUrl = controllers.incomeSources.add.routes.AddBusinessReportingMethod.showAgent().url

  def show(id: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, id)
  }

  def showAgent(id: Option[String]): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true, id)
          }
    }

  def handleRequest(isAgent: Boolean, id: Option[String])(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      id match {
        case None => Logger("application").error(
          s"[BusinessAddedObligationsController][handleRequest] - Error: No id supplied by reporting method page")
          Future(itvcErrorHandler.showInternalServerError())
        case Some(value) => {
          val businessName = user.incomeSources.businesses.find(x => x.incomeSourceId.contains(value)).get.tradingName
          val dates: Seq[DatesModel] = Await.result(nextUpdatesService.getNextUpdates() map {
            case NextUpdatesErrorModel(code, message) => Logger("application").error(
              s"[BusinessAddedObligationsController][handleRequest] - Error: $message, code $code")
              itvcErrorHandler.showInternalServerError()
              Seq.empty
            case NextUpdateModel(start, end, due, obligationType, dateReceived, periodKey) =>
              Seq(DatesModel(Some(start), Some(end), Some(due)))
            case ObligationsModel(obligations) =>
              obligations.flatMap(obligation => obligation.obligations.map(x => DatesModel(Some(x.start), Some(x.end), Some(x.due))))
          }, Duration(100, MILLISECONDS))
          Future {
            if (isAgent) Ok(obligationsView(ObligationsViewModel(businessName, dates), controllers.incomeSources.add.routes.BusinessAddedObligationsController.agentSubmit(), agentBackUrl, true))
            else Ok(obligationsView(ObligationsViewModel(businessName, dates), controllers.incomeSources.add.routes.BusinessAddedObligationsController.submit(), backUrl, false))
          }
        }
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request => ???
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser => ???
        }
  }

}
