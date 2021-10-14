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
import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.core.Nino
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents}
import services.PaymentAllocationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.PaymentAllocation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(val paymentAllocationView: PaymentAllocation,
                                              val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             paymentAllocations: PaymentAllocationsService,
                                             auditingService: AuditingService)
                                            (implicit mcc: MessagesControllerComponents,
                                             ec: ExecutionContext,
                                             val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {


  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  lazy val backUrl: String = controllers.routes.PaymentHistoryController.viewPaymentHistory().url

  def viewPaymentAllocation(documentNumber: String): Action[AnyContent] = action.async {
    implicit user =>
      if (isEnabled(PaymentAllocation)) {
        paymentAllocations.getPaymentAllocation(Nino(user.nino), documentNumber) map {
          case Right(paymentAllocations) =>
            if (isEnabled(TxmEventsApproved)) {
              auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(user, paymentAllocations))
            }
            Ok(paymentAllocationView(paymentAllocations, backUrl = backUrl))
          case _ => itvcErrorHandler.showInternalServerError()
        }
      } else Future.successful(NotFound(itvcErrorHandler.notFoundTemplate(user)))
  }
}
