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

package views.helpers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
class RadioOptionSpec extends AnyWordSpecLike with Matchers {

  "call apply method" when {

    "expected parameter provided" should {
      "create class instance" in {
        RadioOption(Some("someOption"), Some("someMessage"), None).isInstanceOf[RadioOption] shouldBe true
      }
    }
    "unexpected parameter provided" should {
      "create class instance" in {
        RadioOption(None, Some("someMessage"), None).isInstanceOf[RadioOption] shouldBe true
        RadioOption(Some("someOption"), None, None).isInstanceOf[RadioOption] shouldBe true
        RadioOption(None, None, None).isInstanceOf[RadioOption] shouldBe true
      }
    }
  }
}