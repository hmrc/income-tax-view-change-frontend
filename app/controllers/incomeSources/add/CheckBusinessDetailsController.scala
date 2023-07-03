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
import forms.utils.SessionKeys._
import models.addIncomeSource.AddIncomeSourceResponse
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import play.api.Logger
import play.api.mvc._
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
        businessAccountingMethod <- user.session.data.get(addBusinessAccountingMethod)
        accountingPeriodEndDate <- user.session.data.get(addBusinessAccountingPeriodEndDate).map(LocalDate.parse)
    } yield {

      CheckBusinessDetailsViewModel(
        businessName = Some(businessName),
        businessStartDate = Some(businessStartDate),
        accountingPeriodEndDate = accountingPeriodEndDate,
        businessTrade = businessTrade,
        businessAddressLine1 = businessAddressLine1,
        businessAddressLine2 = user.session.data.get(addBusinessAddressLine2),
        businessAddressLine3 = user.session.data.get(addBusinessAddressLine3),
        businessAddressLine4 = user.session.data.get(addBusinessAddressLine4),
        businessPostalCode = user.session.data.get(addBusinessPostalCode),
        businessCountryCode = user.session.data.get(addBusinessCountryCode),
        businessAccountingMethod = user.session.data.get(addBusinessAccountingMethod),
        cashOrAccrualsFlag = businessAccountingMethod)
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

    val postAction: Call = { if (isAgent) controllers.incomeSources.add.routes.CheckBusinessDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit() }

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getDetails(user) match {
          case Right(viewModel) =>
            Ok(checkBusinessDetails(
              viewModel,
              postAction = postAction,
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

  private val sessionKeys = Seq(businessName, businessStartDate, addBusinessAccountingPeriodStartDate,
    businessTrade, addBusinessAddressLine1, addBusinessAddressLine2, addBusinessAddressLine3, addBusinessAddressLine4,
    addBusinessPostalCode, addBusinessCountryCode, addBusinessAccountingMethod)

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      getDetails(user).toOption match {
        case Some(viewModel: CheckBusinessDetailsViewModel) =>
          businessDetailsService.createBusinessDetails(viewModel).map {
          case Left(ex) => Logger("application").error(
            s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()

          case Right(AddIncomeSourceResponse(id)) =>
            Redirect(controllers.incomeSources.add.routes.AddBusinessReportingMethod.show().url + s"?id=$id").withSession(user.session -- sessionKeys)
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

                  case Right(AddIncomeSourceResponse(id)) =>
                    Redirect(controllers.incomeSources.add.routes.AddBusinessReportingMethod.showAgent().url + s"?id=$id").withSession(mtdItUser.session -- sessionKeys)
                }
              case None => Logger("application").error(
                s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
                Future.successful(itvcErrorHandler.showInternalServerError())
            }
        }
  }

}