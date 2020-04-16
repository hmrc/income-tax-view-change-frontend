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
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeBreakdown}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import javax.inject.{Inject, Singleton}
import models.calculation._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CalculationService, FinancialTransactionsService}
import views.html.errorPages.notFound

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSummaryController @Inject()(implicit val appConfig: FrontendAppConfig,
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


  def showIncomeSummary(taxYear: Int): Action[AnyContent] = {

    action.async {
      implicit user =>
        if (isEnabled(IncomeBreakdown)) {
          calculationService.getCalculationDetail(user.nino, taxYear).flatMap {
            case calcDisplayModel: CalcDisplayModel =>
              Future.successful(Ok(views.html.incomeBreakdown(calcDisplayModel, taxYear)))

            case CalcDisplayNoDataFound =>
              Logger.warn(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Not found")
              Future.successful(itvcErrorHandler.showInternalServerError())

            case CalcDisplayError =>
              Logger.error(s"[IncomeSummaryController][showIncomeSummary[$taxYear]] No income data could be retrieved. Downstream error")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
        else Future.successful(NotFound(notFound()))
    }
  }

}
