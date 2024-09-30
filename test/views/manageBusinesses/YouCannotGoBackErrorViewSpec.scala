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

package views.manageBusinesses

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType, Manage}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.YouCannotGoBackError

class YouCannotGoBackErrorViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, journeyType: JourneyType) {
    val errorView: YouCannotGoBackError = app.injector.instanceOf[YouCannotGoBackError]
    val manageSubheadingContent: String = s"${messages(s"cannotGoBack.manage.${journeyType.businessType.key}", "2022", "2023")} " +
      s"${messages("cannotGoBack.reportingMethod")} ${messages("cannotGoBack.annual")}"

    val subheadingContent: String = (journeyType.operation, journeyType.businessType) match {
      case (Add, SelfEmployment) => messages("cannotGoBack.soleTraderAdded")
      case (Add, UkProperty) => messages("cannotGoBack.ukPropertyAdded")
      case (Add, ForeignProperty) => messages("cannotGoBack.foreignPropertyAdded")
      case (_, _) => manageSubheadingContent
    }

    lazy val view: HtmlFormat.Appendable = errorView(isAgent, subheadingContent)
    lazy val document: Document = Jsoup.parse(contentAsString(view))

    val manageLink: String =
      if (isAgent) "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
      else "/report-quarterly/income-and-expenses/view/manage-your-businesses"

    def checkManageContent: Assertion = {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe manageSubheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isEmpty shouldBe true
    }
  }

  "YouCannotGoBackError - Individual" should {
    "render self employment error page" in new Setup(isAgent = false, JourneyType(Add, SelfEmployment)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property - error page" in new Setup(isAgent = false, JourneyType(Add, UkProperty)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render Foreign property - error page" in new Setup(isAgent = false, JourneyType(Add, ForeignProperty)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }

  "YouCannotGoBackError - Agent" should {
    "render self employment error page" in new Setup(isAgent = true, JourneyType(Add, SelfEmployment)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render UK property - error page" in new Setup(isAgent = true, JourneyType(Add, UkProperty)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
    "render Foreign property - error page" in new Setup(isAgent = true, JourneyType(Add, ForeignProperty)) {
      document.getElementById("title").text() shouldBe messages("cannotGoBack.heading")
      document.getElementById("subheading").text() shouldBe subheadingContent
      document.getElementById("manage-link").text() shouldBe messages("cannotGoBack.manageLink")
      document.getElementById("manage-link").attr("href") shouldBe manageLink
      document.getElementById("home-link").text() shouldBe messages("cannotGoBack.homeLink")
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }

  "Manage Journey" should {
    "display the correct content - Self Employment - Individual" in new Setup(isAgent = false, JourneyType(Manage, SelfEmployment)) {
      checkManageContent
    }
    "display the correct content - Self Employment - Agent" in new Setup(isAgent = true, JourneyType(Manage, SelfEmployment)) {
      checkManageContent
    }
    "display the correct content - UK Property - Individual" in new Setup(isAgent = false, JourneyType(Manage, UkProperty)) {
      checkManageContent
    }
    "display the correct content - UK Property - Agent" in new Setup(isAgent = true, JourneyType(Manage, UkProperty)) {
      checkManageContent
    }
    "display the correct content - Foreign Property - Individual" in new Setup(isAgent = false, JourneyType(Manage, ForeignProperty)) {
      checkManageContent
    }
    "display the correct content - Foreign Property - Agent" in new Setup(isAgent = true, JourneyType(Manage, ForeignProperty)) {
      checkManageContent
    }
  }
}
