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

import config.featureswitch.{AgentViewer, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.{Inject, Singleton}
import models.calculation._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(val taxCalcBreakdown: views.html.agent.TaxCalcBreakdown,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        calculationService: CalculationService,
                                        incomeSourceDetailsService: IncomeSourceDetailsService
                                       )(implicit val appConfig: FrontendAppConfig,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         dateFormatter: ImplicitDateFormatterImpl,
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  def showTaxDueSummary(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(AgentViewer)) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { implicit mtdItUser =>
          calculationService.getCalculationDetail(getClientNino(request), taxYear) flatMap {
            case calcDisplayModel: CalcDisplayModel =>
              Future.successful(Ok(taxCalcBreakdown(calcDisplayModel, taxYear, backUrl(taxYear))))

            case CalcDisplayNoDataFound =>
              Logger.warn(s"[Agent][TaxDueController][showTaxDueSummary[$taxYear]] No tax due data could be retrieved. Not found")
              Future.successful(itvcErrorHandler.showInternalServerError())

            case CalcDisplayError =>
              Logger.error(s"[Agent][TaxDueController][showTaxDueSummary[$taxYear]] No tax due data could be retrieved. Downstream error")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
      }
      else {
        Future.failed(new NotFoundException("[Agent][TaxDueSummaryController][showTaxDueSummary] - Agent viewer is disabled"))
      }
  }

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
