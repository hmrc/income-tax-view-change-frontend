
package mocks.controllers.predicates

import mocks.services.{MockAsyncCacheApi, MockIncomeSourceDetailsService}
import testUtils.TestSupport

trait MockBtaNavPredicate extends TestSupport with MockIncomeSourceDetailsService with MockAsyncCacheApi{

}
