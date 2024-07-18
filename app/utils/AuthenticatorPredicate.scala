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

package utils

import auth.{MtdItUser, MtdItUserBase, MtdItUserOptionNino, MtdItUserWithNino}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.IncomeSourceDetailsService
import services.admin.FeatureSwitchService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatorPredicate @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                       val authenticate: AuthenticationPredicate,
                                       val featureSwitchService: FeatureSwitchService,
                                       val authorisedFunctions: AuthorisedFunctions,
                                       val retrieveBtaNavBar: NavBarPredicate,
                                       val featureSwitchPredicate: FeatureSwitchPredicate,
                                       val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                       val retrieveNino: NinoPredicate,
                                       val incomeSourceDetailsService: IncomeSourceDetailsService)
                                      (implicit mcc: MessagesControllerComponents,
                                       val appConfig: FrontendAppConfig,
                                       val itvcErrorHandler: AgentItvcErrorHandler,
                                       val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def authenticatedAction(isAgent: Boolean)
                         (authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser => {
              for {
                fss <- featureSwitchService.getAll
              } yield {
                authenticatedCodeBlock(mtdItUser
                  .copy(featureSwitches = fss)(mtdItUser))
              }
            }.flatten
            }
      }

    } else
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen featureSwitchPredicate andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }

  def authenticatedActionWithNino(authenticatedCodeBlock: MtdItUserWithNino[_] => Future[Result]): Action[AnyContent] = {
      (checkSessionTimeout andThen authenticate andThen retrieveNino).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
  def authenticatedActionOptionNino(authenticatedCodeBlock: MtdItUserOptionNino[_] => Future[Result]): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate).async { implicit user =>
      authenticatedCodeBlock(user)
    }
  }
  def authenticatedActionWithNinoAgent(authenticatedCodeBlock: AuthenticatorAgentResponse => Future[Result]): Action[AnyContent] = {
      Authenticated.async {
        implicit request =>
          implicit agent =>
            authenticatedCodeBlock(AuthenticatorAgentResponse())
      }
  }
  
//  def authenticatedActionNoUser(authenticatedCodeBlock: Request[AnyContent] => Future[Result]): Action[AnyContent] = {
//    Authenticated.async {
//      implicit request =>
//        implicit agent =>
//          authenticatedCodeBlock(request)
//    }
//  }
}

case class AuthenticatorAgentResponse()(implicit val agent: IncomeTaxAgentUser,
                                      implicit val request: Request[AnyContent],
                                      implicit val hc: HeaderCarrier,
                                      implicit val messages: Messages)
