/*
 * Copyright 2021 HM Revenue & Customs
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

package models.outstandingCharges

import play.api.libs.json.{Format, Json}

sealed trait OutstandingChargesResponseModel


case class OutstandingChargesModel(outstandingCharges: List[OutstandingChargeModel]) extends OutstandingChargesResponseModel {
  def bcdChargeType: Option[OutstandingChargeModel] = outstandingCharges.find(_.chargeName == "BCD")
  def aciChargeType: Option[OutstandingChargeModel] = outstandingCharges.find(_.chargeName == "ACI")

  def getAciChargeWithTieBreaker: Option[OutstandingChargeModel] = {
    if(bcdChargeType.isDefined
      && bcdChargeType.get.chargeAmount > 0
      && aciChargeType.isDefined
      && (bcdChargeType.get.tieBreaker == aciChargeType.get.tieBreaker)
      && aciChargeType.get.chargeAmount > 0
    ) {
      aciChargeType
    } else {
      None
    }
  }
}


object OutstandingChargesModel {
  implicit val format: Format[OutstandingChargesModel] = Json.format[OutstandingChargesModel]
}


case class OutstandingChargesErrorModel(code: Int, message: String) extends OutstandingChargesResponseModel

object OutstandingChargesErrorModel {
  implicit val format: Format[OutstandingChargesErrorModel] = Json.format[OutstandingChargesErrorModel]
}