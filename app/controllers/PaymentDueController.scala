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
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}

import javax.inject.Inject
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, FinancialTransactionsResponseModel}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{FinancialDetailsService, FinancialTransactionsService, PaymentDueService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import models.outstandingCharges.OutstandingChargesModel
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class PaymentDueController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val financialTransactionsService: FinancialTransactionsService,
                                     val paymentDueService: PaymentDueService,
                                     val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val auditingService: AuditingService,
                                     implicit val appConfig: FrontendAppConfig,
                                     mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     dateFormatter: ImplicitDateFormatterImpl
                                     ) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def hasFinancialTransactionsError(transactionModels: List[FinancialTransactionsResponseModel]): Boolean = {
    transactionModels.exists(_.isInstanceOf[FinancialTransactionsErrorModel])
  }

  def hasFinancialDetailsError(financialDetails: List[FinancialDetailsResponseModel]): Boolean = {
    financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel])
  }


  val viewPaymentsDue: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if(isEnabled(NewFinancialDetailsApi)) {
        paymentDueService.getWhatYouOweChargesList().map {
          whatYouOweChargesList =>
            Ok(views.html.whatYouOwe(chargesList = whatYouOweChargesList, yearOfMigration = user.incomeSources.yearOfMigration.map(_.toInt),
              paymentEnabled = isEnabled(Payment), implicitDateFormatter = dateFormatter))
        } recover {
          case ex: Exception =>
            Logger.error(s"Error received while getting what you page details: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()
        }
      } else {
        financialTransactionsService.getAllUnpaidFinancialTransactions.map {
          case transactions if hasFinancialTransactionsError(transactions) => itvcErrorHandler.showInternalServerError()
          case transactions: List[FinancialTransactionsModel] => Ok(views.html.paymentDue(financialTransactions = transactions,
            paymentEnabled = isEnabled(Payment), implicitDateFormatter = dateFormatter))
        }
      }
  }

}
