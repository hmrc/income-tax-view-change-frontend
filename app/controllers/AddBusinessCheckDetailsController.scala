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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.CreateBusinessDetailsService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate


class AddBusinessCheckDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                  val createBusinessDetailsService: CreateBusinessDetailsService,
                                                  val retrieveBtaNavBar: NavBarPredicate,
                                                  val retrieveNino: NinoPredicate,
                                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                                  val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                  val itvcErrorHandler: ItvcErrorHandler,
                                             val authorisedFunctions: FrontendAuthorisedFunctions)
                                            (implicit val appConfig: FrontendAppConfig,
                                             mcc: MessagesControllerComponents,
                                             val ec: ExecutionContext,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent]  = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = "",
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }
  }

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val viewModel = CheckBusinessDetailsViewModel(
      businessName = Some("someBusinessName"),
      businessStartDate = Some(LocalDate.of(2022, 11, 11)),
      businessTrade = Some("someBusinessTrade"),
      businessAddressLine1 = "businessAddressLine1",
      businessPostalCode = Some("SE15 4ER"),
      businessAccountingMethod = None
    )
    for {
      res <- createBusinessDetailsService.createBusinessDetails(user.mtditid, viewModel)
    } yield res match {
      case Right(_) =>
        Ok("OK")
      case Left(ex) =>
        Ok(s"ERROR: ${ex}")
    }
  }

  def showAgent(): Action[AnyContent] = Action {
    Ok("")
  }
}
