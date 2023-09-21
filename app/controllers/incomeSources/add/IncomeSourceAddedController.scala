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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceAddedObligations

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedController @Inject()(authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            checkSessionTimeout: SessionTimeoutPredicate,
                                            retrieveNino: NinoPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  def show(incomeSourceId: String, incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceId, incomeSourceType)
  }

  def showAgent(incomeSourceId: String, incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true, incomeSourceId, incomeSourceType)
        }
  }

  private def getBackUrl(incomeSourceId: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    val baseRoute = incomeSourceType match {
      case SelfEmployment =>
        if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent _ else
          controllers.incomeSources.add.routes.BusinessReportingMethodController.show _
      case UkProperty =>
        if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent _ else
          controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show _
      case ForeignProperty =>
        if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent _ else
          controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show _
    }
    baseRoute(incomeSourceId).url
  }

  def getIncomeSourceData(incomeSourceType: IncomeSourceType, incomeSourceId: String)(implicit user: MtdItUser[_]): Option[(LocalDate, Option[String])] = {
    incomeSourceType match {
      case SelfEmployment =>
        user.incomeSources.businesses
          .find(_.incomeSourceId.equals(incomeSourceId))
          .flatMap { addedBusiness =>
            for {
              businessName <- addedBusiness.tradingName
              startDate <- addedBusiness.tradingStartDate
            } yield (startDate, Some(businessName))
          }
      case UkProperty =>
        for {
          newlyAddedProperty <- user.incomeSources.properties.find(incomeSource =>
            incomeSource.incomeSourceId.equals(incomeSourceId) && incomeSource.isUkProperty
          )
          startDate <- newlyAddedProperty.tradingStartDate
        } yield (startDate, None)
      case ForeignProperty =>
        for {
          newlyAddedProperty <- user.incomeSources.properties.find(incomeSource =>
            incomeSource.incomeSourceId.equals(incomeSourceId) && incomeSource.isForeignProperty
          )
          startDate <- newlyAddedProperty.tradingStartDate
        } yield (startDate, None)
    }
  }

  private def handleRequest(isAgent: Boolean, incomeSourceId: String, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val backUrl = getBackUrl(incomeSourceId, isAgent, incomeSourceType)
      getIncomeSourceData(incomeSourceType, incomeSourceId) match {
        case Some(incomeSourceData) =>
          val showPreviousTaxYears: Boolean = incomeSourceData._1.isBefore(dateService.getCurrentTaxYearStart())
          incomeSourceType match {
            case SelfEmployment =>
              incomeSourceData._2 match {
                case Some(businessName) => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
                  Ok(obligationsView(businessName = Some(businessName), sources = viewModel, backUrl = backUrl, isAgent = isAgent, incomeSourceType = SelfEmployment))
                }
                case None => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
                  Ok(obligationsView(sources = viewModel, backUrl = backUrl, isAgent = isAgent, incomeSourceType = SelfEmployment))
                }
              }
            case UkProperty => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
              Ok(obligationsView(viewModel, backUrl, isAgent = isAgent, incomeSourceType = UkProperty))
            }
            case ForeignProperty => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
              Ok(obligationsView(viewModel, backUrl, isAgent = isAgent, incomeSourceType = ForeignProperty))
            }
          }
        case None => Logger("application").error(
          s"[IncomeSourceAddedController][handleRequest] - unable to find incomeSource by id: $incomeSourceId")
          if (isAgent) Future(itvcErrorHandlerAgent.showInternalServerError())
          else Future(itvcErrorHandler.showInternalServerError())
      }
    }
  }


  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

}
