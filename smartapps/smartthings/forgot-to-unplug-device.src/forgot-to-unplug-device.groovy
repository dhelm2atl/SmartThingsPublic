/**
 *  Energy Saver
 *
 *  Copyright 2014 SmartThings
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
    name: "Forgot to unplug device",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Get notified if you're using too much energy",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png"
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        input(name: "aboveThreshold", type: "number", title: "Reports Above...", required: true, description: "in either watts or kw.")
        input(name: "minutes", type: "number", title: "Delay before notifacation in minutes", required: true, description: "Minutes plugged in before alert is sent")
	}
    section {
        input("recipients", "contact", title: "Send notifications to") {
            input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
            input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
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
	initialize()
}

def initialize() {
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
	def SendDelay = (minutes * 60) as int
    log.debug "${SendDelay}"
    def meterValue = evt.value as double

    if (!atomicState.lastValue) {
    	atomicState.lastValue = meterValue
    }

    def lastValue = atomicState.lastValue as double
    atomicState.lastValue = meterValue
	// Todo recurring alerts
    def aboveThresholdValue = aboveThreshold as int
    if (meterValue > aboveThresholdValue) {
    	if (lastValue < aboveThresholdValue) { // only send notifications when crossing the threshold
		    def msg = "${meter} reported ${evt.value} ${evt.unit} which is above your threshold of ${aboveThreshold}."
            log.debug "alert should be sent in ${SendDelay} Seconds"
    	    runIn(SendDelay, sendMessage)
            if (!atomicState.time){
            atomicState.time = now() // So that we can see how long it's been plugged in later if we do recurring alerts.
            }
        } else {
        	log.debug "not sending notification for ${evt.description} because the threshold (${aboveThreshold}) has already been crossed"
        }
    }
}

def sendMessage() {
	
    def currentvalue = meter.currentPower
    def millisSinceInstalled = now() - atomicState.time
    log.debug "the current value of ${meter} is $meter.currentPower"
	log.debug "${meter} has been on for ${millisSinceInstalled / 1000 / 60} minutes"
    def msg = "${meter} has been on for ${millisSinceInstalled / 1000 / 60} minutes"
    if (currentvalue > aboveThreshold) // Verify it's still plugged in.
    {
        if (location.contactBookEnabled) {
            sendNotificationToContacts(msg, recipients)
        }
        else {
            if (sms) {
                sendSms(sms, msg)
            }
            if (pushNotification) {
                sendPush(msg)
            }
        }
    }
    else
    {
    	log.debug"meter dropped below value"
    }
}
