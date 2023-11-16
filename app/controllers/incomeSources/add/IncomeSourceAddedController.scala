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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
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
                                            val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: IncomeSourceAddedObligations,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  def show(incomeSourceId: String, incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
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

  private def handleRequest(isAgent: Boolean, incomeSourceId: String, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      incomeSourceDetailsService.getIncomeSourceFromUser(incomeSourceType, incomeSourceId) match {
        case Some((startDate, businessName)) =>
          val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
          handleSuccess(incomeSourceId, incomeSourceType, startDate, businessName, showPreviousTaxYears, isAgent)
        case None => Logger("application").error(
          s"${if (isAgent) "[Agent]"}" + s"[IncomeSourceAddedController][handleRequest] - unable to find incomeSource by id: $incomeSourceId, IncomeSourceType: $incomeSourceType")
          if (isAgent) Future(itvcErrorHandlerAgent.showInternalServerError())
          else Future(itvcErrorHandler.showInternalServerError())
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting IncomeSourceAdded page: ${ex.getMessage}, IncomeSourceType: $incomeSourceType")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
        else itvcErrorHandler.showInternalServerError()
    }
  }

  def handleSuccess(incomeSourceId: String, incomeSourceType: IncomeSourceType, startDate: LocalDate, businessName: Option[String], showPreviousTaxYears: Boolean, isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    incomeSourceType match {
      case SelfEmployment =>
        businessName match {
          case Some(businessName) => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
            Ok(obligationsView(businessName = Some(businessName), sources = viewModel, isAgent = isAgent, incomeSourceType = SelfEmployment))
          }
          case None => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
            Ok(obligationsView(sources = viewModel, isAgent = isAgent, incomeSourceType = SelfEmployment))
          }
        }
      case UkProperty => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
        Ok(obligationsView(viewModel, isAgent = isAgent, incomeSourceType = UkProperty))
      }
      case ForeignProperty => nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears) map { viewModel =>
        Ok(obligationsView(viewModel, isAgent = isAgent, incomeSourceType = ForeignProperty))
      }
    }
  }


  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
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
