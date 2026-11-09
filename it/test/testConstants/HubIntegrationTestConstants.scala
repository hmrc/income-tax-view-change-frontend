/*
 * Copyright 2026 HM Revenue & Customs
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

import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.liabilityCalculation.{Inputs, LiabilityCalculationResponse, Metadata, PersonalInformation}
import testConstants.BaseIntegrationTestConstants.{business1, testMtditid, testNino, getCurrentTaxYearEnd}

import java.time.LocalDate

object HubIntegrationTestConstants {

  val businessOnlyResponse: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    testMtditid,
    businesses = List(
      business1
    ),
    properties = List(),
    yearOfMigration = Some("2018")
  )

  val endYear = getCurrentTaxYearEnd.getYear - 4

  val b2CessationDate = LocalDate.of(endYear, 12, 31)

  val liabilityCalculationModelSuccessfulNotCrystallised: LiabilityCalculationResponse = LiabilityCalculationResponse(
    inputs = Inputs(PersonalInformation(taxRegime = "UK")),
    metadata = Metadata(calculationTimestamp = Some("2019-02-15T09:35:15.094Z"), calculationType = "inYear"),
    messages = None,
    calculation = None,
    submissionChannel = None
  )


}
