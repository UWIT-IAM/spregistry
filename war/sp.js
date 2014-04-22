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
// vers: 04/16/2014

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

iam_set('hashCookie', 'spck1');
iam_set('hashHandler', hashHandler);

// user/admin role chooser

var adminQS = ''
function setRole(r, d) {
   if (r=='a') {
      adminQS = '&role=admin'
      dojoDom.byId('banner_notice').innerHTML = 'acting as administrator'; 
   } else {
      adminQS = ''
      dojoDom.byId('banner_notice').innerHTML = ''; 
   }
   showHomePage();
   if (d!=null) iam_hideTheDialog(d);
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
      console.log('showcur ' + v_currentSpTab + '!' + currentSp.id);
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

// Catch 'enter' in the new-sp textbox.  Act as if 'Continue'
function checkNewSp(e) {
  console.log(e);
  if (e.keyCode==13) {  // enter
     meta_lookupSp();
     return;
  }
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

function showSpList(e) {
  
  curselsp = (-1);
  nsp = spList.length;
  console.log(nsp + ' service providers loaded');

  var txsp = dijitRegistry.byId('filterSpList').get('value');

  // Count how many will show.  We darken the list when it gets short
  var ndsp = 0;
  var dc = 'dim0';
  for (i=0; i<nsp; i++) {
    if ((txsp.length>0) && spList[i].id.indexOf(txsp)<0) continue;
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
    ndsp += 1;
    if (ndsp>10) {
       htm += '<span class="listitem dim4"><i>.&nbsp;.&nbsp;.&nbsp;</i></span>';
       break;
    }
   
    // decorate the link with org and federation
    ttl = spList[i].org;
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
}

// load the list of SPs.  API call to server.
function loadSpList()
{
   var url = v_root + v_vers + '/rps';
   iam_getRequest(url, null, 'json', function(data, args) {
        spList = data.rps;
        showSpList();
        iam_hashHandler();
      });
}


/* panel sizing */

// index and display panels have been sized already by iam-dojo.js

function setPaneSizes() {
   console.log('Set pane sizes.....');
   var ih = dojo.position(dojo.byId('indexPanel'),true).h;
   var h = ih - 20;

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

}

// presently deactivated
function adjustSPIndexSize() {
return 0;
   var pane = dojo.byId('spIndexPane');
   var pHeight = dojo.position(pane,true).h;
   var tHeight = dojo.position(dojo.byId('indexTitlebar'),true).h;
   var sHeight = dojo.position(dojo.byId('indexSubtitlebar'),true).h;
   var bHeight = dojo.position(dojo.byId('indexPanel'),true).h;
   var h = bHeight - tHeight -sHeight - 40 ;
   dojo.style(pane, {
     height: h + 'px'
   });
   // alert('set to ' + dojo.position(dojo.byId(paneName),true).h);
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

// respond to the edit button
function handleSpEditBtn(cn) {
   console.log('Sp edit');
   iam_hideShow(['metaViewPane','metaViewLinks'],['metaEditPane','metaEditLinks']);
}
// respond to the view button
function handleGroupViewBtn(cn) {
   console.log('group view');
   iam_hideShow(['metaEditPane','metaEditLinks'],['metaViewPane','metaViewLinks']);
}



// pseudo getbyname that works with ie
function _getElementsByIdname(base) {
   var list = [];
   var i = 0;
   while (dojoDom.byId(base + '_' + i)) {
      list.push(dojoDom.byId(base + '_' + i++));
   }
   return list;
}

// respond to one of the 'add xxx' buttons
meta_showMoreFields = function(name, id) {
   // show the first of the hidden ones
   for (e=0; e<20; e++) {
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
function postLoadNewSp() {
   console.log('postLoadNewSp');
   dojo.disconnect(newSpConnect);
   newSpConnect = null;
   postLoadSp();
   iam_showTheDialog('metaEditDialog',[]);
}

// API call to fetch or generate metadata for a new SP
function _lookupSp(rpid, lookup) {
   currentSp = null
   newSpId = rpid;
   v_spLoading = true;
   if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();
   var url = v_root + v_vers + '/new?rpid=' + rpid + adminQS;
   if (!lookup) url += '&nolook=y';
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

meta_lookupSp = function() {
   var dns = dijitRegistry.byId('newSp').get('value').trim();
   if (dns==null || dns=='') {
      iam_showTheNotice('you must provide an entityid');
      return;
   }
   var lookup = dijitRegistry.byId('newSpLookup').get('checked');
   return _lookupSp(dns, lookup);
}  

/* 
 * tools to handle save of metadata
 */

var badRE = new RegExp("[<>&]");
var nameRE = new RegExp("^[a-z][a-z0-9\.\_\-]+$");

function chkText(v, e) {
   if (v.search(badRE)>=0) {
       iam_showTheNotice("invalid " + e);
       return 0;
   }
   return 1;
}

// build the rp xml
function assembleRPMetadata(entityId) {
   rpId = entityId;
   console.log('get rp info for ' + rpId);
   var xml = '<EntityDescriptor entityID="' + entityId + '">';

   // SPSSO
   var pse = '';
   for (i=0; i<5; i++) {
      e = dojoDom.byId('pse_' + i);
      if (e!=null) {
         v = e.value.trim();
         if (chkText(v, "PSE")) return null;
         if (v == '') continue;
         if (pse=='') pse = 'protocolSupportEnumeration="' + e.value.trim();
         else pse = pse + ' ' + e.value.trim();
         
      }
   }
   if (pse=='') {
      iam_showTheNotice("You must provide at least one protocol ");
      return '';
   }
   xml = xml + '<SPSSODescriptor ' + pse + '">';

   // keyinfo
   hadKi = false;
   for (i=0; i<4; i++) {
      kn = dojoDom.byId('kn_' + i);
      kc = dojoDom.byId('kc_' + i);
      knv = kn.value.trim();
      kcv = kc.value.trim();
      if (knv=='' && kcv=='') continue;
      if (chkText(knv, "keyname")) return null;
      if (chkText(knc, "cert pem")) return null;
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
         if (chkText(v, "nameid")) return null;
         xml = xml + '<NameIDFormat>' + v + '</NameIDFormat>';
      }
   }

   // acs
   hadAcs = false;
   for (i=0; i<50; i++) {
      idx = dojoDom.byId('acsi_' + i);
      idxv = idx.value.trim();
      if (idxv=='') continue;
      bv = dojoDom.byId('acsb_' + i).value.trim();
      lv = dojoDom.byId('acsl_' + i).value.trim();
      if (chkText(bv, "acs binding")) return null;
      if (chkText(lv, "acs location")) return null;
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

   // contact
   hadName = false;
   hadMail = false;
   hadPhone = false;
   for (i=0; i<5; i++) {
      // v = dojoDom.byId('ctt_' + i).value.trim();
      var v = dijitRegistry.byId('ctt_' + i).get('value').trim();
      if (v=='') continue;
      xml = xml + '<ContactPerson contactType="' + iam_makeOkXml(v) + '">';
      v = dojoDom.byId('ctgn_' + i).value.trim();
      if (v!='') {
         xml = xml + '<GivenName>' + iam_makeOkXml(v) + '</GivenName>';
         hadName = true;
      }
      v = dojoDom.byId('cte_' + i).value.trim();
      if (v!='') {
         xml = xml + '<EmailAddress>' + iam_makeOkXml(v) + '</EmailAddress>';
         hadMail = true;
      }
      v = dojoDom.byId('ctp_' + i).value.trim();
      if (v!='') {
         xml = xml + '<TelephoneNumber>' + iam_makeOkXml(v) + '</TelephoneNumber>';
         hadPhone = true;
      }
      xml = xml + '</ContactPerson>';
   }
   if (!hadName) {
      iam_showTheNotice("You must provide a contact name");
      return '';
   }
   if (!hadMail) {
      iam_showTheNotice("You must provide a contact Email address");
      return '';
   }

   xml = xml + '</EntityDescriptor>';

   return xml;
}

// after metadata has been saved, reload the SP 

function postSaveRP() {
   console.log('postSaveRP');
   iam_bannerNotice('Changes saved');
   var url = v_root + v_vers + '/rp/?id=' + rpId + '&mdid=UW' + adminQS;
   if (currentSp==null) {
      console.log('post load new SP');
      // add the new sp to the list  ( quicker than reloading the list )
      spList[nsp] = {'id':rpId, 'meta':'UW', 'org':''};
      nsp += 1;
   }
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('href', url);
   document.body.style.cursor = 'default';
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
   iam_bannerNotice('Relying party ' + rpId + ' deleted');
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
   iam_bannerNotice('Request submitted.');
}

// submit the request
attr_requestAttrs = function(entityId) {

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
     inn = dojoDom.byId(w.get('id') + '_in');
     console.log(aid + ' in value ' + inn.value);
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
attr_checkAttr = function(gid, id) {
   chk = dijitRegistry.byId(gid + '_attr_edit_chk_' + id);
   if (chk.get('checked')) {
      dojoDom.byId(gid + '_attr_edit_tr_all_' + id).style.display = '';
      dijitRegistry.byId(gid + '_attr_edit_all_' + id).set('value',true);
   } else {
      dojoDom.byId(gid + '_attr_edit_tr_all_' + id).style.display = 'none';
   }
};

// respond to 'all' checkbox
attr_checkAll = function(gid, id) {
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

attr_showNext = function(gid, i, id) {
   n = i+1;
   dojoDom.byId(gid + '_attr_edit_tr_v_' + n + '_' + id).style.display = '';
}


// save values

// format an attribute
function _attributeXml (gid, id) {
   console.log('atr: ' + gid + '_attr_edit_chk_' + id);
   chk = dijitRegistry.byId(gid + '_attr_edit_chk_' + id);
   if (chk.get('checked')) {
      all = dijitRegistry.byId(gid + '_attr_edit_all_' + id);
     if (all.get('checked')) return '<AttributeRule attributeID="'+id+'" action="replace"><PermitValueRule xsi:type="basic:ANY"/></AttributeRule>';
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
   iam_bannerNotice('Attributes updated: Allow 20 minutes to propagate.');
   showCurrentSp();
}

// save attr changes
attr_saveAttrs = function(gid, entityId) {
   xml = '<FilterPolicyModification><AttributeFilterPolicy policyId="' + gid + '" entityId="' + entityId + '">';
   xml = xml + '<PolicyRequirementRule xsi:type="basic:AttributeRequesterString" value="' + entityId + '" />'
    // get all the attributes
   alist = dojoQuery('.' + gid + '_attr_edit_chk');
   for (a=0; a<alist.length; a++) {
       w = dijitRegistry.byNode(alist[a]);
       xml += _attributeXml(gid, w.get('id').replace(gid+'_attr_edit_chk_',''));
   }
   xml = xml + '</AttributeFilterPolicy></FilterPolicyModification>';
   // alert(xml);
   action = v_root + v_vers + '/rp/attr?id=' + entityId + '&policyId=' + gid + '&xsrf=' + v_xsrf  + adminQS;
   iam_putRequest(action, null, xml, null, _postSaveAttrs);
}

/*
 * gateway tools
 */


function _postSaveProxy() {
   iam_hideTheDialog('proxyEditDialog');
   iam_bannerNotice('Parameters saved: Allow 20 minutes to propagate.');
   showCurrentSp();
}

// submit proxy edits
proxy_saveProxy = function(entityId) {
   var gcid = dojoDom.byId('google_cid').value.trim();
   var gcpw = dojoDom.byId('google_cpw').value.trim();
   var lcid = dojoDom.byId('liveid_cid').value.trim();
   var lcpw = dojoDom.byId('liveid_cpw').value.trim();
   xml = '<Proxys><Proxy entityId="' + currentSp.id + '">';
   if (gcid!='') xml += '<ProxyIdp idp="Google" clientId="' + iam_makeOkXml(gcid) + '" clientSecret="' + iam_makeOkXml(gcpw) + '"/>';
   if (lcid!='') xml += '<ProxyIdp idp="LiveID" clientId="' + iam_makeOkXml(lcid) + '" clientSecret="' + iam_makeOkXml(lcpw) + '"/>';
   xml += '</Proxy></Proxys>';
   console.log(xml);
   var headertxt = {'Content-type': 'application/xhtml+xml; charset=utf-8'};
   var url = v_root + v_vers + '/rp/proxy?id=' + entityId + '&xsrf=' + v_xsrf + adminQS;
   iam_putRequest(url, null, xml, null, _postSaveProxy);
   // location.reload();
}

