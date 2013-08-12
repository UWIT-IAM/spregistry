/* ========================================================================
 * Copyright (c) 2011-2013 The University of Washington
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
// vers: 07/05/2013

// common vars
var v_root = '/sp-registry';
var v_remoteUser = '';
var v_xsrf = '';
var v_etag = '';
var v_loadErrorMessage = "Operation failed. You may need to reload the page to reauthenticate";


// track current sp by hash
var v_currentSpTab = 'm';
var v_spLoading = false;

// sp list ( structures from the server )

var spList;
var nsp = 0;
var currentSp;  // the active sp or null

iam_set('rightSide', 'spDisplay');


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
// iam_hashInit('sprck', hashHandler);

// show the home panel
function showHomePage() {
   iam_hideShow(['spDisplay'],['homeDisplay']);
   currentSp = null;
}
// show the sp panel
function showSpPanel() {
  iam_hideShow(['homeDisplay'],['spDisplay']);
}


// SP display tools

var spKeyListener = null;

function checkSpTabKey(e) {
  console.log(e);
}

// after the sp page has loaded.  set the right tab
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

// after the sp page has loaded.  setup watchers and etc
function postLoadSp() {
   console.log('postload: tab=' + v_currentSpTab);

   dojoDom.byId('spTitle').innerHTML = currentSp.id;
   var sp = dijitRegistry.byId('spPanel');
   sp.watch('selectedChildWidget',
      function(name, otab, ntab) {
         console.log("tab now ", ntab.id);
         var tab = ntab.id.substring(0,1);
         v_currentSpTab = tab;
         iam_hashSetCurrent(tab, null);
      });
   // newSpKeyUp = dojo.connect(dijitRegistry.byId('spDisplay'), 'onKeyUp', checkSpTabKey);
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
          case 68: // D
          case 100:
             if (v_currentSpTab=='m') iam_showTheDialog('metaDeleteDialog');
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


// show an sp by index

function showSp(i, tab) {
   console.log('showsp, i='+i+' current=' + currentSp);
   if (currentSp==null || currentSp.id!=spList[i].id) {
      currentSp = spList[i];
      v_currentSpTab = tab;
      showCurrentSp();
   } else if (tab!=v_currentSpTab) {
      v_currentSpTab = tab;
      setSpTab();
   }
}

// new sp 
// this starts the process.  postLoadSp ends it

function lookupSp() {
   alert('plain lookupsp called');
   var ndns = document.getElementById('new_dns').value.trim();
   if (ndns==null || ndns=='') {
      iam_showTheNotice('You must provide a dns name');
      return;
   }
   console.log(ndns);
   currentSp = '';
   currentSpTab = 'm';
   v_spLoading = true;
   if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();
   var url = v_root + v_vers + '/new?dns=' + ndns + '&view=inner';
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('href', url);
   dijitRegistry.byId('spDisplay').set('onLoad', postLoadSp);
   showSpPanel();
   window.focus();
}

// show the current sp
// this starts the process.  postLoadSp ends it

function showCurrentSp() {
   v_spLoading = true;
   if (currentSp!=null) {
      console.log('showcur ' + v_currentSpTab + '!' + currentSp.id);
      iam_hashSetCurrent(v_currentSpTab,currentSp.id);
   } else console.log('showCur no sur');
   if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();

   var url = v_root + v_vers + '/rp?id=' + currentSp.id + '&mdid=' + currentSp.meta + '&view=inner';
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('loadingMessage', 'Loading ' + currentSp.id + ' . . .' );
   dijitRegistry.byId('spDisplay').set('href', url);
   dijitRegistry.byId('spDisplay').set('onLoad', postLoadSp);
   showSpPanel();
   window.focus();
}

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

// check the new sp entry for enter
function checkNewSp(e) {
  console.log(e);
  if (e.keyCode==13) {  // enter
     meta_lookupSp();
     return;
  }
}

// fill out the list according to filter settings
// 'enter' select the one on top
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

function showSpList(e) {
  
  curselsp = (-1);
  nsp = spList.length;
  // console.log(nsp + ' service providers');
  var txsp = dijitRegistry.byId('filterSpList').get('value');
 
  // count how many
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
  for (i=0; i<nsp; i++) {
    if ((txsp.length>0) && spList[i].id.indexOf(txsp)<0) continue;
    ttl = 'InCommon federation';
    if (spList[i].meta.substring(0,2)=='UW') ttl = 'UW federation';
   
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

// load the sp list
function loadSpList()
{
   var url = v_root + v_vers + '/rps';
   dojo.xhrGet({
     url: url,
     handleAs: 'json',
     load: function(data, args) {
        spList = data.rps;
        showSpList();
        iam_hashHandler();
      },
     error: function(data, args) {
        alert(args.xhr.status);
        if (args.xhr.status==418) {
           iam_showTheNotice('Try refresh');
           return;
        } else alert(iam_getAlertFromXmlData(data));
      }
   });
}


/* panel sizing */

// my group size adjust
function adjustSPIndexSize() {
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

// group pane sizing
function adjustSpPaneSize(type) {
   var pane = dojo.byId(type + 'SpPane');
   var pHeight = dojo.position(pane,true).h;
   var dHeight = dojo.position(dojo.byId('spDisplay'),true).h;
   var tHeight = dojo.position(dojo.byId('spPanel_tablist'),true).h;
   var aHeight = dojo.position(dojo.byId(type + 'SpActions'),true).h;
   var h = dHeight - 50 - tHeight - aHeight;  // room for title
   dojo.style(pane, {
     height: h + 'px'
   });
   // alert(type + 'spPane=' + dojo.position(pane,true).h +  ' display=' + dHeight + '  tab=' + tHeight + ' actions=' + aHeight);
}


function reparse(node) {
require(["dojo/parser"], function(parser){
  parser.parse(node);
});

}





/*
 * RP metadata tools 
 * 
 */

var rpId;
var mdId;
var newSpConnect = null;


   // pseudo getbyname that works with ie
   function _getElementsByIdname(base) {
      var list = [];
      var i = 0;
      while (document.getElementById(base + '_' + i)) {
         list.push(document.getElementById(base + '_' + i++));
      }
      return list;
   }

   // show some inputs
   meta_showMoreFields = function(name, id) {
      // show the first set of hidden ones
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
       plus = document.getElementById(id);
       plus.style.display = 'none';
   };

   function postLoadNewSp() {
      console.log('postLoadNewSp');
      postLoadSp();
      iam_showTheDialog('metaEditDialog',[]);
   }

   function _lookupSp(rpid, nolook) {
      currentSp = '';
      v_spLoading = true;
      if (dijitRegistry.byId('spPane')!=null)  dijitRegistry.byId('spPane').destroyRecursive();
      var url = v_root + v_vers + '/new?rpid=' + rpid;
      if (nolook) url += '&nolook=y';
      console.log(url);
      dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
      dijitRegistry.byId('spDisplay').set('loadingMessage', 'Searching for ' + rpid + ' . . .' );
      dijitRegistry.byId('spDisplay').set('href', url);
      // dijitRegistry.byId('spDisplay').set('onLoad', postLoadNewSp);
      newSpConnect = dojo.connect(dijitRegistry.byId('spDisplay'), 'onLoad', postLoadNewSp);
      showSpPanel();
      window.focus();
   }

   // user gives us dns name to query
   meta_lookupSp = function() {
      var dns = dijitRegistry.byId('newSp').get('value').trim();
      if (dns==null || dns=='') {
         iam_showTheNotice('you must provide an entityid');
         return;
      }
      var ck = dijitRegistry.byId('newSpNolookup').get('checked');
      return _lookupSp(dns, ck);
   }  

var nameRE = new RegExp("^[a-z][a-z0-9\.\_\-]+$");

// build the rp xml
function assembleRPMetadata(entityId) {
   rpId = entityId;
   var xml = '<EntityDescriptor entityID="' + entityId + '">';

   // SPSSO
   pse = '';
   for (i=0; i<5; i++) {
      e = document.getElementById('pse_' + i);
      if (e!=null) {
         v = e.value.trim();
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
      kn = document.getElementById('kn_' + i);
      kc = document.getElementById('kc_' + i);
      knv = kn.value.trim();
      kcv = kc.value.trim();
      if (knv=='' && kcv=='') continue;
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
      e = document.getElementById('ni_' + i);
      if (e!=null) {
         v = e.value.trim();
         if (v == '') continue;
         xml = xml + '<NameIDFormat>' + v + '</NameIDFormat>';
      }
   }

   // acs
   hadAcs = false;
   for (i=0; i<50; i++) {
      idx = document.getElementById('acsi_' + i);
      idxv = idx.value.trim();
      if (idxv=='') continue;
      bv = document.getElementById('acsb_' + i).value.trim();
      lv = document.getElementById('acsl_' + i).value.trim();
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
      v = document.getElementById('orgn_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationName>' + iam_makeOkXml(v) + '</OrganizationName>';
      else {
         iam_showTheNotice("You must provide an Org name");
         return '';
      }
      v = document.getElementById('orgd_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationDisplayName>' + iam_makeOkXml(v) + '</OrganizationDisplayName>';
      else {
         iam_showTheNotice("You must provide an Org display name");
         return '';
      }
      v = document.getElementById('orgu_' + i).value.trim();
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
      v = document.getElementById('ctt_' + i).value.trim();
      if (v=='') continue;
      xml = xml + '<ContactPerson contactType="' + iam_makeOkXml(v) + '">';
      v = document.getElementById('ctgn_' + i).value.trim();
      if (v!='') {
         xml = xml + '<GivenName>' + iam_makeOkXml(v) + '</GivenName>';
         hadName = true;
      }
      v = document.getElementById('cte_' + i).value.trim();
      if (v!='') {
         xml = xml + '<EmailAddress>' + iam_makeOkXml(v) + '</EmailAddress>';
         hadMail = true;
      }
      v = document.getElementById('ctp_' + i).value.trim();
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

function postSaveRP() {
   console.log('postSaveRP');
   iam_showTheNotice('Changes saved');
   var url = v_root + v_vers + '/rp/?id=' + rpId + '&mdid=UW';
   if (newSpConnect!=null) dojo.disconnect(newSpConnect);
   newSpConnect = null;
   dijitRegistry.byId('spDisplay').set('errorMessage', v_loadErrorMessage);
   dijitRegistry.byId('spDisplay').set('href', url);
   // handleGroupViewBtn();
   document.body.style.cursor = 'default';
}


// submit the changes
function saveRP(entityId) {
   xml = assembleRPMetadata(entityId);
   if (xml=='') return false;
   rpid = entityId;
   var url = v_root + v_vers + '/rp?id=' + entityId + '&mdid=UW&xsrf=' + v_xsrf;
   iam_putRequest(url, xml, null, postSaveRP);
}


function postDeleteRP() {
   iam_hideTheDialog('metaDeleteDialog');
   iam_showTheNotice('Relying party ' + rpId + ' deleted');
   iam_hideShow(['spDisplay'],['homeDisplay']);
   document.body.style.cursor = 'default';
}


// submit a delete
function deleteRP(entityId) {
   rpId = entityId;
   var url = v_root + v_vers + '/rp?id=' + entityId + '&mdid=UW&xsrf=' + v_xsrf;
   iam_deleteRequest(url, postDeleteRP);
}

// convert sloppy textarea into comma-separated list
function convertToList(str)
{
   list = str.split(/[\s,]+/);
   ret = list.join(",");
   return (ret);
}

//
//
//   Attribute functions
//

function local_check_gws() {
   ck = dijitRegistry.byId('attr_req_gws_groups').get('checked');
   console.log('ck='+ck);
   if (ck) iam_hideShow([],['attr_req_gws_text_tr']);
   else iam_hideShow(['attr_req_gws_text_tr'],[]);
}

var _okmsg;

function _postReqAttrs() {
   iam_hideTheDialog('attrReqDialog');
   iam_showTheMessage(_okmsg);
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
           if (grps=='') {
              iam_showTheNotice('Please identify the groups you need.');
              return;
           }
           var grpsin = dijitRegistry.byId('attr_req_gws_text_in').get('value').trim();
           if (grps==grpsin) continue;
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
   _okmsg = 'Request submitted<p><ul>' + _okmsg + '</ul>';
   msg = dijitRegistry.byId('attr_req_exptext').get('value').trim();
   if (msg=='') {
      iam_showTheNotice('Please explain why you need the attributes');
      return;
   }
   xml = xml + '<Comments>' + iam_makeOkXml(msg+gws_text) + '</Comments>';
   xml = xml + '</Attributes>';
   action = v_root + v_vers + '/rp/attrReq?id=' + entityId + '&xsrf=' + v_xsrf;
   iam_putRequest(action, xml, null, _postReqAttrs);
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
         if (v.style.display=='none') {
            v.style.display = '';
            break;
         }
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
   iam_showTheMessage('<h3>Attributes updated.</h3><p>Please allow 20 minutes for the changes to propagate to the IdP systems.</h3><p>');
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
   action = v_root + v_vers + '/rp/attr?id=' + entityId + '&policyId=' + gid + '&xsrf=' + v_xsrf ;
   iam_putRequest(action, xml, null, _postSaveAttrs);
}

/*
 * gateway tools
 */


function _postSaveProxy() {
   iam_hideTheDialog('proxyEditDialog');
   iam_showTheMessage('<h3>Gateway parameters saved.</h3><p>Please allow 20 minutes for the changes to propagate to the IdP systems.</h3><p>');
   showCurrentSp();
}

// submit proxy edits
proxy_saveProxy = function(entityId) {
   var gcid = document.getElementById('google_cid').value.trim();
   var gcpw = document.getElementById('google_cpw').value.trim();
   var lcid = document.getElementById('liveid_cid').value.trim();
   var lcpw = document.getElementById('liveid_cpw').value.trim();
   xml = '<Proxys><Proxy entityId="' + currentSp.id + '">';
   if (gcid!='') xml += '<ProxyIdp idp="Google" clientId="' + iam_makeOkXml(gcid) + '" clientSecret="' + iam_makeOkXml(gcpw) + '"/>';
   if (lcid!='') xml += '<ProxyIdp idp="LiveID" clientId="' + iam_makeOkXml(lcid) + '" clientSecret="' + iam_makeOkXml(lcpw) + '"/>';
   xml += '</Proxy></Proxys>';
   console.log(xml);
   var headertxt = {'Content-type': 'application/xhtml+xml; charset=utf-8'};
   var url = v_root + v_vers + '/rp/proxy?id=' + entityId + '&xsrf=' + v_xsrf;
   iam_putRequest(url, xml, null, _postSaveProxy);
   // location.reload();
}

