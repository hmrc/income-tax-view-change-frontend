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
      for {
        latestObligation <- btaPartialService.getObligations(user.nino, sources.businessDetails)
        allEstimates <- getAllEstimates(user.nino, sources.orderedTaxYears)
      } yield (latestObligation, allEstimates) match {
        case (obligation: ObligationModel, estimates) =>
          Logger.debug(s"[BTAPartialController][setupPartial] - yielded: $obligation and $estimates")
          Ok(views.html.btaPartial(obligation, estimates))
        case error =>
          Logger.warn(s"[BTAPartialController][setupPartial] - yielded $error")
          showInternalServerError
      }
  }

  private def getAllEstimates(nino: String, orderedYears: List[Int])(implicit headerCarrier: HeaderCarrier): Future[List[LastTaxCalculationWithYear]] =
    Future.sequence(orderedYears.map {
      year => btaPartialService.getEstimate(nino, year).map {
        est => LastTaxCalculationWithYear(est, year)
      }
    })
}
