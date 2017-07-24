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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.BTAPartialService
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class BTAPartialController @Inject()(implicit val config: AppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     val actionPredicate: AsyncActionPredicate,
                                     val btaPartialService: BTAPartialService
                                      ) extends BaseController {

  val setupPartial: Action[AnyContent] = actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      for{
        obligationDue <- btaPartialService.getObligations(user.nino, sources.businessDetails)
        firstEstimate <- getYears(user.nino, sources.earliestTaxYear)
        lastEstimate <- getYears(user.nino, sources.lastTaxTear)
      } yield (obligationDue, firstEstimate, lastEstimate) match {

        case (obligation: ObligationModel, first: LastTaxCalculation, last: LastTaxCalculation) =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded: $first and $last")
          Ok(views.html.btaPartial(obligation, sendYears(first, last)))

        case (obligation: ObligationModel, first: LastTaxCalculation, NoLastTaxCalculation) =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded: $first and NoLastTaxCalculation")
          Ok(views.html.btaPartial(obligation, Some(List(first))))

        case (obligation: ObligationModel, NoLastTaxCalculation, last: LastTaxCalculation) =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded: NoLastTaxCalculation and $last")
          Ok(views.html.btaPartial(obligation, Some(List(last))))

        case (obligation: ObligationModel, NoLastTaxCalculation, NoLastTaxCalculation) =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded: NoLastTaxCalculation")
          Ok(views.html.btaPartial(obligation, None))

        case a =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded: $a")
          showInternalServerError
      }
  }

  private[BTAPartialController]
  def getYears(nino: String, method: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] =
    if(method != -1)
      btaPartialService.getEstimate(nino, method)
    else
      //TODO: what if there is no tax year???
      Future(LastTaxCalculationError(500, "Could not retrieve tax years"))

  private[BTAPartialController]
  def sendYears(first: LastTaxCalculation, last: LastTaxCalculation): Option[List[LastTaxCalculation]] =
    if(first == last)
      Some(List(first))
    else
      Some(List(first, last))
}
