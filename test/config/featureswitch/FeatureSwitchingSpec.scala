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

package config.featureswitch

import testUtils.TestSupport

class FeatureSwitchingSpec extends TestSupport with FeatureSwitching {

  val expectedDisabledFeatures: Set[FeatureSwitch] = FeatureSwitch.switches

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    FeatureSwitch.switches.map(_.name).foreach(sys.props.remove)
  }

  "Features" should {

    "fold depending on its state, calling the respective branch only" when {
      trait FoldSetup {
        val aValue = 123987
        var hasBeenCalled = false
        def expectedBranch(): Int = {
          hasBeenCalled = true
          aValue
        }
        def unexpectedBranch(): Int = throw new IllegalStateException
      }

      "a feature is disabled" in new FoldSetup {
        expectedDisabledFeatures.head.fold(
          ifEnabled = unexpectedBranch(),
          ifDisabled = expectedBranch()) shouldBe aValue
        hasBeenCalled shouldBe true
      }
    }

  }
}
