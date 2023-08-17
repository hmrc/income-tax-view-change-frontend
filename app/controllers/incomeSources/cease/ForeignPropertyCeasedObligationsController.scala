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

package controllers.incomeSources.cease

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment}
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ForeignPropertyCeasedObligationsController @Inject()(val authenticate: AuthenticationPredicate,
                                                           val authorisedFunctions: AuthorisedFunctions,
                                                           val checkSessionTimeout: SessionTimeoutPredicate,
                                                           val retrieveNino: NinoPredicate,
                                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                           val retrieveBtaNavBar: NavBarPredicate,
                                                           val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                           val obligationsView: IncomeSourceCeasedObligations,
                                                           val nextUpdatesService: NextUpdatesService,
                                                           val dateService: DateServiceInterface)
                                                          (implicit val appConfig: FrontendAppConfig,
                                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                           val itvcErrorHandler: ItvcErrorHandler,
                                                           mcc: MessagesControllerComponents,
                                                           val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  private def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      val foreignPropertyIncomeSources = user.incomeSources.properties.filter(_.isForeignProperty)
      foreignPropertyIncomeSources.headOption match {
        case Some(property) =>
          nextUpdatesService.getObligationsViewModel(property.incomeSourceId, showPreviousTaxYears = false) map { viewModel =>
            Ok(obligationsView(
              businessName = None,
              sources = viewModel,
              backUrl = None,
              isAgent = isAgent,
              incomeSourceType = ForeignProperty))
          }
        case None =>
          Logger("application").error(s"${if(isAgent) "[Agent]"}[ForeignPropertyCeasedObligationsController][handleRequest]:Failed to retrieve incomeSourceId for foreign property")
          Future.successful(errorHandler.showInternalServerError())
      }

    }
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true)
        }
  }
}
