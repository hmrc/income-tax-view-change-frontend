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

package controllers

import config.FrontendAppConfig
import mocks.controllers.predicates.MockAuthenticationPredicate
import models.admin.{ChargeHistory, FeatureSwitch, FeatureSwitchName}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers._
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import services.admin.FeatureSwitchService
import testOnly.controllers.FeatureSwitchController
import testOnly.views.html.FeatureSwitchView
import testUtils.TestSupport

import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchControllerSpec
  extends TestSupport {

  val featureSwitchService: FeatureSwitchService = app.injector.instanceOf[FeatureSwitchService]

  object TestFeatureSwitchController extends FeatureSwitchController(
    featureSwitchView = app.injector.instanceOf[FeatureSwitchView],
    featureSwitchService = featureSwitchService
  )(
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    ec = app.injector.instanceOf[ExecutionContext]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    featureSwitchService.setAll(
      FeatureSwitchName.allFeatureSwitches.map(fsName => (fsName, false)).toMap
    )
  }

  "FeatureSwitchController.enableAll" should {
    "redirect to the feature switch page with all checkboxes ticked" when {
      "all feature switches are disabled" in {

        val request = FakeRequest()

        val result = TestFeatureSwitchController.enableAll()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(testOnly.controllers.routes.FeatureSwitchController.show.url)
        println(s"${Jsoup.parse(contentAsString(result))}")
        Jsoup.parse(contentAsString(result)).select("#forecast-calculation[checked]").toArray should have length 1
      }
    }
  }

//  "FeatureSwitchController.enableAll" should {
//    "redirect to the feature switch page with all checkboxes ticked" when {
//      "all feature switches are disabled" in {
//
//        val result = TestFeatureSwitchController.enableAll()(FakeRequest())
//
//        when(mockFeatureSwitchService.getAll)
//          .thenReturn(Future.successful(List(
//            FeatureSwitch(name = ChargeHistory, isEnabled = false))
//          ))
//
//        when(mockFeatureSwitchService.set(any(), any()))
//          .thenReturn(Future.successful(true))
//
//        result shouldBe SEE_OTHER
//      }
//    }
//  }
}
