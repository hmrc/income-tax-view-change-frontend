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
      statusToString(status = Voluntary, isNextYear = false) shouldBe expectedToString(Voluntary)
      statusToString(status = Annual, isNextYear = false) shouldBe expectedToString(Annual)
      statusToString(status = Mandated, isNextYear = false) shouldBe expectedToString(Mandated)
    }

    "allow NoStatus only for CY+1" in {
      statusToString(status = NoStatus, isNextYear = true) shouldBe "U"
    }

    "throw error for NoStatus in CY-1 or CY" in {
      assertThrows[RuntimeException] {
        statusToString(status = NoStatus, isNextYear = false)
      }
    }

    "throw error for other unsupported statuses" in {
      Seq(Exempt, DigitallyExempt, Dormant).foreach { unsupportedStatus =>
        assertThrows[RuntimeException] {
          statusToString(status = unsupportedStatus, isNextYear = false)
        }
      }
    }
  }

  private val expectedToStatus = Map("V" -> Voluntary, "A" -> Annual, "M" -> Mandated)

  "ITSAStatusRepositorySupport.stringToStatus" should {
    "convert to Status" in {
      stringToStatus(status = "V", isNextYear = false) shouldBe expectedToStatus("V")
      stringToStatus(status = "A", isNextYear = false) shouldBe expectedToStatus("A")
      stringToStatus(status = "M", isNextYear = false) shouldBe expectedToStatus("M")
    }

    "convert 'U' to NoStatus only for CY+1" in {
      stringToStatus(status = "U", isNextYear = true) shouldBe NoStatus
    }

    "throw error for 'U' in CY-1 or CY" in {
      assertThrows[RuntimeException] {
        stringToStatus(status = "U", isNextYear = false)
      }
    }

    "throw error for other unsupported strings" in {
      Seq("E", "DE", "D").foreach { unsupportedString =>
        assertThrows[RuntimeException] {
          stringToStatus(status = unsupportedString, isNextYear = false)
        }
      }
    }
  }

}
