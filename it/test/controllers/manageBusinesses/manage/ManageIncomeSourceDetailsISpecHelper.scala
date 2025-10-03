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

package controllers.manageBusinesses.manage

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import models.UIJourneySessionData
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.LatencyDetails
import services.SessionService
import testConstants.BaseIntegrationTestConstants._

import java.time.LocalDate
import java.time.Month.APRIL

class ManageIncomeSourceDetailsISpecHelper extends ControllerISpecHelper {

  val manageSelfEmploymentShowUrl: String = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, SelfEmployment, Some(testSelfEmploymentId)).url
  val manageUKPropertyShowUrl: String = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, UkProperty, None).url
  val manageForeignPropertyShowUrl: String = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, ForeignProperty, None).url
  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd
  val lastDayOfCurrentTaxYear: LocalDate = LocalDate.of(currentTaxYear, APRIL, 5)
  val taxYear1: Int = currentTaxYear
  val taxYear2: Int = currentTaxYear + 1
  val quarterlyIndicator: String = "Q"
  val annuallyIndicator: String = "A"
  val latencyDetails: LatencyDetails = LatencyDetails(
    latencyEndDate = lastDayOfCurrentTaxYear.plusYears(1),
    taxYear1 = taxYear1.toString,
    latencyIndicator1 = quarterlyIndicator,
    taxYear2 = taxYear2.toString,
    latencyIndicator2 = annuallyIndicator
  )
  val latencyDetails2: LatencyDetails = LatencyDetails(
    latencyEndDate = lastDayOfCurrentTaxYear.plusYears(1),
    taxYear1 = taxYear1.toString,
    latencyIndicator1 = annuallyIndicator,
    taxYear2 = taxYear2.toString,
    latencyIndicator2 = quarterlyIndicator
  )

  val addressAsString: String = "64 Zoo Lane Happy Place Magical Land England ZL1 064 United Kingdom"
  val businessTradingName: String = "business"
  val businessStartDate: String = "1 January 2017"
  val businessAccountingMethod: String = "Cash basis accounting"
  val thisTestSelfEmploymentId = "ABC123456789"
  val thisTestSelfEmploymentIdHashed: String = mkIncomeSourceId(thisTestSelfEmploymentId).toHash.hash
  val messagesAnnually: String = messagesAPI("incomeSources.manage.business-manage-details.annually")
  val messagesQuarterly: String = messagesAPI("incomeSources.manage.business-manage-details.quarterly")
  val messagesAnnuallyGracePeriod: String = messagesAPI("incomeSources.manage.business-manage-details.annually.graceperiod")
  val messagesQuarterlyGracePeriod: String = messagesAPI("incomeSources.manage.business-manage-details.quarterly.graceperiod")
  val messagesChangeLinkText: String = messagesAPI("incomeSources.manage.business-manage-details.change")
  val messagesUnknown: String = messagesAPI("incomeSources.generic.unknown")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]


  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Manage, incomeSourceType).toString)


}
