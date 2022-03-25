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

package controllers.agent

import config.featureswitch.{FeatureSwitching, ITSASubmissionIntegration}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.TaxYears

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaxYearsController @Inject()(taxYearsView: TaxYears,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   incomeSourceDetailsService: IncomeSourceDetailsService)
                                  (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) map { mtdItUser =>
        Ok(taxYearsView(
          taxYears = mtdItUser.incomeSources.orderedTaxYearsByAccountingPeriods.reverse,
          backUrl = backUrl,
          itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
          isAgent = true
        ))
      }
  }

  def backUrl: String = controllers.routes.HomeController.showAgent().url
}
