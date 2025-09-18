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

package viewUtils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import testUtils.UnitSpec

class ExternalUrlHelperSpec extends UnitSpec with GuiceOneAppPerSuite {

  val welshAuthorisationGuidanceUrl = "https://www.gov.uk/government/collections/troi-treth-yn-ddigidol-ar-gyfer-treth-incwm-fel-asiant-cam-wrth-gam.cy"
  val generalAuthorisationGuidanceUrl = "https://www.gov.uk/government/collections/making-tax-digital-for-income-tax-as-an-agent-step-by-step"

  val welshNationalInsuranceRatesUrl = "https://www.gov.uk/cyfraddau-yswiriant-gwladol-ir-hunangyflogedig"
  val generalNationalInsuranceRatesUrl = "https://www.gov.uk/self-employed-national-insurance-rates"

  val welshChooseAgentGuidanceUrl = "https://www.gov.uk/guidance/choose-agents-for-making-tax-digital-for-income-tax.cy"
  val generalChooseAgentGuidanceUrl = "https://www.gov.uk/guidance/choose-agents-for-making-tax-digital-for-income-tax"

  val welshQuarterlyUpdatesGuidanceUrl = "https://www.gov.uk/guidance/defnyddio-r-cynllun-troi-treth-yn-ddigidol-ar-gyfer-treth-incwm/anfon-diweddariadau-chwarterol"
  val generalQuarterlyUpdatesGuidanceUrl = "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"

  val welshSaWhoNeedsToSignUpUrl = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax.cy#pwy-fydd-angen-cofrestru"
  val generalSaWhoNeedsToSignUpUrl = "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-will-need-to-sign-up"


  "clientAuthorisationGuidance" should {
    "return welsh url when language is set to welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("cy")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.clientAuthorisationGuidance shouldBe welshAuthorisationGuidanceUrl
    }

    "return general url when language is set to something else that is not english or welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("it")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.clientAuthorisationGuidance shouldBe generalAuthorisationGuidanceUrl
    }
  }

  "seNationalInsuranceRatesUrl" should {
    "return welsh url when language is set to welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("cy")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.seNationalInsuranceRatesUrl shouldBe welshNationalInsuranceRatesUrl
    }

    "return general url when language is set to something else that is not english or welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("it")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.seNationalInsuranceRatesUrl shouldBe generalNationalInsuranceRatesUrl
    }
  }

  "chooseAgentGuidanceUrl" should {
    "return welsh url when language is set to welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("cy")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.chooseAgentGuidanceUrl shouldBe welshChooseAgentGuidanceUrl
    }

    "return general url when language is set to something else that is not english or welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("it")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.chooseAgentGuidanceUrl shouldBe generalChooseAgentGuidanceUrl
    }
  }

  "quarterlyUpdatesGuidanceUrl" should {
    "return welsh url when language is set to welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("cy")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.quarterlyUpdatesGuidanceUrl shouldBe welshQuarterlyUpdatesGuidanceUrl
    }

    "return general url when language is set to something else that is not english or welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("it")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.quarterlyUpdatesGuidanceUrl shouldBe generalQuarterlyUpdatesGuidanceUrl
    }
  }

  "saWhoNeedsToSignUpUrl" should {
    "return welsh url when language is set to welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("cy")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.saWhoNeedsToSignUpUrl shouldBe welshSaWhoNeedsToSignUpUrl
    }

    "return general url when language is set to something else that is not english or welsh" in {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
      implicit val lang: Lang = Lang("it")
      implicit val messages: Messages = MessagesImpl(lang, messagesApi)
      ExternalUrlHelper.saWhoNeedsToSignUpUrl shouldBe generalSaWhoNeedsToSignUpUrl
    }
  }
}
