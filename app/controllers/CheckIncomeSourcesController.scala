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

import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.PropertyDetailsModel

import java.time.{LocalDate, Month}
import auth.{MtdItUser, MtdItUserWithNino}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.CheckIncomeSources
import views.html.notMigrated.NotMigratedUser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import implicits.ImplicitDateFormatter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

@Singleton
class CheckIncomeSourcesController @Inject()(val checkIncomeSources: CheckIncomeSources,
                                             val notMigrated: NotMigratedUser,
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
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(user.incomeSources)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap {
          implicit mtdItUser =>
            handleRequest(mtdItUser.incomeSources)
        }
  }

  def handleRequest(incomeSourceDetails: IncomeSourceDetailsModel)(implicit user: MtdItUser[_], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    Future.successful(
      if(isDisabled(IncomeSources)) {
        Redirect(controllers.routes.HomeController.show())
      } else {
        Ok(checkIncomeSources(
        soleTraderBusinesses = incomeSourceDetails.businesses,
        ukProperty = incomeSourceDetails.property.find(_.incomeSourceType.contains("uk-property")),
        foreignProperty = incomeSourceDetails.property.find(_.incomeSourceType.contains("foreign-property")),
        ceasedBusinesses = incomeSourceDetails.businesses.filter(_.cessation.map(_.date).nonEmpty),
        isAgent = false))
      }
    )
  }
}


