/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import models.btaNavBar._
import org.scalatest.Matchers
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.{Lang, Messages}
import testUtils.TestSupport

class BtaNavPartialServiceSpec extends TestSupport with ScalaFutures with Matchers {

  val mockService: BtaNavBarService = new BtaNavBarService()(appConfig)
  implicit val message: Messages = messages

  "BtaNavPartialPartialService" when {
    "notificationBadgeCount" should {
      "return a string of 0 when messageCount is 0" in {
        val result: String = mockService.notificationBadgeCount(messageCount = 0)
        result mustBe "0"
      }

      "return a string of 1 when messageCount is 1" in {
        val result: String = mockService.notificationBadgeCount(messageCount = 1)
        result mustBe "1"
      }

      "return a string of 99 when messageCount is 99" in {
        val result: String = mockService.notificationBadgeCount(messageCount = 99)
        result mustBe "99"
      }

      "return a string of +99 when messageCount is 100" in {
        val result: String = mockService.notificationBadgeCount(messageCount = 100)
        result mustBe "+99"
      }

      "return a string of +99 when messageCount is 1000" in {
        val result: String = mockService.notificationBadgeCount(messageCount = 1000)
        result mustBe "+99"
      }
    }

    "formsNav" should {
      "return en messages when there is 1 alert and lang is set to eng" in {
        implicit val lang = messagesApi.preferred(Seq(Lang("en"))).lang
        val testNavLinks = NavLinks("testEn", "testCy", "testUrl", Some(1))
        val result = mockService.formsNav(testNavLinks)(lang)
        result mustBe ListLinks("testEn", "testUrl", Some("1"))
      }

      "return cy messages when there is 1 alert and lang is set to eng" in {
        implicit val lang = messagesApi.preferred(Seq(Lang("cy"))).lang
        val testNavLinks = NavLinks("testEn", "testCy", "testUrl", Some(1))
        val result = mockService.formsNav(testNavLinks)(lang)
        result mustBe ListLinks("testCy", "testUrl", Some("1"))
      }

      "return an empty ListLink with showBoolean set true when there are 0 alerts" in {
        implicit val lang = messages.lang
        val testNavLinks = NavLinks("testEn", "testCy", "testUrl", Some(0))
        val result = mockService.formsNav(testNavLinks)(lang)
        result mustBe ListLinks("", "", showBoolean = Some(false))
      }

      "return an empty ListLink with showBoolean set true when there are no alerts provided" in {
        implicit val lang = messages.lang
        val testNavLinks = NavLinks("testEn", "testCy", "testUrl")
        val result = mockService.formsNav(testNavLinks)(lang)
        result mustBe ListLinks("", "", showBoolean = Some(false))
      }
    }

    "partialList" should {
      "return seq of EN Listlinks when NavContent is provided and lang is set to en" in {
        implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))
        val navContent = NavContent(
          NavLinks("testEnHome", "testCyHome", "testUrl"),
          NavLinks("testEnAccount", "testCyAccount", "testUrl"),
          NavLinks("testEnMessages", "testCyMessages", "testUrl"),
          NavLinks("testEnHelp", "testCyHelp", "testUrl"),
          NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
        )

        val result = mockService.partialList(Some(navContent))(messages)
        result mustBe Seq(
          ListLinks("testEnHome", appConfig.homePageUrl),
          ListLinks("testEnAccount", "testUrl"),
          ListLinks("testEnMessages", "testUrl", Some("0")),
          ListLinks("testEnForm", "testUrl", Some("1")),
          ListLinks("testEnHelp", "testUrl")
        )
      }

      "return seq of CY Listlinks when NavContent is provided and lang is set to cy" in {
        implicit val messages: Messages = messagesApi.preferred(Seq(Lang("cy")))

        val navContent = NavContent(
          NavLinks("testEnHome", "testCyHome", "testUrl"),
          NavLinks("testEnAccount", "testCyAccount", "testUrl"),
          NavLinks("testEnMessages", "testCyMessages", "testUrl"),
          NavLinks("testEnHelp", "testCyHelp", "testUrl"),
          NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
        )

        val result = mockService.partialList(Some(navContent))(messages)
        result mustBe Seq(
          ListLinks("testCyHome", appConfig.homePageUrl),
          ListLinks("testCyAccount", "testUrl"),
          ListLinks("testCyMessages", "testUrl", Some("0")),
          ListLinks("testCyForm", "testUrl", Some("1")),
          ListLinks("testCyHelp", "testUrl"),
        )
      }

      "return seq of Listlinks when forms link alerts is 0" in {

        val navContent = NavContent(
          NavLinks("testEnHome", "testCyHome", "testUrl"),
          NavLinks("testEnAccount", "testCyAccount", "testUrl"),
          NavLinks("testEnMessages", "testCyMessages", "testUrl"),
          NavLinks("testEnHelp", "testCyHelp", "testUrl"),
          NavLinks("testEnForm", "testCyForm", "testUrl", Some(0)),
        )

        val result = mockService.partialList(Some(navContent))
        result mustBe Seq(
          ListLinks("testEnHome", appConfig.homePageUrl),
          ListLinks("testEnAccount", "testUrl"),
          ListLinks("testEnMessages", "testUrl", Some("0")),
          ListLinks("", "", showBoolean = Some(false)),
          ListLinks("testEnHelp", "testUrl"),
        )
      }

      "return seq of Listlinks when message and forms link alerts is 99" in {

        val navContent = NavContent(
          NavLinks("testEnHome", "testCyHome", "testUrl"),
          NavLinks("testEnAccount", "testCyAccount", "testUrl"),
          NavLinks("testEnMessages", "testCyMessages", "testUrl", Some(99)),
          NavLinks("testEnHelp", "testCyHelp", "testUrl"),
          NavLinks("testEnForm", "testCyForm", "testUrl", Some(99)),
        )

        val result = mockService.partialList(Some(navContent))
        result mustBe Seq(
          ListLinks("testEnHome", appConfig.homePageUrl),
          ListLinks("testEnAccount", "testUrl"),
          ListLinks("testEnMessages", "testUrl", Some("99")),
          ListLinks("testEnForm", "testUrl", Some("99")),
          ListLinks("testEnHelp", "testUrl"),
        )
      }

      "return seq of Listlinks when message and forms link alerts is +99" in {

        val navContent = NavContent(
          NavLinks("testEnHome", "testCyHome", "testUrl"),
          NavLinks("testEnAccount", "testCyAccount", "testUrl"),
          NavLinks("testEnMessages", "testCyMessages", "testUrl", Some(100)),
          NavLinks("testEnHelp", "testCyHelp", "testUrl"),
          NavLinks("testEnForm", "testCyForm", "testUrl", Some(100)),
        )

        val result = mockService.partialList(Some(navContent))
        result mustBe Seq(
          ListLinks("testEnHome", appConfig.homePageUrl),
          ListLinks("testEnAccount", "testUrl"),
          ListLinks("testEnMessages", "testUrl", Some("+99")),
          ListLinks("testEnForm", "testUrl", Some("+99")),
          ListLinks("testEnHelp", "testUrl"),
        )
      }

      "return Seq() when no Navcontent is found" in {
        val result = mockService.partialList(None)
        result mustBe Seq()
      }
    }
  }
}
