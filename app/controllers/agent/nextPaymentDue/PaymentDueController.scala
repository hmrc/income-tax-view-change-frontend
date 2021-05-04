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

package controllers.agent.nextPaymentDue

import audit.AuditingService
import audit.models.{WhatYouOweRequestAuditModel, WhatYouOweResponseAuditModel}
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{AgentViewer, FeatureSwitching, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import models.financialDetails.WhatYouOweChargesList
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsResponseModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{FinancialTransactionsService, IncomeSourceDetailsService, PaymentDueService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.nextPaymentDue.paymentDue
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentDueController @Inject()(paymentDue: paymentDue,
                                     val financialTransactionsService: FinancialTransactionsService,
                                     val paymentDueService: PaymentDueService,
                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                     val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                     val auditingService: AuditingService,
                                     val authorisedFunctions: FrontendAuthorisedFunctions
                                    )(implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val languageUtils: LanguageUtils,
                                      dateFormatter: ImplicitDateFormatterImpl,
                                      implicit val ec: ExecutionContext,
                                      val itvcErrorHandler: ItvcErrorHandler
                                    ) extends ClientConfirmedController with FeatureSwitching with I18nSupport {


  def hasFinancialTransactionsError(transactionModels: List[FinancialTransactionsResponseModel]): Boolean = {
    transactionModels.exists(_.isInstanceOf[FinancialTransactionsErrorModel])
  }

  private def view(charge: WhatYouOweChargesList, taxYear: Int)(implicit request: Request[_], user: MtdItUser[_]): Html = {
    paymentDue.apply(
      chargesList = charge,
      currentTaxYear = taxYear,
      paymentEnabled = isEnabled(Payment),
      implicitDateFormatter = dateFormatter,
      backUrl = backUrl,
      user.saUtr
    )
  }

  def show: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          mtdItUser =>
            if (isEnabled(AgentViewer)) {
              auditingService.extendedAudit(WhatYouOweRequestAuditModel(mtdItUser))

              paymentDueService.getWhatYouOweChargesList()(implicitly, mtdItUser).map {
                whatYouOweChargesList => {

                  auditingService.extendedAudit(WhatYouOweResponseAuditModel(mtdItUser, whatYouOweChargesList))

                  Ok(view(whatYouOweChargesList, mtdItUser.incomeSources.getCurrentTaxEndYear)(implicitly, mtdItUser)
                  ).addingToSession(SessionKeys.chargeSummaryBackPage -> "paymentDue")
                }
              } recover {
                case ex: Exception =>
                  Logger.error(s"Error received while getting agent what you page details: ${ex.getMessage}")
                  itvcErrorHandler.showInternalServerError()
              }
            } else {
              Future.failed(new NotFoundException("[NextPaymentDueController][show] - Agent viewer is disabled"))
            }
        }
  }

  lazy val backUrl: String = controllers.agent.routes.HomeController.show().url

}
