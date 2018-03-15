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
import controllers.predicates._
import models._
import models.calculation.LastTaxCalculationWithYear
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.BTAPartialService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class BTAPartialController @Inject()(implicit val config: FrontendAppConfig,
                                     implicit val messagesApi: MessagesApi,
                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val btaPartialService: BTAPartialService
                                    ) extends BaseController {

  val setupPartial: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      getAllEstimates(user.nino, user.incomeSources.orderedTaxYears).map { allEstimates =>
        Ok(views.html.btaPartial(btaPartialService.getNextObligation(user.incomeSources), allEstimates))
      }
  }

  private def getAllEstimates(nino: String, orderedYears: List[Int])(implicit headerCarrier: HeaderCarrier): Future[List[LastTaxCalculationWithYear]] =
    Future.sequence(orderedYears.map {
      year =>
        btaPartialService.getEstimate(nino, year).map {
          est => LastTaxCalculationWithYear(est, year)
        }
    })
}
