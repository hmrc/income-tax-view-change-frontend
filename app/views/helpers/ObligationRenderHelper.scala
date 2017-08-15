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

import models._

object ObligationRenderHelper {

  def subsetObligations(obligations: Option[ObligationsModel]): Option[ObligationsModel] =
    if(obligations.isDefined){
      val obs = obligations.get.obligations
      Some(ObligationsModel(
        doIfNotEmpty(getLatestReceived, obs.filter(_.getObligationStatus == Received))
          ++ obs.filter(_.getObligationStatus == Overdue)
          ++ doIfNotEmpty(getNextDue, obs.filter(_.getObligationStatus.isInstanceOf[Open]))
      ))
    } else None

  private def getLatestReceived(obligations: List[ObligationModel], current: ObligationModel): List[ObligationModel] =
    if(obligations.isEmpty) List(current)
    else
      if(obligations.head.due isAfter current.due) getLatestReceived(obligations.tail, obligations.head)
      else getLatestReceived(obligations.tail, current)

  private def getNextDue(obligations: List[ObligationModel], current: ObligationModel): List[ObligationModel] =
    if(obligations.isEmpty) List(current)
    else
      if(obligations.head.due isBefore current.due) getNextDue(obligations.tail, obligations.head)
      else getNextDue(obligations.tail, current)

  private def doIfNotEmpty(method: (List[ObligationModel],ObligationModel) => List[ObligationModel], list: List[ObligationModel]): List[ObligationModel] =
    if(list.nonEmpty) method(list.tail, list.head) else List()
}
