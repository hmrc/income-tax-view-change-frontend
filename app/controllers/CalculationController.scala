/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}

import audit.AuditingService
import audit.models.EstimatesAuditing.EstimatesAuditModel
import audit.models.BillsAuditing.BillsAuditModel
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import enums.{Crystallised, Estimate}
import models.calculation.{CalcDisplayError, CalcDisplayModel, CalcDisplayNoDataFound}
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, ActionBuilder, AnyContent, Result}
import services.{CalculationService, FinancialTransactionsService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ImplicitDateFormatter

import scala.concurrent.Future

@Singleton
class CalculationController @Inject()(implicit val config: FrontendAppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                      val authenticate: AuthenticationPredicate,
                                      val retrieveNino: NinoPredicate,
                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      val calculationService: CalculationService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService,
                                      val financialTransactionsService: FinancialTransactionsService,
                                      val itvcErrorHandler: ItvcErrorHandler
                                     ) extends BaseController with ImplicitDateFormatter{

  val action: ActionBuilder[MtdItUser] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  val showCalculationForYear: Int => Action[AnyContent] = taxYear => action.async {
    implicit user =>
      implicit val sources: IncomeSourceDetailsModel = user.incomeSources
      calculationService.getCalculationDetail(user.nino, taxYear).flatMap {
        case calcDisplayModel: CalcDisplayModel =>
          calcDisplayModel.calcStatus match {
            case Crystallised =>
              audit(user, calcDisplayModel, isEstimate = false)
              renderCrystallisedView(calcDisplayModel, taxYear)
            case Estimate =>
              audit(user, calcDisplayModel, isEstimate = true)
              Future.successful(Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear)))
          }

        case CalcDisplayNoDataFound =>
          Logger.debug(s"[CalculationController][showCalculationForYear[$taxYear]] No last tax calculation data could be retrieved. Not found")
          Future.successful(NotFound(views.html.noEstimatedTaxLiability(taxYear)))

        case CalcDisplayError =>
          Logger.debug(s"[CalculationController][showCalculationForYear[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
          Future.successful(Ok(views.html.errorPages.estimatedTaxLiabilityError(taxYear)))
      }
  }

  private def renderCrystallisedView(calcDisplayModel: CalcDisplayModel, taxYear: Int)(implicit user: MtdItUser[_]): Future[Result] = {
    implicit val sources: IncomeSourceDetailsModel = user.incomeSources
    financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear).map {
      case transactions: FinancialTransactionsModel => transactions.findChargeForTaxYear(taxYear) match {
        case Some(charge) => Ok(views.html.crystallised(calcDisplayModel, charge, taxYear))
        case _ =>
          Logger.debug(s"[CalculationController][renderCrystallisedView[$taxYear]] No transaction could be retrieved for given year.")
          itvcErrorHandler.showInternalServerError
      }
      case _: FinancialTransactionsErrorModel =>
        Logger.debug(s"[CalculationController][renderCrystallisedView[$taxYear]] FinancialTransactionErrorModel returned from ftResponse")
        itvcErrorHandler.showInternalServerError
    }
  }

  private def audit(user: MtdItUser[_], model: CalcDisplayModel, isEstimate: Boolean)(implicit hc: HeaderCarrier): Unit = {
    auditingService.audit(
      if (isEstimate) EstimatesAuditModel(user, model) else BillsAuditModel(user, model),
      Some(controllers.routes.CalculationController.showCalculationForYear(user.incomeSources.earliestTaxYear.get).url)
    )
  }
}
