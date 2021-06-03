/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.{Assertion, Succeeded}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Call
import play.twirl.api.Html

import scala.collection.JavaConversions._

trait ViewSpec extends TestSupport {

  val testCall: Call = Call("POST", "/test-url")
  val testBackUrl: String = "/test-url"

  implicit val messagesLookUp: Messages = app.injector.instanceOf[MessagesApi].preferred(individualUser)

  class Setup(page: Html) {
    val document: Document = Jsoup.parse(page.body)
    lazy val content: Element = document.selectHead("#content")
  }

  object Selectors {
    val h1: String = "h1"
    val h2: String = "h2"
    val h3: String = "h3"
    val backLink: String = ".link-back"
    val form: String = "form"
    val summaryError: String = "#error-summary-display ul a"
    val inputError: String = ".error-notification"
    val link: String = "a"
    val table: String = "table"
    val tableRow: String = "td"
    val p: String = "p"
  }

  implicit def elementsToHeadOption(elements: Elements): Option[Element] = {
    Option(elements.first())
  }

  implicit class CustomSelectors(element: Element) {

    def selectHead(selector: String): Element = element.select(selector).headOption match {
      case Some(element) => element
      case None => fail(s"$selector not found")
    }

    def selectNth(selector: String, nth: Int): Element = element.selectHead(s"$selector:nth-of-type($nth)")

    def getOptionalSelector(selector: String): Option[Element] = element.select(selector).headOption

    //scalastyle:off
    def h1: Element = {
      element.select(s"${Selectors.h1}") getOrElse fail("h1 not found")
    }

    def h2: Element = {
      element.select(s"${Selectors.h2}") getOrElse fail("h2 not found")
    }

    def h3: Element = {
      element.select(s"${Selectors.h3}") getOrElse fail("h3 not found")
    }

    //scalastyle:on

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

    def selectById(id: String): Element = {
      element.select(s"#$id") getOrElse fail("element with id not found")
    }

    def table(nthOfType: Int = 1): Element = {
      element.select(s"${Selectors.table}:nth-of-type($nthOfType)") getOrElse fail("table element not found")
    }

    def breadcrumbNav: Element = {
      element.selectHead("#breadcrumbs")
    }
  }

  implicit class ElementTests(element: Element) {

    private val errorSummaryHeadingText: String = "There is a problem"

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

    def hasTableWithCorrectSize(table: Int = 1, size: Int): Assertion = {
      element.table(table).select("tr").size() shouldBe size
    }

    def hasErrorSummary(fieldErrors: (String, String)*): Assertion = {
      val errorSummary: Element = element.selectHead("#error-summary-display")
      errorSummary.attr("role") shouldBe "alert"
      errorSummary.attr("tabindex") shouldBe "-1"

      val errorSummaryHeading: Element = errorSummary.selectHead("h2")
      errorSummaryHeading.text shouldBe errorSummaryHeadingText
      errorSummary.attr("aria-labelledby") shouldBe errorSummaryHeading.attr("id")

      fieldErrors.zipWithIndex.map {
        case ((field, error), index) =>
          val listItem: Element = errorSummary.selectHead("div").selectHead("ul").selectNth("li", index + 1)
          val errorLink = listItem.selectHead("a")
          errorLink.attr("href") shouldBe s"#$field"
          errorLink.text shouldBe error
      }.forall(_ == Succeeded) shouldBe true
    }

    def getSummaryListValueNth(index: Int = 0): Element = {
      element.getElementsByClass("govuk-summary-list__value").get(index)
    }

    def getSummaryListActions(id: String): Element = {
      element.getElementById(id)
    }

  }

}
