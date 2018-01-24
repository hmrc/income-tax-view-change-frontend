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
import play.api.mvc.{Action, ActionBuilder, AnyContent}
import services.CalculationService
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class CalculationController @Inject()(implicit val config: FrontendAppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                      val authenticate: AuthenticationPredicate,
                                      val retrieveNino: NinoPredicate,
                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                      val calculationService: CalculationService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService
                                     ) extends BaseController {

  val action: ActionBuilder[MtdItUser] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources

  val getFinancialData: Int => Action[AnyContent] = taxYear => action.async {
    implicit user =>
      implicit val sources: IncomeSourcesModel = user.incomeSources

      for {
        calcResponse <- calculationService.getFinancialData(user.nino, taxYear)
      } yield calcResponse match {
          case calcDisplayModel: CalcDisplayModel =>
            auditEstimate(user, calcDisplayModel.calcAmount.toString)
            calcDisplayModel.calcStatus match {
              case Crystallised => Ok(views.html.crystallised(calcDisplayModel, taxYear))
              case Estimate => Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear))
            }
          case CalcDisplayNoDataFound =>
            Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Not found")
            auditEstimate(user, "No data found")
            NotFound(views.html.noEstimatedTaxLiability(taxYear))
          case CalcDisplayError =>
            Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
            Ok(views.html.estimatedTaxLiabilityError(taxYear))
        }
  }
  private def auditEstimate(user: MtdItUser[_], estimate: String)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(
      EstimatesAuditModel(user, estimate),
      controllers.routes.CalculationController.getFinancialData(user.incomeSources.earliestTaxYear.get).url
    )

}
