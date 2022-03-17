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
import audit.models.PaymentAllocationsResponseAuditModel
import auth.{MtdItUser, MtdItUserWithNino}
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.core.Nino
import models.paymentAllocationCharges.PaymentAllocationError
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Result}
import play.mvc.Http
import play.mvc.Http.Status.NOT_FOUND
import services.PaymentAllocationsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.PaymentAllocation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(val paymentAllocationView: PaymentAllocation,
                                             val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             paymentAllocations: PaymentAllocationsService,
                                             val retrieveBtaNavBar: BtaNavBarPredicate,
                                             auditingService: AuditingService)
                                            (implicit override val mcc: MessagesControllerComponents,,
                                             ec: ExecutionContext,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  /*val action: ActionBuilder[MtdItUser, AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar)*/

  def handleRequest(backUrl: String,
                    itcvErrorHandler: ShowInternalServerError,
                    documentNumber: String,
                    isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    if (isEnabled(PaymentAllocation)) {
      paymentAllocations.getPaymentAllocation(Nino(user.nino), documentNumber) map {
        case Right(paymentAllocations) =>
          auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(user, paymentAllocations))
          Ok(paymentAllocationView(paymentAllocations, backUrl = backUrl, btaNavPartial = user.btaNavPartial, isAgent = isAgent)(implicitly, messages))
        case Left(PaymentAllocationError(Some(Http.Status.NOT_FOUND))) =>
          Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        case _ => itvcErrorHandler.showInternalServerError()
      }
    } else Future.successful(NotFound(itvcErrorHandler.notFoundTemplate(user)))
  }

  def viewPaymentAllocation(documentNumber: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          controllers.routes.PaymentHistoryController.viewPaymentHistory().url,
          itcvErrorHandler = itvcErrorHandler,
          documentNumber = documentNumber,
          isAgent = false
        )
    }

  def viewPaymentAllocationAgent(documentNumber: String): Action[AnyContent] =
    // todo this should be fixed
   /* Authenticated.async { implicit request =>
      implicit agent =>
        handleRequest(
          controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url,
          itcvErrorHandler = itvcErrorHandlerAgent,
          documentNumber = documentNumber,
          isAgent = true
        )
    }*/

  // todo remove it when unification is done
  /*def viewPaymentAllocation(documentNumber: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(PaymentAllocation)) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          paymentAllocationsService.getPaymentAllocation(Nino(getClientNino(request)), documentNumber) map {
            case Right(viewModel: PaymentAllocationViewModel) =>
              auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(mtdItUser, viewModel))
              Ok(paymentAllocationView(viewModel, backUrl = backUrl, isAgent = true))
            case Left(PaymentAllocationError(Some(404))) =>
              Redirect(controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url)
            case _ => itvcErrorHandler.showInternalServerError()
          }
        }
      } else Future.failed(new NotFoundException("[PaymentAllocationsController] - PaymentAllocation is disabled"))
  }*/
}
