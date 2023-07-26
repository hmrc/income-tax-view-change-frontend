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

package controllers.incomeSources.manage

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.manage.{ManageIncomeSources, ManageObligations}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val obligationsView: ManageObligations,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val ec: ExecutionContext,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def showSelfEmployment(taxYear: String, changeTo: String, id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
       mode = "SE",
        isAgent = false,
        taxYear,
        changeTo,
        id
      )
  }
  def showAgentSelfEmployment(taxYear: String, changeTo: String, id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = "SE",
              isAgent = true,
              taxYear,
              changeTo,
              id
            )
        }
  }

  def showUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        mode = "UK",
        isAgent = false,
        taxYear,
        changeTo,
        ""
      )
  }
  def showAgentUKProperty(taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = "UK",
              isAgent = true,
              taxYear,
              changeTo,
              ""
            )
        }
  }

  def showForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        mode = "FP",
        isAgent = false,
        taxYear,
        changeTo,
        ""
      )
  }
  def showAgentForeignProperty(taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = "FP",
              isAgent = true,
              taxYear,
              changeTo,
              ""
            )
        }
  }

  def handleRequest(mode: String, isAgent: Boolean, taxYear: String, changeTo: String, incomeSourceId: String)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      if (isAgent) Future.successful(Redirect(controllers.routes.HomeController.showAgent))
      else Future.successful(Redirect(controllers.routes.HomeController.show()))
    }
    else {
      val backUrl: String = if(isAgent) controllers.incomeSources.manage.routes.ManageConfirmController.showAgent().url else controllers.incomeSources.manage.routes.ManageConfirmController.show().url
      val postUrl: Call = if (isAgent) controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit() else controllers.incomeSources.manage.routes.ManageObligationsController.submit()

      if (mode == "SE" && !user.incomeSources.businesses.exists(x => x.incomeSourceId.contains(incomeSourceId))) {
        Logger("application").error(
          s"[BusinessAddedObligationsController][handleRequest] - unable to find incomeSource by id: $incomeSourceId ")
        if (isAgent) Future(itvcErrorHandlerAgent.showInternalServerError())
        else Future(itvcErrorHandler.showInternalServerError())
      }
      else{
        val addedBusinessName: String = if (mode == "SE") {
          val businessDetailsParams = for {
            addedBusiness <- user.incomeSources.businesses.find(x => x.incomeSourceId.contains(incomeSourceId))
            businessName <- addedBusiness.tradingName
          } yield (addedBusiness, businessName)
          businessDetailsParams match {
            case Some((_, name)) => name
            case None => "Not Found"
          }
        }
        else {
          if (mode == "UK") "UK property"
          else "Foreign property"
        }

        //sanity check on taxYear and changeTo?

        getIncomeSourceId(mode, incomeSourceId) match {
          case Left(error) => Logger("application").error(
            s"[BusinessAddedObligationsController][handleRequest] - ${error.getMessage}")
            if (isAgent) Future(itvcErrorHandlerAgent.showInternalServerError())
            else Future(itvcErrorHandler.showInternalServerError())
          case Right(value) =>
            nextUpdatesService.getObligationsViewModel(value, showPreviousTaxYears = false) map { viewModel =>
              if (isAgent) Ok(obligationsView(viewModel, mode, addedBusinessName, taxYear, changeTo, isAgent, backUrl, postUrl))
              else Ok(obligationsView(viewModel, mode, addedBusinessName, taxYear, changeTo, isAgent, backUrl, postUrl))
          }
        }
      }
    }
  }

  def getIncomeSourceId(mode: String, id: String)(implicit user: MtdItUser[_]): Either[Throwable, String] = {
    mode match {
      case "SE" => Right(id)
      case "UK" => user.incomeSources.properties.find(x => x.isUkProperty) match {
        case Some(value) => Right(value.incomeSourceId.getOrElse(""))
        case None => Left(new Error("Failed to find incomeSource Id"))
      }
      case "FP" => user.incomeSources.properties.find(x => x.isForeignProperty) match {
        case Some(value) => Right(value.incomeSourceId.getOrElse(""))
        case None => Left(new Error("Failed to find incomeSource Id"))
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show()))
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent()))
        }
  }
}