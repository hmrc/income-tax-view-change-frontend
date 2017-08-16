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

package views.helpers

import java.time.LocalDate

import models._
import utils.ImplicitListMethods

object ObligationRenderHelper extends ImplicitListMethods {

  def subsetObligations(obligations: Option[ObligationsModel]): Option[ObligationsModel] =
    obligations.map { obs =>
      ObligationsModel(getLatestReceived(obs.obligations) ++ getAllOverdue(obs.obligations) ++ getNextDue(obs.obligations))
    }

  private def getLatestReceived(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus == Received).maxItemBy(_.due)

  private def getAllOverdue(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus == Overdue)

  private def getNextDue(obligations: List[ObligationModel]): List[ObligationModel] =
    obligations.filter(_.getObligationStatus.isInstanceOf[Open]).minItemBy(_.due)

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

}
