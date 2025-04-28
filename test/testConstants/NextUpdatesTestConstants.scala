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

package testConstants

import models.obligations._
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId, testSelfEmploymentId2}

import java.time.LocalDate

object NextUpdatesTestConstants {

  val mockedCurrentTime20171031: LocalDate = LocalDate.of(2017, 10, 31)

  def fakeNextUpdatesModel(m: SingleObligationModel): SingleObligationModel = new SingleObligationModel(m.start, m.end, m.due, m.obligationType, m.dateReceived, m.periodKey, StatusFulfilled)

  val testStartDate = LocalDate.of(2017, 7, 1)

  val quarterlyBusinessObligation = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2019, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002",
    StatusFulfilled
  ))

  val overdueObligation = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002",
    StatusFulfilled
  ))

  val openObligation = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = mockedCurrentTime20171031,
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#003",
    StatusFulfilled
  ))


  val secondQuarterlyObligation = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 10, 1),
    end = mockedCurrentTime20171031,
    due = mockedCurrentTime20171031,
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002",
    StatusFulfilled
  ))

  val crystallisedObligation = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 10, 1),
    end = LocalDate.of(2018, 10, 30),
    due = mockedCurrentTime20171031,
    obligationType = "Crystallisation",
    dateReceived = None,
    periodKey = "",
    StatusFulfilled
  ))

  val crystallisedObligationTwo = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2018, 10, 1),
    end = LocalDate.of(2019, 10, 30),
    due = LocalDate.of(2020, 10, 31),
    obligationType = "Crystallisation",
    dateReceived = None,
    periodKey = "",
    StatusFulfilled
  ))


  val quarterlyObligationsDataSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(secondQuarterlyObligation, openObligation))

  val nextUpdatesDataSelfEmploymentSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testSelfEmploymentId, List(overdueObligation, openObligation))

  val nextUpdatesDataPropertySuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(overdueObligation, openObligation))

  val obligationsDataSelfEmploymentOnlySuccessModel: ObligationsModel = ObligationsModel(List(nextUpdatesDataSelfEmploymentSuccessModel))

  val obligationsDataPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(List(nextUpdatesDataPropertySuccessModel))

  val previousObligationOne: SingleObligationModel = SingleObligationModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 5, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 4, 1)),
    "#001",
    StatusFulfilled
  )

  val previousObligationTwo: SingleObligationModel = SingleObligationModel(
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 7, 1),
    LocalDate.of(2017, 8, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 7, 1)),
    "#002",
    StatusFulfilled
  )

  val previousObligationThree: SingleObligationModel = SingleObligationModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 1, 1),
    "Quarterly",
    Some(LocalDate.of(2018, 1, 30)),
    "#003",
    StatusFulfilled
  )

  val previousObligationFour: SingleObligationModel = SingleObligationModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 30),
    "Quarterly",
    Some(LocalDate.of(2019, 1, 30)),
    "#004",
    status = StatusFulfilled
  )


  val previousObligationFive: SingleObligationModel = SingleObligationModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 31),
    "Crystallisation",
    Some(LocalDate.of(2018, 1, 31)),
    "Crystallisation",
    status = StatusFulfilled
  )


  val previousObligationsDataSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(previousObligationTwo, previousObligationOne))
  val previousObligationsEOPSDataSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(previousObligationThree, previousObligationFour))
  val previousObligationsCrystallisedSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(previousObligationFive))

  val nextUpdateOverdueJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-30",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#002",
    "status" -> StatusFulfilled.toString
  )
  val nextUpdateOpenJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-31",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#003",
    "status" -> StatusFulfilled.toString
  )
  val obligationsDataSuccessJson = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      nextUpdateOverdueJson,
      nextUpdateOpenJson
    )
  )

  val nextUpdatesDataFromJson: JsValue = Json.obj(
    "identification" -> testSelfEmploymentId,
    "obligations" -> Json.arr(
      nextUpdateOverdueJson,
      nextUpdateOpenJson
    )
  )

  val obligationsDataFromJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      nextUpdatesDataFromJson
    )
  )

  val overdueQuarterlyObligation: SingleObligationModel = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 1),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002",
    status = StatusFulfilled
  ))
  val openQuarterlyObligation: SingleObligationModel = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = mockedCurrentTime20171031,
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#003",
    status = StatusFulfilled
  ))

  val openCrystObligation: SingleObligationModel = fakeNextUpdatesModel(SingleObligationModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2019, 10, 31),
    obligationType = "Crystallisation",
    dateReceived = None,
    periodKey = "",
    status = StatusFulfilled
  ))


  val obligationsQuarterlyDataSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(overdueQuarterlyObligation, openQuarterlyObligation))

  val obligationsCrystallisedSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testMtditid, List(crystallisedObligation))

  val obligationsAllDeadlinesSuccessModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel,
    obligationsQuarterlyDataSuccessModel, obligationsCrystallisedSuccessModel))

  val obligationsAllDeadlinesWithDateReceivedSuccessModel: ObligationsModel = ObligationsModel(
    Seq(
      nextUpdatesDataSelfEmploymentSuccessModel.copy(
        obligations = List(
          openObligation.copy(dateReceived = Some(mockedCurrentTime20171031.plusDays(1))))
      ),
      obligationsQuarterlyDataSuccessModel.copy(
        obligations = List(
          overdueQuarterlyObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(3)))
        )
      ),
      obligationsCrystallisedSuccessModel.copy(
        obligations = List(
          crystallisedObligation.copy(dateReceived = Some(mockedCurrentTime20171031.minusDays(6)))))
    )
  )

  val obligationsCrystallisedEmptySuccessModel: GroupedObligationsModel = GroupedObligationsModel(testNino, List())

  val obligationsPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsQuarterlyDataSuccessModel, obligationsCrystallisedEmptySuccessModel))

  val obligationsCrystallisedOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsCrystallisedSuccessModel))

  val emptyObligationsSuccessModel: ObligationsModel = ObligationsModel(Seq())
  val obligationsSuccessModelFiltered: ObligationsModel = ObligationsModel(Seq(GroupedObligationsModel("ident", List.empty)))

  val twoObligationsSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(overdueObligation, openQuarterlyObligation))

  val crystallisedDeadlineSuccess: GroupedObligationsModel = GroupedObligationsModel(testMtditid, List(openCrystObligation))

  val obligationsDataErrorModel = ObligationsErrorModel(testErrorStatus, testErrorMessage)

  def nextUpdatesErrorModel(status: Int) = ObligationsErrorModel(status, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val obligationsDataAllMinusCrystallisedSuccessModel: ObligationsModel = ObligationsModel(List(
    nextUpdatesDataPropertySuccessModel,
    nextUpdatesDataSelfEmploymentSuccessModel,
    GroupedObligationsModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))

  val obligationsDataAllDataSuccessModel: ObligationsModel = ObligationsModel(List(
    nextUpdatesDataPropertySuccessModel,
    nextUpdatesDataSelfEmploymentSuccessModel,
    crystallisedDeadlineSuccess,
    GroupedObligationsModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))
}
