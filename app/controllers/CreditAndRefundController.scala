/*
 * Copyright 2022 HM Revenue & Customs
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


import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import config.featureswitch.{CreditsRefundsRepay, FeatureSwitching, MFACreditsAndDebits}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CreditService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.CreditAndRefunds
import views.html.errorPages.CustomNotFoundError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditAndRefundController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val creditService: CreditService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val view: CreditAndRefunds,
                                          val customNotFoundErrorView: CustomNotFoundError)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    creditService.getCreditCharges()(implicitly,user) map {
      case _ if isEnabled(CreditsRefundsRepay) == false =>
        Ok(customNotFoundErrorView()(user, messages))
      case financialDetailsModel : List[FinancialDetailsModel] =>
        val balance: Option[BalanceDetails] = financialDetailsModel.headOption.map(balance => balance.balanceDetails)

        val charges: List[(DocumentDetailWithDueDate, FinancialDetail)] = financialDetailsModel.flatMap(
          financialDetails => sortChargesGroupedPaymentTypes(financialDetails.getAllDocumentDetailsWithDueDates().zip(financialDetails.financialDetails))
        )

        Ok(view(charges, balance, isAgent, backUrl, isEnabled(MFACreditsAndDebits))(user, user, messages))
      case _ => Logger("application").error(
        s"${if (isAgent) "[Agent]"}[CreditAndRefundController][show] Invalid response from financial transactions")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show(origin).url,
          itvcErrorHandler = itvcErrorHandler,
          isAgent = false
        )
    }

  def showAgent(): Action[AnyContent] = {
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
            implicit mtdItUser =>
              handleRequest(
                backUrl = controllers.routes.HomeController.showAgent().url,
                itvcErrorHandler = itvcErrorHandlerAgent,
                isAgent = true
              )
          }
    }
  }

  private def sortChargesGroupedPaymentTypes(charges: List[(DocumentDetailWithDueDate, FinancialDetail)])
  : List[(DocumentDetailWithDueDate, FinancialDetail)] = {

    def sortCharges(charges: List[(DocumentDetailWithDueDate, FinancialDetail)])
    : List[(DocumentDetailWithDueDate, FinancialDetail)] = {
      charges
        .sortBy(_._1.documentDetail.paymentOrChargeCredit).reverse
    }

    val chargesGroupedPaymentTypes = charges
      .groupBy[String] {
        charges => {
          charges._1.documentDetail.documentDescription.get.substring(0, 3)
        }
      }.map {
      case (documentId, charges) => (documentId, sortCharges(charges))
    }.map {
      case (_, charges) => charges
    }.toList.flatten.sortBy(_._1.documentDetail.documentDescription.get.substring(0, 3))
    chargesGroupedPaymentTypes
  }
}
