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
import controllers.agent.utils.SessionKeys.businessAccountingMethod
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys.{addBusinessAccountingMethod, addBusinessAddressLine1, addBusinessPostalCode, businessName, businessStartDate, businessTrade, origin}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import services._
import play.api.Logger
import play.api.mvc._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.add.CheckBusinessDetails

import java.net.URI
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
                                             val retrieveBtaNavBar: NavBarPredicate,
                                               val businessDetailsService: CreateBusinessDetailsService)
                                            (implicit val ec: ExecutionContext,
                                             implicit override val mcc: MessagesControllerComponents,

                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  lazy val businessAddressUrl: String = controllers.routes.AddBusinessAddressController.show().url
  lazy val businessAccountingMethodUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.show().url

  lazy val agentBusinessAddressUrl: String = controllers.routes.AddBusinessAddressController.showAgent().url
  lazy val agentBusinessAccountingMethodUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.showAgent().url

  private def getBackURL(referer: Option[String], origin: Option[String]): String = {
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

  def getDetails(implicit user: MtdItUser[_]): Either[Throwable, CheckBusinessDetailsViewModel] = {

    case class MissingKey(msg: String)

    val errors: Seq[String] = Seq(
      user.session.data.get(businessName).orElse(Option(MissingKey("MissingKey: addBusinessName"))),
      user.session.data.get(businessStartDate).orElse(Option(MissingKey("MissingKey: addBusinessStartDate"))),
      user.session.data.get(businessTrade).orElse(Option(MissingKey("MissingKey: addBusinessTrade"))),
      user.session.data.get(addBusinessAddressLine1).orElse(Option(MissingKey("MissingKey: addBusinessAddressLine1"))),
      user.session.data.get(addBusinessPostalCode).orElse(Option(MissingKey("MissingKey: addBusinessPostalCode")))
    ).collect {
      case Some(MissingKey(msg)) => MissingKey(msg)
    }.map(e => e.msg)


    val result: Option[CheckBusinessDetailsViewModel] = for {
        businessName <- user.session.data.get(businessName)
        businessStartDate <- user.session.data.get(businessStartDate).map(LocalDate.parse)
        businessTrade <- user.session.data.get(businessTrade)
        businessAddressLine1 <- user.session.data.get(addBusinessAddressLine1)
        businessPostalCode <- user.session.data.get(addBusinessPostalCode)
    } yield {
      val businessAccountingMethod = user.session.data.get(addBusinessAccountingMethod)

      CheckBusinessDetailsViewModel(
        Some(businessName),
        Some(businessStartDate),
        Some(businessTrade),
        Some(businessAddressLine1),
        Some(businessPostalCode),
        businessAccountingMethod
      )
    }

    result match {
      case Some(checkBusinessDetailsViewModel) =>
        Right(checkBusinessDetailsViewModel)
      case None =>
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }


  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getDetails(user) match {
          case Right(viewModel) =>
            Ok(checkBusinessDetails(
              viewModel,
              isAgent,
              backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER), origin)
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
      getDetails(user).toOption match {
        case Some(viewModel: CheckBusinessDetailsViewModel) =>
          businessDetailsService.createBusinessDetails(viewModel).map {
          case Left(ex) => Logger("application").error(
            s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()

          case Right(IncomeSource(id)) =>
            Redirect(controllers.incomeSources.add.routes.AddBusinessReportingMethod.show().url + s"?id=$id").withNewSession
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
            getDetails(mtdItUser).toOption match {
              case Some(viewModel: CheckBusinessDetailsViewModel) =>
                businessDetailsService.createBusinessDetails(viewModel).map {
                  case Left(ex) => Logger("application").error(
                    s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
                    itvcErrorHandler.showInternalServerError()

                  case Right(IncomeSource(id)) =>
                    Redirect(controllers.incomeSources.add.routes.AddBusinessReportingMethod.showAgent().url + s"?id=$id").withNewSession
                }
              case None => Logger("application").error(
                s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
                Future.successful(itvcErrorHandler.showInternalServerError())
            }
        }
  }

}