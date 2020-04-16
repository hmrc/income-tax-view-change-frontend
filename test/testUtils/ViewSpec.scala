/*
 * Copyright 2020 HM Revenue & Customs
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

package testUtils

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Call
import play.twirl.api.Html

trait ViewSpec extends TestSupport {

  val testCall: Call = Call("POST", "/test-url")
  val testBackUrl: String = "/test-url"

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(user)

  class Setup(page: Html) {
    val document: Document = Jsoup.parse(page.body)
    val content: Element = document.selectFirst("#content")
  }

  object Selectors {
    val h1: String = "h1"
    val backLink: String = ".back-link"
    val form: String = "form"
    val summaryError: String = "#error-summary-display ul a"
    val inputError: String = ".error-notification"
    val link: String = "a"
    val table: String = "table"
    val tableRow: String = "td"
  }

  implicit def elementsToHeadOption(elements: Elements): Option[Element] = {
    Option(elements.first())
  }

  implicit class CustomSelectors(element: Element) {

    //noinspection ScalaStyle
    def h1: Element = {
      element.select(Selectors.h1) getOrElse fail("h1 not found")
    }

    def backLink: Element = {
      element.select(Selectors.backLink) getOrElse fail("back link not found")
    }

    def form: Element = {
      element.select(Selectors.form) getOrElse fail("form not found")
    }

    def summaryError: Element = {
      element.select(Selectors.summaryError) getOrElse fail("error summary list item not found")
    }

    def inputError: Element = {
      element.select(Selectors.inputError) getOrElse fail(s"input error not found")
    }

    def link: Element = {
      element.select(Selectors.link) getOrElse fail("link element not found")
    }

    def table: Element = {
      element.select(Selectors.table) getOrElse fail("table element not found")
    }
  }

  implicit class ElementTests(element: Element) {

    def hasPageHeading(heading: String): Assertion = element.h1.text shouldBe heading

    def hasBackLinkTo(href: String): Assertion = element.backLink.attr("href") shouldBe href

    def hasCorrectLink(message: String, href: String): Assertion = {
      val link = element.link
      link.text() shouldBe message
      link.attr("href") shouldBe href
    }

    def hasFormWith(method: String, action: String): Assertion = {
      element.form.attr("method") shouldBe method
      element.form.attr("action") shouldBe action
    }

    def doesNotHave(selector: String): Assertion = {
      element.select(selector).fold(succeed)(_ => fail(s"$selector was found"))
    }

    def hasFormError(errorKey: String, errorMessage: String): Assertion = {
      element.summaryError.attr("href") shouldBe s"#$errorKey"
      element.summaryError.text shouldBe errorMessage
      element.inputError.text shouldBe errorMessage
    }

    def hasTableWithCorrectSize(size: Int): Assertion = {
      element.table.select("tr").size() shouldBe size
    }
  }

}
