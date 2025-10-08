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

package repositories

import models.itsaStatus.ITSAStatus._
import org.scalatest.matchers.should.Matchers
import repositories.ITSAStatusRepositorySupport._
import testUtils.UnitSpec

class ITSAStatusRepositorySupportSpec extends UnitSpec with Matchers {

  private val expectedToString = Map(Voluntary -> "V", Annual -> "A", Mandated -> "M")

  "ITSAStatusRepositorySupport.statusToString" should {
    "convert to String" in {
      statusToString(status = Voluntary) shouldBe expectedToString(Voluntary)
      statusToString(status = Annual) shouldBe expectedToString(Annual)
      statusToString(status = Mandated) shouldBe expectedToString(Mandated)
    }

    "allow NoStatus only for CY+1" in {
      statusToString(status = NoStatus) shouldBe "U"
    }

    "Return U for unsupported statuses" in {
      Seq(Exempt, DigitallyExempt, Dormant).foreach { _ =>
        statusToString(status = NoStatus) shouldBe "U"
      }
    }
  }

  private val expectedToStatus = Map("V" -> Voluntary, "A" -> Annual, "M" -> Mandated)

  "ITSAStatusRepositorySupport.stringToStatus" should {
    "convert to Status" in {
      stringToStatus(status = "V") shouldBe expectedToStatus("V")
      stringToStatus(status = "A") shouldBe expectedToStatus("A")
      stringToStatus(status = "M") shouldBe expectedToStatus("M")
    }

    "convert 'U' to NoStatus only for CY+1" in {
      stringToStatus(status = "U") shouldBe NoStatus
    }
  }

}
