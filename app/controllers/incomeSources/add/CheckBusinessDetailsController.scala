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
import controllers.agent.utils.SessionKeys
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.addBusinessStartDate
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import play.api.Logger
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.CheckBusinessDetails

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckBusinessDetailsController @Inject()(val checkBusinessDetails: CheckBusinessDetails,
                                             val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val retrieveBtaNavBar: NavBarPredicate)
                                            (implicit val ec: ExecutionContext,
                                             implicit override val mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.routes.HomeController.showAgent.url
            )
        }
  }

  def getDetails(implicit user: MtdItUser[_]): Either[Throwable, CheckBusinessDetailsViewModel] = {

    case class MissingKey(msg: String)

    val errors: Seq[String] = Seq(
      user.session.data.get("addBusinessName").orElse(Option(MissingKey("MissingKey: addBusinessName"))),
      user.session.data.get("addBusinessStartDate").orElse(Option(MissingKey("MissingKey: addBusinessStartDate"))),
      user.session.data.get("addBusinessTrade").orElse(Option(MissingKey("MissingKey: addBusinessTrade"))),
      user.session.data.get("addBusinessAddressLine1").orElse(Option(MissingKey("MissingKey: addBusinessAddressLine1"))),
      user.session.data.get("addBusinessPostalCode").orElse(Option(MissingKey("MissingKey: addBusinessAccountingMethod"))),
    ).collect {
      case Some(MissingKey(msg)) => MissingKey(msg)
    }.map(e => e.msg)

    val result: Option[CheckBusinessDetailsViewModel] = for {
      businessName <- user.session.data.get("addBusinessName")
      businessStartDate <- user.session.data.get("addBusinessStartDate").map(LocalDate.parse)
      businessTrade <- user.session.data.get("addBusinessTrade")
      businessAddressLine1 <- user.session.data.get("addBusinessAddressLine1")
      businessPostalCode <- user.session.data.get("addBusinessPostalCode")
      businessAccountingMethod <- user.session.data.get("addBusinessAccountingMethod")
    } yield CheckBusinessDetailsViewModel(
      Some(businessName),
      Some(businessStartDate),
      Some(businessTrade),
      Some(businessAddressLine1),
      Some(businessPostalCode),
      Some(businessAccountingMethod)
    )

    result match {
      case Some(checkBusinessDetailsViewModel) =>
        Right(checkBusinessDetailsViewModel)
      case None =>
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }


  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getDetails(user) match {
          case Right(viewModel) =>
            Ok(checkBusinessDetails(
              viewModel,
              isAgent,
              backUrl
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

  def changeBusinessName(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessNameAgent(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessStartDate(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessStartDateAgent(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessTrade(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessTradeAgent(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessAddress(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessAddressAgent(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessAccountingMethod(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
      )
  }

  def changeBusinessAccountingMethodAgent(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = true,
        backUrl = controllers.routes.HomeController.show().url
      )
  }
}