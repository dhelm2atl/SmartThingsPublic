/**
 *  My First SmartApp
 *
 *  Copyright 2016 John Luikart
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
    definition(
    name: "Curling Iron Detector",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Notify when curling has been plugged in to long",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
    )

preferences {
    section {
    input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
    input(name: "threshold", type: "number", title: "Reports Above...", required: true, description: "in either watts or kw.")
	}
    section("Text after X minutes...") {
        input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to", required: true) {
        input "phoneNumber", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
    def meterValue = evt.value as double
    def thresholdValue = threshold as int
    if (meterValue > thresholdValue) {
        log.debug "${meter} reported energy consumption above ${threshold}."
        runIn( minutesLater, notifyContacts)
    }
}

def notifyContacts(){
	sendNotificationToContacts("Curling Iron has been plugged in for ${minutesLater} minutes!", recipients)
    if ( phoneNumber ) {
        log.debug("Sending text message...")
        sendSms( phoneNumber, "Curling Iron has been plugged in for ${minutesLater} minutes!")
    }
}