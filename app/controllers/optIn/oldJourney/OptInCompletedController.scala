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

package controllers.optIn.oldJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.itsaStatus.ITSAStatus.Voluntary
import models.optin.OptInCompletedViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optIn.OptInService
import services.optIn.core.OptInProposition
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optIn.oldJourney.OptInCompletedView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptInCompletedController @Inject()(val view: OptInCompletedView,
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

  def show(isAgent: Boolean = false): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        withReportingObligationsFS {
          withRecover(isAgent) {
            for {
              proposition: OptInProposition <- optInService.fetchOptInProposition()
              intent <- optInService.fetchSavedChosenTaxYear()
            } yield {
              intent.map { optInTaxYear =>
                val model =
                  OptInCompletedViewModel(
                    isAgent = isAgent,
                    optInTaxYear = optInTaxYear,
                    showAnnualReportingAdvice = proposition.showAnnualReportingAdvice(optInTaxYear),
                    isCurrentYear = proposition.isCurrentTaxYear(optInTaxYear),
                    optInIncludedNextYear = proposition.nextTaxYear.status == Voluntary,
                    annualWithFollowingYearMandated = proposition.annualWithFollowingYearMandated()
                  )
                Ok(view(model))
              }.getOrElse(errorHandler(isAgent).showInternalServerError())
            }
          }
        }
    }

}