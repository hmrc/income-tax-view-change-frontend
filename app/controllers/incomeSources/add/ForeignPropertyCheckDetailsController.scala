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
import forms.utils.SessionKeys.{businessName, businessStartDate, businessTrade, foreignPropertyStartDate, _}
import models.addIncomeSource.AddIncomeSourceResponse
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckForeignPropertyViewModel}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.add.ForeignPropertyCheckDetails

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyCheckDetailsController @Inject()(val checkForeignPropertyDetails: ForeignPropertyCheckDetails,
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

  lazy val businessAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url
  lazy val agentBusinessAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url
  lazy val backUrlIndividual: String = businessAccountingMethodUrl
  lazy val backUrlAgent: String = agentBusinessAccountingMethodUrl

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val backUrl: String = if (isAgent) backUrlIndividual else backUrlAgent
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit()

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getDetails(user) match {
          case Right(viewModel) =>
            Ok(checkForeignPropertyDetails(
              viewModel,
              postAction = postAction,
              isAgent,
              backUrl = backUrl
            ))
          case Left(ex) =>
            if (isAgent) {
              Logger("application").error(
                s"[Agent][ForeignPropertyCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandlerAgent.showInternalServerError()
            } else {
              Logger("application").error(
                s"[ForeignPropertyCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()
            }
        }
      }
    }
  }

  def getDetails(implicit user: MtdItUser[_]): Either[Throwable, CheckForeignPropertyViewModel] = {

    case class MissingKey(msg: String)

    val errors: Seq[String] = Seq(
      user.session.data.get(foreignPropertyStartDate).orElse(Option(MissingKey("MissingKey: addForeignPropertyStartDate")))
    ).collect {
      case Some(MissingKey(msg)) => MissingKey(msg)
    }.map(e => e.msg)

    val result: Option[CheckForeignPropertyViewModel] = for {
      foreignPropertyStartDate <- user.session.data.get(foreignPropertyStartDate).map(LocalDate.parse)
      foreignPropertyAccountingMethod <- user.session.data.get(addForeignPropertyAccountingMethod)
    } yield {
      CheckForeignPropertyViewModel(
        tradingStartDate = foreignPropertyStartDate,
        cashOrAccrualsFlag = foreignPropertyAccountingMethod)
    }
    result match {
      case Some(checkForeignPropertyViewModel) =>
        Right(checkForeignPropertyViewModel)
      case None =>
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
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

  def submit(): Action[AnyContent] = ???

  def submitAgent: Action[AnyContent] = ???

  }
