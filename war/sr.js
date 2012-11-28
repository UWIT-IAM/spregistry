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

// Trim leading and following spaces from a string
String.prototype.trim = function () {
   return this.replace(/^\s*|\s*$/g,"");
}

// common vars
var v_root = '/sp-registry/v1';
var v_remoteUser = '';
var v_xsrf = '';
var v_etag = '';
var v_loadErrorMessage = "Operation failed. You may need to reload the page to reauthenticate";


// sp list

var spList;
var nsp = 0;


/* initializations */

function getAlertFromData(data) {
   // alert(data);
   data = data.replace(/<\?[^\n]*\?>/, '');
   var doc = dojox.xml.parser.parse(data);
   var ae = doc.getElementsByTagName('alert');
   // alert(ae)
   if (ae!=null && ae.item(0)!=null) return ae.item(0).firstChild.nodeValue;
   else return null
}


// show the hash group if present (onload)
function showHashGroup() {
   var h = dojo.doc.location.hash;
   if (h.length>2) {
      var tgt = h.substring(1);
      alert('hash = ' + tgt);
      for (i=0; i<nsp; i++) {
        if (spList[i].id==tgt) {
           showSp(i);
           return;
        }
      }
   }
   showGroupPanel();
}

function showDialog(id) {
  dijit.byId(id).show();
}

function showDetail(p) {
  dojo.byId(p+'messyDetail').style.display = '';
  dojo.byId(p+'showDetail').style.display = 'none';
  dojo.byId(p+'hideDetail').style.display = '';
}
function hideDetail(p) {
  dojo.byId(p+'messyDetail').style.display = 'none';
  dojo.byId(p+'showDetail').style.display = '';
  dojo.byId(p+'hideDetail').style.display = 'none';
}


/* searches and results */

var currentSp;

// show an SP

function showSp(i) {
   currentSp = spList[i];
   showCurrentSp();
}

function lookupSp() {
   var ndns = document.getElementById('new_dns').value.trim();
   if (ndns==null || ndns=='') {
      alert('you must provide a dns name');
      return;
   }
   alert(ndns);
   currentSp = '';
   dojo.doc.location.hash = '';
   v_spLoading = true;
   if (dijit.byId('groupPanel')!=null)  dijit.byId('groupPanel').destroyRecursive();
   var url = v_root + v_vers + '/new?dns=' + ndns + '&view=inner';
   dijit.byId('groupDisplay').set('errorMessage', v_loadErrorMessage);
   dijit.byId('groupDisplay').set('href', url);
   dijit.byId('groupDisplay').set('onLoad', postLoadSp);
   showGroupPanel();
   window.focus();
}

function showCurrentSp() {
   dojo.doc.location.hash = currentSp.id;
   v_spLoading = true;
   if (dijit.byId('groupPanel')!=null)  dijit.byId('groupPanel').destroyRecursive();

   var url = v_root + v_vers + '/rp?id=' + currentSp.id + '&mdid=' + currentSp.meta + '&view=inner';
   dijit.byId('groupDisplay').set('errorMessage', v_loadErrorMessage);
   dijit.byId('groupDisplay').set('href', url);
   dijit.byId('groupDisplay').set('onLoad', postLoadSp);
   // dojo.doc.location.hash = v_groupPaneSelect + ':' + selectedGroup;
   showGroupPanel();
   window.focus();
}

function postLoadSp() {
}

function showGroupPanel() {
  dojo.byId('homeDisplay').style.display = 'none';
  dojo.byId('newDisplay').style.display = 'none';
  dojo.byId('groupDisplay').style.display = '';
}
function showNewGroupPanel() {
  dojo.byId('homeDisplay').style.display = 'none';
  dojo.byId('groupDisplay').style.display = 'none';
  dojo.byId('newDisplay').style.display = '';
}

function setSearchOver(i) {
  require(["dojo/dom-class"], function(domClass){
    domClass.add('spitem' + i, 'groupitemhover');
  });
}
function setSearchOut(i) {
  require(["dojo/dom-class"], function(domClass){
    domClass.remove('spitem' + i, 'groupitemhover');
  });
}

// fill out the list according to filter settings
function showSpList() {
  nsp = spList.length;
  // alert(nsp);
  var txsp = dijit.byId('filterSpList').attr('value');
 
  var htm = '<dl>';
  for (i=0; i<nsp; i++) {
    if ((txsp.length>0) && spList[i].id.indexOf(txsp)<0) continue;
   
    htm = htm + '<dt>' + spList[i].meta.substring(0,2) + ': </dt>' +
        '<dd id="spitem' + i + '" class="groupitem"' +
        'onMouseDown="showSp(\'' + i + '\')" ' +
        'onMouseOver="setSearchOver(\'' + i + '\')" ' +
        'onMouseOut="setSearchOut(\'' + i + '\')">' +
      spList[i].id +
     '</dd>';
  }
 
 htm += '</dl>';
 dijit.byId('myGroupsPanel').set('content',htm);
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
      },
     error: function(data, args) {
        alert("error j: " + data) ;
      }
   });
}


/* panel sizing */

// my group size adjust
function adjustMGPaneSize() {
   var pane = dojo.byId('myGroupsPanel');
   var pHeight = dojo.position(pane,true).h;
   var tHeight = dojo.position(dojo.byId('groupsTitlebar'),true).h;
   var bHeight = dojo.position(dojo.byId('groupsPane'),true).h;
   var h = bHeight - tHeight -20 ;
   dojo.style(pane, {
     height: h + 'px'
   });
   // alert('set to ' + dojo.position(dojo.byId(paneName),true).h);
}

// group pane sizing
function adjustGroupPaneSize(paneName) {
   var pane = dojo.byId(paneName);
   var pHeight = dojo.position(pane,true).h;
   var tHeight = dojo.position(dojo.byId('groupPanel'),true).h;
   var h = tHeight - 50;  // room for title
   dojo.style(pane, {
     height: h + 'px'
   });
   // alert(paneName + ': parent=' + tHeight + '  pane_was=' + pHeight + ' pane_now=' + dojo.position(pane,true).h);
}

// basic fade

function widgetFade(id) {
  dojo.style(node, "opacity", "1");
  var fadeArgs = {
     node: node,
     duration: 3000
  };
  dojo.fadeOut(fadeArgs).play();
}

function reparse(node) {
require(["dojo/parser"], function(parser){
  parser.parse(node);
});

}
// group selector functions

function handleGroupViewBtn() {
  dijit.byId('groupEditPane').domNode.style.display = 'none';
  dijit.byId('groupViewPane').domNode.style.display = '';
}
function handleGroupEditBtn() {
  dijit.byId('groupViewPane').domNode.style.display = 'none';
  dijit.byId('groupEditPane').domNode.style.display = '';
}
function handleGroupNewBtn() {
  dijit.byId('groupViewPane').domNode.style.display = 'none';
  dijit.byId('groupEditPane').domNode.style.display = 'none';
}

function handleAttrViewBtn() {
  dijit.byId('attrEditPane').domNode.style.display = 'none';
  dijit.byId('attrViewPane').domNode.style.display = '';
}
function handleAttrEditBtn() {
  dijit.byId('attrViewPane').domNode.style.display = 'none';
  dijit.byId('attrEditPane').domNode.style.display = '';
}
function handleProxyViewBtn() {
  dijit.byId('proxyEditPane').domNode.style.display = 'none';
  dijit.byId('proxyViewPane').domNode.style.display = '';
}
function handleProxyEditBtn() {
  dijit.byId('proxyViewPane').domNode.style.display = 'none';
  dijit.byId('proxyEditPane').domNode.style.display = '';
}
function showGroupDialog(id) {
  dijit.byId(id).show();
}


// generic ajax action
function doRequest(method, url, xml, ifmatch, postRequest) {
   document.body.style.cursor = 'wait';
   var headertxt = {'Content-type': 'application/xhtml+xml; charset=utf-8'};
   // alert(method + ': ' + url);
   dojo.xhrPut({
     url: url,
     handleAs: 'text',
     putData: xml,
     headers: headertxt,
     failOk: true,
     load: function(data, args) {
        // alert('put: ' + xml);
        document.body.style.cursor = 'default';
        postRequest();
      },
     error: function(data, args) {
        alert(data);
        if (args.xhr.status==400) alert(getAlertFromData(data));
        else if (args.xhr.status==401) alert("no permission for " + groupIdent);
        else alert("put failed: " + args.xhr.status + " " + data + args) ;
        document.body.style.cursor = 'default';
      }
   });
}
/*
 * RP metadata save 
 * 
 */

var nameRE = new RegExp("^[a-z][a-z0-9\.\_\-]+$");
var rpId;
var mdId;

// build the rp xml
function assembleRPMetadata() {
   entityId = document.getElementById('entityid').value.trim();
   if (entityId=='') {
      alert("You must provide an EntityID");
      return '';
   }
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
      alert("You must provide at least one protocol ");
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
      alert("You must provide KeyInfo");
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
      alert("You must provide an ACS");
      return '';
   }
   xml = xml + '</SPSSODescriptor>';
   // org (only one really)
   for (i=0; i<1; i++) {
      xml = xml + '<Organization>';
      v = document.getElementById('orgn_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationName>' + v + '</OrganizationName>';
      else {
         alert("You must provide an Org name");
         return '';
      }
      v = document.getElementById('orgd_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationDisplayName>' + v + '</OrganizationDisplayName>';
      else {
         alert("You must provide an Org display name");
         return '';
      }
      v = document.getElementById('orgu_' + i).value.trim();
      if (v!='') xml = xml + '<OrganizationURL>' + v + '</OrganizationURL>';
      else {
         alert("You must provide an Org URL");
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
      xml = xml + '<ContactPerson contactType="' + v + '">';
      v = document.getElementById('ctgn_' + i).value.trim();
      if (v!='') {
         xml = xml + '<GivenName>' + v + '</GivenName>';
         hadName = true;
      }
      v = document.getElementById('cte_' + i).value.trim();
      if (v!='') {
         xml = xml + '<EmailAddress>' + v + '</EmailAddress>';
         hadMail = true;
      }
      v = document.getElementById('ctp_' + i).value.trim();
      if (v!='') {
         xml = xml + '<TelephoneNumber>' + v + '</TelephoneNumber>';
         hadPhone = true;
      }
      xml = xml + '</ContactPerson>';
   }
   if (!hadName) {
      alert("You must provide a contact name");
      return '';
   }
   if (!hadMail) {
      alert("You must provide a contact Email address");
      return '';
   }

   xml = xml + '</EntityDescriptor>';

   return xml;
}

function postSaveRP() {
   var url = v_root + v_vers + '/rp/?id=' + rpId + '&mdid=UW';
   dijit.byId('groupDisplay').set('errorMessage', v_loadErrorMessage);
   dijit.byId('groupDisplay').set('href', url);
   // handleGroupViewBtn();
   document.body.style.cursor = 'default';
}


// submit the changes
function saveRP() {
   xml = assembleRPMetadata();
   if (xml=='') return false;
   var url = v_root + v_vers + '/rp?id=' + entityId + '&mdid=UW&xsrf=' + v_xsrf;
   doRequest('put', url, xml, '', postSaveRP);
}

// group delete
function deleteGroup(cn, regid, se) {
   if (se) {
      alert("You must disable this group's enhanced security before deleting it.");
      return true;
   }
   v_groupCn = cn;
   v_groupRegid = regid;
   dojo.byId('deleteGroupText').innerHTML = '<tt>'+v_groupCn+'</tt>';
   dijit.byId('groupDeleteDialog').show();
}

function reallyDeleteGroup(dialog) {
   document.body.style.cursor = 'wait';
   dojo.xhrDelete({
      url: v_root + '/group/' + v_groupCn + '?xsrf=' + v_xsrf,
      handleAs: 'text',
      load: function(data, args) {
          dojo.byId('groupPanel').innerHTML = "deleted";
          // dojo.toggleClass(searchResultTree.getNodesByItem(selectedId)[0].rowNode, "dijitTreeRowSelected", false);
       },
       error: function(data, args) {
          alert('delete failed with status: ' + args.xhr.status);
       }
   });
   document.body.style.cursor = 'default';
   if (dialog) dijit.byId('groupDeleteDialog').hide();
}



function postSaveProxy() {
   handleProxyViewBtn();
}


// submit the changes
function saveProxy() {
   var cid = document.getElementById('google_cid').value.trim();
   var cpw = document.getElementById('google_cpw').value.trim();
   xml = '<Proxys><Proxy idp="google" entityId="' + currentSp.id + '" clientId="' + cid + '" clientSecret="' + cpw + '"/></Proxys>';
   // alert(xml);
   var headertxt = {'Content-type': 'application/xhtml+xml; charset=utf-8'};
   var url = v_root + v_vers + '/rp/proxy?id=' + currentSp.id + '&xsrf=' + v_xsrf;
   doRequest('put', url, xml, '', postSaveProxy);
   document.getElementById('proxyViewClientId').innerHTML = cid;
}


// convert sloppy textarea into comma-separated list
function convertToList(str)
{
   list = str.split(/[\s,]+/);
   ret = list.join(",");
   return (ret);
}

