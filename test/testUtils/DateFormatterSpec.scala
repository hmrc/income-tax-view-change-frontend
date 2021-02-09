/*
 * Copyright 2021 HM Revenue & Customs
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

package testUtils

import java.time.LocalDate

import implicits.ImplicitDateFormatter
import javax.inject.Inject
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.play.language.LanguageUtils

class DateFormatterSpec extends TestSupport with ImplicitDateFormatter {

  private trait Test

  "The implicit date formatter" when {

    "given a date as a string" should {

      "convert it to a LocalDate" in new Test{
        "2017-04-01".toLocalDate shouldBe LocalDate.parse("2017-04-01")
      }

      "convert it to a LocalDate with single digit values" in new Test{
        "2017-6-1".toLocalDate shouldBe LocalDate.parse("2017-06-01")
      }
    }

    "The implicit date formatter" should {

      "change localDates to full dates" in new Test{
        "2017-04-01".toLocalDate.toLongDate shouldBe "1 April 2017"
      }

      "change LocalDateTime to long date output" in new Test {
        "2017-04-01T11:23:45.123Z".toLocalDateTime.toLongDateTime shouldBe "1 April 2017"
      }
    }
  }
}