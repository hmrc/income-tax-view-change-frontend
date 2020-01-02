/*
 * Copyright 2020 HM Revenue & Customs
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

package config.filters

import javax.inject.Inject

import play.api.Configuration
import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

class ServiceFilters @Inject()(configuration: Configuration,
                               defaultFilters: FrontendFilters,
                               csrfWithExclusion: ExcludingCSRFFilter
                              ) extends DefaultHttpFilters({

  // this adds marking of routes to excludes csrf check, see https://dominikdorn.com/2014/07/playframework-2-3-global-csrf-protection-disable-csrf-selectively/
  defaultFilters.filters.filterNot(f => f.isInstanceOf[CSRFFilter]) :+ csrfWithExclusion

}: _*)

