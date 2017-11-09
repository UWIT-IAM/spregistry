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

// iam dojo javascript tools

// common variables
var v_remoteUser = '';
var v_xsrf = '';
var v_etag = '';
var iam_loadErrorMessage = "Operation failed. You may need to reload the page to reauthenticate";


// globally define some common dojo objects
var dojoDom;
var dojoDoc;
var dojoConstruct;
var dojoCookie;
var dijitRegistry;
var dojoQuery;
var dijitFocus;
var dojoWindow;

// content area sizes
var contentHeight;
var contentWidth;
var contentTop;


require([
   "dijit/registry",
   "dojo/window",
   "dojo/_base/window",
   "dojo/dom",
   "dojo/dom-construct",
   "dojo/query",
   "dijit/focus",
   "dojo/cookie"], function(registry, window, baseWindow, dom, construct, query, focus, cookie) {
      dijitRegistry = registry;
      dojoWindow = window;
      dojoDoc = baseWindow.doc;
      dojoDom = dom;
      dojoConstruct = construct;
      dojoCookie = cookie;
      dojoQuery = query;
      dijitFocus = focus;
});

// Trim leading and following spaces from a string
String.prototype.trim = function () {
   return this.replace(/^\s*|\s*$/g,"");
}


/** 
 ** Standard dialog and alert tools
 ** 
 **/

// safe focus
function iam_focus(id) {
   var e = dojoDom.byId(id);
   console.log('set focus to ' + id);
   if (e!=null) e.focus();
}

function iam_showTheDialog(d, req) {
   dig = dijitRegistry.byId(d);
   if (dig!=null) {
      dig.show();
      iam_focus(d);
   }
}

function iam_hideTheDialog(d) {
   dig = dijitRegistry.byId(d);
   if (dig!=null) dig.hide();
   else console.log(d + 'not found');
}

// open a dialog whose html is a url
// dialog is: d
// content is: d + 'Content'

function iam_loadTheDialog(d, html) {
   var cdiv = d + 'Content';
   iam_getRequest(html, {'Accept': 'text/xml'}, 'text', function(data, args) {
        // set the container height 
        var bx = dojoWindow.getBox();
        var bh2 = (bx.h)/2;
        var fdd = dojoDom.byId(cdiv);
        console.log('half height: ' + bh2);
        dojo.style(fdd, {
          height: bh2 + 'px'
        });

        // load the html into the container
        var fd = dijitRegistry.byId(cdiv);
        fd.set('content',data);

        // show the dialog
        dijitRegistry.byId(d).show();
        // reposition it
        fdd = dojoDom.byId(d);
        dojo.style(fdd, {
          top: '100px'
        });
      });
}

// hide and show some divs
function iam_hideShow(hides, shows) {
  for (var i=0;i<hides.length;i++) {
     var e = dojoDom.byId(hides[i]);
     if (e!=null) e.style.display = 'none';
  }
  for (var i=0;i<shows.length;i++) {
     var e = dojoDom.byId(shows[i]);
     if (e!=null) e.style.display = '';
  }
}

// fade out/in
function iam_fadeOutIn(outs, ins) {
   require(["dojo/_base/fx"], function(fx){
    for (var i=0;i<outs.length;i++) {
      fx.fadeOut({
        node: dojoDom.byId(outs[i]),
        duration: '1000'
      }).play();
    }
    for (var i=0;i<ins.length;i++) {
      fx.fadeIn({
        node: dojoDom.byId(ins[i]),
        duration: '1000'
      }).play();
    }
   });
}

// show a fading notice
function iam_bannerNotice(msg) {
   console.log('showing notice: ' + msg);
   var ele = dojoDom.byId('bannerNotice');
   var jele = dijitRegistry.byId('bannerNotice');
   jele.set('content', msg);
   jele.set('aria-hidden', 'false');
   ele.style.display = "";
   ele.style.opacity = "1.0";
   require(["dojo/_base/fx"], function(fx){
      fx.fadeOut({
        node: ele,
        duration: '6000',
        onEnd: function() {
          jele.set('aria-hidden', 'true');
          console.log('notice gone');
        }
      }).play();
    });
}
   
// better alert tools

// show a formatted message
function iam_showTheMessage(msg, ttl) {
  require(["dojo/ready","iam/Dialog"], function(ready, dialog) {
    ready(function(){
     if (ttl==undefined) ttl = '';
     myd = new dialog({
        title:ttl,
        cancelLabel: 'OK',
        content: msg,
        role: 'alert',
        onHide: function() {
           console.log('notice hidden, destroying');
           this.destroyRecursive();
        }
     });
     console.log('showing notice');
     myd.show();
  });
 });
}

// show a text message
function iam_showTheNotice(msg) {
  iam_showTheMessage('<h3 tabindex="0">' + msg + '</h3>');
}

// xmlify a string
function iam_makeOkXml(str) {
   return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/'/g,'&apos;').replace(/"/g,'&quot;');
}

/** 
 **   Hash tools 
 **/

var _hashCookie = 'iamhash';
var _hashHandler = null;
var _hashCurrentTab = '1';
var _hashCurrentValue = '';

function iam_hashInit(cookie, handler) {
   _hashCookie = cookie;
   _hashHandler = handler;
}

// note change of values
function iam_hashSetCurrent(t, v) {
   var n = false;
   if (t!=_hashCurrentTab) {
      _hashCurrentTab = t;
      n = true;
   }
   if (v!=null && v!=_hashCurrentValue) {
      _hashCurrentValue = v;
      n = true;
   }
   if (n) {
      console.log('setting hash cookie');
      dojoDoc.location.hash = _hashCurrentTab + _hashCurrentValue;
      dojoCookie(_hashCookie, '#' + _hashCurrentTab + _hashCurrentValue, {path:'/'}); 
   }
}


// hash change:  do something with the current hash

function iam_hashHandler(hsh) {
   console.log('iamhashhandler: ' + hsh);
   if (_hashHandler==null) return; 

   if ( typeof(hsh) == 'undefined') hsh = '';
   // var h = dojoDoc.location.hash;
   var h = '#' + hsh;
   if ( typeof(h) == 'undefined') h = '';
   if (h.length<3) {
      var ch = dojoCookie(_hashCookie);
      if (typeof(ch) != 'undefined') {
         h = ch;
         console.log('hash from from cookie');
      }
   }
   console.log('hash = ' + h);
   if (h.charAt(0)==_hashCurrentTab && h.substring(1)==_hashCurrentValue) return;  // no change

   if (h.length>1) {
      _hashCurrentTab = h.charAt(1);  // skip the '#'
      _hashCurrentValue = h.substring(2);
      _hashHandler(_hashCurrentTab, _hashCurrentValue);
   } else _hashHandler(null,null);
}

// connect us to hash changes
//require(["dojo/on"], function(on){
//   console.log('onhash connecting');
//   on(dojo.global, "onhashchange", iam_hashHandler);
//   console.log('onhash connected');
//});

require(["dojo/hash", "dojo/topic"], function(hash, topic){
  topic.subscribe("/dojo/hashchange", iam_hashHandler);
});




/** 
 **    Ajax requests
 **/

function _showAlertFromXmlData(data) {
   require(["dojox/xml/parser"], function(xparser) {
      console.log('parse ' + data);
      if (data==null) return;
      data = data.replace(/<\?[^\n]*\?>/, ''); // strip any header
      var doc = xparser.parse(data);
      var ae = doc.getElementsByTagName('alert');
      console.log(ae)
      if (ae==null || ae.item(0)==null) return null;
      val = ae.item(0).firstChild.nodeValue;
      console.log('val: ' + val);
      iam_showTheNotice(val);
   });
}

// ajax get
function iam_getRequest(url, headers, handleas, loader) {
   console.log('get req');
   dojo.xhrGet({
     url: url,
     headers: headers,
     handleAs: handleas,
     failOk: true,
     load: loader,
     error: function(data, args) {
        console.log('xhr error status: ' + args.xhr.status);
        console.log(data);
        console.log(args.xhr.responseText);
        // _showAlertFromXmlData(args.xhr.responseText);
      }
   });
}


// ajax put
function iam_putRequest(url, headers, data, handleas, postRequest) {
   document.body.style.cursor = 'wait';
   dojo.xhrPut({
     url: url,
     headers: headers,
     handleAs: handleas,
     putData: data,
     failOk: true,
     load: function(data, args) {
        document.body.style.cursor = 'default';
        if (postRequest!=null) postRequest(data,args);
      },
     error: function(data, args) {
        console.log('xhr error status: ' + args.xhr.status);
        console.log(data);
        console.log(args.xhr.responseText);
        _showAlertFromXmlData(args.xhr.responseText);
        document.body.style.cursor = 'default';
      }
   });
}


// ajax delete
function iam_deleteRequest(url, headers, handleas, postRequest) {
   document.body.style.cursor = 'wait';
   dojo.xhrDelete({
     url: url,
     headers: headers,
     handleAs: handleas,
     failOk: true,
     load: function(data, args) {
        document.body.style.cursor = 'default';
        if (postRequest!=null) postRequest(data, args);
      },
     error: function(data, args) {
        console.log('error' + args.xhr.status);
        console.log(data);
        // _showAlertFromXmlData(args.xhr.responseText);
        document.body.style.cursor = 'default';
      }
   });
}


/** 
 ** Basic window part sizing
 ** ( Index on left, display on right )
 **/

var _localSizer = null;
function iam_setLocalSizer(l) {
   _localSizer = l;
}

function iam_setPanelSizes() {
   console.log('adjust panels');
   require(["dojo/window"], function(win) {
     v_viewport = win.getBox();
     var tbh = dojo.position(dojo.byId('topbanner'),true).h;
     var bh = dojo.position(dojo.byId('banner'),true).h;
     var fh = dojo.position(dojo.byId('footer'),true).h;
     console.log('vh='+v_viewport.h+' vw='+v_viewport.w+' tbh='+tbh+' bh='+bh+' fh='+fh);

     var ch = v_viewport.h - tbh - bh - fh; 
     var cw = v_viewport.w;
     var ct = tbh + bh + 30;
     console.log('ch='+ch+' cw='+cw+' ct='+ct);
     contentHeight = ch;
     contentWidth = cw;
     contentTop = ct;

     var iw = cw * .30;
     var ih = ch - 80;
     var dw = cw - iw - 40; // allow space between
     var dh = ch - 80;

     var pan = dojo.byId('indexPanel');
     if (pan!=null) dojo.style(pan, {
        height: ih + 'px',
        width: iw + 'px',
        top: ct + 'px',
        left: '5px'
     });

     pan = dojo.byId('displayPanel');
     if (pan!=null) dojo.style(dojo.byId('displayPanel'), {
        height: dh + 'px',
        width: dw + 'px',
        top: ct + 'px',
        right: '5px'
     });

   });
   if (_localSizer!=null) _localSizer();
}




/**
 **  stuff
 **/

function iam_widgetFade(node) {
  dojo.style(node, "opacity", "1");
  var fadeArgs = {
     node: node,
     duration: 3000
  };
  dojo.fadeOut(fadeArgs).play();
}

// set parameters

function iam_set(p, v) {
   if (p=='rightSide') _rightSideDiv = v;
   if (p=='hashCookie') _hashCookie = v;
   if (p=='hashHandler') _hashHandler = v;
}

