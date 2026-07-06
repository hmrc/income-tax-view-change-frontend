/*
 * Copyright 2026 HM Revenue & Customs
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

package businessDetails.testConstants

import play.api.libs.json.Json
import common.helpers.WiremockHelper.baseUrl

object AddressLookupTestConstants {

  val accessibilityHost = "http://localhost:123456"
  
  //Todo update this once accessibility statement is available for income-tax-business-details-frontend
  val accessibilityPath = {
    "/accessibility-statement/income-tax-view-change"
    //"/accessibility-statement/income-tax-business-details"
  }
  
  val accessibilityUrl = {
    accessibilityHost + accessibilityPath
  }

  val accessibilityFooterUrl = {
    accessibilityUrl + "?referrerUrl=%2Freport-quarterly%2Fincome-and-expenses%2Fview"
  }

  val ukRequestBodyIndividual = Json.parse(
    s"""{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "$baseUrl/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "$baseUrl/session-timeout",
      |      "timeoutKeepAliveUrl": "$baseUrl/keep-alive"
      |    },
      |    "signOutHref": "$baseUrl/sign-out",
      |    "accessibilityFooterUrl": "$accessibilityUrl",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |        "postcode": true
      |      },
      |      "showOrganisationName": false,
      |      "line1MaxLength": 35,
      |      "line2MaxLength": 35,
      |      "line3MaxLength": 35,
      |      "townMaxLength": 35
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "phaseFeedbackLink": "$baseUrl/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": true
      |  },
      |  "labels": {
      |    "en": {
      |      "selectPageLabels": {
      |        "heading": "Select business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Manage your Self Assessment"
      |      }
      |    },
      |    "cy": {
      |      "selectPageLabels": {
      |        "heading": "Dewiswch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Rheoli’ch Hunanasesiad"
      |      }
      |    }
      |  }
      |}""".stripMargin)

  val ukRequestBodyAgent = Json.parse(
    s"""
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "$baseUrl/agents/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "$baseUrl/session-timeout",
      |      "timeoutKeepAliveUrl": "$baseUrl/keep-alive"
      |    },
      |    "signOutHref": "$baseUrl/sign-out",
      |    "accessibilityFooterUrl": "$accessibilityUrl",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |        "postcode": true
      |      },
      |      "showOrganisationName": false,
      |      "line1MaxLength": 35,
      |      "line2MaxLength": 35,
      |      "line3MaxLength": 35,
      |      "townMaxLength": 35
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": true,
      |      "showConfirmChangeText": true
      |    },
      |    "phaseFeedbackLink": "$baseUrl/agents/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": true
      |  },
      |  "labels": {
      |    "en": {
      |      "selectPageLabels": {
      |        "heading": "Select business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Manage your Self Assessment"
      |      }
      |    },
      |    "cy": {
      |      "selectPageLabels": {
      |        "heading": "Dewiswch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "appLevelLabels": {
      |        "navTitle": "Rheoli’ch Hunanasesiad"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
  
  val internationalRequestBodyInvididual = Json.parse(
    s"""
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "$baseUrl/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "$baseUrl/session-timeout",
      |      "timeoutKeepAliveUrl": "$baseUrl/keep-alive"
      |    },
      |    "signOutHref": "$baseUrl/sign-out",
      |    "accessibilityFooterUrl": "$accessibilityUrl",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": false,
      |      "showConfirmChangeText": true
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |       "addressLine1": true,
      |       "addressLine2": true
      |      },
      |      "showOrganisationName": false,
      |      "line1MaxLength": 35,
      |      "line2MaxLength": 35,
      |      "line3MaxLength": 35,
      |      "townMaxLength": 35
      |    },
      |    "phaseFeedbackLink": "$baseUrl/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": false
      |  },
      |  "labels": {
      |    "en": {
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Enter the country or territory for your business address",
      |        "title": "Enter the country or territory for your business address",
      |        "countryLabel": "Enter country or territory"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Enter your international business address",
      |          "title": "Enter your international business address",
      |          "postcodeLabel": "Postcode or zipcode"
      |        }
      |      }
      |    },
      |    "cy": {
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Nodwch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "title": "Nodwch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "countryLabel": "Nodwch wlad neu diriogaeth"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "title": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "postcodeLabel": "Cod post neu god ‘zip’"
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)

  val internationalRequestBodyAgent = Json.parse(
    s"""
      |{
      |  "version": 2,
      |  "options": {
      |    "continueUrl": "$baseUrl/agents/manage-your-businesses/add/business-address/id/",
      |    "timeoutConfig": {
      |      "timeoutAmount": 3600,
      |      "timeoutUrl": "$baseUrl/session-timeout",
      |      "timeoutKeepAliveUrl": "$baseUrl/keep-alive"
      |    },
      |    "signOutHref": "$baseUrl/sign-out",
      |    "accessibilityFooterUrl": "$accessibilityUrl",
      |    "selectPageConfig": {
      |      "proposalListLimit": 15
      |    },
      |    "confirmPageConfig": {
      |      "showChangeLink": true,
      |      "showSearchAgainLink": false,
      |      "showConfirmChangeText": true
      |    },
      |    "manualAddressEntryConfig": {
      |      "mandatoryFields": {
      |       "addressLine1": true,
      |       "addressLine2": true
      |      },
      |      "showOrganisationName": false,
      |      "line1MaxLength": 35,
      |      "line2MaxLength": 35,
      |      "line3MaxLength": 35,
      |      "townMaxLength": 35
      |    },
      |    "phaseFeedbackLink": "$baseUrl/agents/feedback",
      |    "deskProServiceName": "cds-reimbursement-claim",
      |    "showPhaseBanner": true,
      |    "ukMode": false
      |  },
      |  "labels": {
      |    "en": {
      |      "confirmPageLabels": {
      |        "heading": "Confirm business address"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "What is your business address?"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Enter the country or territory for your business address",
      |        "title": "Enter the country or territory for your business address",
      |        "countryLabel": "Enter country or territory"
      |      },
      |      "editPageLabels": {
      |        "heading": "Enter your business address"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Enter your international business address",
      |          "title": "Enter your international business address",
      |          "postcodeLabel": "Postcode or zipcode"
      |        }
      |      }
      |    },
      |    "cy": {
      |      "confirmPageLabels": {
      |        "heading": "Cadarnhewch gyfeiriad y busnes"
      |      },
      |      "lookupPageLabels": {
      |        "heading": "Beth yw cyfeiriad eich busnes?"
      |      },
      |      "editPageLabels": {
      |        "heading": "Nodwch gyfeiriad eich busnes"
      |      },
      |      "countryPickerLabels": {
      |        "heading": "Nodwch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "title": "Nodwch y wlad neu’r diriogaeth ar gyfer cyfeiriad eich busnes",
      |        "countryLabel": "Nodwch wlad neu diriogaeth"
      |      },
      |      "international": {
      |        "editPageLabels": {
      |          "heading": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "title": "Nodwch gyfeiriad rhyngwladol eich busnes",
      |          "postcodeLabel": "Cod post neu god ‘zip’"
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
