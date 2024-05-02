/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.claimToAdjustPoa

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.financialDetails.PoaAndTotalAmount
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationListService, ClaimToAdjustService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouNeedToKnowController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            val view: WhatYouNeedToKnow,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            auth: AuthenticatorPredicate,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val calculationListService: CalculationListService,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

    private def getRedirect(isAgent: Boolean, totalAmountLessThanPoa: Boolean): String = {
      ((isAgent, totalAmountLessThanPoa) match {
        case (false, false) => controllers.routes.HomeController.show()
        case (false, _) => controllers.routes.HomeController.show()
        case (_, false) => controllers.routes.HomeController.show()
        case (_, _) => controllers.routes.HomeController.show()
      }).url  }

  //continue button redirects to the HomeController successfully

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      claimToAdjustService.getPoATaxYear.flatMap {
        case Right(Some(poaTaxYear)) =>
          Future.successful(Ok(view(isAgent, poaTaxYear, getRedirect(isAgent, totalAmountLessThanPoa = true))))
        case Right(None) => Logger("application").error(s"[WhatYouNeedToKnowController][handleRequest]")
          Future.successful(showInternalServerError(isAgent))
        case Left(ex) =>
          Logger("application").error(s"[WhatYouNeedToKnowController][handleRequest] ${ex.getMessage} - ${ex.getCause}")
          Future.successful(showInternalServerError(isAgent))
      }
  }

  // I have created a new method to include the check for which amount is greater than the other, feel free to call it
  // or just include the code as part of the show method

//  def amountComparison(isAgent: Boolean, poaTaxYear: TaxYear): Action[AnyContent] = {
//    auth.authenticatedAction(isAgent) {
//    implicit user =>
//      claimToAdjustService.getDocumentDetail.flatMap {
//        case Right(poaModel: PoaAndTotalAmount) => {
//          if (poaModel.originalAmount >= poaModel.poaRelevantAmount) {
//            Future.successful(Ok(view(isAgent, poaTaxYear, getRedirect(isAgent, false))))
//          } else {
//            Future.successful(Ok(view(isAgent, poaTaxYear, getRedirect(isAgent, true))))
//          }
//        }
//        case Left(error: Throwable) =>
//          Logger("application").error(s"[WhatYouNeedToKnowController][handleRequest] ${error.getMessage} - ${error.getCause}")
//          Future.successful(showInternalServerError(isAgent))
//        }
//      }
//  }
}
