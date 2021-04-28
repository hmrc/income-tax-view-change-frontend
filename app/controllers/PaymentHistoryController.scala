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
import audit.models.{PaymentHistoryRequestAuditModel, PaymentHistoryResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatterImpl
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import services.PaymentHistoryService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentHistoryController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                         val authenticate: AuthenticationPredicate,
                                         val retrieveNino: NinoPredicate,
                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                         auditingService: AuditingService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         paymentHistoryService: PaymentHistoryService,
                                         dateFormatter: ImplicitDateFormatterImpl)
                                        (implicit mcc: MessagesControllerComponents,
                                         ec: ExecutionContext,
                                         val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {


  def action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  val viewPaymentHistory: Action[AnyContent] = action.async {
    implicit user =>
      if (!isEnabled(PaymentHistory)) {
        Future.successful(NotFound(itvcErrorHandler.notFoundTemplate(user)))
      } else {
        auditingService.extendedAudit(PaymentHistoryRequestAuditModel(user))
        paymentHistoryService.getPaymentHistory.map {
          case Right(payments) =>
            auditingService.extendedAudit(PaymentHistoryResponseAuditModel(user, payments))
            Ok(views.html.paymentHistory(payments, dateFormatter, backUrl = backUrl, user.saUtr))
          case Left(_) => itvcErrorHandler.showInternalServerError()
        }
      }
  }

  lazy val backUrl: String = controllers.routes.HomeController.home().url

}

