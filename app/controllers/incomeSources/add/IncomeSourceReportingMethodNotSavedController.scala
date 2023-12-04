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
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceReportingMethodNotSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceReportingMethodNotSavedController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                                              val authenticate: AuthenticationPredicate,
                                                              val authorisedFunctions: AuthorisedFunctions,
                                                              val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                              val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                              val retrieveBtaNavBar: NavBarPredicate,
                                                              val view: IncomeSourceReportingMethodNotSaved)
                                                             (implicit val ec: ExecutionContext,
                                                              implicit override val mcc: MessagesControllerComponents,
                                                              implicit val itvcAgentErrorHandler: AgentItvcErrorHandler,
                                                              implicit val itvcErrorHandler: ItvcErrorHandler,
                                                              val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = withIncomeSourcesFS {

    val action: Call = (incomeSourceType, isAgent) match {
      case (UkProperty, true) => controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty)
      case (UkProperty, false) => controllers.incomeSources.add.routes.IncomeSourceAddedController.show(UkProperty)
      case (ForeignProperty, true) => controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty)
      case (ForeignProperty, false) => controllers.incomeSources.add.routes.IncomeSourceAddedController.show(ForeignProperty)
      case (SelfEmployment, true) => controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment)
      case (SelfEmployment, false) => controllers.incomeSources.add.routes.IncomeSourceAddedController.show(SelfEmployment)
    }

    Future.successful(Ok(view(incomeSourceType = incomeSourceType, continueAction = action, isAgent = isAgent)))
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }
}
