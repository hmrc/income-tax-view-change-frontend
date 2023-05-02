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
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.NewIncomeSources
import models.incomeSourceDetails.viewmodels.IncomeSourcesViewModel
import java.time.{LocalDate, Month}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NewIncomeSourcesController @Inject()(val newIncomeSources: NewIncomeSources,
                                           val checkSessionTimeout: SessionTimeoutPredicate,
                                           val authenticate: AuthenticationPredicate,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           val retrieveNino: NinoPredicate,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           val incomeSourceDetailsService: IncomeSourceDetailsService,
                                           val retrieveBtaNavBar: NavBarPredicate)
                                          (implicit val ec: ExecutionContext,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
//        sources = dummyBusinessesAndPropertyIncome,
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
//              sources = dummyBusinessesAndPropertyIncome,
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
        Ok(newIncomeSources(
          IncomeSourcesViewModel(
            soleTraderBusinesses = sources.businesses,
            ukProperty = sources.property.find(_.incomeSourceType.contains("uk-property")),
            foreignProperty = sources.property.find(_.incomeSourceType.contains("foreign-property")),
            ceasedBusinesses = sources.businesses.filter(_.cessation.map(_.date).nonEmpty)
          ),
          isAgent = isAgent,
          backUrl = backUrl
        ))
      }
    )
  }
}
