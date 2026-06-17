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

package models

import common.enums.TaxYearSummary.CalculationType.*
import common.mocks.services.MockDateService
import common.models.liabilitycalculation.Metadata
import common.testUtils.UnitSpec
import org.scalatest.matchers.should.Matchers

class MetadataSpec extends UnitSpec with Matchers with MockDateService {

  "MetadataSpec" when {

    "calling. isCalculationNotCrystallised()" when {

      "the calculationType is InYear" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = InYear.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationNotCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is IntentToFinalise" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = IntentToFinalise.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationNotCrystallised
          val expected = true

          actual shouldBe expected
        }
      }


      crystallisedTypes.foreach { crystallisedCalculationType =>

        s"the calculationType is ${crystallisedCalculationType.toString}" should {

          "return false" in {

            val data =
              Metadata(
                calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
                calculationType = crystallisedCalculationType.value,
                calculationReason = None,
                periodFrom = None,
                periodTo = None
              )

            val actual = data.isCalculationNotCrystallised
            val expected = false

            actual shouldBe expected
          }
        }
      }
    }

    "calling. isCalculationCrystallised()" when {

      "the calculationType is Crystallisation" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = Crystallisation.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is ConfirmAmendment" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = ConfirmAmendment.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is Correction" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = Correction.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is DeclareFinalisation" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = DeclareFinalisation.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is IntentToAmend" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = IntentToAmend.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      "the calculationType is Amendment" should {

        "return true" in {

          val data =
            Metadata(
              calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
              calculationType = IntentToAmend.value,
              calculationReason = None,
              periodFrom = None,
              periodTo = None
            )

          val actual = data.isCalculationCrystallised
          val expected = true

          actual shouldBe expected
        }
      }

      notCrystallisedTypes.foreach { notCrystallisedCalculationType =>

        s"the calculationType is ${notCrystallisedCalculationType.toString}" should {

          "return false" in {

            val data =
              Metadata(
                calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
                calculationType = notCrystallisedCalculationType.value,
                calculationReason = None,
                periodFrom = None,
                periodTo = None
              )

            val actual = data.isCalculationCrystallised
            val expected = false

            actual shouldBe expected
          }
        }
      }
    }
  }
}