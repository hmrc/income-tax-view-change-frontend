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

import testConstants.PropertyDetailsTestConstants.{ceasedPropertyDetails, propertyDetails}
import testUtils.UnitSpec

class PropertyDetailsModelSpec extends UnitSpec {

  "The PropertyDetailsModel" when {
    "A property is ceased, the isCeased method" should {
      "return true" in {
        ceasedPropertyDetails.isCeased shouldBe true
      }
    }

    "A property is not ceased, the isCeased method" should {
      "return false" in {
        propertyDetails.isCeased shouldBe false
      }
    }
  }
}
