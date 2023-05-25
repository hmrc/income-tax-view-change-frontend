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

package controllers

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.BusinessNameForm
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.AddBusinessStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    checkSessionTimeout: SessionTimeoutPredicate,
                                                    retrieveNino: NinoPredicate,
                                                    val addBusinessStartDate: AddBusinessStartDate,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    incomeSourceDetailsService: IncomeSourceDetailsService)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(request.session.toString))
  }

  def showAgent(): Action[AnyContent] = Action.async {
    Future.successful(Ok)
  }
}
