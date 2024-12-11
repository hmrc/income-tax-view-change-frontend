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

package controllers.claimToAdjustPoa

import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import models.admin.AdjustPaymentsOnAccount
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{HTML, OK, contentAsString, contentType, defaultAwaitTimeout, redirectLocation, status}

class ApiFailureSubmittingPoaControllerSpec extends MockAuthActions {

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings()
    .build()

  val testController = fakeApplication().injector.instanceOf[ApiFailureSubmittingPoaController]

  val firstParagraphView = "Your payments on account could not be updated."

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          s"render the submitting POA API failure page" when {
            "called when the AdjustPaymentsOnAccount FS is on" in {
              enable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()

              val result = action(fakeRequest)
              val document: Document = Jsoup.parse(contentAsString(result))

              status(result) shouldBe OK
              contentType(result) shouldBe Some(HTML)
              document.getElementById("paragraph-text-1").text() shouldBe firstParagraphView
            }
          }
          s"redirect to the home page" when {
            "called when the AdjustPaymentsOnAccount FS is off" in {
              disable(AdjustPaymentsOnAccount)
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()

              val result = action(fakeRequest)

              status(result) shouldBe SEE_OTHER
              val expectedRedirectUrl = if (isAgent) {
                controllers.routes.HomeController.showAgent.url
              } else {
                controllers.routes.HomeController.show().url
              }
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
        }
      }
    }
  }
}
