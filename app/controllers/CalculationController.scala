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
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import enums.{Crystallised, Estimate}
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, ActionBuilder, AnyContent, Result}
import play.twirl.api.Html
import services.{CalculationService, FinancialTransactionsService, ServiceInfoPartialService}
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
                                      val serviceInfoPartialService: ServiceInfoPartialService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService,
                                      val financialTransactionsService: FinancialTransactionsService,
                                      val itvcErrorHandler: ItvcErrorHandler
                                     ) extends BaseController with ImplicitDateFormatter{

  import itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest

  val action: ActionBuilder[MtdItUser] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  val getFinancialData: Int => Action[AnyContent] = taxYear => action.async {
    implicit user =>
      implicit val sources: IncomeSourcesModel = user.incomeSources

      for {
        serviceInfo <- serviceInfoPartialService.serviceInfoPartial(user.userDetails.map(_.name))
        calcResponse <- calculationService.getFinancialData(user.nino, taxYear)
        ftResponse <- financialTransactionsService.getFinancialTransactions(user.mtditid)
      } yield calcResponse match {
          case calcDisplayModel: CalcDisplayModel =>
            auditEstimate(user, calcDisplayModel.calcAmount.toString)
            calcDisplayModel.calcStatus match {
              case Crystallised => renderCrystallisedView(calcDisplayModel, taxYear, ftResponse)(user, serviceInfo)
              case Estimate => Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear)(serviceInfo))
            }
          case CalcDisplayNoDataFound =>
            Logger.debug(s"[CalculationController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Not found")
            auditEstimate(user, "No data found")
            NotFound(views.html.noEstimatedTaxLiability(taxYear)(serviceInfo))
          case CalcDisplayError =>
            Logger.debug(s"[CalculationController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
            Ok(views.html.estimatedTaxLiabilityError(taxYear)(serviceInfo))
        }
  }

  def renderCrystallisedView(calcDisplayModel: CalcDisplayModel, taxYear: Int, ftResponse: FinancialTransactionsResponseModel)
                            (implicit user: MtdItUser[_], serviceInfo: Html): Result = {
    implicit val sources: IncomeSourcesModel = user.incomeSources
    ftResponse match {
      case transactions: FinancialTransactionsModel => transactions.findChargeForTaxYear(taxYear) match {
        case Some(charge) => Ok(views.html.crystallised(calcDisplayModel, charge, taxYear)(serviceInfo))
        case _ =>
          Logger.debug(s"[CalculationController][renderCrystallisedView[$taxYear]] No transaction could be retrieved for given year.")
          itvcErrorHandler.showInternalServerError
      }
      case _: FinancialTransactionsErrorModel =>
        Logger.debug(s"[CalculationController][renderCrystallisedView[$taxYear]] FinancialTransactionErrorModel returned from ftResponse")
        itvcErrorHandler.showInternalServerError
    }
  }

  private def auditEstimate(user: MtdItUser[_], estimate: String)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(
      EstimatesAuditModel(user, estimate),
      controllers.routes.CalculationController.getFinancialData(user.incomeSources.earliestTaxYear.get).url
    )

}
