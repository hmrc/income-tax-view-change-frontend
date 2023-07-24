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
import models.calculationList.CalculationListResponseModel
import models.incomeSourceDetails.viewmodels.{ViewBusinessDetailsViewModel, ViewLatencyDetailsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, LatencyDetails}
import play.api.Logger
import play.api.mvc._
import services.{CalculationListService, DateService, ITSAStatusService, IncomeSourceDetailsService}
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
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val calculationListService: CalculationListService)
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
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ViewBusinessDetailsViewModel] = {
    val desiredIncomeSource: BusinessDetailsModel = sources.businesses
      .filterNot(_.isCeased)
      .filter(_.incomeSourceId.getOrElse(throw new MissingFieldException("incomeSourceId missing")) == id)
      .head

    val isTaxYearOneCrystallised: Future[Option[Boolean]]  = calculationListService.isTaxYearCrystallised(desiredIncomeSource.latencyDetails.get.taxYear1.toInt)
    val isTaxYearTwoCrystallised: Future[Option[Boolean]] = calculationListService.isTaxYearCrystallised(desiredIncomeSource.latencyDetails.get.taxYear2.toInt)
    val istaStatus: Future[Boolean] = itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear

    for {
      i <- isTaxYearOneCrystallised
      j <- isTaxYearTwoCrystallised
      k <- istaStatus
    } yield (
      ViewBusinessDetailsViewModel(
        incomeSourceId = desiredIncomeSource.incomeSourceId.getOrElse(throw new MissingFieldException("Missing incomeSourceId field")),
        tradingName = desiredIncomeSource.tradingName,
        tradingStartDate = desiredIncomeSource.tradingStartDate,
        address = desiredIncomeSource.address,
        businessAccountingMethod = desiredIncomeSource.cashOrAccruals,
        itsaHasMandatedOrVoluntaryStatusCurrentYear = i,
        taxYearOneCrystallised = j,
        taxYearTwoCrystallised = Option(k),
        latencyDetails = Option(ViewLatencyDetailsViewModel(
          latencyEndDate = desiredIncomeSource.latencyDetails.get.latencyEndDate,
          taxYear1 = desiredIncomeSource.latencyDetails.get.taxYear1.toInt,
          latencyIndicator1 = desiredIncomeSource.latencyDetails.get.latencyIndicator1,
          taxYear2 = desiredIncomeSource.latencyDetails.get.taxYear2.toInt,
          latencyIndicator2 = desiredIncomeSource.latencyDetails.get.latencyIndicator2)
      ))
    )
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, id: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      getViewIncomeSourceChosenViewModel(sources = sources, id = id).flatMap{value =>
        Future.successful(Ok(view(viewModel = value,
          isAgent = isAgent,
          backUrl = backUrl
        )))
      }
    }
  }
}