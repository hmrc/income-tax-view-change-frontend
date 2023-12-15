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

package utils

import org.scalacheck.Gen
import testUtils.TestSupport

class ConvertersSpec extends TestSupport {
  import converters.OptionExtension
  "OptionExtension" when {
    "call trim method" should {
      "Some('') return None" in {
        Some("").trim() shouldBe None
      }
      "Some('anyString') return Some(_)" in {
        val underTest = Gen.oneOf( ('A' to 'Z') ++ ('a' to 'z') ).sample.get.toString
        Some(underTest).trim().isDefined shouldBe true
      }
    }
  }
}
