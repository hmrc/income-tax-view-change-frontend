/*
 * Copyright 2020 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.BillsAuditing.BillsAuditModel
import audit.models.EstimatesAuditing.EstimatesAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import enums.{Crystallised, Estimate}
import implicits.ImplicitDateFormatter
import javax.inject.{Inject, Singleton}
import models.calculation._
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CalculationService, FinancialTransactionsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationController @Inject()(implicit val appConfig: FrontendAppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      implicit val ec: ExecutionContext,
                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                      val authenticate: AuthenticationPredicate,
                                      val retrieveNino: NinoPredicate,
                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      val calculationService: CalculationService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService,
                                      val financialTransactionsService: FinancialTransactionsService,
                                      val itvcErrorHandler: ItvcErrorHandler
                                     ) extends BaseController with ImplicitDateFormatter with FeatureSwitching {

  val action: ActionBuilder[MtdItUser] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  def renderCalculationPage(taxYear: Int): Action[AnyContent] = {
    if (taxYear > 0) {
      showCalculationForYear(taxYear)
    } else {
      action.async { implicit request =>
        Future.successful(BadRequest(views.html.errorPages.standardError(messagesApi.apply("standardError.title"),
          messagesApi.apply("standardError.heading"),
          messagesApi.apply("standardError.message"))))
      }
    }
  }

  private val showCalculationForYear: Int => Action[AnyContent] = taxYear => action.async {
    implicit user =>
      implicit val sources: IncomeSourceDetailsModel = user.incomeSources
      calculationService.getCalculationDetail(user.nino, taxYear).flatMap {
        case calcDisplayModel: CalcDisplayModel =>
          calcDisplayModel.calcStatus match {
            case Crystallised =>
              auditCalcDisplayModel(user, calcDisplayModel, isEstimate = false)
              renderCrystallisedView(calcDisplayModel, taxYear)
            case Estimate =>
              auditCalcDisplayModel(user, calcDisplayModel, isEstimate = true)
              Future.successful(Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear)))
          }

        case CalcDisplayNoDataFound =>
          Logger.warn(s"[CalculationController][showCalculationForYear[$taxYear]] No last tax calculation data could be retrieved. Not found")
          Future.successful(NotFound(views.html.noEstimatedTaxLiability(taxYear)))

        case CalcDisplayError =>
          Logger.error(s"[CalculationController][showCalculationForYear[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
          Future.successful(Ok(views.html.errorPages.estimatedTaxLiabilityError(taxYear)))
      }
  }

  private def renderCrystallisedView(model: CrystallisedViewModel, taxYear: Int)(implicit user: MtdItUser[_]): Future[Result] = {
    implicit val sources: IncomeSourceDetailsModel = user.incomeSources
    financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear).map {
      case transactions: FinancialTransactionsModel =>
        (transactions.findChargeForTaxYear(taxYear), model) match {
          case (Some(charge), calcDisplayModel: CalcDisplayModel) =>
            Ok(views.html.crystallised(calcDisplayModel, charge, taxYear, isEnabled(Payment)))
          case _ =>
            Logger.error(s"[CalculationController][renderCrystallisedView[$taxYear]] No transaction could be retrieved for given year.")
            itvcErrorHandler.showInternalServerError
        }
      case _: FinancialTransactionsErrorModel =>
        Logger.error(s"[CalculationController][renderCrystallisedView[$taxYear]] FinancialTransactionErrorModel returned from ftResponse")
        itvcErrorHandler.showInternalServerError
    }
  }

  private def auditCalcDisplayModel(user: MtdItUser[_], model: CalcDisplayModel, isEstimate: Boolean)
                                   (implicit hc: HeaderCarrier): Unit =
    auditingService.audit(
      if (isEstimate) EstimatesAuditModel(user, model) else BillsAuditModel(user, model),
      Some(controllers.routes.CalculationController.renderCalculationPage(user.incomeSources.earliestTaxYear.get).url)
    )
}

