/* ========================================================================
 * Copyright (c) 2009-2011 The University of Washington
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
 * ========================================================================
 */

/* rp register javascript */

 var v_root;
 var v_vers;
 var v_remoteUser;
 var v_sectionType = '';
 var v_entityId;
 var v_mdId;
 var v_canEdit;

 var userId;
 var userEtag;
 var pageEtag;
 var request;
 var action;
 var finalAction = '';
 var finalErrorElement = null;
 var supplementals = '{supplementals}';

 var useHotkeys = true;

 // list of popins
 var popins = new Array();
 var popinZIndex = 10;


// Trim leading and following spaces from a string
String.prototype.trim = function () {
   return this.replace(/^\s*|\s*$/,"");
}


// Get error text from ws response (first span)
// --- ms part of this don't quite work yet
function getErrorFromXML(xml) {
  ret = '';
  try {//Internet Explorer
     xmlDoc=new ActiveXObject('Microsoft.XMLDOM');
     xmlDoc.async="false";
     xmlDoc.loadXML(xml.substring(xml.indexOf('<html')));
     ret = xmlDoc.getElementsByTagName("span")[0].childNodes[0].nodeValue;
  }  catch(e) {
     // alert (e.description);
     try { // all others
        parser=new DOMParser();
        xmlDoc=parser.parseFromString(xml,'text/xml');
        ret = xmlDoc.getElementsByTagName("span")[0].childNodes[0].nodeValue;
     } catch(e) {
        // alert (e.description);
        ret = 'unknown error';
     }
  }
  return (ret.trim());
}

// Get a request oject
function request_object() {
  try {
     return new ActiveXObject('Msxml2.XMLHTTP');
  } catch(e) {
     try {
       return new ActiveXObject('Microsoft.XMLHTTP');
     } catch(e) {
       return new XMLHttpRequest();
     }
  }
}


// On success update page, else report error
function handleRequestResponse()
{
   if(request.readyState==4) {
            //  alert('status: ' + request.status);
      if (request.status==302) {
         document.getElementById('requestStatusDiv').innerHTML = 'Relogin needed: Refresh the page.';
         alert('Your session has expired. Refresh the page to relogin.');
      } else {
         document.getElementById('requestStatusDiv').innerHTML = request.status + ': ' + request.statusText;
         if (request.status==200 || request.status==201) {
              // alert('OK' + request.responseText);
            if ( typeof finalAction == 'function' ) finalAction();
            else window.location = finalAction;
         } else {
              alert(request.responseText);
            if (finalErrorElement!=null) {
               finalErrorElement.innerHTML = request.responseText;
               finalErrorElement.style.display = '';
            }
            etext = getErrorFromXML(request.responseText);
            if (etext=='') etext = request.statusText;
            alert(etext);
         }
      }
   }
}


// Perform a post request
function doRequest(method, action, data, ifmatch)
{
   document.getElementById('requestStatusDiv').innerHTML = 'processing...';
   request=request_object();
   request.open(method ,action,true);
   request.onreadystatechange=handleRequestResponse;
   request.setRequestHeader('Content-type', 'application/xhtml+xml; charset=utf-8');
   if (ifmatch!=null && ifmatch.length>0) request.setRequestHeader('if-Match', ifmatch);
   document.body.style.cursor = "wait";
   request.send(data);
}



// Show the help
function showHelp(h, posid)
{
   if (document.getElementById(h + 'Help').style.display == 'block') return hideHelp(h);
   s = document.getElementById(h + 'Help');
   tgt = document.getElementById(posid);
   y = getTopOffset(tgt) + tgt.offsetHeight - 4;
   s.style.top = y + 'px';
   x = getLeftOffset(tgt) - 20;
   s.style.left = x + 'px';
   s.style.width = "40%";
   s.style.display = 'block';
   s.focus();
   popins.push(h+'Help');
}

// Hide the help
function hideHelp(h)
{
    document.getElementById(h + 'Help').style.display = 'none';
}



// Prevent Enter from submitting a form
function noenter(e) 
{
  if (!e) e = window.event;
  return !(e && e.keyCode == 13);
}

function showInfo(id)
{
   info = document.getElementById(id);
   if (info!=null) {
      info.style.display = 'block';
      info.style.zIndex = popinZIndex++;
   }
   popins.push(id);
}


// Show (hide) an element

function toggleVis(id)
{
   div = document.getElementById(id);
   if (div) {
      if (div.style.display == 'none') div.style.display = '';
      else div.style.display = 'none';
   }
}

function togglePolicyVis(id) {
   detail = document.getElementById(id + '.detail');
   // alert ('detail: ' + id + '.detail'); 
   if (detail) {
      if (detail.style.display == 'none') {
          detail.style.display = '';
      } else {
          detail.style.display = 'none';
      }
      toggleVis(id + ".plus");
      toggleVis(id + ".minus");
   }
}


// set details visible
function showDetails() {
  toHide = getElementsByClass('cleanDetail');
  for (var i=0; i<toHide.length; i++) {
    toHide[i].style.display = 'none';
  }
  toShow = getElementsByClass('messyDetail');
  for (var i=0; i<toShow.length; i++) {
    toShow[i].style.display = '';
  }
}

// set details invisible
function hideDetails() {
  toHide = getElementsByClass('messyDetail');
  for (var i=0; i<toHide.length; i++) {
    toHide[i].style.display = 'none';
  }
  toShow = getElementsByClass('cleanDetail');
  for (var i=0; i<toShow.length; i++) {
    toShow[i].style.display = '';
  }
}

// toggle detail elements
function toggleDetail() {
  toggleVis('showDetail');
  toggleVis('hideDetail');
  toggleVis('messyDetail');
}




function goTo(loc)
{
   window.location = loc;
}

// get top offest of an element

function getTopOffset( element ) {
  var offset = 0;
  while( element != null ) {
    offset += element.offsetTop;
    element = element.offsetParent;
  }
  return offset;
}

// get left offest of an element

function getLeftOffset( element ) {
  var offset = 0;
  while( element != null ) {
    offset += element.offsetLeft;
    element = element.offsetParent;
  }
  return offset;
}


// show a popin
var popinId;
function showPopin(id, posid)
{
   popinId = id;
   info = document.getElementById(id);
   infoParent = info.parentNode;
   tgt = document.getElementById(posid);
   if (info!=null) {
      x = getTopOffset(tgt);
      info.style.top = getTopOffset(tgt) + 'px';
      info.style.right = 20 + 'px';
      if(navigator.appName == "Microsoft Internet Explorer") info.style.width = "50%";
      info.style.display = 'block';
      info.style.zIndex = popinZIndex++;
   }
   info.focus();
   popins.push(id);
}

String.prototype.startsWith = function(str)
{return (this.match("^"+str)==str)}

function isChildOf(e, id) {
  while( e != null ) {
    if (e.id == id ) return (true);
    e = e.offsetParent;
  }
  return false;
}

function hidePopin(event, id)
{
// if (event.relatedTarget.id.startsWith(id)) return (true);
if (isChildOf(event.relatedTarget, popinId)) return (true);
   cx = event.clientX;
   cy = event.clientY;
   info = document.getElementById(id);
   info.style.display='none';
}

// hide all popins
function hide_popins()
{
   for (i=0; i<popins.length; i++) {
     p = document.getElementById(popins[i]);
     if (p) if (p.style.display != 'none') p.style.display = 'none';
   }
}

// hotkey response

function hotkey(e) {

  if (!useHotkeys) return (true);

  // get the key
  if (window.event) e = window.event;
  code = e.keyCode;
  // scode = String.fromCharCode(code).toLowerCase();

  // escape
  if (code == 27) {
     hide_popins();
     return true;
  }

  // ignore other keys in text areas
  if(window.event) ele = window.event.srcElement;
  else ele = e.target;
  if (ele.type != null) return true;

  // question mark
  if (code==191 || (code==0 && e.shiftKey)) {  // ( combo of shift+0 seems to work on mac FF )
     document.getElementById('Hotkeys').style.display = 'block';
     popins.push('Hotkeys');
     return false;
  }

  // ignore any ctrl-keys
  if (e.ctrlKey) return true;

  // all windows
  switch (code) {
     case 80: goTo( v_root + v_vers + '/rps');    // p
               return false;
  }

  // in a sp window
  if (v_entityId != '') {
    switch (code) {
     case 68: toggleDetail();       // d
               return false;
     case 77: goTo( v_root + v_vers + '/rp?id=' + v_entityId + '&mdid=' + v_mdId);    // m
               return false;
     case 65: goTo( v_root + v_vers + '/rp/attr?id=' + v_entityId + '&mdid=' + v_mdId); // a
               return false;
    }
  
    if (v_sectionType=='rp') {
      switch (code) {
         case 69: if (v_canEdit) goTo( v_root + v_vers + '/rp?id=' + v_entityId + '&mdid=' + v_mdId + '&view=edit');    // e
               return false;
         case 86: goTo( v_root + v_vers + '/rp?id=' + v_entityId + '&mdid=' + v_mdId);    // v
               return false;
      }
    }

    if (v_sectionType=='rpattr') {
      switch (code) {
         case 69: if (v_canEdit) goTo( v_root + v_vers + '/rp/attr?id=' + v_entityId + '&mdid=' + v_mdId + '&view=edit');    // e
               return false;
         case 86: goTo( v_root + v_vers + '/rp/attr?id=' + v_entityId + '&mdid=' + v_mdId);    // v
               return false;
         case 82: showPopin('arDiv','arLocator');  // r
               return false;
      }
    }
  }

  return true;
}

function rpOnLoad() {
  // activate hotkeys
  if (window.addEventListener) window.addEventListener('keyup', hotkey, false);
  else document.attachEvent('onkeyup', hotkey);

  if ( typeof localOnLoad == 'function' ) localOnLoad();
}


