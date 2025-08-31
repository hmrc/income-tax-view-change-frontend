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

import audit.AuditingService
import auth.MtdItUser
import auth.authV2.AuthActions
import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.claimToAdjustPoa.routes._
import models.claimToAdjustPoa.{PoaAmendmentData, SelectYourReason}
import models.core.CheckMode
import models.nrs.RawPayload
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.claimToAdjustPoa.{ClaimToAdjustPoaCalculationService, RecalculatePoaHelper}
import services.{ClaimToAdjustService, NrsService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ErrorRecovery
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.CheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(val authActions: AuthActions,
                                           val claimToAdjustService: ClaimToAdjustService,
                                           val poaSessionService: PaymentOnAccountSessionService,
                                           val ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                           val nrsService: NrsService,
                                           val checkYourAnswers: CheckYourAnswers,
                                           val auditingService: AuditingService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           val individualErrorHandler: ItvcErrorHandler,
                                           val agentErrorHandler: AgentItvcErrorHandler,
                                           val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends FrontendController(mcc) with RecalculatePoaHelper with I18nSupport with WithSessionAndPoa with ErrorRecovery{

  def show(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
      implicit user =>
        withSessionDataAndPoa() { (session, poa) =>
          withValidSession(session) { (reason, amount) =>
            EitherT.rightT(
              Ok(
                checkYourAnswers(
                  isAgent = user.isAgent(),
                  poaReason = reason,
                  taxYear = poa.taxYear,
                  adjustedFirstPoaAmount = amount,
                  adjustedSecondPoaAmount = amount,
                  redirectUrl = ConfirmationForAdjustingPoaController.show(user.isAgent()).url,
                  changePoaAmountUrl = EnterPoaAmountController.show(user.isAgent(), CheckMode).url,
                  changePoaReasonUrl = SelectYourReasonController.show(user.isAgent(), CheckMode).url
                )
              )
            )
          }
        } recover logAndRedirect
    }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent).async {
    implicit user =>
      handleSubmitPoaData(
        claimToAdjustService = claimToAdjustService,
        ctaCalculationService = ctaCalculationService,
        poaSessionService = poaSessionService,
        auditingService = auditingService,
        nrsService = nrsService
      ) recover logAndRedirect
  }

  private def withValidSession(session: PoaAmendmentData)
                              (block: (SelectYourReason, BigDecimal) => EitherT[Future, Throwable, Result])
                              (implicit user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {

    (session.poaAdjustmentReason, session.newPoaAmount) match {
      case (Some(reason), Some(amount)) =>
        block(reason, amount)
      case (None, _) =>
        EitherT.rightT(logAndRedirect(s"Payment On Account Adjustment reason missing from session"))
      case (_, None) =>
        EitherT.rightT(logAndRedirect(s"New Payment on Account missing from session"))
    }
  }
}
