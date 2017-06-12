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

package views

import java.time.LocalDate

import assets.Messages
import models.ObligationModel
import org.mockito.ArgumentMatchers
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import utils.ImplicitDateFormatter.localDate
import utils.ImplicitLongDate._
import utils.TestSupport

class HelpersSpec extends TestSupport {

  "The Helpers.currentTime" should {
    "return the current time" in {
      object TestHelpers extends Helpers()(fakeApplication.injector.instanceOf[MessagesApi])

      TestHelpers.currentTime() shouldBe LocalDate.now
    }
  }
  "The Helpers.getObligationStatus" should {
    val testDate = localDate("2017-09-05")
    object TestHelpers extends Helpers()(fakeApplication.injector.instanceOf[MessagesApi]) {
      override def currentTime(): LocalDate = testDate
    }

    "return the correct status" when {
      "an obligation is not met and due date has past" in {
        val obligationModel =
            ObligationModel(
              start = ArgumentMatchers.any(),
              end = ArgumentMatchers.any(),
              due = localDate("2017-08-05"),
              met = false
        )
        TestHelpers.getObligationStatus(obligationModel) shouldBe Html(Messages.Helpers.overdue)
      }

      "an obligation has not been met and due date has not passed" in {
        val obligationModel =
          ObligationModel(
            start = ArgumentMatchers.any(),
            end = ArgumentMatchers.any(),
            due = localDate("2017-11-05"),
            met = false
          )
        TestHelpers.getObligationStatus(obligationModel) shouldBe Html(Messages.Helpers.due(localDate("2017-11-05").toLongDate))
      }

      "an obligation has been met" in {
        val obligationModel =
          ObligationModel(
            start = ArgumentMatchers.any(),
            end = ArgumentMatchers.any(),
            due = ArgumentMatchers.any(),
            met = true
          )
        TestHelpers.getObligationStatus(obligationModel) shouldBe Html(Messages.Helpers.received)
      }
    }

  }
}
