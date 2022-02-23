/*
 * Copyright 2022 HM Revenue & Customs
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
import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import services.PaymentHistoryService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.PaymentHistory
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryController @Inject()(val paymentHistoryView: PaymentHistory,
                                         val checkSessionTimeout: SessionTimeoutPredicate,
                                         val authenticate: AuthenticationPredicate,
                                         val retrieveNino: NinoPredicate,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                         auditingService: AuditingService,
                                         retrieveBtaNavBar: BtaNavBarPredicate,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService)
                                        (implicit mcc: MessagesControllerComponents,
                                         ec: ExecutionContext,
                                         val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {


  def action: ActionBuilder[MtdItUser, AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar)

  val viewPaymentHistory: Action[AnyContent] = action.async {
    implicit user =>
      if (!isEnabled(PaymentHistory)) {
        Future.successful(NotFound(itvcErrorHandler.notFoundTemplate(user)))
      } else {
        paymentHistoryService.getPaymentHistory.map {
          case Right(payments) =>
            if (isEnabled(TxmEventsApproved)) {
              auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments))
            }
            Ok(paymentHistoryView(payments, backUrl = backUrl, user.saUtr, btaNavPartial = user.btaNavPartial))
          case Left(_) => itvcErrorHandler.showInternalServerError()
        }
      }
  }

  lazy val backUrl: String = controllers.routes.HomeController.home().url

}

