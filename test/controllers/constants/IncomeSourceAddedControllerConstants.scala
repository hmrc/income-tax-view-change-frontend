/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.constants

import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import testConstants.BaseTestConstants.testSelfEmploymentId

import java.time.LocalDate

object IncomeSourceAddedControllerConstants {

  val testObligationsModel: ObligationsModel =
    ObligationsModel(
      Seq(
        GroupedObligationsModel(testSelfEmploymentId,
          List(
            SingleObligationModel(
              start = LocalDate.of(2022, 7, 1),
              end = LocalDate.of(2022, 7, 2),
              due = LocalDate.of(2022, 8, 2),
              obligationType = "Quarterly",
              dateReceived = None,
              periodKey = "#001",
              status = StatusFulfilled
            ),
            SingleObligationModel(
              start = LocalDate.of(2022, 7, 1),
              end = LocalDate.of(2022, 7, 2),
              due = LocalDate.of(2022, 8, 2),
              obligationType = "Quarterly",
              dateReceived = None,
              periodKey = "#002",
              status = StatusFulfilled
            )
          ))
      ))

}
