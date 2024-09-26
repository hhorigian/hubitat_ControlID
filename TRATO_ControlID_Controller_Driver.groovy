/**
 *  ControlID Driver-BETA01
 *
 *  Copyright 2024 VH
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
 *
*  ControlID Driver for Hubitat - ChangeLog
*
*  VH - 2024 
*
*  Version 1.0.0 - Limited Release
*
 */

metadata {
  definition (name: "ControlID Controller Driver", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Contact Sensor"
    capability "Sensor"
    capability "Switch"  
  }
      
  }

  import groovy.transform.Field

  preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "debugOutput", type: "bool", title: "Habilitar  Log", defaultValue: false
        input name: "ipdomodulo", type: "string", title: "Mol IP", defaultValue: ""     
      
  }   
  
command "AbrirSecBox"
attribute "StatusSecBox", "string"


def initialized()
{
    state.currentip = ""
    log.debug "initialized()"
}


def installed()
{
    logTrace('installed()')
    boardstatus = "offline"
}


def uninstalled() {
    logTrace('uninstalled()')
    unschedule()
} //OK


def on() {
     sendEvent(name: "switch", value: "on", isStateChange: true)
     sendEvent(name: "StatusSecBox", value: "Aberta", isStateChange: true)      

     AbrirSecBox() 
}


def off() {
     sendEvent(name: "StatusSecBox", value: "Fechada", isStateChange: true)
    //     sendCommand(off)
}


def AtualizaIP(ipADD) {
    state.currentip = ipADD
    ipdomodulo  = state.currentip
    device.updateSetting("ipdomodulo", [value:"${ipADD}", type:"string"])
    log.info "Device com IP atualizada " + state.currentip
    
}


def AtualizaSession(sessionid) {
    state.varsession = sessionid
    log.info "Session ID atualizada " +  state.varsession    
}


def AtualizaStatusSecBox(statusSec) {
    state.varstatussec = statusSec
    log.info "Status do SecBox Atualizado " +  state.varstatussec  
    
    if (state.varstatussec == true) { 
       sendEvent(name: "StatusSecBox", value: "Aberta", isStateChange: true)      
        sendEvent(name: "switch", value: "on", isStateChange: true)
           
        }else
      {
       sendEvent(name: "StatusSecBox", value: "Fechada", isStateChange: true)  
       sendEvent(name: "switch", value: "off", isStateChange: true)
      }
    
}


///////   AbrirSecBox  ///////////
def AbrirSecBox(){
    
    def postParams = [
		uri: "http://" + state.currentip + "/execute_actions.fcgi?session=" + state.varsession,
        contentType: "application/json",
        body: '{"actions": [{"action": "sec_box","parameters": "id=65793,reason=3,timeout=3000"}]}'       
	    ]
        //log.info postParams
	asynchttpPost('myCallbackMethodAbrirSecBox', postParams)
    
}

def myCallbackMethodAbrirSecBox(response, data) {
    if (response.getStatus() == 200) {        
        log.info "SecBox ABERTA "
        sendEvent(name: "StatusSecBox", value: "Aberta", isStateChange: true) 
        sendEvent(name: "switch", value: "on", isStateChange: true)
        pauseExecution(300)     


    }else {        
        log.info "SecBox Failed: Status = ${response.getStatus()} -  " 
    } 

}




//DEBUG
private logDebug(msg) {
  if (settings?.debugOutput || settings?.debugOutput == null) {
    log.debug "$msg"
  }
}

