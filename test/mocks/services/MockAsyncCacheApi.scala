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

package mocks.services

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.cache.AsyncCacheApi
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockAsyncCacheApi extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockAsyncCacheApi: AsyncCacheApi = mock[AsyncCacheApi]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAsyncCacheApi)
    when(mockAsyncCacheApi.get(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
  }

}
