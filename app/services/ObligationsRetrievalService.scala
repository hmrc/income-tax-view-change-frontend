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

package services

import auth.MtdItUser
import config.ItvcErrorHandler
import models.incomeSourceDetails.viewmodels.DatesModel
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, ObligationsModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationsRetrievalService @Inject()(val itvcErrorHandler: ItvcErrorHandler,nextUpdatesService: NextUpdatesService)(implicit hc: HeaderCarrier){

  def getObligationDates(id: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Seq[DatesModel]] = {
    nextUpdatesService.getNextUpdates() map {
      case NextUpdatesErrorModel(code, message) => Logger("application").error(
        s"[BusinessAddedObligationsController][handleRequest] - Error: $message, code $code")
        itvcErrorHandler.showInternalServerError()
        Seq.empty
      case NextUpdateModel(start, end, due, _, _, periodKey) =>
        Seq(DatesModel(start, end, due, periodKey, isFinalDec = false))
      case model: ObligationsModel =>
        Seq(model.allCrystallised map {
          source => DatesModel(source.obligation.start, source.obligation.end, source.obligation.due, source.obligation.periodKey, isFinalDec = true)
        },
          model.obligations.filter(x => x.identification == id).flatMap(obligation => obligation.obligations.map(x => DatesModel(x.start, x.end, x.due, x.periodKey, isFinalDec = false)))
        ).flatten
    }
  }

}
