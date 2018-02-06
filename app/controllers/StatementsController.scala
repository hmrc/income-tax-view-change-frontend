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

import config.FrontendAppConfig
import controllers.predicates.{AuthenticationPredicate, SessionTimeoutPredicate}
import models.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.FinancialTransactionsService

@Singleton
class StatementsController @Inject()(implicit val config: FrontendAppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val financialTransactionsService: FinancialTransactionsService
                                    ) extends BaseController {

  val getStatements: Action[AnyContent] = (checkSessionTimeout andThen authenticate).async {
    implicit user =>
      for {
        financialTransactionsResponse <- financialTransactionsService.getFinancialTransactions(user.mtditid)
      } yield financialTransactionsResponse match {
        case model: FinancialTransactionsModel =>
          Logger.debug("[StatementsController][getStatements] Success Response received from financialTransactionsService")
          Ok(views.html.statements(model.withYears().sortWith(_.taxYear > _.taxYear)))
        case _: FinancialTransactionsErrorModel =>
          Logger.debug("[StatementsController][getStatements] Error Response received from financialTransactionsService")
          Ok(views.html.errorPages.statementsError())
      }
  }
}
