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

package models

import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class OutstandingChargesModelSpec extends UnitSpec with Matchers {

  val validOutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", "2021-12-01", 123456.67, 1234), OutstandingChargeModel("ACI", "2021-12-01", 12.67, 1234),
      OutstandingChargeModel("LATE", "2021-12-01", 123456.67, 1234)))

  val validOutstandingChargesModelWithNoMatchingTieBreaker = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", "2021-12-01", 123456.67, 4321), OutstandingChargeModel("ACI", "2021-12-01", 12.67, 1234)))

  val validOutstandingChargesModelWithOneChargeValueZero = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", "2021-12-01", 0, 1234), OutstandingChargeModel("ACI", "2021-12-01", 12.67, 1234)))

  val validOutstandingChargesModelWithNoBcdCharges = OutstandingChargesModel(
    List(OutstandingChargeModel("LATE", "2021-12-01", 123456.67, 1234), OutstandingChargeModel("ACI", "2021-12-01", 12.67, 1234)))

  "The OutstandingChargesModel" when {

    "getBcdAndAciTieBreakerChargesModelList return right values when valid model with tie breaker values is passed" in {
      validOutstandingChargesModel.getBcdAndAciTieBreakerChargesModelList shouldBe List(
        OutstandingChargeModel("BCD", "2021-12-01", 123456.67, 1234), OutstandingChargeModel("ACI", "2021-12-01", 12.67, 1234))
    }

    "getBcdAndAciTieBreakerChargesModelList return right values when valid model with no tie breaker values is passed" in {
      validOutstandingChargesModelWithNoMatchingTieBreaker.getBcdAndAciTieBreakerChargesModelList shouldBe List()
    }

    "getBcdAndAciTieBreakerChargesModelList return right values when valid model with bcd charge type value as zero is passed" in {
      validOutstandingChargesModelWithOneChargeValueZero.getBcdAndAciTieBreakerChargesModelList shouldBe List()
    }

    "getBcdAndAciTieBreakerChargesModelList return right values when valid model with no BCD charge type is passed" in {
      validOutstandingChargesModelWithNoBcdCharges.getBcdAndAciTieBreakerChargesModelList shouldBe List()
    }
  }
}
