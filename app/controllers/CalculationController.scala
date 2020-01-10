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

import javax.inject.{Inject, Singleton}
import audit.AuditingService
import audit.models.EstimatesAuditing.{BasicEstimatesAuditModel, EstimatesAuditModel}
import audit.models.BillsAuditing.{BasicBillsAuditModel, BillsAuditModel}
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import enums.{Crystallised, Estimate}
import implicits.ImplicitDateFormatter
import models.calculation._
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CalculationService, FinancialTransactionsService}
import uk.gov.hmrc.http.HeaderCarrier

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
                                     ) extends BaseController with ImplicitDateFormatter {

  val action: ActionBuilder[MtdItUser] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  def renderCalculationPage(taxYear: Int): Action[AnyContent] = {
    if(taxYear > 0) {
      if (config.features.calcDataApiEnabled()) {
        showCalculationForYear(taxYear)
      } else {
        basicShowCalculationForYear(taxYear)
      }
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

  private val basicShowCalculationForYear: Int => Action[AnyContent] = taxYear => action.async {
    implicit user =>
      implicit val sources: IncomeSourceDetailsModel = user.incomeSources
      for {
        id <- calculationService.getCalculationId(user.nino, taxYear)
        calc <- calculationService.getLatestCalculation(user.nino, id)
        result <- matchCalcResponse(calc, taxYear)
      } yield result
  }

  private def matchCalcResponse(calc: CalculationResponseModel, taxYear: Int)(implicit user: MtdItUser[AnyContent]) = calc match {
    case calcModel: Calculation =>
      if(calcModel.crystallised) {
        auditCalculationModel(user, calcModel, isEstimate = false)
        renderCrystallisedView(calcModel, taxYear)
      } else {
        auditCalculationModel(user, calcModel, isEstimate = true)
        (calcModel.timestamp, calcModel.totalIncomeTaxAndNicsDue) match {
          case (Some(timestamp), Some(currentEstimate)) =>
            val viewModel = EstimatesViewModel(
              timestamp,
              currentEstimate,
              taxYear,
              calcModel.incomeTaxNicAmount
            )
            Future.successful(Ok(views.html.getLatestCalculation.estimate(viewModel)))
          case _ =>
            Logger.error(s"[CalculationController][basicShowCalculationForYear[$taxYear] Valid calculation information could not be retrieved.")
            Future.successful(itvcErrorHandler.showInternalServerError)
        }
      }

    case _: CalculationErrorModel =>
      Future.successful(Ok(views.html.errorPages.estimatedTaxLiabilityError(taxYear)))
  }

  private def renderCrystallisedView(model: CrystallisedViewModel, taxYear: Int)(implicit user: MtdItUser[_]): Future[Result] = {
    implicit val sources: IncomeSourceDetailsModel = user.incomeSources
    financialTransactionsService.getFinancialTransactions(user.mtditid, taxYear).map {
      case transactions: FinancialTransactionsModel =>
        (transactions.findChargeForTaxYear(taxYear), model) match {
          case (Some(charge), calcDisplayModel: CalcDisplayModel) =>
            Ok(views.html.crystallised(calcDisplayModel, charge, taxYear))
          case (Some(charge), calculationModel: Calculation) =>
            calculationModel.totalIncomeTaxAndNicsDue match {
              case Some(currentBill) =>
                val viewModel = BillsViewModel(
                  currentBill,
                  charge.isPaid,
                  taxYear
                )
                Ok(views.html.getLatestCalculation.bill(viewModel))
              case None =>
                Logger.error(s"[CalculationController][renderCrystallisedView[$taxYear]] No current bill amount could be retrieved.")
                itvcErrorHandler.showInternalServerError
            }
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

  private def auditCalculationModel(user: MtdItUser[_], model: Calculation, isEstimate: Boolean)
                                   (implicit hc: HeaderCarrier): Unit =
    auditingService.audit(
      if (isEstimate) BasicEstimatesAuditModel(user, model) else BasicBillsAuditModel(user, model),
      Some(controllers.routes.CalculationController.renderCalculationPage(user.incomeSources.earliestTaxYear.get).url)
    )
}
