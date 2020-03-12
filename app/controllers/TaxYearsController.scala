/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AuditingService
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.CalculationService

import scala.concurrent.ExecutionContext

class TaxYearsController @Inject()(implicit val appConfig: FrontendAppConfig,
                                   implicit val messagesApi: MessagesApi,
                                   implicit val ec: ExecutionContext,
                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                   val authenticate: AuthenticationPredicate,
                                   val retrieveNino: NinoPredicate,
                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                   val calculationService: CalculationService,
                                   val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   val auditingService: AuditingService
                                  ) extends BaseController with FeatureSwitching {

  val viewTaxYears: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears).map {
        case taxYearCalResponse if taxYearCalResponse.exists(_.isError) =>
          itvcErrorHandler.showInternalServerError
        case taxYearCalResponse =>
          Ok(views.html.taxYears(taxYearCalResponse.filter(_.isCalculation)))
          .addingToSession("singleEstimate" -> "false")
      }
  }
}
