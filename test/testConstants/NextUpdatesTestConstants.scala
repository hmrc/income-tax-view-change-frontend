/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId, testSelfEmploymentId2}
import models.nextUpdates.{ObligationsModel, NextUpdateModel, NextUpdatesErrorModel, NextUpdatesModel}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object NextUpdatesTestConstants {

  def fakeNextUpdatesModel(m: NextUpdateModel): NextUpdateModel = new NextUpdateModel(m.start, m.end, m.due, m.obligationType, m.dateReceived, m.periodKey) {
    override def currentTime() = LocalDate.of(2017, 10, 31)
  }

  val testStartDate = LocalDate.of(2017, 7, 1)

  val quarterlyBusinessObligation = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2019, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val overdueObligation = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 30),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val openObligation = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 7, 1),
    end = LocalDate.of(2017, 9, 30),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#003"
  ))


  val secondQuarterlyObligation = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 10, 1),
    end = LocalDate.of(2017, 10, 31),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Quarterly",
    dateReceived = None,
    periodKey = "#002"
  ))

  val crystallisedObligation = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 10, 1),
    end = LocalDate.of(2018, 10, 30),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))

  val crystallisedObligationTwo = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2018, 10, 1),
    end = LocalDate.of(2019, 10, 30),
    due = LocalDate.of(2020, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))


  val quarterlyObligationsDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(secondQuarterlyObligation, openObligation))

  val nextUpdatesDataSelfEmploymentSuccessModel: NextUpdatesModel = NextUpdatesModel(testSelfEmploymentId, List(overdueObligation, openObligation))

  val nextUpdatesDataPropertySuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(overdueObligation, openObligation))

  val obligationsDataSelfEmploymentOnlySuccessModel: ObligationsModel = ObligationsModel(List(nextUpdatesDataSelfEmploymentSuccessModel))

  val obligationsDataPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(List(nextUpdatesDataPropertySuccessModel))

  val previousObligationOne: NextUpdateModel = NextUpdateModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 5, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 4, 1)),
    "#001"
  )

  val previousObligationTwo: NextUpdateModel = NextUpdateModel(
    LocalDate.of(2017, 4, 1),
    LocalDate.of(2017, 7, 1),
    LocalDate.of(2017, 8, 1),
    "Quarterly",
    Some(LocalDate.of(2017, 7, 1)),
    "#002"
  )

  val previousObligationThree: NextUpdateModel = NextUpdateModel(
    LocalDate.of(2017, 1, 1),
    LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 1, 1),
    "EOPS",
    Some(LocalDate.of(2018, 1, 30)),
    "EOPS"
  )

  val previousObligationFour: NextUpdateModel = NextUpdateModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 30),
    "EOPS",
    Some(LocalDate.of(2019, 1, 30)),
    "EOPS"
  )


  val previousObligationFive: NextUpdateModel = NextUpdateModel(
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 1),
    LocalDate.of(2019, 1, 31),
    "Crystallised",
    Some(LocalDate.of(2018, 1, 31)),
    "Crystallised "
  )


  val previousObligationsDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(previousObligationTwo, previousObligationOne))
  val previousObligationsEOPSDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(previousObligationThree, previousObligationFour))
  val previousObligationsCrystallisedSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(previousObligationFive))

  val nextUpdateOverdueJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-30",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#002"
  )
  val nextUpdateOpenJson = Json.obj(
    "start" -> "2017-07-01",
    "end" -> "2017-09-30",
    "due" -> "2017-10-31",
    "obligationType" -> "Quarterly",
    "periodKey" -> "#003"
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

  val overdueEOPSObligation: NextUpdateModel = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 1),
    obligationType = "EOPS",
    dateReceived = None,
    periodKey = "#002"
  ))
  val openEOPSObligation: NextUpdateModel = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2017, 10, 31),
    obligationType = "EOPS",
    dateReceived = None,
    periodKey = "#003"
  ))

  val openCrystObligation: NextUpdateModel = fakeNextUpdatesModel(NextUpdateModel(
    start = LocalDate.of(2017, 4, 6),
    end = LocalDate.of(2018, 4, 5),
    due = LocalDate.of(2019, 10, 31),
    obligationType = "Crystallised",
    dateReceived = None,
    periodKey = ""
  ))


  val obligationsEOPSDataSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(overdueEOPSObligation, openEOPSObligation))

  val obligationsCrystallisedSuccessModel: NextUpdatesModel = NextUpdatesModel(testMtditid, List(crystallisedObligation))

  val obligationsAllDeadlinesSuccessModel: ObligationsModel = ObligationsModel(Seq(nextUpdatesDataSelfEmploymentSuccessModel,
    obligationsEOPSDataSuccessModel, obligationsCrystallisedSuccessModel))

  val obligationsCrystallisedEmptySuccessModel: NextUpdatesModel = NextUpdatesModel(testNino, List())

  val obligationsPropertyOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsEOPSDataSuccessModel, obligationsCrystallisedEmptySuccessModel))

  val obligationsCrystallisedOnlySuccessModel: ObligationsModel = ObligationsModel(Seq(obligationsCrystallisedSuccessModel))

  val emptyObligationsSuccessModel: ObligationsModel = ObligationsModel(Seq())

  val nextUpdateEOPSOverdueJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end" -> "2018-04-05",
    "due" -> "2017-07-31",
    "periodKey" -> "#002"
  )
  val nextUpdateEOPSOpenJson: JsValue = Json.obj(
    "start" -> "2017-04-06",
    "end" -> "2018-04-05",
    "due" -> "2018-05-01",
    "periodKey" -> "#003"
  )
  val obligationsEOPSDataSuccessJson: JsValue = Json.obj(
    "obligations" -> Json.arr(
      nextUpdateEOPSOverdueJson,
      nextUpdateEOPSOpenJson
    )
  )


  val twoObligationsSuccessModel: NextUpdatesModel = NextUpdatesModel(testPropertyIncomeId, List(overdueObligation, openEOPSObligation))

  val crystallisedDeadlineSuccess: NextUpdatesModel = NextUpdatesModel(testMtditid, List(openCrystObligation))

  val obligationsDataErrorModel = NextUpdatesErrorModel(testErrorStatus, testErrorMessage)
  val obligations4xxDataErrorModel = NextUpdatesErrorModel(404, testErrorMessage)

  val obligationsDataErrorJson = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )

  val obligationsDataAllMinusCrystallisedSuccessModel: ObligationsModel = ObligationsModel(List(
    nextUpdatesDataPropertySuccessModel,
    nextUpdatesDataSelfEmploymentSuccessModel,
    NextUpdatesModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))

  val obligationsDataAllDataSuccessModel: ObligationsModel = ObligationsModel(List(
    nextUpdatesDataPropertySuccessModel,
    nextUpdatesDataSelfEmploymentSuccessModel,
    crystallisedDeadlineSuccess,
    NextUpdatesModel(testSelfEmploymentId2, List(overdueObligation, openObligation))
  ))
}
