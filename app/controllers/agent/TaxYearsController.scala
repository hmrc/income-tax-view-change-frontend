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

package controllers.agent

import auth.MtdItUser
import config.featureswitch.{AgentViewer, FeatureSwitching, ITSASubmissionIntegration}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.TaxYears

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject()(taxYears: TaxYears,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   calculationService: CalculationService,
                                   incomeSourceDetailsService: IncomeSourceDetailsService)
                                  (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if(isEnabled(AgentViewer)) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { implicit mtdUser =>
          withCalculationYears { years =>
            Ok(taxYears(
              taxYears = years,
              backUrl = backUrl,
              itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration)
            ))
          }
        }
      } else {
        Future.failed(new NotFoundException("[TaxYearsController][show] - Agent viewer is disabled"))
      }
  }

  def backUrl: String = controllers.agent.routes.HomeController.show().url

  private def withCalculationYears(f: List[Int] => Result)(implicit user: MtdItUser[_]): Future[Result] = {
    calculationService.getAllLatestCalculations(user.nino, user.incomeSources.orderedTaxYears(true)) map {
      case taxYearsResponse if taxYearsResponse.exists(_.isError) =>
        itvcErrorHandler.showInternalServerError()
      case taxYearsResponse => f(taxYearsResponse.map(_.year).reverse)
    }
  }

}
