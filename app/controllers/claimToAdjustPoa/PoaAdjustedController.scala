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

import auth.MtdItUser
import auth.authV2.AuthActions
import cats.data.EitherT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.AfterSubmissionPage
import models.admin.YourSelfAssessmentCharges
import models.claimToAdjustPoa.{Increase, PaymentOnAccountViewModel, PoaAmendmentData, SelectYourReason}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, DateService, PaymentOnAccountSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ErrorRecovery
import utils.claimToAdjust.WithSessionAndPoa
import views.html.claimToAdjustPoa.PoaAdjustedView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PoaAdjustedController @Inject()(val authActions: AuthActions,
                                      val view: PoaAdjustedView,
                                      val poaSessionService: PaymentOnAccountSessionService,
                                      val claimToAdjustService: ClaimToAdjustService,
                                      val dateService: DateService)
                                     (implicit val appConfig: FrontendAppConfig,
                                      val individualErrorHandler: ItvcErrorHandler,
                                      val agentErrorHandler: AgentItvcErrorHandler,
                                      val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with WithSessionAndPoa with FeatureSwitching with ErrorRecovery {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrPrimaryAgentWithClient(isAgent) async {
    implicit user =>
      withSessionDataAndPoa(journeyState = AfterSubmissionPage) { (session, poa) =>
        checkAndLogAPIDataSet(session, poa)
        EitherT.liftF(handleView(poa, session))
      } recover logAndRedirect
  }

  private def checkAndLogAPIDataSet(session: PoaAmendmentData, poa: PaymentOnAccountViewModel): Unit = {
    if (session.newPoaAmount.contains(poa.totalAmountOne)) {
      Logger("application").info(s"Amount returned from API equals amount in mongo: ${poa.totalAmountOne}")
    }
    else {
      Logger("application").error(s"Amount returned from API: ${poa.totalAmountOne} does not equal amount in mongo: ${session.newPoaAmount}")
    }
  }

  private def handleView(poa: PaymentOnAccountViewModel, session: PoaAmendmentData)(implicit user: MtdItUser[_]): Future[Result] = {
    poaSessionService.setCompletedJourney(hc, ec).flatMap {
      case Right(_) => Future.successful(
        Ok(view(user.isAgent(), poa.taxYear, poa.totalAmountOne, showOverdueCharges(poa.taxYear, session.poaAdjustmentReason), isEnabled(YourSelfAssessmentCharges))))
      case Left(ex) =>
        Future.successful(logAndRedirect(s"Error setting journey completed flag in mongo${ex.getMessage} - ${ex.getCause}"))
    }
  }

  private def showOverdueCharges(poaTaxYear: TaxYear, reason: Option[SelectYourReason]): Boolean = {
    val poaOneDeadline = LocalDate.of(poaTaxYear.endYear, 1, 31)
    dateService.getCurrentDate.isAfter(poaOneDeadline) && reason.contains(Increase)
  }
}