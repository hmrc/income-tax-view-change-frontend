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

package views.triggeredMigration

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.TriggeredMigration.{TriggeredMigrationAdded, TriggeredMigrationCeased, TriggeredMigrationState}
import models.core.IncomeSourceId
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.triggeredMigration.CheckHmrcRecordsView

class CheckHmrcRecordsViewSpec extends TestSupport{

  val view: CheckHmrcRecordsView = app.injector.instanceOf[CheckHmrcRecordsView]

  val noCeasedBusinesses = 0
  val oneCeasedBusiness = 1
  val twoCeasedBusinesses = 2

  class Setup(activeSoleTrader: Boolean, activeUkProperty: Boolean, activeForeignProperty: Boolean, numOfCeasedBusinesses: Int, triggeredMigrationState: Option[TriggeredMigrationState]) {
    val soleTraderDetails: List[CheckHmrcRecordsSoleTraderDetails] = if (activeSoleTrader) {
      List(CheckHmrcRecordsSoleTraderDetails(IncomeSourceId("XA00001234"), Some("business"), Some("nextUpdates.business")))
    } else {
      List.empty
    }

    val viewModel: CheckHmrcRecordsViewModel = CheckHmrcRecordsViewModel(
      soleTraderBusinesses = soleTraderDetails,
      hasActiveUkProperty = activeUkProperty,
      hasActiveForeignProperty = activeForeignProperty,
      triggeredMigrationState = triggeredMigrationState,
      numberOfCeasedBusinesses = numOfCeasedBusinesses
    )
    val pageDocument = Jsoup.parse(contentAsString(view(viewModel, isAgent = false)))
  }

  object CheckHmrcRecordsMessages {
    val heading = "Check HMRC records only list your active businesses"
    val title = "Check HMRC records only list your active businesses - Manage your Self Assessment - GOV.UK"
    val desc = "You now have quarterly deadlines for your sole trader and/or property businesses listed here."
    val inset = "Making sure this page is correct will help avoid both missing deadlines for your active businesses and having deadlines for an income source you may have closed down or sold."
    val bulletStart = "If necessary, you must:"
    val bullet1 = "add any businesses that are missing"
    val bullet2 = "cease any that you no longer get income from"

    val yourActiveBusinessesHeading = "Your active businesses"
    val soleTraderHeading = "Sole trader businesses"
    val addASoleTraderBusinessText = "Add a sole trader business"
    val soleTraderGuidance = "You’re self-employed if you run your own business as an individual and work for yourself. This is also known as being a ’sole trader’. If you work through a limited company, you’re not a sole trader."

    val propertyHeading = "Property businesses"
    val ukPropertyHeading = "UK property"
    val foreignPropertyHeading = "Foreign property"
    val addAPropertyBusinessText = "Add a property business"
    val addForeignPropertyBusinessText = "Add foreign property business"
    val noActivePropertyText = "If you get income from one or more properties in the UK, you have a UK property business. If the property is abroad, you have a foreign property business. For example: letting houses, flats or holiday homes either on a long or short term basis."

    val ceaseText = "Cease"
    val ceaseLinkUkPropertyHref = "/report-quarterly/income-and-expenses/view/manage-your-businesses/cease/uk-property-confirm-cease?isTriggeredMigration=true"
    val ceaseLinkForeignPropertyHref = "/report-quarterly/income-and-expenses/view/manage-your-businesses/cease/foreign-property-confirm-cease?isTriggeredMigration=true"
    val businessNameText = "Business name"
    val businessStateText = "Business state"
    val activeText = "Active"
    val unknownText = "Unknown"

    val confirmRecordsHeading = "Confirm HMRC records only list your active businesses"
    val confirmRecordsText = "This page only needs to list all your active sole trader and property income sources. Any other business details that are not right, misspelt or out of date, can be amended at a later date."
    val confirmRecordsButton = "Confirm and continue"

    val ceasedBannerHeading = "Ceased"
    val ceasedBannerText = "You have ceased a business"
    val ceasedBannerLinkText = "View your ceased businesses"
    val ceasedSectionHeading = "Your ceased businesses"
    val ceasedSectionText = "One or more businesses have ceased."
    val ceasedSectionLinkText = "View all ceased businesses"

    val addedBannerHeading = "Added"
    val addedBannerTextSelfEmployment = "Sole trader business added"
    val addedBannerTextUkProperty = "UK property business added"
    val addedBannerTextForeignProperty = "Foreign property business added"
    val addedBannerLinkTextSelfEmployment = "View your sole trader business"
    val addedBannerLinkTextUkProperty = "View your UK property business"
    val addedBannerLinkTextForeignProperty = "View your foreign property business"
  }

  "Check HMRC records page" when {
    "checking hmrc records with an active sole trader, uk and foreign property business" should {
      checkCommonContent(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true)

      "have the correct sole trader business details" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("sole-trader-business-0").text() shouldBe "business"
        pageDocument.getElementById("sole-trader-business-name-0").text() shouldBe CheckHmrcRecordsMessages.businessNameText
        pageDocument.getElementById("sole-trader-business-name-value-0").text() shouldBe "nextUpdates.business"
        pageDocument.getElementById("sole-trader-business-state-0").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("sole-trader-business-state-value-0").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("sole-trader-cease-link-0").text() shouldBe CheckHmrcRecordsMessages.ceaseText
      }

      "have the correct uk property details" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("uk-property-heading").text() shouldBe CheckHmrcRecordsMessages.ukPropertyHeading
        pageDocument.getElementById("uk-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("uk-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("uk-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("uk-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkUkPropertyHref
      }

      "have the correct foreign property details" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("foreign-property-heading").text() shouldBe CheckHmrcRecordsMessages.foreignPropertyHeading
        pageDocument.getElementById("foreign-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("foreign-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("foreign-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("foreign-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkForeignPropertyHref
      }
    }
    "checking hmrc records with no active sole trader, uk or foreign property business" should {
      checkCommonContent(activeSoleTrader = false, activeUkProperty = false, activeForeignProperty = false)

      "have the correct property details" in new Setup(activeSoleTrader = false, activeUkProperty = false, activeForeignProperty = false, noCeasedBusinesses, None) {
        pageDocument.getElementById("property-no-active-business-desc").text() shouldBe CheckHmrcRecordsMessages.noActivePropertyText
        pageDocument.getElementById("uk-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addAPropertyBusinessText
        pageDocument.getElementById("foreign-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addForeignPropertyBusinessText
      }
    }
    "checking hmrc records with an active sole trader business and no active uk or foreign property business" should {
      checkCommonContent(activeSoleTrader = true, activeUkProperty = false, activeForeignProperty = false)

      "have the correct sole trader business details" in new Setup(activeSoleTrader = true, activeUkProperty = false, activeForeignProperty = false,noCeasedBusinesses, None) {
        pageDocument.getElementById("sole-trader-business-0").text() shouldBe "business"
        pageDocument.getElementById("sole-trader-business-name-0").text() shouldBe CheckHmrcRecordsMessages.businessNameText
        pageDocument.getElementById("sole-trader-business-name-value-0").text() shouldBe "nextUpdates.business"
        pageDocument.getElementById("sole-trader-business-state-0").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("sole-trader-business-state-value-0").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("sole-trader-cease-link-0").text() shouldBe CheckHmrcRecordsMessages.ceaseText
      }

      "have the correct property details" in new Setup(activeSoleTrader = true, activeUkProperty = false, activeForeignProperty = false, noCeasedBusinesses, None) {
        pageDocument.getElementById("property-no-active-business-desc").text() shouldBe CheckHmrcRecordsMessages.noActivePropertyText
        pageDocument.getElementById("uk-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addAPropertyBusinessText
        pageDocument.getElementById("foreign-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addForeignPropertyBusinessText
      }
    }
    "checking hmrc records with no active sole trader business and an active uk and foreign property business" should {
      checkCommonContent(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = true)

      "have the correct uk property details" in new Setup(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("uk-property-heading").text() shouldBe CheckHmrcRecordsMessages.ukPropertyHeading
        pageDocument.getElementById("uk-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("uk-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("uk-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("uk-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkUkPropertyHref

      }

      "have the correct foreign property details" in new Setup(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("foreign-property-heading").text() shouldBe CheckHmrcRecordsMessages.foreignPropertyHeading
        pageDocument.getElementById("foreign-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("foreign-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("foreign-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("foreign-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkForeignPropertyHref
      }
    }
    "checking hmrc records with no active sole trader business and an active uk property business only" should {
      checkCommonContent(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = false)

      "have the correct uk property details" in new Setup(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = false, noCeasedBusinesses, None) {
        pageDocument.getElementById("uk-property-heading").text() shouldBe CheckHmrcRecordsMessages.ukPropertyHeading
        pageDocument.getElementById("uk-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("uk-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("uk-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("uk-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkUkPropertyHref
      }

      "have the correct foreign property link" in new Setup(activeSoleTrader = false, activeUkProperty = true, activeForeignProperty = false, noCeasedBusinesses, None) {
        pageDocument.getElementById("foreign-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addForeignPropertyBusinessText
        pageDocument.getElementById("foreign-property-add-link").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses/add-foreign-property/business-start-date?isTriggeredMigration=true"
      }
    }
    "checking hmrc records with no active sole trader business and an active foreign property business only" should {
      checkCommonContent(activeSoleTrader = false, activeUkProperty = false, activeForeignProperty = true)

      "have the correct foreign property details" in new Setup(activeSoleTrader = false, activeUkProperty = false, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("foreign-property-heading").text() shouldBe CheckHmrcRecordsMessages.foreignPropertyHeading
        pageDocument.getElementById("foreign-property-business-state").text() shouldBe CheckHmrcRecordsMessages.businessStateText
        pageDocument.getElementById("foreign-property-business-state-value").text() shouldBe CheckHmrcRecordsMessages.activeText
        pageDocument.getElementById("foreign-property-cease-link").text() shouldBe CheckHmrcRecordsMessages.ceaseText
        pageDocument.getElementById("foreign-property-cease-link").attr("href") shouldBe CheckHmrcRecordsMessages.ceaseLinkForeignPropertyHref
      }

      "have the add uk property link" in new Setup(activeSoleTrader = false, activeUkProperty = false, activeForeignProperty = true, noCeasedBusinesses, None) {
        pageDocument.getElementById("uk-property-add-link").text() shouldBe CheckHmrcRecordsMessages.addAPropertyBusinessText
        pageDocument.getElementById("uk-property-add-link").select("a").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses/add-uk-property/business-start-date?isTriggeredMigration=true"
      }
    }

    "checking hmrc records with ceased businesses and ceased banner shown" should {
      "display the ceased banner after ceasing a business" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, Some(TriggeredMigrationCeased)) {
        pageDocument.getElementById("ceased-banner-heading").text() shouldBe CheckHmrcRecordsMessages.ceasedBannerHeading
        pageDocument.getElementById("ceased-banner-text-1").text() shouldBe CheckHmrcRecordsMessages.ceasedBannerText
        pageDocument.getElementById("ceased-banner-text-2").text() shouldBe CheckHmrcRecordsMessages.ceasedBannerLinkText
      }

      "display the ceased section when user has at least one ceased business" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, None) {
        pageDocument.getElementById("ceased-section-heading").text() shouldBe CheckHmrcRecordsMessages.ceasedSectionHeading
        pageDocument.getElementById("ceased-section-text").text() shouldBe CheckHmrcRecordsMessages.ceasedSectionText
        pageDocument.getElementById("ceased-section-link").text() shouldBe CheckHmrcRecordsMessages.ceasedSectionLinkText
      }
    }
    "checking hmrc records with ceased businesses and ceased banner not shown" should {
      "not display the ceased banner or ceased section when there's no ceased businesses" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, noCeasedBusinesses, None) {
        Option(pageDocument.getElementById("ceased-banner-heading")) shouldBe None
        Option(pageDocument.getElementById("ceased-section-heading")) shouldBe None
      }
    }

    "checking hmrc records with added banner shown" should {
      "display the added banner after adding a self employment business" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, Some(TriggeredMigrationAdded(SelfEmployment))) {
        pageDocument.getElementById("added-banner-heading").text() shouldBe CheckHmrcRecordsMessages.addedBannerHeading
        pageDocument.getElementById("added-banner-text-1").text() shouldBe CheckHmrcRecordsMessages.addedBannerTextSelfEmployment
        pageDocument.getElementById("added-banner-text-2").text() shouldBe CheckHmrcRecordsMessages.addedBannerLinkTextSelfEmployment
      }

      "display the added banner after adding a uk property business" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, Some(TriggeredMigrationAdded(UkProperty))) {
        pageDocument.getElementById("added-banner-heading").text() shouldBe CheckHmrcRecordsMessages.addedBannerHeading
        pageDocument.getElementById("added-banner-text-1").text() shouldBe CheckHmrcRecordsMessages.addedBannerTextUkProperty
        pageDocument.getElementById("added-banner-text-2").text() shouldBe CheckHmrcRecordsMessages.addedBannerLinkTextUkProperty
      }

      "display the added banner after adding a foreign property business" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, Some(TriggeredMigrationAdded(ForeignProperty))) {
        pageDocument.getElementById("added-banner-heading").text() shouldBe CheckHmrcRecordsMessages.addedBannerHeading
        pageDocument.getElementById("added-banner-text-1").text() shouldBe CheckHmrcRecordsMessages.addedBannerTextForeignProperty
        pageDocument.getElementById("added-banner-text-2").text() shouldBe CheckHmrcRecordsMessages.addedBannerLinkTextForeignProperty
      }

      "not display the added banner when no business has been added" in new Setup(activeSoleTrader = true, activeUkProperty = true, activeForeignProperty = true, oneCeasedBusiness, None) {
        Option(pageDocument.getElementById("added-banner-heading")) shouldBe None
      }
    }
  }

  def checkCommonContent(activeSoleTrader: Boolean, activeUkProperty: Boolean, activeForeignProperty: Boolean): Unit = {
    "have the correct title" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.title() shouldBe CheckHmrcRecordsMessages.title
    }
    "have the correct heading" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.select("h1").text() shouldBe CheckHmrcRecordsMessages.heading
    }

    "have the correct description" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("check-hmrc-records-desc").text() shouldBe CheckHmrcRecordsMessages.desc
    }

    "have the correct inset" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("check-hmrc-records-inset").text() shouldBe CheckHmrcRecordsMessages.inset
    }

    "have the correct bullet points" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("check-hmrc-records-bullet-start").text() shouldBe CheckHmrcRecordsMessages.bulletStart
      pageDocument.getElementById("check-hmrc-records-bullets").text() shouldBe CheckHmrcRecordsMessages.bullet1 + " " + CheckHmrcRecordsMessages.bullet2
    }

    "have the correct your active businesses heading" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("your-active-businesses-heading").text() shouldBe CheckHmrcRecordsMessages.yourActiveBusinessesHeading
    }

    "have the correct sole trader heading" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("sole-trader-heading").text() shouldBe CheckHmrcRecordsMessages.soleTraderHeading
    }

    "have the correct sole trader guidance" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("sole-trader-guidance").text() shouldBe CheckHmrcRecordsMessages.soleTraderGuidance
    }

    "have the correct property heading" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("property-heading").text() shouldBe CheckHmrcRecordsMessages.propertyHeading
    }

    "have the correct add a sole trader business text" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("sole-trader-add-link").text() shouldBe CheckHmrcRecordsMessages.addASoleTraderBusinessText
      pageDocument.getElementById("sole-trader-add-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses/add-sole-trader/business-name?isTriggeredMigration=true"
    }

    "have the correct confirm records heading" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("confirm-records-heading").text() shouldBe CheckHmrcRecordsMessages.confirmRecordsHeading
    }

    "have the correct confirm records text" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("confirm-records-desc").text() shouldBe CheckHmrcRecordsMessages.confirmRecordsText
    }

    "confirm and continue button is displayed" in new Setup(activeSoleTrader = activeSoleTrader, activeUkProperty = activeUkProperty, activeForeignProperty = activeForeignProperty, noCeasedBusinesses, None) {
      pageDocument.getElementById("continue-button").text() shouldBe CheckHmrcRecordsMessages.confirmRecordsButton
    }
  }
}
