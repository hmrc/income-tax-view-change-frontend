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

import audit.AuditingService
import audit.models.TaxCalculationDetailsResponseAuditModel
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.calculation._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(taxCalcBreakdown: views.html.agent.TaxCalcBreakdown,
                                        val appConfig: FrontendAppConfig,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        calculationService: CalculationService,
                                        incomeSourceDetailsService: IncomeSourceDetailsService,
                                        val auditingService: AuditingService
                                       )(implicit mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def showTaxDueSummary(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap { implicit mtdItUser =>
        calculationService.getCalculationDetail(getClientNino(request), taxYear) flatMap {
          case calcDisplayModel: CalcDisplayModel =>
            if (isEnabled(TxmEventsApproved)) {
              auditingService.extendedAudit(TaxCalculationDetailsResponseAuditModel(mtdItUser, calcDisplayModel, taxYear))
            }
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

  def backUrl(taxYear: Int): String = controllers.agent.routes.TaxYearOverviewController.show(taxYear).url

}
