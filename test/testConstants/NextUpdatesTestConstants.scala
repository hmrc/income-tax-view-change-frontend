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
import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId}

import java.time.LocalDate

object NextUpdatesTestConstants {

  val heading: String = "Your updates and deadlines"
  val title: String = "Your updates and deadlines - Manage your Self Assessment - GOV.UK"
  val summary: String = "What are the update types?"
  val summaryQuarterly: String = "Quarterly updates"
  val quarterlyLine1: String = "A quarterly update is a record of all your business income in a 3 month period."
  val quarterlyLine2: String = "Using your record-keeping software, you must send 4 quarterly updates in a year for each source of income."
  val declarationLine1: String = "Your final declaration is the last step in your tax return, where you confirm you’ve submitted all your income and expenses (to the best of your knowledge). This is done using your record-keeping software."
  val summaryDeclaration: String = "Final declaration"
  val updatesInSoftware: String = "Submitting updates in software"
  val updatesInSoftwareDesc: String = "Use your compatible record keeping software (opens in new tab) to keep digital records of all your business income and expenses. You must submit these updates through your software by each date shown."
  val info: String = "To view previously submitted updates visit the tax years page."
  val oneYearOptOutMessage: String = "You are currently reporting quarterly on a voluntary basis for the 2023 to 2024 tax year. You can choose to opt out of quarterly updates and report annually instead."
  val multiYearOptOutMessage: String = "You are currently reporting quarterly on a voluntary basis. You can choose to opt out of quarterly updates and report annually instead."
  val reportingObligationsLink: String = "Depending on your circumstances, you may be able to view and change your reporting obligations."
  val quarterly: String = "Quarterly update"
  val businessIncome: String = "Business income"
  val noNextUpdatesHeading: String = "Report deadlines"
  val noNextUpdatesTitle: String = "Report deadlines - Manage your Self Assessment - GOV.UK"
  val noNextUpdatesText: String = "You don’t have any reports due right now. Your next deadline will show here on the first Monday of next month."

  val mockedCurrentTime20171031: LocalDate = LocalDate.of(2017, 10, 31)

  def fakeNextUpdatesModel(m: SingleObligationModel): SingleObligationModel = SingleObligationModel(m.start, m.end, m.due, m.obligationType, m.dateReceived, m.periodKey, StatusFulfilled)

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

  val quarterlyObligation2016 = SingleObligationModel(
    start = LocalDate.of(2016, 5, 1),
    end = LocalDate.of(2016, 7, 30),
    due = LocalDate.of(2016, 7, 30),
    obligationType = "Quarterly",
    dateReceived = Some(LocalDate.of(2016, 7, 30)),
    periodKey = "#001",
    StatusFulfilled
  )

  val quarterlyObligation2017First = SingleObligationModel(
    start = LocalDate.of(2017, 1, 1),
    end = LocalDate.of(2017, 3, 30),
    due = LocalDate.of(2017, 3, 30),
    obligationType = "Quarterly",
    dateReceived = Some(LocalDate.of(2017, 3, 30)),
    periodKey = "#003",
    StatusFulfilled
  )

  val quarterlyObligation2017Second = SingleObligationModel(
    start = LocalDate.of(2017, 4, 1),
    end = LocalDate.of(2017, 6, 30),
    due = LocalDate.of(2017, 6, 30),
    obligationType = "Quarterly",
    dateReceived = Some(LocalDate.of(2017, 6, 30)),
    periodKey = "#004",
    StatusFulfilled
  )

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

  val nextUpdatesDataSelfEmploymentSuccessModel: GroupedObligationsModel = GroupedObligationsModel(testSelfEmploymentId, List(overdueObligation, openObligation))

  val obligationsModelDataSucessful: GroupedObligationsModel = GroupedObligationsModel(testSelfEmploymentId, List(quarterlyObligation2016, quarterlyObligation2017First, quarterlyObligation2017Second))

  val nextUpdatesDataPropertySuccessModel: GroupedObligationsModel = GroupedObligationsModel(testPropertyIncomeId, List(overdueObligation, openObligation))

  val obligationsDataSelfEmploymentOnlySuccessModel: ObligationsModel = ObligationsModel(List(nextUpdatesDataSelfEmploymentSuccessModel))

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

  val obligationsDataErrorModel = ObligationsErrorModel(testErrorStatus, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

}
