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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.{BaseAgentController, ClientConfirmedController}
import controllers.predicates.{AuthenticationPredicate, FeatureSwitchPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, ActionFunction, AnyContent, BodyParser, MessagesControllerComponents, Request, Result}
import services.IncomeSourceDetailsService
import services.admin.FeatureSwitchService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
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
                                       val incomeSourceDetailsService: IncomeSourceDetailsService)
                                      (implicit mcc: MessagesControllerComponents,
                                       val appConfig: FrontendAppConfig,
                                       val itvcErrorHandler: AgentItvcErrorHandler,
                                       val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport with FeatureSwitching{

  def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>

              val res = for {
                fss <- featureSwitchService.getAll
              } yield {
                val newRequest = MtdItUser[AnyContent](
                  mtditid = mtdItUser.mtditid,
                  nino = mtdItUser.nino,
                  userName = mtdItUser.userName,
                  incomeSources = mtdItUser.incomeSources,
                  btaNavPartial = mtdItUser.btaNavPartial,
                  saUtr = mtdItUser.saUtr,
                  credId = mtdItUser.credId,
                  userType = mtdItUser.userType,
                  arn = mtdItUser.arn,
                  featureSwitches = fss)(request)
                authenticatedCodeBlock(newRequest)
              }
              res.flatten
            }
      }

    } else
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar andThen featureSwitchPredicate).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}
