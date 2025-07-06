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

package implicits

import org.scalatest.matchers.should.Matchers
import play.twirl.api.Html
import testUtils.UnitSpec

class HtmlFormatterSpec extends UnitSpec with Matchers {

  "HtmlFormatter.toNbsp" when {

    "calling toNbsp with a string containing spaces" should {

      "return a Html with no breaking spaces when given a string date" in {

        val input = HtmlFormatter.NbspString("21 Apr 2021").toNbsp
        val result = Html("21&nbsp;Apr&nbsp;2021")

        input shouldBe result
      }

      "return a Html with no breaking spaces when given a string Tax year" in {

        val input = HtmlFormatter.NbspString("2017 to 2018").toNbsp
        val result = Html("2017&nbsp;to&nbsp;2018")

        input shouldBe result
      }

      "return a Html with no formatting applied if the string contains no spaces" in {

        val inputString = "HelloWorld"
        val input = HtmlFormatter.NbspString(inputString).toNbsp
        val result = Html(inputString)

        input shouldBe result
      }
    }
  }
}
