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

package helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest._
import org.scalatest.matchers._
import play.api.libs.json.Reads
import play.api.libs.ws.WSResponse

import scala.annotation.tailrec
import scala.collection.JavaConverters._

trait CustomMatchers extends UnitSpec with GivenWhenThen {

  def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
    new HavePropertyMatcher[WSResponse, Int] {
      def apply(response: WSResponse) = {
        Then(s"the response status should be '$expectedValue'")
        HavePropertyMatchResult(
          response.status == expectedValue,
          "httpStatus",
          expectedValue,
          response.status
        )
      }
    }

  def jsonBodyAs[T](expectedValue: T)(implicit reads: Reads[T]): HavePropertyMatcher[WSResponse, T] =
    new HavePropertyMatcher[WSResponse, T] {
      def apply(response: WSResponse) = {
        Then(s"the response json should be '${expectedValue.toString}'")
        HavePropertyMatchResult(
          response.json.as[T] == expectedValue,
          "jsonBodyAs",
          expectedValue,
          response.json.as[T]
        )
      }
    }

  def emptyBody: HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse) = {
        Then(s"the response body should be empty")
        HavePropertyMatchResult(
          response.body == "",
          "emptyBody",
          "",
          response.body
        )
      }
    }

  def pageTitle(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)
        Then(s"the page title should be '$expectedValue'")
        HavePropertyMatchResult(
          body.title == expectedValue,
          "pageTitle",
          expectedValue,
          body.title
        )
      }
    }

  def elementValueByID(id: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {
      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)
        Then(s"the value of elementId '$id' should be '$expectedValue'")
        HavePropertyMatchResult(
          body.select(s"#$id").`val` == expectedValue,
          s"elementByID($id)",
          expectedValue,
          body.select(s"#$id").`val`
        )
      }
    }

  def elementTextByID(id: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    new HavePropertyMatcher[WSResponse, String] {

      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)
        Then(s"the text of elementId '$id' should be '$expectedValue'")

        HavePropertyMatchResult(
          body.select(s"#$id").text == expectedValue,
          s"elementByID($id)",
          expectedValue,
          body.select(s"#$id").text
        )
      }
    }

  def elementTextBySelector(selector: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] = {
    HavePropertyMatcher[WSResponse, String] { response =>
      val body = Jsoup.parse(response.body)
      Then(s"the text of element should be '$expectedValue'")

      HavePropertyMatchResult(
        body.select(selector).text == expectedValue,
        s"select($selector)",
        expectedValue,
        body.select(selector).text
      )
    }
  }

  def elementAttributeBySelector(selector: String, attr: String)(expectedValue: String): HavePropertyMatcher[WSResponse, String] = {
    HavePropertyMatcher[WSResponse, String] { response =>
      val body = Jsoup.parse(response.body)
      Then(s"the text of element should be '$expectedValue'")

      HavePropertyMatchResult(
        body.select(selector).attr(attr) == expectedValue,
        s"select($selector)",
        expectedValue,
        body.select(selector).attr(attr)
      )
    }
  }

  def elementTextBySelectorList(selectors: String*)(expectedValue: String): HavePropertyMatcher[WSResponse, String] = {
    HavePropertyMatcher[WSResponse, String] { response =>
      val body: Element = Jsoup.parse(response.body).body()
      Then(s"the text of the element should be '$expectedValue'")
      def selectHead(element: Element, selector: String): Element = {
        element.select(selector).asScala.headOption match {
          case Some(element) => element
          case None => fail(s"Could not find element with selector: $selector")
        }
      }

      @tailrec
      def recursiveSelector(selectors: List[String], currentElement: Element): Element = {
        selectors match {
          case head :: tail => recursiveSelector(tail, selectHead(currentElement, head))
          case Nil => currentElement
        }
      }

      val selectedText: String = recursiveSelector(selectors.toList, body).text
      HavePropertyMatchResult(
        selectedText == expectedValue,
        s"selecting $selectors",
        expectedValue,
        selectedText
      )
    }
  }

  def nElementsWithClass(classTag: String)(expectedCount: Int): HavePropertyMatcher[WSResponse, Int] =
    new HavePropertyMatcher[WSResponse, Int] {
      def apply(response: WSResponse) = {
        val body = Jsoup.parse(response.body)
        Then(s"there should be $expectedCount elements with the class '$classTag'")
        HavePropertyMatchResult(
          body.getElementsByClass(classTag).size() == expectedCount,
          s"countElementsOfClass($classTag)",
          expectedCount,
          body.getElementsByClass(classTag).size()
        )
      }
    }

  def redirectURI(expectedValue: String): HavePropertyMatcher[WSResponse, String] = new HavePropertyMatcher[WSResponse, String] {
    def apply(response: WSResponse) = {
      val redirectLocation: Option[String] = response.header("Location")
      Then(s"the redirect location should be '$expectedValue'")
      HavePropertyMatchResult(
        redirectLocation.contains(expectedValue),
        "redirectURI",
        expectedValue,
        redirectLocation.getOrElse("")
      )
    }
  }

  def isElementVisibleById(id: String)(expectedValue: Boolean): HavePropertyMatcher[WSResponse, Boolean] = new HavePropertyMatcher[WSResponse, Boolean] {

    def apply(response: WSResponse) = {
      val body = Jsoup.parse(response.body)
      Then(s"it is $expectedValue that elementId '$id' should be on the page")
      val actual = Option(body.getElementById(id)).isDefined
      HavePropertyMatchResult(
        actual == expectedValue,
        s"elementByID($id)",
        expectedValue,
        actual
      )
    }
  }

  def isElementVisibleBySomething(id: String)(expectedValue: Boolean): HavePropertyMatcher[WSResponse, Boolean] = new HavePropertyMatcher[WSResponse, Boolean] {

    def apply(response: WSResponse) = {
      val body = Jsoup.parse(response.body)
      Then(s"it is $expectedValue that elementId '$id' should be on the page")

      HavePropertyMatchResult(
        body.toString.contains(id) == expectedValue,
        s"elementByID($id)",
        expectedValue,
        body.toString.contains(id)
      )
    }
  }
}
