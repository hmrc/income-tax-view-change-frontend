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
import controllers.predicates._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.manage.ManageIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val manageIncomeSources: ManageIncomeSources,
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

  def showUkProperty: Action[AnyContent] = Action(Ok)
  def showUkPropertyAgent: Action[AnyContent] = Action(Ok)
  def showForeignProperty: Action[AnyContent] = Action(Ok)
  def showForeignPropertyAgent: Action[AnyContent] = Action(Ok)
  def showSoleTraderBusiness(incomeSourceId: String): Action[AnyContent] = Action(Ok)
  def showSoleTraderBusinessAgent(incomeSourceId: String): Action[AnyContent] = Action(Ok)
  def submitUkProperty: Action[AnyContent] = Action(Ok)
  def submitUkPropertyAgent: Action[AnyContent] = Action(Ok)
  def submitForeignProperty: Action[AnyContent] = Action(Ok)
  def submitForeignPropertyAgent: Action[AnyContent] = Action(Ok)
  def submitSoleTraderBusiness: Action[AnyContent] = Action(Ok)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = Action(Ok)
}