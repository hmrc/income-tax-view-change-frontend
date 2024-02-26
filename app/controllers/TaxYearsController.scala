/*
 * Copyright 2023 HM Revenue & Customs
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
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateService, DateServiceInterface, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.TaxYears

import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject()(taxYearsView: TaxYears,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   implicit val dateService: DateServiceInterface,
                                   val auth: AuthenticatorPredicate)
                                  (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler
                                  ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private val earliestSubmissionTaxYear = 2023
  def handleRequest(backUrl: String,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    Future.successful(Ok(taxYearsView(
      taxYears = user.incomeSources.orderedTaxYearsByAccountingPeriods.reverse,
      backUrl,
      isAgent = isAgent,
      utr = user.saUtr,
      itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      earliestSubmissionTaxYear = earliestSubmissionTaxYear,
      btaNavPartial = user.btaNavPartial,
      origin = origin)))
  }

  def showTaxYears(origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        isAgent = false,
        origin = origin
      )
  }

  def showAgentTaxYears: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.HomeController.showAgent.url,
        isAgent = true
      )
  }
}
