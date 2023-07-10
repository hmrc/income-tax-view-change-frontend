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

package services

import config.ItvcErrorHandler
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import mocks.services.MockNextUpdatesService
import models.incomeSourceDetails.viewmodels.DatesModel
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Results.Status
import play.api.test.Helpers.status
import testOnly.forms.StubDataForm.status
import testUtils.TestSupport

import java.time.LocalDate
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.util.Success
class ObligationsRetrievalServiceSpec extends TestSupport
  with FeatureSwitching
  with MockNextUpdatesService{

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  object TestObligationsService extends ObligationsRetrievalService(
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    mockNextUpdatesService
  )

  "ObligationsRetrievalService" should {
    ".getObligationDates" should {
      "return the correct set of dates given an ObligationsModel" in {
        disableAllSwitches()
        enable(IncomeSources)

        val day = LocalDate.of(2023,1,1)
        val nextModel: ObligationsModel = ObligationsModel(Seq(
          NextUpdatesModel("123", List(
            NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "C")
          ))
        ))
        when(mockNextUpdatesService.getNextUpdates(any())(any(),any())).
          thenReturn(Future(nextModel))

        val result = TestObligationsService.getObligationDates("123")
        result.futureValue shouldBe Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false))
      }
      "return the correct set of dates given a NextUpdateModel" in {
        disableAllSwitches()
        enable(IncomeSources)

        val day = LocalDate.of(2023,1,1)
        val nextModel: NextUpdateModel = NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "C")
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(nextModel))

        val result = TestObligationsService.getObligationDates("123")
        result.futureValue shouldBe (Seq(DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = false)))
      }
      "show correct error when given a NextUpdatesErrorModel" in {
        disableAllSwitches()
        enable(IncomeSources)

        val nextModel: NextUpdatesErrorModel = NextUpdatesErrorModel(1, "fail")
        when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
          thenReturn(Future(nextModel))

        val result = TestObligationsService.getObligationDates("123")
        result.futureValue shouldBe Seq.empty
      }
    }
  }
}
