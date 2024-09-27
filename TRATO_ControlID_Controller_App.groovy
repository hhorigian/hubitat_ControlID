/**
 *  ControlID  App for Hubitat - Version for: ID FLex
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
 *  for the specific language governing permissions and limitations under the License.*
  *
*  ControlID  App for Hubitat - ChangeLog
*
*  VH - 2024 
*
*  Version 1.0.0 - Limited Release
 */

definition(
    name: "ControlID Controller - App",
    namespace: "TRATO",
    author: "VH",
    description: "ControlID Controller para Hubitat",
    category: "Lights",
    iconUrl: "",
    iconX2Url: "")

import groovy.transform.Field
import groovy.json.JsonSlurper 


preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    section("ControlID Setup Configuration") {
        input "thisName", "text", title: "Nome personalizado para o Módulo", submitOnChange: true
		if(thisName) app.updateLabel("$thisName")
        input name: "molIPAddress", type: "text", title: "ControlID IP Address", submitOnChange: true, required: true, defaultValue: "192.168.1.208" 
        input name: "debugOutput", type: "bool", title: "Habilitar Log", defaultValue: false        
        input name: "pollFrequency", type: "number", title: "Frequência para obter o feedback do status (em segundos)", defaultValue: 30
        //input name: "relaycount", type: "text", title: "MolSmart Relay Count", required: false, defaultValue: "3"         
        
    }
    }

//verifysessionfrequency = "30"
//@Field static Integer checkInterval = 600
   
    
}

def installed() {
    log.debug "installed(): Installing ControlID App Parent App"
    state.childscreated == 0
    initialize()
    
}

def updated() {
    log.debug "updated(): Updating ControlID App"
    initialize()

}

def uninstalled() {
    unschedule()
    log.debug "uninstalled(): Uninstalling ControlID App"
}


def initialize() {

    unschedule()
    state.varsessionvalid = "false"
    //if (state.childscreated == 0) {
        
    for(int i = 1; i<= 1 ; i++) {
        def contactName = "ControlID-" + Integer.toString(i) + "_${app.id}"
	    logDebug "initialize(): adding driver = " + contactName
        
        def contactDev = getChildDevice(contactName)
	    if(!contactDev) contactDev = addChildDevice("TRATO", "ControlID Controller Driver", contactName, null, [name: "ControlIDCTRL " + Integer.toString(i), inputNumber: thisName])
 
          //create a random number to assign to the controller
          randomDecimal = Math.random()
          finalInt = (randomDecimal * 1000).round()
          sessionname = "CIDSession-" + finalInt
          setGlobalVar(sessionname, '')
    
    }
    
     def ipmolsmart = settings.molIPAddress
     def devices = getAllChildDevices()
     for (aDevice in devices)
    
    {
             
        aDevice.AtualizaIP(ipmolsmart)  //coloco o ip no relay.
        logDebug "Coloco el ip en cada device = $aDevice, " + ipmolsmart
        
    }
    DoLogin()
    
        schedule("* 0/30 * ? * * *", VerifySession, [overwrite: false])  // default 30 minutos seg.       
        schedule("*/5 * * ? * * *", VerifyDoorStatus, [overwrite: false])  // usualmente cada 1 seg.       

       logDebug "Configuro a frequência de atualização para cada  ${verifysessionfrequency} minute(s)"       
        //schedule("*/${pollFrequency} * * ? * * *", VerifySession, [overwrite: false])  // usualmente cada 1 seg.   
        state.childscreated = 1  
        
    /*} 
    else
    {
        log.info "Childs já foram criados"    
    }*/
      
}


///////   LOGIN  ///////////
def DoLogin(){
    
    def postParams = [
		uri: "http://" + settings.molIPAddress + "/login.fcgi",
        contentType: "application/json",
        body: '{"login": "admin","password": "admin"}'
	]
	asynchttpPost('myCallbackMethodLogin', postParams)
    
}

def myCallbackMethodLogin(response, data) {
    if (response.getStatus() == 200) {
        
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(response.getData())    
        state.varsession = object.session 
        log.info "New Login - IP " + settings.molIPAddress  +  " Session ID = " + state.varsession   
        
        
    }else {        
        log.info "Login Failed: Status = ${response.getStatus()} - IP " + settings.molIPAddress
    } 
    VerifySession()
}


/////   VerifySession  ///////
def VerifySession(){
    
    def postParams = [
		uri: "http://" + settings.molIPAddress + "/session_is_valid.fcgi?session=" + state.varsession ,
        contentType: "application/json"
        //body: '{session=' + varsession 
	]
    asynchttpPost('myCallbackMethodVerify', postParams)
    
}

def myCallbackMethodVerify(response, data) {
       
    if  (response.getStatus() == 200) {        
        log.info "Session is Valid - IP " + settings.molIPAddress +  " - sesssion Verified = " + state.varsession
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(response.getData()) 
		//log.info "Session is valid = " + object.session_is_valid 
        state.varsessionvalid = object.session_is_valid        

            if ( state.lastsessionid != state.varsession ) {
            //coloco o session id 
            def devices = getAllChildDevices()
            for (aDevice in devices)
            {             
            aDevice.AtualizaSession(state.varsession)  
            logDebug "Atualizo o Session ID no ControlID Device = $aDevice"    
            }
            }
                
        state.lastsessionid = state.varsession
    }   else {        
        log.info "Session Invalid/Response Failed: Status = ${response.getStatus()} - IP " + settings.molIPAddress +  " sesssion id = " + state.varsession
        DoLogin()
    } 
    
}


/////   VerifyDoorStatus  ///////
def VerifyDoorStatus(){
    
    def postParams = [
		uri: "http://" + settings.molIPAddress + "/doors_state.fcgi?session=" + state.varsession ,
        contentType: "application/json"
        //body: '{session=' + varsession 
	]
    //log.debug "postParams VerifyDoorStatus = " + postParams
    asynchttpPost('myCallbackMethodStatus', postParams)

}

def myCallbackMethodStatus(response, data) {
    if  ( (response.getStatus() == 200)  )  {
        
        //log.info "Entrou para o Status do SecBox" 
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(response.getData()) 
        state.varstatussec = object.sec_boxes.open[1]
        
        if ( state.laststatusstec != state.varstatussec ) {
        log.debug "Alteracao de  Status da Porta. Agora =  " + state.varstatussec     
        //coloco o status da porta no device 
        def devices = getAllChildDevices()
        for (aDevice in devices)
        {             
        aDevice.AtualizaStatusSecBox(state.varstatussec)  
        logDebug "Atualizo o SecBox Status no Driverce = $aDevice"    
        }
        }    
        
        state.laststatusstec = state.varstatussec

    }else {        
        log.info "SecBox Status Failed: Status = ${response.getStatus()} - IP " + settings.molIPAddress +  " sesssion id = " + state.varsession
        if ( response.getStatus() == 401)    {
            log.info "Failed Login. Trying Login Process."
            DoLogin() 
        }
    } 
    
}




//DEBUG
private logDebug(msg) {
  if (settings?.debugOutput || settings?.debugOutput == null) {
    log.debug "$msg"
  }
}


void logTrace(String msg) {
    if ((Boolean)settings.logTrace != false) {
        log.trace "${drvThis}: ${msg}"
    }
}
