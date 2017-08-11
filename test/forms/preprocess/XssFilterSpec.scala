/*
 * Copyright 2017 HM Revenue & Customs
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

package forms.preprocess

import utils.TestSupport

class XssFilterSpec extends TestSupport {

  "The XssFilter" should {

    "strip out <script> tags" in {
      XssFilter.filter("<script style='wacky'>banana hello 123") shouldBe "banana hello 123"
    }

    "strip out <script> and </script> tags" in {
      XssFilter.filter("<script>banana hello 123") shouldBe "banana hello 123"
    }

    "strip out </script> tags" in {
      XssFilter.filter("banana hello 123</script>") shouldBe "banana hello 123"
    }

    "strip out vbscript" in {
      XssFilter.filter("vbscript:banana hello 123") shouldBe "banana hello 123"
    }

    "strip out javascript" in {
      XssFilter.filter("javascript:banana hello 123") shouldBe "banana hello 123"
    }

    "strip out expression" in {
      XssFilter.filter("expression(banana hello 123)") shouldBe ""
    }

    "strip out onload" in {
      XssFilter.filter("onload=banana hello 123") shouldBe "banana hello 123"
    }

    "strip out eval" in {
      XssFilter.filter("eval(banana hello 123)") shouldBe ""
    }

    "strip out src" in {
      XssFilter.filter("src='banana hello 123'") shouldBe ""
    }

  }

}
