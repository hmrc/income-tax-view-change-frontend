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
      statusToString(Voluntary) shouldBe expectedToString(Voluntary)
      statusToString(Annual) shouldBe expectedToString(Annual)
      statusToString(Mandated) shouldBe expectedToString(Mandated)
    }

    "throw error for unsupported statuses" in {
      Seq(NoStatus, Exempt, DigitallyExempt, Dormant).foreach { unsupportedStatus =>
        assertThrows[RuntimeException] {
          statusToString(unsupportedStatus)
        }
      }
    }
  }

  private val expectedToStatus = Map("V" -> Voluntary, "A" -> Annual, "M" -> Mandated)

  "ITSAStatusRepositorySupport.stringToStatus" should {
    "convert to Status" in {
      stringToStatus("V") shouldBe expectedToStatus("V")
      stringToStatus("A") shouldBe expectedToStatus("A")
      stringToStatus("M") shouldBe expectedToStatus("M")
    }

    "throw error for unsupported strings" in {
      Seq("U", "E", "DE", "D").foreach { unsupportedString =>
        assertThrows[RuntimeException] {
          stringToStatus(unsupportedString)
        }
      }
    }
  }

}
