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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.manage.ManageIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class ManageUKPropertyController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                           val checkSessionTimeout: SessionTimeoutPredicate,
                                           val authenticate: AuthenticationPredicate,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           val retrieveNino: NinoPredicate,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           val incomeSourceDetailsService: IncomeSourceDetailsService,
                                           val retrieveBtaNavBar: NavBarPredicate)
                                          (implicit val ec: ExecutionContext,
                                                 implicit override val mcc: MessagesControllerComponents,
                                                 val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(origin: Option[String] = None): Action[AnyContent] = Action {
    Ok("Page WIP")
  }

  def showAgent(): Action[AnyContent] = Action {
    Ok("Agent Page WIP")
  }

}