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

package views.manageBusinesses

import enums.IncomeSourceJourney.SelfEmployment
import models.core.IncomeSourceId
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, ViewBusinessDetailsViewModel, ViewIncomeSourcesViewModel, ViewPropertyDetailsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.ManageYourBusinessesView

import java.time.LocalDate

class ManageYourBusinessesViewSpec extends TestSupport {

  val view: ManageYourBusinessesView = app.injector.instanceOf[ManageYourBusinessesView]

  val businessDetailsViewModel: ViewBusinessDetailsViewModel = ViewBusinessDetailsViewModel(
    IncomeSourceId("ID"),
    Some("Income-Source"),
    Some("Trading-Name"),
    Some(LocalDate.of(2024, 1, 1))
  )

  val propertyDetailsViewModel: ViewPropertyDetailsViewModel = ViewPropertyDetailsViewModel(
    Some(LocalDate.of(2024, 1, 1))
  )

  val ceasedBusinessDetailsViewModel: CeasedBusinessDetailsViewModel = CeasedBusinessDetailsViewModel(
    Some("Trading-Name"),
    SelfEmployment,
    Some(LocalDate.of(2024, 1, 1)),
    LocalDate.of(2024, 1, 2)
  )

  "ManageYourBusinessesView" when {
    "the user is an Agent" should {
      "return the correct content when the user has a sole trader business" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkSoleTraderComponents(pageDocument)

      }
      "return the correct content when the user has a uk property business" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = None,
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkUkPropertyComponents(pageDocument)
      }
      "return the correct content when the user has a foreign property business" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkForeignPropertyComponents(pageDocument)

      }

      "return the correct content when the user has 1 ceased business" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = false)
      }

      "return the correct content when the user has multiple ceased businesses" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel, ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = true)


      }

      "return the correct content when the user has all business types" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        checkAgentTitle(pageDocument)
        checkSoleTraderComponents(pageDocument)
        checkUkPropertyComponents(pageDocument)
        checkForeignPropertyComponents(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = false)
      }


      "return no business start dates when the displayStartDate flag is false" in {
        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = false
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = true,
                backUrl = ""
              )
            )
          )

        pageDocument.select("#business-date-0").isEmpty shouldBe true
        pageDocument.select("#foreign-date").isEmpty shouldBe true
        pageDocument.select("#uk-date").isEmpty shouldBe true
      }
    }

    "the user is an individual" should {
      "return the correct content when the user has a sole trader business" in {
        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkSoleTraderComponents(pageDocument)
      }
      "return the correct content when the user has a uk property business" in {
        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = None,
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkUkPropertyComponents(pageDocument)
      }
      "return the correct content when the user has a foreign property business" in {
        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkForeignPropertyComponents(pageDocument)
      }

      "return the correct content when the user has 1 ceased business" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = false)
      }

      "return the correct content when the user has multiple ceased businesses" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(),
            viewUkProperty = None,
            viewForeignProperty = None,
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel, ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = true)
      }

      "return the correct content when the user has all business types" in {

        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        checkTitle(pageDocument)
        checkSoleTraderComponents(pageDocument)
        checkUkPropertyComponents(pageDocument)
        checkForeignPropertyComponents(pageDocument)
        checkCeasedBusinessComponents(pageDocument, isMultiple = false)
      }

      "return no business start dates when the displayStartDate flag is false" in {
        val viewIncomeSourcesViewModel: ViewIncomeSourcesViewModel =
          ViewIncomeSourcesViewModel(
            viewSoleTraderBusinesses = List(businessDetailsViewModel),
            viewUkProperty = Some(propertyDetailsViewModel),
            viewForeignProperty = Some(propertyDetailsViewModel),
            viewCeasedBusinesses = List(ceasedBusinessDetailsViewModel),
            displayStartDate = false
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                sources = viewIncomeSourcesViewModel,
                isAgent = false,
                backUrl = ""
              )
            )
          )

        pageDocument.select("#business-date-0").isEmpty shouldBe true
        pageDocument.select("#foreign-date").isEmpty shouldBe true
        pageDocument.select("#uk-date").isEmpty shouldBe true
      }
    }
  }

  def checkTitle(pageDocument: Document): Unit = {
    pageDocument.title() shouldBe "Your businesses - Manage your Self Assessment - GOV.UK"
  }

  def checkAgentTitle(pageDocument: Document): Unit = {
    pageDocument.title() shouldBe "Your businesses - Manage your Self Assessment - GOV.UK"
  }

  def checkSoleTraderComponents(pageDocument: Document): Unit = {
    pageDocument.getElementById("self-employed-h1").text() shouldBe "Sole trader businesses"
    pageDocument.getElementById("self-employed-desc").text() shouldBe "You’re a sole trader if you run your own business as an individual and work for yourself. This is also known as being self‑employed."
    pageDocument.getElementById("self-employed-inset").text() shouldBe "Do not add limited companies or business partnerships here."
    pageDocument.getElementById("business-type-0").text() shouldBe "Income-Source"
    pageDocument.getElementById("view-manage-link-0").text() shouldBe "View and manage Income-Source"
    pageDocument.getElementById("cease-link-0").text() shouldBe "Cease Income-Source"
    /* Adding test below which checks for the visually hidden content */
    pageDocument.select("#view-manage-link-0 > a > span").text() shouldBe "Income-Source"
    pageDocument.getElementById("business-date-started-text-0").text() shouldBe "Date started"
    pageDocument.getElementById("business-date-0").text() shouldBe "1 January 2024"
    pageDocument.getElementById("business-trade-name-0").text() shouldBe "Trading-Name"
    pageDocument.getElementById("business-trade-name-text-0").text() shouldBe "Name"
  }

  def checkUkPropertyComponents(pageDocument: Document): Unit = {
    pageDocument.getElementById("uk-property-title").text() shouldBe "UK property"
    pageDocument.getElementById("uk-view-manage-link").text() shouldBe "View and manage UK property"
    pageDocument.getElementById("uk-cease-link").text() shouldBe "Cease UK property"
    /* Adding test below which checks for the visually hidden content */
    pageDocument.select("#uk-view-manage-link > a > span").text() shouldBe "UK property"
    pageDocument.getElementById("uk-date-text").text() shouldBe "Start date"
    pageDocument.getElementById("uk-date").text() shouldBe "1 January 2024"
  }

  def checkForeignPropertyComponents(pageDocument: Document): Unit = {
    pageDocument.getElementById("foreign-property-title").text() shouldBe "Foreign property"
    pageDocument.getElementById("foreign-view-manage-link").text() shouldBe "View and manage Foreign property"
    pageDocument.getElementById("foreign-cease-link").text() shouldBe "Cease Foreign property"
    /* Adding test below which checks for the visually hidden content */
    pageDocument.select("#foreign-view-manage-link > a > span").text() shouldBe "Foreign property"
    pageDocument.getElementById("foreign-date-text").text() shouldBe "Start date"
    pageDocument.getElementById("foreign-date").text() shouldBe "1 January 2024"
  }

  def checkCeasedBusinessComponents(pageDocument: Document, isMultiple: Boolean): Unit = {
    pageDocument.getElementById("ceasedBusinesses-heading").text() shouldBe "Businesses that have ceased"

    if (isMultiple) {
      pageDocument.getElementById("multiple-ceased-business").text() shouldBe "2 businesses have ceased."
    } else {
      pageDocument.getElementById("single-ceased-business").text() shouldBe "1 business has ceased."
    }

    pageDocument.getElementById("ceasedBusinesses-viewall").text() shouldBe "View all ceased businesses"
  }

}
