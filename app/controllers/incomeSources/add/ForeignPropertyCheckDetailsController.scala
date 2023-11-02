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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckPropertyViewModel
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.ForeignPropertyCheckDetails

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
                                                      val businessDetailsService: CreateBusinessDetailsService,
                                                      val sessionService: SessionService
                                                     )
                                                     (implicit val ec: ExecutionContext,
                                                      implicit override val mcc: MessagesControllerComponents,
                                                      val appConfig: FrontendAppConfig,
                                                      implicit val itvcErrorHandler: ItvcErrorHandler,
                                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  lazy val foreignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty).url
  lazy val agentForeignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty).url
  lazy val backUrlIndividual: String = foreignPropertyAccountingMethodUrl
  lazy val backUrlAgent: String = agentForeignPropertyAccountingMethodUrl

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    val backUrl: String = if (isAgent) backUrlAgent else backUrlIndividual
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submitAgent() else {
      controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit()
    }
    getDetails(user).map{
      case Right(viewModel) =>
        Ok(checkForeignPropertyDetails(
          viewModel,
          postAction = postAction,
          isAgent,
          backUrl = backUrl
        ))
      case Left(ex) =>
        Logger("application").error(
          s"[ForeignPropertyCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    } recover {
      case ex: Exception =>
        Logger("application").error(
          s"[ForeignPropertyCheckDetailsController][handleRequest] - Error: Unable to construct getCheckPropertyViewModel ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def getDetails(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckPropertyViewModel]] = {

    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, JourneyType(Add, ForeignProperty)).flatMap { startDate: Either[Throwable, Option[LocalDate]] =>
      sessionService.getMongoKeyTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, ForeignProperty)).map { accMethod: Either[Throwable, Option[String]] =>
        val errors: Seq[String] = getErrors(startDate, accMethod)
        val result: Option[CheckPropertyViewModel] = getResult(startDate, accMethod)

        result match {
          case Some(checkForeignPropertyViewModel) =>
            Right(checkForeignPropertyViewModel)
          case None =>
            Left(new IllegalArgumentException(s"Missing required session data: ${errors.map(x => x.mkString(" "))}"))
        }
      }
    }
  }

  def getResult(startDate: Either[Throwable, Option[LocalDate]], accMethod: Either[Throwable, Option[String]]): Option[CheckPropertyViewModel] = {
    (startDate, accMethod) match {
      case (Right(dateMaybe), Right(methodMaybe)) =>
        for {
          foreignPropertyStartDate <- dateMaybe
          cashOrAccrualsFlag <- methodMaybe
        } yield {
          CheckPropertyViewModel(
            tradingStartDate = foreignPropertyStartDate,
            cashOrAccrualsFlag = cashOrAccrualsFlag)
        }
      case (_, _) => None
    }
  }

  def getErrors(startDate: Either[Throwable, Option[LocalDate]], accMethod: Either[Throwable, Option[String]]): Seq[String] = {
    case class MissingKey(msg: String)

    Seq(
      startDate match {
        case Right(nameOpt) => nameOpt match {
          case Some(name) => name
          case None => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
        }
        case Left(_) => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
      },
      accMethod match {
        case Right(nameOpt) => nameOpt match {
          case Some(name) => name
          case None => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
        }
        case Left(_) => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
      }
    ).collect {
      case Some(MissingKey(msg)) => msg
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

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmit(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmit(isAgent = true)
        }
  }

  def handleSubmit(isAgent: Boolean)(implicit user: MtdItUser[AnyContent]): Future[Result] = {
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceNotAddedController.showAgent(ForeignProperty)
    else routes.IncomeSourceNotAddedController.show(ForeignProperty)

    getDetails(user) flatMap {
      case Right(viewModel: CheckPropertyViewModel) =>
        businessDetailsService.createForeignProperty(viewModel).flatMap {
          case Left(ex) =>
            Logger("application").error(
              s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
            Future.successful(Redirect(redirectErrorUrl))

          case Right(CreateIncomeSourceResponse(id)) =>
            sessionService.deleteMongoData(JourneyType(Add, UkProperty))
            Future.successful(
              if (isAgent) Redirect(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(id).url)
              else Redirect(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(id).url)
            ).recover {
              case _: Exception => Redirect(redirectErrorUrl)
            }
        }
      case Left(_) =>
        Logger("application").error(
          s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
        sessionService.deleteMongoData(JourneyType(Add, UkProperty))
        Future.successful(Redirect(redirectErrorUrl))
    }
  }
}
