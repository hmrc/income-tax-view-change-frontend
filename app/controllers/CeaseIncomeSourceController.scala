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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.viewmodels.{AddIncomeSourcesViewModel, BusinessDetailsViewModel, CeasedBusinessDetailsViewModel, PropertyDetailsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import views.html.incomeSources.cease.CeaseIncomeSources

import java.time.{LocalDate, Month}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseIncomeSourceController @Inject()(val ceaseIncomeSources: CeaseIncomeSources,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: FrontendAuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val checkSessionTimeout: SessionTimeoutPredicate)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.routes.HomeController.show().url
    )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.routes.HomeController.showAgent.url
            )
        }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    Future.successful(
      if (isDisabled(IncomeSources)) {
        Redirect(controllers.routes.HomeController.show())
      } else {
        Ok(
          ceaseIncomeSources(
            sourcesTest,
            isAgent,
            backUrl
          )
        )
      }
    )
  }

  val soleTrader: BusinessDetailsViewModel = BusinessDetailsViewModel(tradingName = "TESLA", tradingStartDate = LocalDate.of(2020, 1, 5))

  val ukProperty: PropertyDetailsViewModel = PropertyDetailsViewModel(tradingStartDate = LocalDate.of(2020, 1, 5))

  val foreignProperty: PropertyDetailsViewModel = PropertyDetailsViewModel(tradingStartDate = LocalDate.of(2099, 1, 5))

  val ceasedBusiness: CeasedBusinessDetailsViewModel = CeasedBusinessDetailsViewModel(tradingName = "Phoenix", tradingStartDate = LocalDate.of(2019, 1, 5), cessationDate = LocalDate.of(2021, 1, 5))

  val sourcesTest: AddIncomeSourcesViewModel = AddIncomeSourcesViewModel(soleTraderBusinesses = List(soleTrader), ukProperty = Some(ukProperty), foreignProperty = Some(foreignProperty), ceasedBusinesses = List(ceasedBusiness))

}
