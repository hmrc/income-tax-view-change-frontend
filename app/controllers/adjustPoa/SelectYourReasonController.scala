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

package controllers.adjustPoa

import cats.data.EitherT
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.adjustPoa.SelectYourReasonFormProvider
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.adjustPoa.SelectYourReasonView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectYourReasonController @Inject()(
                                            val view: SelectYourReasonView,
                                            val formProvider: SelectYourReasonFormProvider,
                                            val sessionService: PaymentOnAccountSessionService,
                                            val claimToAdjustService: ClaimToAdjustService,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching  {


//  private def getBackUrl(isAgent: Boolean, isChange: Boolean): String = {
//    ((isAgent, isChange) match {
//      case (false, false) => routes.AddIncomeSourceController.show()
//      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
//      case (_, false) => routes.AddIncomeSourceController.showAgent()
//      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
//    }).url
//  }

//  private def getPostAction(isAgent: Boolean, isChange: Boolean): Call = {
//    (isAgent, isChange) match {
//      case (false, false) => routes.AddBusinessNameController.submit()
//      case (false, _) => routes.AddBusinessNameController.submitChange()
//      case (_, false) => routes.AddBusinessNameController.submitAgent()
//      case (_, _) => routes.AddBusinessNameController.submitChangeAgent()
//    }
//  }

//  private def getRedirect(isAgent: Boolean, isChange: Boolean): Call = {
//    (isAgent, isChange) match {
//      case (_, false) => routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, SelfEmployment)
//      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
//      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
//    }
//  }


  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>

      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

      val result = for {
        session <- EitherT(sessionService.getMongo)
        poa <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
      } yield {
        (session, poa) match {
          case (Some(s), Some(p)) =>
            val form = formProvider.apply()
            Ok(view(
              selectYourReasonForm = s.poaAdjustmentReason.fold(form)(form.fill),
              taxYear = p.taxYear,
              isAgent = isAgent,
              isChange = isChange,
              backUrl = "TODO",
              useFallbackLink = true))
          case (None, _) =>
            Logger("application").error(s"[SelectYourReasonController][handleRequest] Couldn't resolve view, session missing")
            errorHandler.showInternalServerError()
          case (_, None) =>
            Logger("application").error(s"[SelectYourReasonController][handleRequest] Couldn't resolve view, POA missing")
            errorHandler.showInternalServerError()
        }
      }

      result.fold(
        ex => {
          Logger("application").error(s"[SelectYourReasonController][handleRequest]: ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
        },
        view => view
      )
    }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>

      formProvider.apply()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, TaxYear(2023, 2024), isAgent, isChange, "TODO", true))),
          value => {
            for {
              result <- sessionService.setAdjustmentReason(value)
            } yield {
              result match {
                case Right(value) => Ok(s"${value}")
                case _ => throw new IllegalArgumentException("")
              }
            }
          }).recover {
        case ex =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[SelectYourReasonController][handleRequest] ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
      }
  }
}
