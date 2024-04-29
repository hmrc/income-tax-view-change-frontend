/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.adjustPoa

import auth.MtdItUser
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{InitialPage, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectYourReasonController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val sessionService: SessionService,
                                          auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {


//  private def getBackUrl(isAgent: Boolean, isChange: Boolean): String = {
//    ((isAgent, isChange) match {
//      case (false, false) => routes.AddIncomeSourceController.show()
//      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
//      case (_, false) => routes.AddIncomeSourceController.showAgent()
//      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
//    }).url
//  }

//  private def getPostAction(isAgent: Boolean, isChange: Boolean): Call = {
//    (isAgent, isChange) match {
//      case (false, false) => routes.AddBusinessNameController.submit()
//      case (false, _) => routes.AddBusinessNameController.submitChange()
//      case (_, false) => routes.AddBusinessNameController.submitAgent()
//      case (_, _) => routes.AddBusinessNameController.submitChangeAgent()
//    }
//  }

//  private def getRedirect(isAgent: Boolean, isChange: Boolean): Call = {
//    (isAgent, isChange) match {
//      case (_, false) => routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, SelfEmployment)
//      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
//      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
//    }
//  }

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      withSessionData(JourneyType(Add, SelfEmployment), journeyState = InitialPage) { sessionData =>
        Future.successful {
          Ok("")
        }
      }.recover {
        case ex =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[SelectYourReasonController][handleRequest] ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
      }
    }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      withSessionData(JourneyType(Add, SelfEmployment), InitialPage) { sessionData =>
        Future.successful(Ok(""))
      }
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    Future.successful(Ok(""))
  }
}
