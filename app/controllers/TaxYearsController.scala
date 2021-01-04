/*
 * Copyright 2021 HM Revenue & Customs
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
import config.featureswitch.{API5, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.Inject
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{CalculationService, FinancialTransactionsService}

import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject()(implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val executionContext: ExecutionContext,
                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                   val authenticate: AuthenticationPredicate,
                                   val retrieveNino: NinoPredicate,
                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                   val calculationService: CalculationService,
                                   val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                   val itvcErrorHandler: ItvcErrorHandler,
                                   val auditingService: AuditingService,
                                   val financialTransactionsService: FinancialTransactionsService
                                  ) extends BaseController with I18nSupport with FeatureSwitching{

  val viewTaxYears: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears(isEnabled(API5))).flatMap {
        case taxYearCalResponse if taxYearCalResponse.exists(_.isError) =>
          Future.successful(itvcErrorHandler.showInternalServerError)
        case taxYearCalResponse =>
          financialTransactionsService.getAllFinancialTransactions.map { transactions =>
            transactions.map(x => x._2) match {
              case x if x.exists(_.isInstanceOf[FinancialTransactionsErrorModel]) =>
                itvcErrorHandler.showInternalServerError()
              case x => {
                val transactions = x.map(_.asInstanceOf[FinancialTransactionsModel]).flatMap(_.withYears())
                Ok(views.html.taxYears(taxYearCalResponse.filter(_.isCalculation), transactions))
                  .addingToSession("singleEstimate" -> "false")
              }
            }
          }
      }
  }
}
