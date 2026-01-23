/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import mocks.auth.MockAuthActions
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockSessionService}
import models.core.IncomeSourceId.mkIncomeSourceId
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Mockito.mock
import play.api
import services._
import testConstants.BaseTestConstants.testSelfEmploymentId

trait ManageIncomeSourceDetailsHelper
  extends MockAuthActions
    with MockSessionService
    with MockDateService
    with MockITSAStatusService
    with MockCalculationListService {

  lazy val incomeSourceIdHash: String = mkIncomeSourceId(testSelfEmploymentId).toHash.hash
  lazy val heading: String = "Manage your details"

  def title() = "Manage your details - Manage your Self Assessment - GOV.UK"

  lazy val link: String = "Change"
  lazy val incomeSourceId: String = "XAIS00000000008"
  lazy val businessWithLatencyAddress: String = "8 Test New Court New Town New City NE12 6CI United Kingdom"
  lazy val businessWithLatencyAddress2: String = "64 Zoo Lane Happy Place Magical Land England ZL1 064 United Kingdom"
  lazy val unknown: String = "Unknown"
  lazy val annually: String = "Annually"
  lazy val quarterly: String = "Quarterly"
  lazy val annuallyGracePeriod: String = "Annual"
  lazy val quarterlyGracePeriod: String = "Quarterly"
  lazy val reportingMethod: String = "Reporting frequency"

  lazy val mockDateServiceInjected: DateService = mock(classOfDateService)

  override lazy val app =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[SessionService].toInstance(mockSessionService),
        api.inject.bind[DateService].toInstance(mockDateServiceInjected),
        api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService),
        api.inject.bind[CalculationListService].toInstance(mockCalculationListService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
      ).build()

  lazy val testController = app.injector.instanceOf[ManageIncomeSourceDetailsController]

  def getHeading(document: Document): String = document.select("h1:nth-child(1)").text

  def hasChangeFirstYearReportingMethodLink(document: Document): Boolean = Option(document.getElementById("change-link-1")).isDefined

  def hasChangeSecondYearReportingMethodLink(document: Document): Boolean = Option(document.getElementById("change-link-2")).isDefined

  def hasGracePeriodInfo(document: Document): Boolean = Option(document.getElementById("graceperiodinfo")).isDefined

  def hasInsetText(document: Document): Boolean = Option(document.getElementsByClass("govuk-inset-text")).isDefined

  def manageDetailsTable(document: Document): Element = document.getElementById("manage-details-table")

  def getManageDetailsSummaryRows(document: Document): Elements =
    manageDetailsTable(document)
      .getElementsByClass("govuk-summary-list__row")

  def getManageDetailsSummaryValues(document: Document): Elements =
    manageDetailsTable(document)
      .getElementsByClass("govuk-summary-list__value")

  def getManageDetailsSummaryKeys(document: Document): Elements =
    manageDetailsTable(document)
      .getElementsByClass("govuk-summary-list__key")

  def hasReportingFrequencyContent(document: Document): Boolean = Option(document.getElementById("reportingFrequency")).isDefined
}
