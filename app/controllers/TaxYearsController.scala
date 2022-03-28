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

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, ITSASubmissionIntegration}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.TaxYears

import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject()(taxYearsView: TaxYears,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   incomeSourceDetailsService: IncomeSourceDetailsService)
                                  (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler,
                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                   val retrieveBtaNavBar: BtaNavBarPredicate,
                                   val authenticate: AuthenticationPredicate,
                                   val retrieveNino: NinoPredicate,
                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                   val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter
                                  ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    Future.successful(Ok(taxYearsView(
      taxYears = user.incomeSources.orderedTaxYearsByAccountingPeriods.reverse,
      backUrl,
      isAgent = isAgent,
      utr = user.saUtr,
      itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      btaNavPartial = user.btaNavPartial)))
  }

  def showTaxYears: Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          backUrl = controllers.routes.HomeController.show().url,
          isAgent = false
        )
    }
  }

  def showAgentTaxYears: Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
          handleRequest(
            backUrl = controllers.routes.HomeController.showAgent().url,
            isAgent = true
          )
        }
    }
  }
}
