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

package controllers.optOut

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import cats.data.OptionT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import controllers.agent.predicates.ClientConfirmedController
import models.optout.{MultiYearOptOutCheckpointViewModel, OneYearOptOutCheckpointViewModel, OptOutCheckpointViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import utils.AuthenticatorPredicate
import views.html.optOut.{CheckOptOutAnswers, ConfirmOptOut}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import audit.AuditingService
//import audit.models.{CheckYourAnswersAuditModel, Outcome}

class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        checkOptOutAnswers: CheckOptOutAnswers,
                                        optOutService: OptOutService,
                                        auth: AuthenticatorPredicate,
                                        auditingService: AuditingService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {
        val cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url

        val resultToReturn = for {
          viewModel <- OptionT(optOutService.optOutCheckPointPageViewModel())
          result <- OptionT(Future.successful(Option(toPropositionView(isAgent, viewModel, cancelURL))))
        } yield result

        resultToReturn.getOrElse(handleError("No qualified tax year available for opt out", isAgent))

      }
  }

  private def toPropositionView(isAgent: Boolean, viewModel: OptOutCheckpointViewModel, cancelURL: String)(implicit mtdItUser: MtdItUser[_]) = viewModel match {
    case oneYear: OneYearOptOutCheckpointViewModel => Ok(view(oneYear, isAgent = isAgent, cancelURL))
    case multiYear: MultiYearOptOutCheckpointViewModel => Ok(checkOptOutAnswers(multiYear, isAgent, cancelURL))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case ITSAStatusUpdateResponseSuccess(_) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
//          val checkYourAnswersAuditModel: CheckYourAnswersAuditModel = CheckYourAnswersAuditModel(
//            nino = nino, outcome = Outcome, optOutRequestedFromTaxYear = String, currentYear = String,
//            beforeITSAStatusCurrentYearMinusOne = String, beforeITSAStatusCurrentYear = String,
//            beforeITSAStatusCurrentYearPlusOne = String, afterAssumedITSAStatusCurrentYearMinusOne = String,
//            afterAssumedITSAStatusCurrentYear = String, afterAssumedITSAStatusCurrentYearPlusOne = String,
//            currentYearMinusOneCrystallised = Boolean)
//          for {
//            clientName <- fetchClientName
//            nino <- request.session.get(SessionKeys.clientNino)
//            clientMTDID <- request.session.get(SessionKeys.clientMTDID)
//            arn <- user.agentReferenceNumber
//            saUtr <- request.session.get(SessionKeys.clientUTR)
//          } yield
//          auditingService.extendedAudit(CheckYourAnswersAuditModel(
//            nino = nino,
//            "outcome" -> outcome,
//            "optOutRequestedFromTaxYear" -> optOutRequestedFromTaxYear,
//            "currentYear" -> currentYear,
//            "beforeITSAStatusCurrentYearMinusOne" -> beforeITSAStatusCurrentYearMinusOne,
//            "beforeITSAStatusCurrentYear" -> beforeITSAStatusCurrentYear,
//            "beforeITSAStatusCurrentYearPlusOne" -> beforeITSAStatusCurrentYearPlusOne,
//            "afterAssumedITSAStatusCurrentYearMinusOne" -> afterAssumedITSAStatusCurrentYearMinusOne,
//            "afterAssumedITSAStatusCurrentYear" -> afterAssumedITSAStatusCurrentYear,
//            "afterAssumedITSAStatusCurrentYearPlusOne" -> afterAssumedITSAStatusCurrentYearMinusOne,
//            "currentYearMinusOneCrystallised" -> currentYearMinusOneCrystallised)
//          ))
//
        case _ => Redirect(routes.OptOutErrorController.show(isAgent))
      }

//      for {
//        clientName <- fetchClientName
//        nino <- request.session.get(SessionKeys.clientNino)
//        clientMTDID <- request.session.get(SessionKeys.clientMTDID)
//        arn <- user.agentReferenceNumber
//        saUtr <- request.session.get(SessionKeys.clientUTR)
//      } yield
//        auditingService.extendedAudit(ConfirmClientDetailsAuditModel(
//          clientName = clientName,
//          nino = nino,
//          mtditid = clientMTDID,
//          arn = arn,
//          saUtr = saUtr,
//          credId = user.credId
//        ))
  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception => handleError(s"request failed :: $ex", isAgent)
    }
  }

  private def handleError(message: String, isAgent: Boolean)(implicit request: Request[_]): Result = {
    val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    Logger("application").error(message)
    errorHandler(isAgent).showInternalServerError()
  }

}