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

package services.admin

import config.FrontendAppConfig
import mocks.repositories.MockFeatureSwitchRepository
import mocks.connectors.MockFeatureSwitchConnector
import models.admin.{FeatureSwitch, FeatureSwitchName}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import testUtils.TestSupport

import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchServiceSpec extends TestSupport with MockFeatureSwitchRepository with MockFeatureSwitchConnector {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  val exampleFSName: FeatureSwitchName = FeatureSwitchName.get("nav-bar").get
  val anotherFSName: FeatureSwitchName = FeatureSwitchName.get("opt-in-opt-out-content-update-r17").get

  object TestFSService extends FeatureSwitchService(
    mockFeatureSwitchRepository,
    mockFeatureSwitchConnector,
    mockFrontendAppConfig
  )(
    app.injector.instanceOf[ExecutionContext]
  )

  override val appConfig: FrontendAppConfig = mockFrontendAppConfig

  "FeatureSwitchService.getAll" should {
    "return a list of all FS and whether they are enabled" when {
      "read from mongo FS is disabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn false
        disableAllSwitches()
        enable(exampleFSName)
        disable(anotherFSName)

        val result = TestFSService.getAll()
        result.futureValue should contain(FeatureSwitch(exampleFSName, isEnabled = true))
        result.futureValue should contain(FeatureSwitch(anotherFSName, isEnabled = false))
      }
      "read from mongo FS is enabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn true
        mockConnectorGetAllSwitches(List(FeatureSwitch(exampleFSName, true), FeatureSwitch(anotherFSName, false)))

        val result = TestFSService.getAll()
        result.futureValue should contain(FeatureSwitch(exampleFSName, isEnabled = true))
        result.futureValue should contain(FeatureSwitch(anotherFSName, isEnabled = false))
      }
    }
  }

  "FeatureSwitchService.set" should {
    "return true if FS successfully set" when {
      "read FS from mongo FS is enabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn true
        mockConnectorSetFeatureSwitch(true)

        val result = TestFSService.set(exampleFSName, true)
        result.futureValue shouldBe true
      }
      "read FS from mongo FS is disabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn false

        val result = TestFSService.set(exampleFSName, false)
        result.futureValue shouldBe true
      }
    }
    "return false if FS not set" when {
      "read FS from mongo FS is enabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn true
        mockConnectorSetFeatureSwitch(false)

        val result = TestFSService.set(exampleFSName, true)
        result.futureValue shouldBe false
      }
    }
  }

  "FeatureSwitchService.setAll" should {
    "set the FS in config" when {
      "read FS from mongo FS is disabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn false
        disableAllSwitches()
        TestFSService.setAll(Map(exampleFSName -> true, anotherFSName -> false))
        isEnabled(exampleFSName) shouldBe true
        isEnabled(anotherFSName) shouldBe false
      }
    }
    "call the repository" when {
      "read FS from mongo FS is enabled" in {
        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo) thenReturn true

        val fsMap = Map(exampleFSName -> true, anotherFSName -> false)

        when(mockFeatureSwitchConnector.setSwitches(ArgumentMatchers.eq(fsMap))(any()))
          .thenReturn(Future.successful(true))

        disableAllSwitches()

        TestFSService.setAll(fsMap)

        verify(mockFeatureSwitchConnector).setSwitches(ArgumentMatchers.eq(fsMap))(any())
      }
    }
  }
}
