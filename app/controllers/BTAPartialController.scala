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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import config.AppConfig
import controllers.predicates.AsyncActionPredicate
import models.ObligationModel
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Action}
import services.BTAPartialService

@Singleton
class BTAPartialController @Inject()(implicit val config: AppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     val actionPredicate: AsyncActionPredicate,
                                     val btaPartialService: BTAPartialService
                                      ) extends BaseController {

  val setupPartialInt: Int => Action[AnyContent] = taxYear => actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      for{
        obligationDue <- btaPartialService.getObligations(user.nino, sources.businessDetails)
        lastEstimate <- btaPartialService.getEstimate(user.nino, taxYear)
      } yield (obligationDue, lastEstimate) match {
        case (obligation: ObligationModel, estimate: Option[BigDecimal]) => Ok(views.html.btaPartial(obligation, estimate))
        case _ => showInternalServerError
      }
  }

}
