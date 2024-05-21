/*
 * Copyright 2024 HM Revenue & Customs
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

package services.optout.optoutproposition

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import testUtils.UnitSpec

class OptOutPropositionYearsToUpdateSpec extends UnitSpec {

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  "OptOutDataYearsToUpdate" should {

    "update years PY, CY, NY" when {
      "PY, CY, NY are offered" when {
        "customer intent is PY" when {
          "NY is known i.e. one of A,V" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = previousTaxYearOptOut
            optOutData.optOutYearsToUpdate(intent) shouldBe expectedOffered
          }
        }
      }
    }

    "update years CY, NY" when {
      "PY, CY, NY are offered" when {
        "customer intent is CY" when {
          "NY is known i.e. one of A,V" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = currentTaxYearOptOut
            val only_CY_and_NY = expectedOffered.drop(1)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_CY_and_NY
          }
        }
      }
    }

    "update years NY" when {
      "PY, CY, NY are offered" when {
        "customer intent is NY" when {
          "NY is known i.e. one of A,V" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = nextTaxYearOptOut
            val only_NY = expectedOffered.drop(2)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }

    "update years PY, CY" when {
      "PY, CY, NY are offered" when {
        "customer intent is PY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = previousTaxYearOptOut
            val only_PY_and_CY = expectedOffered.dropRight(1)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_PY_and_CY
          }
        }
      }
    }

    "update years CY" when {
      "PY, CY, NY are offered" when {
        "customer intent is CY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = currentTaxYearOptOut
            val only_CY = expectedOffered.drop(1).dropRight(1)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_CY
          }
        }
      }
    }

    "update years NY" when {
      "PY, CY, NY are offered" when {
        "customer intent is NY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = nextTaxYearOptOut
            val only_NY = expectedOffered.drop(2)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }

    "update years CY" when {
      "CY, NY are offered" when {
        "customer intent is CY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(currentTaxYearOptOut, nextTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = currentTaxYearOptOut
            val only_CY = expectedOffered.dropRight(1)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_CY
          }
        }
      }
    }

    "update years NY" when {
      "CY, NY are offered" when {
        "customer intent is NY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(currentTaxYearOptOut, nextTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = nextTaxYearOptOut
            val only_NY = expectedOffered.drop(1)
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }

    "update years PY, NY" when {
      "PY, NY are offered" when {
        "customer intent is PY" when {
          s"NY is ${ITSAStatus.Voluntary}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe false
            optOutData.isMultiYearOptOut shouldBe true
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut, nextTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = previousTaxYearOptOut
            val only_NY = expectedOffered
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }

    "update years PY" when {
      "PY is offered" when {
        "customer intent is PY" when {
          s"NY is ${ITSAStatus.NoStatus}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.NoStatus, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe true
            optOutData.isMultiYearOptOut shouldBe false
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = previousTaxYearOptOut
            val only_PY = expectedOffered
            optOutData.optOutYearsToUpdate(intent) shouldBe only_PY
          }
        }
      }
    }

    "update years PY" when {
      "PY are offered" when {
        "customer intent is PY" when {
          s"NY is ${ITSAStatus.Mandated}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = false)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Mandated, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe true
            optOutData.isMultiYearOptOut shouldBe false
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(previousTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = previousTaxYearOptOut
            val only_PY = expectedOffered
            optOutData.optOutYearsToUpdate(intent) shouldBe only_PY
          }
        }
      }
    }

    "update years CY" when {
      "CY are offered" when {
        "customer intent is CY" when {
          s"NY is ${ITSAStatus.Mandated}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Mandated, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe true
            optOutData.isMultiYearOptOut shouldBe false
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(currentTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = currentTaxYearOptOut
            val only_NY = expectedOffered
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }

    "update years NY" when {
      "NY is offered" when {
        "customer intent is NY" when {
          s"NY is ${ITSAStatus.Voluntary}" in {

            val previousTaxYearOptOut = PreviousOptOutTaxYear(ITSAStatus.Voluntary, previousTaxYear, crystallised = true)
            val currentTaxYearOptOut = CurrentOptOutTaxYear(ITSAStatus.Mandated, currentTaxYear)
            val nextTaxYearOptOut = NextOptOutTaxYear(ITSAStatus.Voluntary, nextTaxYear, currentTaxYearOptOut)

            val optOutData = OptOutProposition(previousTaxYearOptOut, currentTaxYearOptOut, nextTaxYearOptOut)

            optOutData.isOneYearOptOut shouldBe true
            optOutData.isMultiYearOptOut shouldBe false
            optOutData.isNoOptOutAvailable shouldBe false

            val expectedOffered = Seq(nextTaxYearOptOut)
            optOutData.availableOptOutYears shouldBe expectedOffered
            val intent = nextTaxYearOptOut
            val only_NY = expectedOffered
            optOutData.optOutYearsToUpdate(intent) shouldBe only_NY
          }
        }
      }
    }



  }
}
