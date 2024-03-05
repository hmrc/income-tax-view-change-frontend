/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.manageBusinesses.add

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.IncomeSourceType
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SessionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import views.html.manageBusinesses.add.ReportingFrequency
class ReportingFrequencyController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                             val reportingFrequencyView: ReportingFrequency,
                                             val sessionService: SessionService,
                                             auth: AuthenticatorPredicate)
                                            (implicit val appConfig: FrontendAppConfig,
                                             val ec: ExecutionContext,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                            ) {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType
      )
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Future[Result] = {
      Future.successful {
        Ok(reportingFrequencyView())
      }
  }


}
