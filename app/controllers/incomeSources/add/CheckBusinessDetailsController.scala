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
import models.createIncomeSource.CreateIncomeSourcesResponse
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils.{getBusinessDetailsFromSession, removeIncomeSourceDetailsFromSession}
import views.html.incomeSources.add.CheckBusinessDetails

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckBusinessDetailsController @Inject()(val checkBusinessDetails: CheckBusinessDetails,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               val retrieveNino: NinoPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val businessDetailsService: CreateBusinessDetailsService)
                                              (implicit val ec: ExecutionContext,
                                               implicit override val mcc: MessagesControllerComponents,
                                               val appConfig: FrontendAppConfig,
                                               implicit val itvcErrorHandler: ItvcErrorHandler,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with FeatureSwitching {

  lazy val businessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show().url
  lazy val agentBusinessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent().url

  lazy val businessAccountingMethodUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.show().url
  lazy val agentBusinessAccountingMethodUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.showAgent().url

  private def getBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(url) if url.equals(businessAccountingMethodUrl) => businessAccountingMethodUrl
      case _ => businessAddressUrl
    }
  }

  private def getAgentBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(`agentBusinessAccountingMethodUrl`) => agentBusinessAccountingMethodUrl
      case _ => agentBusinessAddressUrl
    }
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true
            )
        }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val backUrl: String = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER))
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.CheckBusinessDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit()

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getBusinessDetailsFromSession(user) match {
          case Right(viewModel) =>
            Ok(checkBusinessDetails(
              viewModel,
              postAction = postAction,
              isAgent,
              backUrl = backUrl
            ))
          case Left(ex) =>
            if (isAgent) {
              Logger("application").error(
                s"[Agent][CheckBusinessDetailsController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandlerAgent.showInternalServerError()
            } else {
              Logger("application").error(
                s"[CheckBusinessDetailsController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()
            }
        }
      }
    }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      getBusinessDetailsFromSession(user).toOption match {
        case Some(viewModel: CheckBusinessDetailsViewModel) =>
          businessDetailsService.createBusinessDetails(viewModel).map {
            case Left(ex) => Logger("application").error(
              s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()

            case Right(CreateIncomeSourcesResponse(id)) =>
              removeIncomeSourceDetailsFromSession(user)
              Redirect(controllers.incomeSources.add.routes.BusinessReportingMethodController.show(id).url)
          }
        case None => Logger("application").error(
          s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            getBusinessDetailsFromSession(mtdItUser).toOption match {
              case Some(viewModel: CheckBusinessDetailsViewModel) =>
                businessDetailsService.createBusinessDetails(viewModel).map {
                  case Left(ex) => Logger("application").error(
                    s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
                    itvcErrorHandler.showInternalServerError()

                  case Right(CreateIncomeSourcesResponse(id)) =>
                    removeIncomeSourceDetailsFromSession(mtdItUser)
                    Redirect(controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(id).url)
                }
              case None => Logger("application").error(
                s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
                Future.successful(itvcErrorHandler.showInternalServerError())
            }
        }
  }

}