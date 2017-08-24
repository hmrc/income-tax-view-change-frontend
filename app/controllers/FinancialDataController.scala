/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AppConfig
import controllers.predicates.AsyncActionPredicate
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.FinancialDataService

import scala.concurrent.Future

@Singleton
class FinancialDataController @Inject()(implicit val config: AppConfig,
                                                implicit val messagesApi: MessagesApi,
                                                val actionPredicate: AsyncActionPredicate,
                                                val financialDataService: FinancialDataService
                                               ) extends BaseController {

  val redirectToEarliestEstimatedTaxLiability: Action[AnyContent] = actionPredicate.async {
      implicit request => implicit user => implicit sources =>
        (sources.businessDetails, sources.propertyDetails) match {
          case (None, None) =>
            Logger.debug("[FinancialDataController][redirectToEarliestEstimatedTaxLiability] No Income Sources.")
            Future.successful(showInternalServerError)
          case (_: Option[IncomeModel], _: Option[IncomeModel]) =>
            Future.successful(Redirect(controllers.routes.FinancialDataController.getFinancialData(sources.earliestTaxYear.get)))
        }
    }

  val getFinancialData: Int => Action[AnyContent] = taxYear => actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      financialDataService.getFinancialData(user.nino, taxYear).map {
        case calcDisplayModel: CalcDisplayModel =>
          Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear))
        case CalcDisplayNoDataFound =>
          Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Not found")
          NotFound(views.html.noEstimatedTaxLiability(taxYear))
        case CalcDisplayError =>
          Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
          Ok(views.html.estimatedTaxLiabilityError(taxYear))
      }
  }
}
