/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import java.time.LocalDate

import assets.Messages
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import play.api.i18n.MessagesApi
import services.ReportDeadlinesService

import scala.concurrent.Future

class HomeControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  val reportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]

  object TestHomeController extends HomeController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    reportDeadlinesService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi]
  )

  val updateDate: LocalDate = LocalDate.of(2018, 1, 1)

  "navigating to the home page" should {

    "return OK (200)" when {
      "there is a update date to display" in {
        when(reportDeadlinesService.getNextDeadlineDueDate(any())(any(), any(), any())) thenReturn Future.successful(updateDate)
        mockSingleBusinessIncomeSource()

        val result = TestHomeController.home(fakeRequestWithActiveSession)

        status(result) shouldBe Status.OK
        Jsoup.parse(bodyOf(result)).title shouldBe Messages.HomePage.title
      }
    }

  }

}
