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
import config.featureswitch.{FeatureSwitching, IncomeSources, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import exceptions.MissingFieldException
import models.incomeSourceDetails.viewmodels.ViewBusinessDetailsViewModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, LatencyDetails}
import play.api.Logger
import play.api.mvc._
import services.{DateService, ITSAStatusService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.manage.BusinessManageDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ManageSelfEmploymentController @Inject()(val view: BusinessManageDetails,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               val retrieveNino: NinoPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val itsaStatusService: ITSAStatusService,
                                               val dateService: DateService,
                                               val retrieveBtaNavBar: NavBarPredicate)
                                              (implicit val ec: ExecutionContext,
                                             implicit override val mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url,
        id = id
      )
  }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url,
              id = id
            )
        }
  }

  def getViewIncomeSourceChosenViewModel(sources: IncomeSourceDetailsModel, id: String)
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ViewBusinessDetailsViewModel]] = {
    val desiredIncomeSource: BusinessDetailsModel = sources.businesses
      .filterNot(_.isCeased)
      .filter(_.incomeSourceId.getOrElse(throw new MissingFieldException("incomeSourceId missing")) == id)
      .head

    val latencyDetails: Option[LatencyDetails] = desiredIncomeSource.latencyDetails
    latencyDetails match {
      case Some(x) =>
        val currentTaxYear = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
        x match {
          case LatencyDetails(_, _, _, taxYear2, _) if // use BusinessReportingMethodController
        }
    }
    Try {
      ViewBusinessDetailsViewModel(
        incomeSourceId = desiredIncomeSource.incomeSourceId.getOrElse(throw new MissingFieldException("Missing incomeSourceId field")),
        tradingName = desiredIncomeSource.tradingName,
        tradingStartDate = desiredIncomeSource.tradingStartDate,
        address = desiredIncomeSource.address,
        businessAccountingMethod = desiredIncomeSource.cashOrAccruals,
        itsaHasMandatedOrVoluntaryStatusCurrentYear = itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
      )
    }.toEither
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, id: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      Future {
        getViewIncomeSourceChosenViewModel(sources = sources, id = id) match {
          case Right(viewModel: ViewBusinessDetailsViewModel) =>
            Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          case Left(ex: Exception) =>
            if (isAgent) {
              Logger("application").error(
                s"[Agent][ManageSelfEmploymentController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandlerAgent.showInternalServerError()
            } else {
              Logger("application").error(
                s"[ManageSelfEmploymentController][handleRequest] - Error: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()
            }
        }
      }
    }
  }
}