/* ========================================================================
 * Copyright (c) 2011-2014 The University of Washington
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

// sp-registry javascript

// globals
var v_root = '/spreg';
var v_remoteUser = '';
var v_xsrf = '';
var v_etag = '';
var v_loadErrorMessage = "Operation failed. You may need to reload the page to reauthenticate";


// track current sp by hash
var v_currentSpTab = 'm';
var v_spLoading = false;

// sp list ( loaded by api from the server )

var spList;     // all the sp
var nsp = 0;
var currentSp;  // the active sp or null
var newSpId = '';
var numMetaEditKey = 0;
var numProxyEditKey = 0;

var spListMine = false;  // set to list only user's sps

iam_set('rightSide', 'spDisplay');


/* Context save and restore.  Hash data in the url and in the cookie.  */

// hash change causes sp display (callback from iam tools)
function hashHandler(tab, spid) {
   console.log('hash handler: tab=' + tab + ' sp=' + spid);
   if (v_spLoading) {
      console.log('load in progress. ignoring');
      return;
   }
   if (spid!=null) {
      // find the hash sp
      for (i=0; i<nsp; i++) {
        if (spList[i].id==spid) {
           v_currentSpTab = tab;
           showSp(i, tab);
           return;
        }
      }
   } else showHomePage();
}

var roleCookie = 'spck2';
iam_set('hashCookie', 'spck1');
iam_set('hashHandler', hashHandler);

// user/admin role chooser

var adminQS = ''
function setRole(r, d) {
   console.log('in: cookie = ' + dojoCookie(roleCookie) + '  role=' + r);
   if (r=='a') {
      dojoCookie(roleCookie, 'a');
      // dojoDom.byId('banner_notice').innerHTML = '<span style="font-size:larger;color:#a00000">Acting as administrator</span>'; 
   } else {
      dojoCookie(roleCookie, null, {expires: -1});
      // dojoDom.byId('banner_notice').innerHTML = ''; 
   }
   console.log('out: cookie = ' + dojoCookie(roleCookie));
   dojoDoc.location = v_root;
}


// show the home panel
function showHomePage() {
   iam_hideShow(['spDisplay'],['homeDisplay']);
   currentSp = null;
}
// show the sp panel
function showSpPanel() {
  iam_hideShow(['homeDisplay'],['spDisplay']);
}


/*
 * Methods used to handle the SP display 
 */

var spKeyListener = null;

//  Switch to the correct tab

function setSpTab() {
   console.log('setSpTab: tab=' + v_currentSpTab);
   var tab = null;
   var tabid = 'metaSpContainer';
   if (v_currentSpTab == 'a') tabid = 'attrSpContainer';
   else if (v_currentSpTab == 'p') tabid = 'proxySpContainer';
   tab = dijitRegistry.byId(tabid);
   console.log('tab: ' + tab);
   if (tab!=null) dijitRegistry.byId('spPanel').selectChild(tab);
   tabnode = dojo.byId('spPanel_tablist_' + tabid);
   if (tabnode!=null) tabnode.focus();
   else console.log('no tabnod? ' + tabid);
   v_spLoading = false;
}

// Setup hash and key listeners, set the tab
// called after the SP display loads

function postLoadSp() {
   console.log('postLoad');

   if (currentSp!=null) dojoDom.byId('spTitle').innerHTML = currentSp.meta  + ' | ' + currentSp.id;
   else dojoDom.byId('spTitle').innerHTML = newSpId;

   var sp = dijitRegistry.byId('spPanel');
   sp.watch('selectedChildWidget',
      function(name, otab, ntab) {
         console.log("tab now ", ntab.id);
         var tab = ntab.id.substring(0,1);
         v_currentSpTab = tab;
         iam_hashSetCurrent(tab, null);
      });
   if (spKeyListener!=null) spKeyListener.remove();
   require(["dojo/on"], function(on){
     spKeyListener = on(dojoDom.byId("spDisplay"), "keypress", function(e){
       console.log(e.charOrCode + ' - ' +  e.charCode + ' ' + e.keyCode);
       switch (e.charCode) {
          case 69: // E
          case 101:
             if (v_currentSpTab=='m') iam_showTheDialog('metaEditDialog');
             else if (v_currentSpTab=='a') iam_showTheDialog('attrEditDialog');
             else if (v_currentSpTab=='p') iam_showTheDialog('proxyEditDialog');
             break;
          case 82: // R
          case 114:
             if (v_currentSpTab=='a') iam_showTheDialog('attrReqDialog');
             break;
       }
     });
   });
   numMetaEditKey = 0;
   numProxyEditKey = 0;
   setSpTab();

}


// display an SP by index

function showSp(i, tab) {
   console.log('showsp, i='+i+' current=' + currentSp);
   if (currentSp==null || currentSp.id!=spList[i].id || currentSp.meta!=spList[i].meta) {
      currentSp = spList[i];
      v_currentSpTab = tab;
      showCurrentSp();
   } else if (tab!=v_currentSpTab) {
      v_currentSpTab = tab;
      setSpTab();
   }
}


// display the current SP
// this starts the load.  postLoadSp ends it

function showCurrentSp() {
   v_spLoading = true;
   if (currentSp!=null) {
      console.log('showcur ' + v_currentSpTab + '!' + currentSp.id + ' admins=' + currentSp.admins);
      iam_hashSetCurrent(v_currentSpTab,currentSp.id);
   } else console.log('showCur no sur');
   if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();

   var url = v_root + v_vers + '/rp?id=' + currentSp.id + '&mdid=' + currentSp.meta + '&view=inner' + adminQS;
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('loadingMessage', 'Loading ' + currentSp.id + ' . . .' );
   dijitRegistry.byId('spDisplay').set('onLoad', postLoadSp);
   dijitRegistry.byId('spDisplay').set('href', url);
   showSpPanel();
   window.focus();
}

/*
 * SP list tools
 */

// decorate the 'current' sp in the list
function setSearchOver(i) {
  require(["dojo/dom-class"], function(domClass){
    domClass.add('spitem' + i, 'listitemhover');
  });
}
function setSearchOut(i) {
  require(["dojo/dom-class"], function(domClass){
    domClass.remove('spitem' + i, 'listitemhover');
  });
}

// filter the sp list according to the filter textbox
// 'enter' select the one on top
// activated by keypress in the filter textbox

var curselsp = (-1);
function checkSpFilter(e) {
  console.log(e);
  if (curselsp>=0) {
     if (e.keyCode==13) {  // enter
        showSp(curselsp, 'm');
        return;
     }
     if (e.keyCode==40) {  // down
        for (i=curselsp+1; i<spList.length;i++) {
           nsp = dojoDom.byId('spitem' + i);
           if (nsp) {
              setSearchOut(curselsp);
              setSearchOver(i);
              curselsp = i;
              return;
           }
        }
        return;
     }
     if (e.keyCode==38) {  // up
        for (i=curselsp-1; i>=0;i--) {
           nsp = dojoDom.byId('spitem' + i);
           if (nsp) {
              setSearchOut(curselsp);
              setSearchOver(i);
              curselsp = i;
              return;
           }
        }
        return;
     }
  }
  showSpList();
}

// display the filtered sp list

function _isadmin(sp) {
   for (a in sp.admins) {
     admin = sp.admins[a];
     if (v_remoteUser.indexOf('@')>0 && admin==v_remoteUser) return true;
     if (admin==v_remoteUser+'@uw.edu' || admin==v_remoteUser+'@washington.edu') return true;
   }
   return false;
}

function showSpList() {
  
  curselsp = (-1);
  nsp = spList.length;

  var txsp = dijitRegistry.byId('filterSpList').get('value');

  // Count how many will show.  We darken the list when it gets short
  var ndsp = 0;
  var dc = 'dim0';
  for (i=0; i<nsp; i++) {
    if ((txsp.length>0) && spList[i].id.indexOf(txsp)<0) continue;
    if (spListMine && !_isadmin(spList[i])) continue;
    ndsp += 1;
    if (ndsp==5) dc = 'dim1';
    if (ndsp==10) dc = 'dim2';
    if (ndsp==15) dc = 'dim3';
    if (ndsp==20) dc = 'dim4';
  }
  console.log('list size = ' + ndsp);

  var htm = '';
  ndsp = 0;
  for (i=0; i<nsp; i++) {
    if ((txsp.length>0) && spList[i].id.indexOf(txsp)<0) continue;
    if (spListMine && !_isadmin(spList[i])) continue;
    ndsp += 1;
    if (ndsp>10000) {  // the 10000 effectively disables this ... feature
       htm += '<span class="listitem dim4"><i>.&nbsp;.&nbsp;.&nbsp;</i></span>';
       break;
    }
   
    // decorate the link with org and federation
    ttl = '';
    if (spList[i].meta.substring(0,2)=='UW') ttl += ' (UW)';
    else ttl += ' (InCommon)';
   
    if (curselsp<0) {
       cls = ' class="listitem listitemhover ' + dc + '" '
       curselsp = i;
    } else cls = ' class="listitem ' + dc + '" '
    htm = htm + 
        '<span id="spitem' + i + '"' +  cls +
        'onMouseDown="showSp(\'' + i + '\',\'m\')" ' +
        'onMouseOver="setSearchOver(\'' + i + '\')" ' +
        'onMouseOut="setSearchOut(\'' + i + '\')" title="' + ttl + '">' +
      spList[i].id +
     '</span><br>';
  }
 
  dijitRegistry.byId('spIndexPane').set('content',htm);
  adjustSPIndexSize();
  
}

// load the list of SPs.  API call to server.
function loadSpList()
{
   var url = v_root + v_vers + '/rps';
   if (dojoCookie('sp-mine')=='m') {
      dijitRegistry.byId('justmine').set('checked', 1);
      spListMine = true;
   }
   iam_getRequest(url, null, 'json', function(data, args) {
        spList = data.rps;
        showSpList();
        iam_hashHandler();
      });
}

function toggleListMine () {
  if (dijitRegistry.byId('justmine').get('checked')) {
     dojoCookie('sp-mine', 'm');
     spListMine = true;
  } else {
     dojoCookie('sp-mine', 'a');
     spListMine = false;
  }
  showSpList();
}


/* panel sizing */

// index and display panels have been sized already by iam-dojo.js

function setPaneSizes() {
   console.log('Set pane sizes.....');

   var dh = dojo.position(dojo.byId('displayPanel'),true).h;
   var dw = dojo.position(dojo.byId('displayPanel'),true).w;
   var idh = dh - 20;
   var idw = dw - 10;
   console.log('spDisp height:' + idh + ' width:' + idw);

     dojo.style(dojo.byId('spDisplay'), {
        height: idh + 'px',
        width: idw + 'px',
        top: '0px',
        left: '0px'
      });
   
     dojo.style(dojo.byId('homeDisplay'), {
        height: idh + 'px',
        width: idw + 'px',
        top: '0px',
        left: '0px'
      });

     if (currentSp!=null) showCurrentSp();

}

// set the index list to correct size 
function adjustSPIndexSize() {
   var ih = dojo.position(dojo.byId('indexPanel'),true).h;
   var iy = dojo.position(dojo.byId('indexPanel'),true).y;
   var spiy = dojo.position(dojo.byId('spIndexPane'),true).y;
   var spih = ih - (spiy - iy) - 20;

   console.log('set sp ind ht = ' + spih);
     dojo.style(dojo.byId('spIndexPane'), {
        height: spih + 'px'
      });
}

// Size the SP ddetail isplay
function adjustSpPaneSize(paneName) {
   var pane = dojoDom.byId(paneName);
   var tHeight = dojo.position(dojo.byId('spDisplay'),true).h;
   var h = tHeight - 140;
   dojo.style(pane, {
     height: h + 'px'
   });

}


function reparse(node) {
console.log('REPARSE');
require(["dojo/parser"], function(parser){
  parser.parse(node);
});

}





/*
 * RP metadata tab functions 
 * 
 */

var rpId;
var mdId;
var newSpConnect = null;

// pseudo getbyname that works with ie
function _getElementsByIdname(base) {
   var list = [];
   var i = 0;
   while (dojoDom.byId(base + '_' + i)) {
      list.push(dojoDom.byId(base + '_' + i++));
   }
   return list;
}

// respond to acs clear button
function meta_clearACS(i) {
   dijitRegistry.byId('acsi_'+i).set('value','');
   dijitRegistry.byId('acsl_'+i).set('value','');
   dojoDom.byId('acs' + i + '_0').style.display = 'none';
   dojoDom.byId('acs' + i + '_1').style.display = 'none';
}

// respond to acs clear button
function meta_clearKI(i) {
   console.log('clear KI ' + i);
   dijitRegistry.byId('kn_'+i).set('value','');
   dijitRegistry.byId('kc_'+i).set('value','');
   dojoDom.byId('ki' + i + '_0').style.display = 'none';
   dojoDom.byId('ki' + i + '_1').style.display = 'none';
}

// respond to contact clear button
function meta_clearCT(i) {
   dijitRegistry.byId('ctgn_'+i).set('value','');
   dijitRegistry.byId('cte_'+i).set('value','');
   dijitRegistry.byId('ctp_'+i).set('value','');
   dojoDom.byId('ct' + i + '_0').style.display = 'none';
   dojoDom.byId('ct' + i + '_1').style.display = 'none';
   dojoDom.byId('ct' + i + '_2').style.display = 'none';
   dojoDom.byId('ct' + i + '_3').style.display = 'none';
}

// respond to one of the 'add xxx' buttons
meta_showMoreFields = function(name, id) {
   // show the first of the hidden ones
   for (e=0; e<50; e++) {
      var enam = name + e;
      list = _getElementsByIdname(name+e);
      if (list.length==0) break;
      if (list[0].style.display == '') continue;
      for (i=0; i<list.length; i++) {
         list[i].style.display = '';
         list[i].className = 'messyDetail';
      }
      return;
    }
    plus = dojoDom.byId(id);
    plus.style.display = 'none';
};

// after load of new sp, remove the connect and show the edit dialog
// if something went wrong there are no edit elements
function postLoadNewSp() {
   console.log('postLoadNewSp');
   dojo.disconnect(newSpConnect);
   newSpConnect = null;
   var v = dojoDom.byId('orgn_0');
   if (v=='') return;
   postLoadSp();
   iam_showTheDialog('metaEditDialog',[]);
}

// Catch 'enter' in the new-sp entityid textbox.  Act as if 'Continue'
function checkNewSp(e) {
  if (e.keyCode==13) {  // enter
     meta_newSp(0);
     return;
  }
}
// Catch 'enter' in the new-sp url textbox.  Act as if 'Continue'
function checkNewUrl(e) {
  if (e.keyCode==13) {  // enter
     meta_lookupUrl();
     return;
  }
}


// Catch 'check' in the new-sp get-from-sp.
function checkNewSpFromSp() {
   if (dijitRegistry.byId('newSpLookup').get('checked')) {
      console.log('got a check show on sp');
      dijitRegistry.byId('newSpLookupUrl').set('checked', false);
      iam_hideShow(['newSpUrlBox'],[]);
   }
}
// Catch 'check' in the new-sp get-from-url.
function checkNewSpFromUrl() {
   if (dijitRegistry.byId('newSpLookupUrl').get('checked')) {
      console.log('got a check show on url');
      dijitRegistry.byId('newSpLookup').set('checked', false);
      iam_hideShow([], ['newSpUrlBox']);
   } else {
      console.log('got a check hide on url');
      iam_hideShow(['newSpUrlBox'],[]);
   }
}


// API call to fetch or generate metadata for a new SP
function _newSp(rpid, lookup) {
   currentSp = null
   newSpId = rpid;
   v_spLoading = true;
   if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();
   var url = v_root + v_vers + '/new?rpid=' + rpid + adminQS;
   if (lookup) url += '&lookup=' + lookup;
   console.log(url);
   dijitRegistry.byId('spDisplay').set('errorMessage', 'Request for metadata failed.  Is the SP online?');
   if (lookup) dijitRegistry.byId('spDisplay').set('loadingMessage', 'Searching for ' + rpid + ' . . .' );
   else dijitRegistry.byId('spDisplay').set('loadingMessage', 'Processing . . .');
   dijitRegistry.byId('spDisplay').set('href', url);
   newSpConnect = dojo.connect(dijitRegistry.byId('spDisplay'), 'onLoad', postLoadNewSp);
   showSpPanel();
   window.focus();
}

// start a lookup of a new SP from the textbox

function meta_newSp(override) {
   var dns = dijitRegistry.byId('newSp').get('value').trim();
   console.log('entityid=' + dns + ', override=' + override)
   if (dns==null || dns=='') {
      iam_showTheNotice('you must provide an entityid');
      return;
   }
   if ( ! (override>0 || dns.indexOf('http://')==0 || dns.indexOf('https://')==0) ) { 
      iam_showTheDialog('invalidEntityId');
      return;
   }
   iam_hideTheDialog('invalidEntityId');
   iam_showTheDialog('newSpChooser');
}

function meta_lookupSp() {
   var dns = dijitRegistry.byId('newSp').get('value').trim();
   iam_hideTheDialog('newSpChooser');
   return _newSp(dns, 'sp');
}

function meta_lookupUrl() {
   var dns = dijitRegistry.byId('newSp').get('value').trim();
   var url = dijitRegistry.byId('newSpUrl').get('value').trim();
   if (url=='') {
      iam_showTheNotice('You must provide a URL');
      return;
   }
   iam_hideTheDialog('newSpChooser');
   return _newSp(dns, url);
}  

function meta_manualSp() {
   var dns = dijitRegistry.byId('newSp').get('value').trim();
   iam_hideTheDialog('newSpChooser');
   return _newSp(dns, null);
}


/* 
 * tools to handle save of metadata
 */

// If the new sp edit is cancelled without save, show a warning
function metaEditHide() {
   console.log("edit popup hide");
   if (currentSp==null || numMetaEditKey>1) {  // '1' to ignore the 'close' click
      iam_showTheDialog('metaNotSavedDialog');
   }
}

// if you wanted to check if any metadata editing was done, these might be useful
function metaEditShow() {
   console.log("edit popup show");
}
function metaEditKey() {
   console.log("edit popup key");
   numMetaEditKey += 1;
   if (numMetaEditKey==1) dijitRegistry.byId('metaEditSaver').set('disabled',0);
}

var badRE = new RegExp("[<>&]");
var nameRE = new RegExp("^[a-z][a-z0-9\.\_\-]+$");

function badText(v, e) {
   if (v.search(badRE)>=0) {
       iam_showTheNotice("invalid " + e);
       return 1;
   }
   return 0;
}

// build the rp xml
var pse_chks =  {"pse_10":"urn:oasis:names:tc:SAML:1.0:protocol",
                 "pse_11":"urn:oasis:names:tc:SAML:1.1:protocol", 
                 "pse_20":"urn:oasis:names:tc:SAML:2.0:protocol"};

function assembleRPMetadata(entityId) {
   rpId = entityId;
   console.log('get rp info for ' + rpId);
   var xml = '<EntityDescriptor entityID="' + entityId + '">';

   // SPSSO
   var pse = '';
   
   for ( var p in pse_chks ) {
      var ck = dijitRegistry.byId(p).get('checked');
      if (!ck) continue;
      if (pse=='') pse = 'protocolSupportEnumeration="' + pse_chks[p];
      else pse = pse + ' ' + pse_chks[p];
   }
   if (pse=='') {
      iam_showTheNotice("You must provide at least one protocol ");
      return '';
   }
   xml = xml + '<SPSSODescriptor ' + pse + '">';

   // keyinfo
   hadKi = false;
   for (i=0; i<10; i++) {
      console.log('ki: ' + i);
      knv = dojoDom.byId('kn_' + i).value.trim();
      kcv = dojoDom.byId('kc_' + i).value.trim();
      if (knv=='' && kcv=='') continue;
      if (badText(knv, "keyname")) return '';
      if (badText(kcv, "cert pem")) return '';
      ki = '<KeyDescriptor><ds:KeyInfo>';
      if (knv!='') ki = ki + '<ds:KeyName>' + knv + '</ds:KeyName>';
      if (kcv!='') ki = ki + '<ds:X509Data><ds:X509Certificate>' + kcv + '</ds:X509Certificate></ds:X509Data>';
      ki = ki + '</ds:KeyInfo></KeyDescriptor>';
      xml = xml + ki;
      hadKi = true;
   }
   if (!hadKi) {
      iam_showTheNotice("You must provide KeyInfo");
      return '';
   }
   // nameid
   for (i=0; i<5; i++) {
      e = dojoDom.byId('ni_' + i);
      if (e!=null) {
         v = e.value.trim();
         if (v == '') continue;
         if (badText(v, "nameid")) return '';
         xml = xml + '<NameIDFormat>' + v + '</NameIDFormat>';
      }
   }

   // acs
   hadAcs = false;
   for (i=0; i<50; i++) {
      idx = dojoDom.byId('acsi_' + i);
      idxv = idx.value.trim();
      bv = dijitRegistry.byId('acsb_' + i).value.trim();
      lv = dojoDom.byId('acsl_' + i).value.trim();
      if (idxv=='' && lv=='') continue;
      if (idxv=='') {
         iam_showTheNotice("You must provide an index for each ACS location specified");
         return '';
      }
      if (lv=='') {
         iam_showTheNotice("You must provide a location for each ACS index specified");
         return '';
      }
      if (badText(bv, "acs binding")) return '';
      if (badText(lv, "acs location")) return '';
      xml = xml + '<AssertionConsumerService index="' + idxv + '" ';
      xml = xml + 'Binding="' + bv + '" Location="' + lv + '"/>';
      hadAcs = true;
   }
   if (!hadAcs) {
      iam_showTheNotice("You must provide an ACS");
      return '';
   }
   xml = xml + '</SPSSODescriptor>';
   // org (only one really)
   for (i=0; i<1; i++) {
      xml = xml + '<Organization>';
      v = dojoDom.byId('orgn_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationName>' + iam_makeOkXml(v) + '</OrganizationName>';
      else {
         iam_showTheNotice("You must provide an Org name");
         return '';
      }
      v = dojoDom.byId('orgd_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationDisplayName>' + iam_makeOkXml(v) + '</OrganizationDisplayName>';
      else {
         iam_showTheNotice("You must provide an Org display name");
         return '';
      }
      v = dojoDom.byId('orgu_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationURL>' + iam_makeOkXml(v) + '</OrganizationURL>';
      else {
         iam_showTheNotice("You must provide an Org URL");
         return '';
      }
      xml = xml + '</Organization>';
   }

   // contact:  need at least one; for each need name and email
   hadOne = false;
   hadName = false;
   hadMail = false;
   for (i=0; i<20; i++) {
      hadName = false;
      hadMail = false;
      var v = dijitRegistry.byId('ctt_' + i).get('value').trim();
      var vn = dojoDom.byId('ctgn_' + i).value.trim();
      var ve = dojoDom.byId('cte_' + i).value.trim();
      hadName = true;
      if (vn=='' && ve=='') continue;
      if (vn=='') {
         iam_showTheNotice("You must provide a name for each contact");
         return '';
      }
      if (ve=='') {
         iam_showTheNotice("You must provide an email for each contact");
         return '';
      }
      hadOne = true;
      xml = xml + '<ContactPerson contactType="' + iam_makeOkXml(v) + '">';
      xml = xml + '<GivenName>' + iam_makeOkXml(vn) + '</GivenName>';
      xml = xml + '<EmailAddress>' + iam_makeOkXml(ve) + '</EmailAddress>';
      v = dojoDom.byId('ctp_' + i).value.trim();
      if (v!='') {
         xml = xml + '<TelephoneNumber>' + iam_makeOkXml(v) + '</TelephoneNumber>';
         hadPhone = true;
      }
      xml = xml + '</ContactPerson>';
   }
   if (!hadOne) {
      iam_showTheNotice("You must provide at least one contact");
      return '';
   }

   xml = xml + '</EntityDescriptor>';

   return xml;
}

// after metadata has been saved, reload the SP 

function postSaveRP() {
   console.log('postSaveRP');
   // iam_bannerNotice('Changes saved');
   iam_showTheMessage('Changes saved.<p>Allow 40 minutes for the changes<br>to propagate to the IdP.');
   var url = v_root + v_vers + '/rp/?id=' + rpId + '&mdid=UW' + adminQS;
   if (currentSp==null) {
      console.log('post load new SP');
      // add the new sp to the list  ( quicker than reloading the list )
      spList[nsp] = {'id':rpId, 'meta':'UW', 'org':''};
      currentSp = spList[nsp];
      nsp += 1;
   }
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('href', url);
   document.body.style.cursor = 'default';
   numMetaEditKey = 0;
}


// respond to the 'save changes' button

function saveRP(entityId) {
   xml = assembleRPMetadata(entityId);
   if (xml=='') return false;
   rpid = entityId;
   var url = v_root + v_vers + '/rp?id=' + entityId + '&mdid=UW&xsrf=' + v_xsrf + adminQS;
   console.log('req: ' + url);
   iam_putRequest(url, null, xml, null, postSaveRP);
}


// after SP has been deleted, show message and return to home page

function postDeleteRP() {
   console.log('post delete');
   iam_hideTheDialog('metaDeleteDialog');
   iam_showTheMessage('Relying party ' + rpId + ' deleted');
   iam_hideShow(['spDisplay'],['homeDisplay']);
   document.body.style.cursor = 'default';
   rpId = null;
}


// Respond to the 'delete' confirmation

function deleteRP(entityId) {
   rpId = entityId;
   var url = v_root + v_vers + '/rp?id=' + entityId + '&mdid=UW&xsrf=' + v_xsrf + adminQS;
   iam_deleteRequest(url, null, null, postDeleteRP);
}

// convert sloppy textarea into comma-separated list
function convertToList(str)
{
   list = str.split(/[\s,]+/);
   ret = list.join(",");
   return (ret);
}

/*
 *   Attribute functions
 */

function local_check_gws() {
   ck = dijitRegistry.byId('attr_req_gws_groups').get('checked');
   console.log('ck='+ck);
   if (ck) iam_hideShow([],['attr_req_gws_text_tr']);
   else iam_hideShow(['attr_req_gws_text_tr'],[]);
}

var _okmsg;

function _postReqAttrs() {
   iam_hideTheDialog('attrReqDialog');
   iam_showTheMessage('Request submitted.');
}

// submit the request
function attr_requestAttrs(entityId) {

   _okmsg = '';
   var gws_text = '';
   var grps = dijitRegistry.byId('attr_req_gws_text').get('value').trim();
   alist = dojoQuery('.attr_req_chk');
   xml = '<Attributes>';
   for (a=0; a<alist.length; a++) {
     w = dijitRegistry.byNode(alist[a]);
     wname = w.get('value');
     wid = w.get('id');
     aid = wid.replace('attr_req_','');
     if (aid=='default') continue;  // not a real attribute
     inn = dojoDom.byId(w.get('id') + '_in');
     // console.log(aid + ' in value ' + inn.value);
     if (w.get('checked')) {
        if (aid=='gws_groups') {
           var grpsin = dijitRegistry.byId('attr_req_gws_text_in').get('value').trim();
           if (grps==grpsin) continue;
           if (grps=='') {
              iam_showTheNotice('You must identify the groups you need.');
              return;
           }
           xml += '<Add id="' + aid + '"/>';
           _okmsg += '<li>Adding: ' + aid + '</li>';
           gws_text = '\n\nGroups requested:\n' + grps;
           if (grpsin!='') gws_text += '\nPrevious groups:\n' + grpsin;
        } else if (inn.value=='') {
           xml += '<Add id="' + aid + '"/>';
           _okmsg += '<li>Adding: ' + aid + '</li>';
        }
     } else {
        if (inn.value!='') {
           xml += '<Drop id="' + aid + '"/>';
           _okmsg += '<li>Dropping: ' + aid + '</li>';
        }
     }
   }
   if (_okmsg=='') {
      iam_showTheNotice('There are no changes to request.');
      return;
   }
   _okmsg = 'Request submitted<ul>' + _okmsg + '</ul>';
   msg = dijitRegistry.byId('attr_req_exptext').get('value').trim();
   if (msg=='') {
      iam_showTheNotice('You must explain why you need the attributes');
      return;
   }
   xml = xml + '<Comments>' + iam_makeOkXml(msg+gws_text) + '</Comments>';
   xml = xml + '</Attributes>';
   action = v_root + v_vers + '/rp/attrReq?id=' + entityId + '&xsrf=' + v_xsrf + adminQS;
   iam_putRequest(action, null, xml, null, _postReqAttrs);
};

// edit functions

// respond to attribute checkbox
function attr_checkAttr(gid, id) {
   chk = dijitRegistry.byId(gid + '_attr_edit_chk_' + id);
   if (chk.get('checked')) {
      dojoDom.byId(gid + '_attr_edit_tr_all_' + id).style.display = '';
      dijitRegistry.byId(gid + '_attr_edit_all_' + id).set('value',true);
   } else {
      dojoDom.byId(gid + '_attr_edit_tr_all_' + id).style.display = 'none';
   }
};

// respond to 'all' checkbox
function attr_checkAll(gid, id) {
   chk = dijitRegistry.byId(gid + '_attr_edit_all_' + id);
   if (chk.get('checked')) {
      for (i=0;i<99;i++) {
         v = dojoDom.byId(gid + '_attr_edit_tr_v_' + i + '_' + id);
         if (v==null) break;
         v.style.display = 'none';
      }
   } else {
      for (i=0;i<99;i++) {
         v = dojoDom.byId(gid + '_attr_edit_tr_v_' + i + '_' + id);
         if (v==null) break;
         v.style.display = '';
         v = dijitRegistry.byId(gid + '_attr_edit_v_' + i + '_' + id);
         if (v.get('value').trim()=='') break;
      }
   }
};

function attr_showNext(gid, i, id) {
   n = i+1;
   dojoDom.byId(gid + '_attr_edit_tr_v_' + n + '_' + id).style.display = '';
}


// save values

// format an attribute
function _attributeXml (gid, id) {
   console.log('attr: ' + gid + '_attr_edit_chk_' + id);
   chk = dijitRegistry.byId(gid + '_attr_edit_chk_' + id);
   if (chk.get('checked')) {
      all = dijitRegistry.byId(gid + '_attr_edit_all_' + id);
     if (all == null || all.get('checked')) return '<AttributeRule attributeID="'+id+'" action="replace"><PermitValueRule xsi:type="basic:ANY"/></AttributeRule>';
      nv = 0;
     txt = '<PermitValueRule xsi:type="basic:OR">';
     for (i=0;i<99;i++) {
        vw = dijitRegistry.byId(gid + '_attr_edit_v_' + i + '_' + id);
        if (vw==null) break;
        v = vw.get('value').trim();
        if (v!='') {
           if (id=='gws_groups') {
              if (v.indexOf('urn:')!=0) v = 'urn:mace:washington.edu:groups:' + v;
              console.log('chk fix val: ' + v);
           } 
          if (dijitRegistry.byId(gid + '_attr_edit_x_' + i + '_' + id).get('checked')) {
              txt += '<basic:Rule xsi:type="basic:AttributeValueRegex" regex="' + iam_makeOkXml(v) + '"/>';
          } else { 
              txt += '<basic:Rule xsi:type="basic:AttributeValueString" value="' + iam_makeOkXml(v) + '"/>';
          }
          nv += 1;
        }
     }
     if (nv>0) {
         txt += '</PermitValueRule>'
         return '<AttributeRule attributeID="' + id + '" action="replace">' + txt + '</AttributeRule>';
     }
   } else { // not checked
     inn = dojoDom.byId(gid + '_attr_edit_chk_' + id + '_in');
     console.log(id + ' in value ' + inn.value);
     if (inn.value!='') return '<AttributeRule attributeID="' + id + '" action="remove"></AttributeRule>';
   }
   return '';
}

function _postSaveAttrs() {
   iam_hideTheDialog('attrEditDialog');
   iam_showTheMessage('Attributes updated: Allow 20 minutes to propagate.');
   showCurrentSp();
}

// save attr changes
function attr_saveAttrs(gid, entityId) {
   xml = '<FilterPolicyModification><AttributeFilterPolicy policyId="' + gid + '" entityId="' + entityId + '">';
   xml = xml + '<PolicyRequirementRule xsi:type="basic:AttributeRequesterString" value="' + entityId + '" />'
    // get all the attributes
   alist = dojoQuery('.' + gid + '_attr_edit_chk');
   for (a=0; a<alist.length; a++) {
       w = dijitRegistry.byNode(alist[a]);
       aid = w.get('id').replace(gid+'_attr_edit_chk_','');
       if (aid=='default') continue;  // not a real attribute
       xml += _attributeXml(gid, aid);
   }
   xml = xml + '</AttributeFilterPolicy></FilterPolicyModification>';
   action = v_root + v_vers + '/rp/attr?id=' + entityId + '&policyId=' + gid + '&xsrf=' + v_xsrf  + adminQS;
   iam_putRequest(action, null, xml, null, _postSaveAttrs);
}

/*
 * gateway tools
 */


function proxyEditHide() {
   console.log("proxy popup hide");
   var gcid = dojoDom.byId('google_cid').value.trim();
   if (gcid!='' && numProxyEditKey>1) {  // '1' to ignore the 'close' click
      iam_showTheDialog('proxyNotSavedDialog');
   }
}

function proxyEditShow() {
   console.log("proxy popup show");
}
function proxyEditKey() {
   console.log("proxy popup key");
   numProxyEditKey += 1;
   if (numProxyEditKey==1) dijitRegistry.byId('proxyEditSaver').set('disabled',0);
}

function _postSaveProxy() {
   iam_hideTheDialog('proxyEditDialog');
   iam_showTheNotice('Parameters saved: Allow 20 minutes to propagate.');
   showCurrentSp();
   numProxyEditKey = 0;
}

// submit proxy edits
function proxy_saveProxy(entityId) {
   var gcid = dojoDom.byId('google_cid').value.trim();
   var gcpw = dojoDom.byId('google_cpw').value.trim();
   var fcid = dojoDom.byId('facebook_cid').value.trim();
   var fcpw = dojoDom.byId('facebook_cpw').value.trim();
   xml = '<Proxys><Proxy entityId="' + currentSp.id + '">';
   if (gcid!='') xml += '<ProxyIdp idp="Google" clientId="' + iam_makeOkXml(gcid) + '" clientSecret="' + iam_makeOkXml(gcpw) + '"/>';
   if (fcid!='') xml += '<ProxyIdp idp="Facebook" clientId="' + iam_makeOkXml(fcid) + '" clientSecret="' + iam_makeOkXml(fcpw) + '"/>';
   xml += '</Proxy></Proxys>';
   // console.log(xml);
   var url = v_root + v_vers + '/rp/proxy?id=' + entityId + '&xsrf=' + v_xsrf + adminQS;
   iam_putRequest(url, null, xml, null, _postSaveProxy);
}

function proxy_deleteProxy(entityId) {
   var gcid = dojoDom.byId('google_cid').value.trim();
   var gcpw = dojoDom.byId('google_cpw').value.trim();
   xml = '<Proxys><Proxy entityId="' + currentSp.id + '">'
        + '</Proxy></Proxys>';
   console.log(xml);
   var url = v_root + v_vers + '/rp/proxy?id=' + entityId + '&xsrf=' + v_xsrf + adminQS;
   iam_putRequest(url, null, xml, null, _postSaveProxy);
}
