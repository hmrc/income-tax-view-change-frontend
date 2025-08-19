/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.optIn

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optin.SignUpCompletedViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ReportingObligationsUtils
import views.html.optIn.SignUpCompletedView

import scala.concurrent.{ExecutionContext, Future}

class SignUpCompletedController @Inject()(val view: SignUpCompletedView,
                                          val optInService: OptInService,
                                          val authActions: AuthActions,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ReportingObligationsUtils {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean): Action[AnyContent] = {
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      withSignUpRFChecks {
        withRecover(isAgent) {
          optInService.saveIntent(TaxYear(2026, 2027))
          for {
            proposition <- optInService.fetchOptInProposition()
            intent <- optInService.fetchSavedChosenTaxYear()
          } yield {
            intent.map { optInTaxYear =>
              val model = SignUpCompletedViewModel(
                isAgent = isAgent,
                optInTaxYear = optInTaxYear,
                isCurrentYear = proposition.isCurrentTaxYear(optInTaxYear),
                isCurrentYearAnnual = proposition.currentTaxYear.status == ITSAStatus.Annual,
                isNextYearMandated = proposition.nextTaxYear.status == ITSAStatus.Mandated,
                compatibleSoftwareLink = appConfig.compatibleSoftwareLink,
                criteriaForMtdLink = appConfig.saWhoNeedsToSignUpUrl,
                fileYourReturnLink = appConfig.selfAssessmentTaxReturn
              )

              Ok(view(model))
            }.getOrElse(errorHandler(isAgent).showInternalServerError())
          }
        }
      }
    }
  }
}
