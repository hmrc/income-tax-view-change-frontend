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

package views.helpers.injected

import testConstants.BaseTestConstants.testUserName
import testConstants.MessagesLookUp.{BtaServiceInfoHeader => btaServiceMessages}
import testUtils.ViewSpec
import views.html.helpers.injected.RenderServiceInfoHelper

class RenderServiceInfoHelperSpec extends ViewSpec {

  val renderServiceInfoHelper: RenderServiceInfoHelper = app.injector.instanceOf[RenderServiceInfoHelper]

  class Test(userName: Option[String] = Some(testUserName)) extends Setup(renderServiceInfoHelper(userName))


  "The renderServiceInfoHelper" when {

    "user details are passed to it" should {

      "render the user name" in new Test() {
        document.getElementById("service-info-user-name").text() shouldBe testUserName
      }

      "have a link to BTA home" which  {

        s"should have the text '${btaServiceMessages.btaHome}'" in new Test() {
          document.getElementById("service-info-home-link").text() shouldBe btaServiceMessages.btaHome
        }

        s"should have a link to '${appConfig.businessTaxAccount}'" in new Test() {
          document.getElementById("service-info-home-link").attr("href") shouldBe appConfig.businessTaxAccount
        }

      }

      "have a link to Manage Account" which  {

        s"should have the text '${btaServiceMessages.btaManageAccount}'" in new Test() {
          document.getElementById("service-info-manage-account-link").text() shouldBe btaServiceMessages.btaManageAccount
        }

        s"should have a link to '${appConfig.businessTaxAccount}'" in new Test() {
          document.getElementById("service-info-manage-account-link").attr("href") shouldBe appConfig.btaManageAccountUrl
        }

      }

      "have a link to Messages" which {

        s"should have the text '${btaServiceMessages.btaMessages}'" in new Test() {
          document.getElementById("service-info-messages-link").text() shouldBe btaServiceMessages.btaMessages
        }

        s"should have a link to '${appConfig.btaMessagesUrl}'" in new Test() {
          document.getElementById("service-info-messages-link").attr("href") shouldBe appConfig.btaMessagesUrl
        }

      }
    }

    "no user details are passed to it" should {

      "Not render the service-info-user-name" in new Test(None) {
        document.getOptionalSelector("#service-info-user-name") shouldBe None
      }
    }
  }
}
