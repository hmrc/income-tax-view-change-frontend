/*
 * Copyright 2026 HM Revenue & Customs
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

package financials.controllers

import common.auth.AuthActions
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.services.DateServiceInterface
import financials.services.SelfServeTimeToPayService
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelfServeTimeToPayController @Inject()(val authActions: AuthActions,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val selfServeTimeToPayService: SelfServeTimeToPayService,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                              implicit val dateService: DateServiceInterface,
                                             )(implicit val appConfig: FrontendAppConfig,
                                               val mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def fetchUrl(): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      selfServeTimeToPayService.startSelfServeTimeToPayJourney.map {
        case Left(value) => {
          Logger("application").error(s"Unexpected future failed error")
          itvcErrorHandler.showInternalServerError()
        }
        case Right(url) => SeeOther(url)
      }
  }
}
