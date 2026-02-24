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

package controllers

import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.MtdItUser
import enums.MTDIndividual
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks._
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.ObligationsModel
import org.mongodb.scala.bson.BsonDocument
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NextUpdatesIntegrationTestConstants._
import org.mongodb.scala.{SingleObservableFuture, ObservableFuture}

class NextUpdatesControllerForOptOutISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    optOutSessionDataRepository.repository.collection.deleteMany(BsonDocument()).toFuture().futureValue
  }

  val path = "/submission-deadlines"

  s"GET $path" when {

    val testPropertyOnlyUser: MtdItUser[_] = getTestUser(MTDIndividual, ukPropertyOnlyResponse)

    "one year opt-out scenarios" ignore {

      "show opt-out message if the user has Previous Year as Voluntary, Current Year as NoStatus, Next Year as NoStatus" in {
        enable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()
        optOutSessionDataRepository.saveIntent(TaxYear.forYearEnd(2024)).futureValue

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())


        val savedTaxYearOpn: Option[TaxYear] = optOutSessionDataRepository.fetchSavedIntent().futureValue
        savedTaxYearOpn.isDefined shouldBe true
        savedTaxYearOpn.get shouldBe TaxYear.forYearEnd(2024)

        val res = buildGETMTDClient(path).futureValue

        val afterResetOpn: Option[TaxYear] = optOutSessionDataRepository.fetchSavedIntent().futureValue
        //assert intent tax-year is reset to none
        afterResetOpn.isDefined shouldBe false

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#one-year-opt-out-message")(expectedValue = "You are currently reporting quarterly on a " +
            "voluntary basis for the 2021 to 2022 tax year. You can choose to opt out of quarterly updates and " +
            "report annually instead.")
        )

      }

      "show multi year opt-out message if the user has Previous Year as Voluntary, Current Year as Voluntary, Next Year as Voluntary" in {
        enable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino,
          previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        optOutSessionDataRepository.saveIntent(TaxYear.forYearEnd(2024)).futureValue
        val savedTaxYearOpn: Option[TaxYear] = optOutSessionDataRepository.fetchSavedIntent().futureValue
        savedTaxYearOpn.isDefined shouldBe true
        savedTaxYearOpn.get shouldBe TaxYear.forYearEnd(2024)

        val res = buildGETMTDClient(path).futureValue

        val afterResetOpn: Option[TaxYear] = optOutSessionDataRepository.fetchSavedIntent().futureValue
        //assert intent tax-year is reset to none
        afterResetOpn.isDefined shouldBe false

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#multi-year-opt-out-message")(expectedValue = "You are currently reporting quarterly on a " +
            "voluntary basis. You can choose to opt out of quarterly updates and report annually instead.")
        )
      }
    }

  }

}
