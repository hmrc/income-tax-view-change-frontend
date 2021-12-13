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

package mocks.controllers.predicates

import config.ItvcErrorHandler
import controllers.predicates.{IncomeSourceDetailsPredicate, IncomeSourceDetailsPredicateNoCache}
import mocks.services._
import play.api.cache.AsyncCacheApi
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport

trait MockIncomeSourceDetailsPredicateNoCache extends TestSupport with MockIncomeSourceDetailsService{

  object MockIncomeSourceDetailsPredicateNoCache extends IncomeSourceDetailsPredicateNoCache(
    mockIncomeSourceDetailsService, app.injector.instanceOf[ItvcErrorHandler])(
    ec, app.injector.instanceOf[MessagesControllerComponents])
}
