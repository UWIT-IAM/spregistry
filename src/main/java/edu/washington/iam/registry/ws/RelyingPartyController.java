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

package edu.washington.iam.registry.ws;

import java.lang.Exception;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.BufferedWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import edu.washington.iam.registry.rp.RelyingPartyEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import edu.washington.iam.registry.rp.RelyingParty;
import edu.washington.iam.registry.rp.RelyingPartyManager;
import edu.washington.iam.tools.XMLHelper;
import edu.washington.iam.tools.DNSVerifier;
import edu.washington.iam.tools.DNSVerifyException;
import edu.washington.iam.tools.Group;
import edu.washington.iam.tools.GroupManager;

import edu.washington.iam.registry.filter.FilterPolicyManager;
import edu.washington.iam.registry.filter.AttributeFilterPolicy;
import edu.washington.iam.registry.filter.FilterPolicyGroup;
import edu.washington.iam.registry.filter.Attribute;

import edu.washington.iam.registry.proxy.Proxy;
import edu.washington.iam.registry.proxy.ProxyIdp;
import edu.washington.iam.registry.proxy.ProxyManager;

import edu.washington.iam.registry.exception.RelyingPartyException;
import edu.washington.iam.registry.exception.FilterPolicyException;
import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.registry.exception.NoPermissionException;
import edu.washington.iam.registry.exception.ProxyException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateParsingException;

import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceUtils;


@Controller
public class RelyingPartyController {

    private final Logger log =  LoggerFactory.getLogger(getClass());

    public static String SECURE_LOGIN_CLASS = "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken";

    private static FilterPolicyManager filterPolicyManager;
    private static RelyingPartyManager rpManager;
    private static ProxyManager proxyManager;

    private static DNSVerifier dnsVerifier;
    private static GroupManager groupManager;
    private String adminGroupName = null;
    private Group adminGroup = null;

    public DNSVerifier getDnsVerifier() {
        return dnsVerifier;
    }
    public void setDnsVerifier(DNSVerifier v) {
        dnsVerifier = v;
    }
    public void setGroupManager(GroupManager v) {
        groupManager = v;
    }

    private MailSender mailSender;
    private SimpleMailMessage templateMessage;

    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }

    private static String browserRootPath;
    private static String certRootPath;
    private static String loginCookie;
    private static String roleCookie = "spck2";
    private static String logoutUrl;

    private static String mailTo = "pubcookie@u.washington.edu";
    private static String requestMailTo = "iam-support@u.washington.edu";

    // sessions
    private String standardLoginPath = "/login";
    private String secureLoginPath = "/securelogin";
    private String googleLoginPath = "/googlelogin";
    private String incommonLoginPath = "/incommonlogin";
    private long standardLoginSec = 9*60*60;  // 9 hour session lifetime
    private long secureLoginSec = 1*60*60;  // 1 hour session lifetime
    private String googleIdentityProvider = "https://idp-gw.u.washington.edu/google"; //value overridden by spreg.properties
    private String spRegistryUrl = "https://iam-tools.u.washington.edu/spreg/";

    private String myEntityId = null;
    private String eppnName = "eppn";  // env var name of user eppn
    
    // key for crypt ops
    private static String cryptKey;

    class RPSession {
       private String viewType;
       private String remoteUser;
       private String rootPath;
       private String servletPath;
       private String pageType;
       private String pageTitle;
       private long ifMatch;
       private long ifNoneMatch;
       private String errorCode;
       private String errorText;
       private boolean isBrowser;
       private String xsrfCode;
       private String remoteAddr;
       private String loginMethod;
       private boolean isAdmin;
       private boolean authn2;
       private boolean isUWLogin;
       private String userIdProvider;
       private String userDisplayName;
       private ModelAndView mv;
       private long timeLeft;
       private boolean isProxy;
       private List altNames;
       private boolean adminRole;
       private boolean isMobile;
    }

    /* send user to login chooser page */ 
    private ModelAndView loginChooserMV(RPSession session, HttpServletRequest request, HttpServletResponse response) {

       String rp = "";
       if (request.getPathInfo()!=null) rp = request.getPathInfo();
       String rqs = "";  
       if (request.getQueryString()!=null) rqs = "?" +  request.getQueryString();
       String red = browserRootPath + request.getServletPath() + rp + rqs;
       log.debug("no user yet: final path=" + red);

       String view = "browser";
       if (session.isMobile) view = "mobile";
       ModelAndView mv = new ModelAndView(view + "/chooser");
       mv.addObject("root", browserRootPath);
       mv.addObject("vers", request.getServletPath());
       mv.addObject("pathextra", rp + rqs);
       mv.addObject("uwloginpath", standardLoginPath);
       mv.addObject("googleloginpath", googleLoginPath); 
       mv.addObject("incommonloginpath", incommonLoginPath);
       return (mv);
    }

    private RPSession processRequestInfo(HttpServletRequest request, HttpServletResponse response) {
       return processRequestInfo(request, response, true);
    }

    private RPSession processRequestInfo(HttpServletRequest request, HttpServletResponse response, boolean canLogin) {
        RPSession session = new RPSession();
        session.isAdmin = false;
        session.adminRole = false;
        session.isUWLogin = false;
        session.isProxy = false;
        String reloginPath = null;

        log.info("RP new session =============== path=" + request.getPathInfo());

        session.isMobile = false;
        Device currentDevice = DeviceUtils.getCurrentDevice(request);
        if (currentDevice!=null) session.isMobile = currentDevice.isMobile();
        log.debug("mobile? " + session.isMobile);

        // see if logged in (browser has login cookie; cert user has cert)

        int resetAdmin = 1;  // on expired or no cookie, reset the 'admin role cookei'
        Cookie[] cookies = request.getCookies();
        if (cookies!=null) {
          for (int i=0; i<cookies.length; i++) {
            if (cookies[i].getName().equals(loginCookie)) {
               log.debug("got cookie " + cookies[i].getName());
               String cookieStr = RPCrypt.decode(cookies[i].getValue());
               if (cookieStr==null) continue;
               String[] cookieData = cookieStr.split(";");
               if (cookieData.length==5) {

                  if (cookieData[3].charAt(0)=='2') session.authn2 = true;

                  log.debug("login time = " + cookieData[4]);
                  long cSec = new Long(cookieData[4]);
                  long nSec = new Date().getTime()/1000;
                  if (cookieData[1].indexOf("@")<0) session.isUWLogin = true;  // klugey way to know UW people
                  session.timeLeft = (cSec+standardLoginSec) - nSec;
                  if (session.timeLeft>0) {
                     if ((nSec>(cSec+secureLoginSec)) && session.authn2) {
                        log.debug("secure expired");
                        session.authn2 = false;
                        resetAdmin = 2;
                     }

                     // cookie OK
                     session.remoteUser = cookieData[1];
                     session.xsrfCode = cookieData[2];
                     log.debug("login for " + session.remoteUser );
                     if (session.authn2) log.debug("secure login");
                     if (adminGroup.isMember(session.remoteUser)) {
                        log.debug("is admin");
                        session.isAdmin = true;
                     }

                     if (resetAdmin==1) resetAdmin = 0;
                  } else {
                     log.debug("cookie expired for " + cookieData[1]);
                     // remember where they logged in last
                     if (session.isUWLogin) reloginPath = browserRootPath + request.getServletPath() + standardLoginPath;
                     else if (cookieData[1].indexOf("gmail.com")>0) reloginPath = browserRootPath + request.getServletPath() + googleLoginPath;
                     // let others choose
                  }
               }
            } else if (cookies[i].getName().equals(roleCookie) && cookies[i].getValue().equals("a")) {
               log.debug("got role=admin cookie");
               session.adminRole = true;
            }
          }
        }

        if (resetAdmin>0) {
           log.debug("clearing expired admn request");
           session.adminRole = false;
           Cookie c = new Cookie(roleCookie, "x");
           c.setSecure(true);
           c.setPath("/");
           response.addCookie(c);
        }

        if (session.remoteUser!=null) {
           // ok, is a logged in browser
           session.viewType = "browser";
           session.isBrowser = true;
           session.rootPath = browserRootPath;

        } else {
           // maybe is cert client
           // use the CN portion of the DN as the client userid
           X509Certificate[] certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
           if (certs != null) {
             session.viewType = "xml";
             session.isBrowser = false;
             session.rootPath = certRootPath;
             X509Certificate cert = certs[0];
             String dn = cert.getSubjectX500Principal().getName();
             session.remoteUser = dn.replaceAll(".*CN=", "").replaceAll(",.*","");
             log.info(".. remote user by cert, dn=" + dn + ", cn=" + session.remoteUser);
             session.altNames = new Vector();
             try {
                Collection altNames = cert.getSubjectAlternativeNames();
                if (altNames!=null) {
                   for (Iterator i = altNames.iterator(); i.hasNext(); ) {
                      List item = (List)i.next();
                      Integer type = (Integer)item.get(0);
                      if (type.intValue() == 2) {
                         String altName = (String)item.get(1);
                         log.info(".. adding altname " + altName);
                         session.altNames.add(altName);
                      }
                   }
                } else session.altNames.add(session.remoteUser);  // rules say cn meaningful only when altnames not present
             } catch (CertificateParsingException e) {
                log.info(".. altname parse failed: " + e);
             }
           }

        }

        /* send missing remoteUser to login */

        if (session.remoteUser==null) {
           if (canLogin) {
              if (reloginPath!=null) {
                 log.debug("no user yet:  relogin at " + reloginPath);
                  try {
                     response.sendRedirect(reloginPath);
                  } catch (IOException e) {
                     log.error("redirect: " + e);
                  }
              }
              log.debug("no user yet:  send to choose");
              session.mv = loginChooserMV(session, request, response);
              return session;
           }
           return null;
        }

        // only admins can get admin role
        if (!session.isAdmin) session.adminRole = false;
        if (session.adminRole && !session.authn2) {  // admin needs 2f
           log.debug("need secure login for admin role");
           sendToLogin(request, response, secureLoginPath);
        }
        session.servletPath = request.getServletPath();
        session.remoteAddr = request.getRemoteAddr();

        // etag headers
        session.ifMatch = getLongHeader(request, "If-Match");
        session.ifNoneMatch = getLongHeader(request, "If-None-Match");
        log.info("tags: match=" + session.ifMatch + ", nonematch=" + session.ifNoneMatch);

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max_age=1");
        response.setHeader("X-UA-Compatible", "IE=7");

        log.info("user: " + session.remoteUser);
        if (session.viewType.equals("browser") && session.isMobile) session.viewType = "mobile";
        return session;
    }

    private String fixPathName(String start, HttpServletRequest request) {
       String path = request.getPathInfo();
       // log.debug("full path = " + path);
       path = path.substring(start.length());
       // log.debug("trunc path = " + path);
       int slash = path.indexOf("/");
       if (slash>0) path = path.substring(0, slash);
       // log.debug("fixed path = " + path);
       return path;
    }

    /* send user to login page */
    private void sendToLogin(HttpServletRequest request, HttpServletResponse response, String loginPath) {

       // delete any existing sessions first
       Cookie[] cookies = request.getCookies();
       if (cookies!=null) {
         for (int i=0; i<cookies.length; i++) {
           if (cookies[i].getName().startsWith("_shib")) {
              log.debug("clearing cookie " + cookies[i].getName());
              Cookie c = new Cookie(cookies[i].getName(), "");
              c.setSecure(true);
              c.setPath("/");
              c.setMaxAge(0);
              response.addCookie(c);
           }
         }
       }
    
       String rp = "";
       if (request.getPathInfo()!=null) rp = request.getPathInfo();
       String rqs = "";
       if (request.getQueryString()!=null) rqs = "?" +  request.getQueryString();
       String red = browserRootPath + request.getServletPath() + loginPath + rp + rqs;
       log.debug("no user yet: redirect for login to " + red);
       try {
          response.sendRedirect(red);
       } catch (IOException e) {
          log.error("redirect: " + e);
       }
    }

    // create basic model and view
    private ModelAndView basicModelAndView(RPSession session, String view, String basePage) {
        ModelAndView mv = new ModelAndView(view + "/" + basePage);
        mv.addObject("XMLHelper", XMLHelper.class);
        mv.addObject("remote_user", session.remoteUser);
        mv.addObject("root", session.rootPath);
        mv.addObject("vers", session.servletPath);
        if (session.pageType != null) mv.addObject("pageType", view + "/" + session.pageType);
        mv.addObject("pageTitle", session.pageTitle);
        mv.addObject("xsrf", session.xsrfCode);
        mv.addObject("timeLeft", session.timeLeft);
        mv.addObject("adminRole", session.adminRole);
        return mv;
    }
    private ModelAndView basicModelAndView(RPSession session) {
        return (basicModelAndView(session, session.viewType, "page"));
    }
    private ModelAndView basicModelAndView(RPSession session, String view) {
        return (basicModelAndView(session, view, "page"));
    }

    // create 'empty' model and view
    private ModelAndView emptyMV(String message, String alert) {
        ModelAndView mv = new ModelAndView("empty");
        if (message!=null) mv.addObject("msg", message);
        if (message!=null) mv.addObject("alert", alert);
        return mv;
    }
    private ModelAndView emptyMV(String msg) {
        return emptyMV(msg, null);
    }
    private ModelAndView emptyMV() {
        return emptyMV("session error");
    }

    /*
     * Process login page.
     * Set a cookie and redirect back to original request
     * Encode remoteuser, method and time into the login cookie
     *
     * method = 0 -> google
     * method = 1 -> incommon shib
     * method = 2 -> 2-factor uw shib
     */

    private ModelAndView loginPage(HttpServletRequest request, HttpServletResponse response, int method) {
        //String remoteUser = request.getRemoteUser();
        String remoteUser = "mattjm@washington.edu";  //netid@washington.edu goes here for testing locally
        log.debug("social login attempt, shib remoteUser value: " + remoteUser);
        if (method==0) {  // social login
           String idp = (String)request.getAttribute("Shib-Identity-Provider");
           String mail = (String)request.getAttribute("mail");
           log.info("social login from " + idp + ", email = " + mail);
           log.debug("hardwired idp we expect is: " + googleIdentityProvider);
           if (idp.equals(googleIdentityProvider)) {
              remoteUser = mail;
           } else {
              log.debug("invalid social login");
              return emptyMV("invalid social login");
           }
        }

       String methodKey = "P";
       if (method==2) methodKey = "2";
       String aclass = (String)request.getAttribute("Shib-AuthnContext-Class");
       if (aclass!=null && aclass.equals(SECURE_LOGIN_CLASS)) methodKey = "2";
       log.debug("method = " + method + ", key = " + methodKey);

       if (remoteUser!=null) {
           if (remoteUser.endsWith("@washington.edu")) {
              remoteUser = remoteUser.substring(0, remoteUser.lastIndexOf("@washington.edu"));
              log.info("dropped @washington.edu to get id = " + remoteUser);
           }

           if (remoteUser.endsWith("@uw.edu")) {
              // no longer allow google's @uw to be same as UW login
              // remoteUser = remoteUser.substring(0, remoteUser.lastIndexOf("@uw.edu"));
              // log.info("dropped @uw.edu to get id = " + remoteUser);
              ////return loginChooserMV(session, request, response);  // return to login chooser
              // until we can report some misuse
              return emptyMV("invalid social login");
           }

           double dbl = Math.random();
           long modtime = new Date().getTime();  // milliseconds
           log.debug("login: ck = ...;" + remoteUser + ";" + dbl + ";" + methodKey + ";" + modtime/1000);
           String enc = RPCrypt.encode(Double.toString(modtime)+ ";" + remoteUser + ";" + dbl + ";" + methodKey + ";" + modtime/1000);
           log.debug("login: enc = " + enc);
           Cookie c = new Cookie(loginCookie, enc);
           c.setSecure(true);
           c.setPath("/");
           response.addCookie(c);
           try {
              String rp = request.getPathInfo();
              int sp = rp.indexOf("/", 2);
              log.debug("in path = " +  rp);
              String red = browserRootPath + request.getServletPath();
              if (sp>1) red = red + rp.substring(sp);
              if (request.getQueryString()!=null)  red = red + "?" + request.getQueryString();
              log.debug("logon ok, return to " + red);
              response.sendRedirect(red);
           } catch (IOException e) {
              log.error("redirect: " + e);
              return emptyMV("redirect error");
           }
       } else {
           // send login failed message
           ModelAndView mv = new ModelAndView("browser/nologin");
           mv.addObject("root", browserRootPath);
           mv.addObject("vers", request.getServletPath());
           mv.addObject("pageTitle", "login failed");
           mv.addObject("myEntityId", myEntityId);
           return mv;
       }
       return emptyMV();
    }

    @RequestMapping(value="/login/**", method=RequestMethod.GET)
    public ModelAndView basicLoginPage(HttpServletRequest request, HttpServletResponse response) {
        return loginPage(request, response, 1);
    }

    @RequestMapping(value="/securelogin/**", method=RequestMethod.GET)
    public ModelAndView secureLoginPage(HttpServletRequest request, HttpServletResponse response) {
        return loginPage(request, response, 2);
    }

    @RequestMapping(value="/googlelogin/**", method=RequestMethod.GET)
    public ModelAndView googleLoginPage(HttpServletRequest request, HttpServletResponse response) {
        log.debug("hitting /googlelogin/** endpoint now");
        return loginPage(request, response, 0);
    }

    @RequestMapping(value="/incommonlogin/**", method=RequestMethod.GET)
    public ModelAndView incommonLoginPage(HttpServletRequest request, HttpServletResponse response) {
        return loginPage(request, response, 1);
    }

    /*
     * Process logoutt page
     * Clear cookies, redirect to shib logout
     */

    @RequestMapping(value="/logout/**", method=RequestMethod.GET)
    public ModelAndView logoutPage(HttpServletRequest request, HttpServletResponse response) {
        // clear cookies
        Cookie[] cookies = request.getCookies();
        if (cookies!=null) {
          for (int i=0; i<cookies.length; i++) {
            String ckName = cookies[i].getName();
            if (ckName.equals(loginCookie) || ckName.equals(roleCookie) || ckName.startsWith("_shib")) {
               log.debug("cookie to clear " + ckName);
               Cookie c = new Cookie(ckName, "void");
               c.setSecure(true);
               c.setPath("/");
               c.setMaxAge(0);
               response.addCookie(c);
            }
          }
        }
/**
        try {
           log.debug("redirect to: " +  logoutUrl);
           response.sendRedirect(logoutUrl);
        } catch (IOException e) {
           log.error("redirect: " + e);
        }
        return emptyMV("configuration error");
 **/
       String view = "browser";
       Device currentDevice = DeviceUtils.getCurrentDevice(request);
       if (currentDevice!=null && currentDevice.isMobile()) view = "mobile";
       ModelAndView mv = new ModelAndView(view + "/chooser");
       mv.addObject("root", browserRootPath);
       mv.addObject("vers", request.getServletPath());
       mv.addObject("pagetype", "browser/loggedout");
       mv.addObject("pathextra", "");
       mv.addObject("uwloginpath", standardLoginPath);
       mv.addObject("googleloginpath", googleLoginPath);
       mv.addObject("incommonloginpath", incommonLoginPath);
       return (mv);
    }

    // show main page
    @RequestMapping(value="/", method=RequestMethod.GET)
    public ModelAndView homePage(HttpServletRequest request, HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);
        log.info("/ view");
        log.info(".. path=" + request.getPathInfo());

        session.pageTitle = "SP registry home";
        session.pageType = "home";

        ModelAndView mv = basicModelAndView(session);
        mv.addObject("isAdmin", session.isAdmin);
        mv.addObject("isProxy", session.isProxy);

        return (mv);
    }

    // home pages (if 'v1' alone)
    @RequestMapping(value="/v1", method=RequestMethod.GET)
    public ModelAndView homePageV1(HttpServletRequest request, HttpServletResponse response) {
        log.info("v1 view");
        return homePage(request, response);
    }



    // show rp list page
    @RequestMapping(value="/rps", method=RequestMethod.GET)
    public ModelAndView getRelyingParties(@RequestParam(value="selectrp", required=false) String selRp,
            @RequestParam(value="selecttype", required=false) String selType,
            HttpServletRequest request, HttpServletResponse response) {


        RPSession session = processRequestInfo(request, response, false);
        if (session==null) {
           response.setStatus(418);
           return (emptyMV());
        }
        if (session.mv!=null) return (session.mv);

      
        /** The optional args are actually ignored 
        List<RelyingPartyEntry> relyingPartyIds = null;
        if (selType!=null && selType.equalsIgnoreCase("all")) selType = null;
        relyingPartyIds = rpManager.searchRelyingPartyIds(selRp, selType);
         **/
        List<RelyingParty> relyingPartyIds = rpManager.getRelyingParties();
        log.info("found " + relyingPartyIds.size() + " rps" );
 
        ModelAndView mv = basicModelAndView(session, "json", "rps");
        // mv.addObject("selectrp", selRp==null?"":selRp);
        // mv.addObject("selecttype", selType==null?"all":selType);
        mv.addObject("relyingParties", relyingPartyIds);

        return (mv);
    }

    // specific party
    @RequestMapping(value="/rp", method=RequestMethod.GET)
    public ModelAndView getRelyingParty(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=true) String mdid,
            @RequestParam(value="view", required=false) String view,
            @RequestParam(value="dns", required=false) String dns,
            @RequestParam(value="role", required=false) String role,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);

        session.pageType = "rp";
        session.pageTitle = "Service provider";

        RelyingParty rp = null;
        RelyingParty rrp = null;
        List<RelyingParty> relyingPartyHistory = null;
        List<Proxy> proxyHistory = null;

        boolean canEdit = false;
 
        String errmsg = null;

        ModelAndView mv = basicModelAndView(session, "browser", "rp");

        try {
           rp = rpManager.getRelyingPartyById(id, mdid);
        } catch (RelyingPartyException e) {
           return emptyMV("not found");
        }
        session.pageTitle = rp.getEntityId();
        try {
            if (userCanEdit(session, id)) {
                log.debug("user can edit");
                canEdit = true;
            }
        } catch (DNSVerifyException e) {
           mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        }
        try {
            relyingPartyHistory = rpManager.getRelyingPartyHistoryById(id);
        } catch (RelyingPartyException e){
            log.debug("Exception occurred getting metadata history");
        }
        try {
            proxyHistory = proxyManager.getProxyHistory(id);
        } catch (ProxyException e){
            log.debug("Exception occurred getting proxy history");
        }


        log.info("returning rp id=" + id );
        List<FilterPolicyGroup> filterPolicyGroups = filterPolicyManager.getFilterPolicyGroups();
        List<Attribute> attributes = filterPolicyManager.getAttributes(rp);

        Proxy proxy = proxyManager.getProxy(id);

        mv.addObject("canEdit", canEdit);
        mv.addObject("relyingParty", rp);
        mv.addObject("filterPolicyGroups", filterPolicyGroups);
        mv.addObject("filterPolicyManager", filterPolicyManager);
        mv.addObject("attributes", attributes);
        mv.addObject("relyingPartyId", id);
        mv.addObject("relyingPartyHistory", relyingPartyHistory);
        mv.addObject("proxyHistory", proxyHistory);
        mv.addObject("proxy", proxy);
        mv.addObject("isAdmin", session.isAdmin);
        mv.addObject("isProxy", session.isProxy);
        mv.addObject("dateFormatter", new SimpleDateFormat("yy/MM/dd"));
        return (mv); 
    }

    // rp's metadata (API endpoint)
    @RequestMapping(value="/ws/metadata", method=RequestMethod.GET)
    public ModelAndView getRelyingParty(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=true) String mdid,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);

        session.pageType = "metadata";
        session.pageTitle = "Service provider";

        RelyingParty rp = null;

        String errmsg = null;

        ModelAndView mv = basicModelAndView(session, "xml", "metadata");

        try {
           rp = rpManager.getRelyingPartyById(id, mdid);
        } catch (RelyingPartyException e) {
           response.setStatus(404);
           return emptyMV("not found");
        }
        log.info("returning metadata id=" + id );
        try {
           StringWriter writer = new StringWriter();
           BufferedWriter xout = new BufferedWriter(writer);
           rp.writeXml(xout);
           xout.close();
           mv.addObject("metadata", writer.toString());
        } catch (IOException e) {
           log.error("string writer errro: " + e);
           response.setStatus(500);
           return emptyMV("internal error");
        }

        return (mv); 
    }


    // new rp
    @RequestMapping(value="/new", method=RequestMethod.GET)
    public ModelAndView getRelyingPartyNew(@RequestParam(value="rpid", required=true) String rpid,
            @RequestParam(value="mdid", required=false) String mdid,
            @RequestParam(value="view", required=false) String view,
            @RequestParam(value="role", required=false) String role,
            @RequestParam(value="lookup", required=false) String lookup, // enter manual data for this entity
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);

        session.pageType = "rp";
        session.pageTitle = "New service provider";
        if (lookup!=null) lookup = java.net.URLDecoder.decode(lookup);
        
        String dns = dnsFromEntityId(rpid);

        if (dns.length()==0) return (emptyMV());

        RelyingParty rp = null;
        boolean canEdit = true;
        String errmsg = null;
        ModelAndView mv = basicModelAndView(session, "browser", "rp");

        // check access
        try {
            if (userCanEdit(session, dns)) {
                log.debug("user owns dns");
            } else {
                // response.setStatus(200);  // 403
                return emptyMV("No permission for " + rpid);
            }
        } catch (DNSVerifyException e) {
           // mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           // response.setStatus(500);
           return emptyMV("Could not verify ownership:\n" + e.getCause());
        }

        // check that it doesn't already exist
        try {
           rpManager.getRelyingPartyById(rpid, "UW");
           log.debug("wants new, but already exists");
           return emptyMV("Entity " + rpid + " already exists in the UW federation metadata");
        } catch (RelyingPartyException e) {
           log.debug("really new");
        }

        mv.addObject("newEntity", true);

        if (lookup!=null) {
           mv.addObject("newByLookup", true);
           try {
              if (lookup.equals("sp")) lookup = "https://" + hostPortFromEntityId(rpid) +  "/Shibboleth.sso/Metadata";
              log.debug("SP by lookup:  " + lookup);
              rp = rpManager.genRelyingPartyByLookup(lookup);
              if (rp!=null) log.debug("rp: " + rp.getEntityId());
              try {
                  RelyingParty orp = rpManager.getRelyingPartyById(rp.getEntityId(), "UW");
                  if (orp!=null) rp = orp;
              }
              catch (RelyingPartyException e){
                  log.debug("rp doesn't already exist in our metadata");
              }

              if(!hostPortFromEntityId(rpid).equals(hostPortFromEntityId(rp.getEntityId()))){
                  log.info(String.format("requested dns '%s' not equal to fetched entityId '%s'",
                                         hostPortFromEntityId(rpid), rp.getEntityId()));
                  return emptyMV(
                   "The entityID you supplied appears to be on a host already registered with a different entityID. " +
                   "Shibboleth can support multiple entityIDs on one SP, but in most cases that isn't the best approach. " + 
                   "If you actually do need to register additional entityIDs for an existing SP, " + 
                   "you'll need to use the manual registration process.");
              }

              mv.addObject("relyingParty", rp);
              mv.addObject("relyingPartyId", rp.getEntityId());
              session.pageTitle = rp.getEntityId();
           } catch (RelyingPartyException e) {
              mv.addObject("rpnotfound", true);
              log.debug("metadata not found for " + dns);
              log.debug(e.getMessage());
           }
        }
        if (rp==null) {
           if (lookup!=null) return emptyMV("URL did not respond with metadata");
           rp = rpManager.genRelyingPartyByName(rpid, dns);
           mv.addObject("relyingParty", rp);
           mv.addObject("relyingPartyId", rpid);
           session.pageTitle = rpid;
        }

        List<FilterPolicyGroup> filterPolicyGroups = filterPolicyManager.getFilterPolicyGroups();
        List<Attribute> attributes = filterPolicyManager.getAttributes(rp);
        mv.addObject("filterPolicyGroups", filterPolicyGroups);
        mv.addObject("filterPolicyManager", filterPolicyManager);
        return (mv); 
    }

    // update an rp metadata
    @RequestMapping(value="/rp", method=RequestMethod.PUT)
    public ModelAndView putRelyingParty(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=true) String mdid,
            @RequestParam(value="role", required=false) String role,
            @RequestParam(value="xsrf", required=false) String paramXsrf,
            InputStream in,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());

        log.info("PUT update for: " + id);
        int status = 200;

        if (session.isBrowser && !(paramXsrf!=null && paramXsrf.equals(session.xsrfCode))) {
            log.info("got invalid xsrf=" + paramXsrf + ", expected+" + session.xsrfCode);
            return emptyMV("invalid session (xsrf)");
        }

        ModelAndView mv = emptyMV("OK dokey");

        try {
            if (!userCanEdit(session, id)) {
                status = 401;
                mv.addObject("alert", "You are not an owner of that entity.");
                response.setStatus(status);
                return mv;
            }
        } catch (DNSVerifyException e) {
           mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        }

        // if new, check for URL entityid
        try {
           rpManager.getRelyingPartyById(id, mdid);
        } catch (RelyingPartyException e) {
           if (!isProperEntityId(session, id)) {
               log.info("improper entity id");
               status = 400;
               mv.addObject("alert", "The posted entityid was not a URL\n");
               response.setStatus(status);
               return mv;
           }
        }

        RelyingParty relyingParty = null;
        try {
            Document doc = null;
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            //In parser we trust.  (no server side input validation on XML payload)
            doc = builder.parse(in);
            relyingParty = new RelyingParty(doc.getDocumentElement(), mdid, rpManager.isMetadataEditable(mdid));
        } catch (Exception e) {
            log.info("parse error: " + e);
            status = 400;
            mv.addObject("alert", e.getMessage());
            response.setStatus(status);
            return mv;
        }

        if (!relyingParty.getEntityId().equals(id)) {
            log.info("entity id mismatch");
            status = 400;
            mv.addObject("alert", "The posted entityid did not match the request\n");
            response.setStatus(status);
            return mv;
        }

        try {
            status = rpManager.updateRelyingParty(relyingParty, mdid);
        } catch (RelyingPartyException e) {
            status = 400;
            mv.addObject("alert", e.getMessage());
            response.setStatus(status);
            return mv;
        }

        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(mailTo);
        String act = "updated";
        if (status==201) act = "created";
        else if (status>=400) act = "attempted edit of";
        msg.setSubject("Service provider metadata " + act + " by " + session.remoteUser);
        msg.setText( "User '" + session.remoteUser + "' " + act + " metadata for '" + id + "'.\nRequest status: " + status +
               "\n\nThis message is advisory.  No response is indicated.");
        try{
            this.mailSender.send(msg);
        } catch(MailException ex) {
            log.error("sending mail: " + ex.getMessage());            
        }

        response.setStatus(status);
        return mv;
    }


    // API update an rp metadata
    @RequestMapping(value="/ws/metadata", method=RequestMethod.PUT)
    public ModelAndView putRelyingParty(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=true) String mdid,
            InputStream in,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());
        log.info("API PUT update for: " + id);
        int status = 403;

        ModelAndView mv = basicModelAndView(session, "xml", "empty");

        String dns = dnsFromEntityId(id);
        for  (String alt : (List<String>) session.altNames) {
           if (dns.equals(alt) || (alt.startsWith("*.") && dns.endsWith(alt.substring(1))) ) {
              log.info("dns match found for " + dns);
              status = 200;
              break;
           }
        }
        if (status==403) {
           mv.addObject("alert", "You are not an owner of that entity.");
           response.setStatus(status);
           return mv;
        }
        if (!rpManager.isMetadataEditable(mdid)) {
           status = 400;
           mv.addObject("alert", "The metadata was not found or is not editable");
           response.setStatus(status);
           return mv;
        }

        // if new, check for URL entityid
        try {
           rpManager.getRelyingPartyById(id, mdid);
        } catch (RelyingPartyException e) {
           if (!isProperEntityId(session, id)) {
               log.info("improper entity id");
               status = 400;
               mv.addObject("alert", "The posted entityid was not a URL\n");
               response.setStatus(status);
               return mv;
           }
        }

        RelyingParty rp = null;
        try {
            Document doc = null;
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            doc = builder.parse (in);
            rp = new RelyingParty(doc.getDocumentElement(), mdid, rpManager.isMetadataEditable(mdid));
        } catch (Exception e) {
            log.info("parse error: " + e);
            status = 400;
            mv.addObject("alert", e.getMessage());
            response.setStatus(status);
            return mv;
        }
        if(rp.getEntityId().equals(id)){
           try {
               rpManager.updateRelyingParty(rp, mdid);
           } catch (RelyingPartyException e) {
              status = 400;
              mv.addObject("alert", "Update of the metadata failed:\n" + e.getMessage());
           }
        }
        else{
            mv.addObject("alert", String.format("Id %s doesn't match %s",
                    rp.getEntityId(), id));
        }

        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(mailTo);
        String act = "updated";
        if (status==201) act = "created";
        else if (status>=400) act = "attempted edit of";
        msg.setSubject("Service provider metadata " + act + " by " + session.remoteUser);
        msg.setText( "User '" + session.remoteUser + "' " + act + " metadata for '" + id + "'.\nRequest status: " + status +
               "\n\nThis message is advisory.  No response is indicated.");
        try{
            this.mailSender.send(msg);
        } catch(MailException ex) {
            log.error("sending mail: " + ex.getMessage());            
        }

        response.setStatus(status);
        return mv;
    }

    // delete an rp
    @RequestMapping(value="/rp", method=RequestMethod.DELETE)
    public ModelAndView deleteRelyingParty(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=true) String mdid,
            @RequestParam(value="role", required=false) String role,
            @RequestParam(value="xsrf", required=false) String paramXsrf,
            InputStream in,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());

        log.info("DELETE for: " + id);
        int status = 200;

       if (session.isBrowser && !(paramXsrf!=null && paramXsrf.equals(session.xsrfCode))) {
           log.info("got invalid xsrf=" + paramXsrf + ", expected+" + session.xsrfCode);
           return emptyMV("invalid session (xsrf)");
       }

        ModelAndView mv = emptyMV("OK dokey delete rp");

        try {
            if (!userCanEdit(session, id)) {
                status = 401;
                mv.addObject("alert", "You are not the owner.");
            } else {
                status = proxyManager.removeProxy(id);
                status = filterPolicyManager.removeEditableRelyingParty(id);
                status = rpManager.removeRelyingParty(id, mdid);
            }
        } catch (FilterPolicyException e) {
           mv.addObject("alert", "delete of filter policy failed:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        } catch (DNSVerifyException e) {
           mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        }  catch (ProxyException e) {
            mv.addObject("alert", "Proxy (gateway) error:\n" + e.getCause());
            response.setStatus(500);
            return mv;
        }
        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(mailTo);
        msg.setText( "User '" + session.remoteUser + "' deleted metadata for '" + id + "'.\nRequest status: " + status);
        try{
            this.mailSender.send(msg);
        } catch(MailException ex) {
            log.error("sending mail: " + ex.getMessage());            
        }

        response.setStatus(status);
        return mv;
    }


    // rp attributes 
    @RequestMapping(value="/rp/attr", method=RequestMethod.GET)
    public ModelAndView getRelyingPartyAttributes(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="mdid", required=false) String mdid,
            @RequestParam(value="view", required=false) String view,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);

        session.pageType = "relying-party-attr";
        session.pageTitle = "Service provider";
        if (view==null) view = "";
        else if (view.equals("edit")) session.pageType = "relying-party-attr-edit";

        RelyingParty rp = null;
        String errmsg = null;

        try {
           rp = rpManager.getRelyingPartyById(id, mdid);
        } catch (RelyingPartyException e) {
           return emptyMV("SP not found");
        }
        session.pageTitle = rp.getEntityId();

        List<FilterPolicyGroup> filterPolicyGroups = filterPolicyManager.getFilterPolicyGroups();
        List<Attribute> attributes = filterPolicyManager.getAttributes();

        ModelAndView mv = basicModelAndView(session);

        log.info("returning attrs for id=" + id );

        mv.addObject("relyingPartyId", id);
        mv.addObject("relyingParty", rp);
        mv.addObject("filterPolicyGroups", filterPolicyGroups);
        mv.addObject("filterPolicyManager", filterPolicyManager);
        mv.addObject("remoteUser", session.remoteUser);
        try {
            if (userCanEdit(session, id)) mv.addObject("domainOwner",true);
        } catch (DNSVerifyException e) {
           mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        }
        mv.addObject("attributes", attributes);
        return (mv); 
    }

    // update an rp's attrs
    @RequestMapping(value="/rp/attr", method=RequestMethod.PUT)
    public ModelAndView putRelyingPartyAttributes(@RequestParam(value="id", required=true) String id,
            @RequestParam(value="policyId", required=true) String policyId,
            @RequestParam(value="xsrf", required=false) String paramXsrf,
            InputStream in,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());
        log.info("PUT update attrs for " + id + " in " + policyId);
        int status = 200;

/**
       if (session.isBrowser && !(paramXsrf!=null && paramXsrf.equals(session.xsrfCode))) {
           log.info("got invalid xsrf=" + paramXsrf + ", expected+" + session.xsrfCode);
           return emptyMV("invalid session (xsrf)");
       }
 **/

        ModelAndView mv = emptyMV("OK dokey");

        if (!session.isAdmin) {
            status = 401;
            mv.addObject("alert", "You are not permitted to update attriubtes.");
            response.setStatus(status);
            return mv;
        }

        Document doc = null;
        try {
           DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = builderFactory.newDocumentBuilder();
           doc = builder.parse (in);
        } catch (Exception e) {
           log.info("parse error: " + e);
           status = 400;
           mv.addObject("alert", "The posted document was not valid:\n" + e.getMessage());
        }
        if (doc!=null) {
           try {
              filterPolicyManager.updateRelyingParty(policyId, doc);
              status = 200;
           } catch (FilterPolicyException e) {
              status = 400;
              mv.addObject("alert", "Update of the entity failed:" + e.getMessage());
           }
        }

        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(mailTo);
        String act = "updated";
        if (status==201) act = "created";
        msg.setSubject("Service provider attributes " + act + " by " + session.remoteUser);
        msg.setText( "User '" + session.remoteUser + "' " + act + " attributes for '" + id + "'.\nRequest status: " + status + "\n");
        try{
            this.mailSender.send(msg);
        } catch(MailException ex) {
            log.error("sending mail: " + ex.getMessage());            
        }

        response.setStatus(status);
        return mv;
    }


    // request for attributes
    @RequestMapping(value="/rp/attrReq", method=RequestMethod.PUT)
    public ModelAndView putRelyingPartyAttrReq(@RequestParam(value="id", required=true) String id,
            InputStream in,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());
        log.info("PUT request for: " + id);
        int status = 200;

        ModelAndView mv = emptyMV("OK dokey");

        try {
            if (!userCanEdit(session, id)) {
                status = 401;
                mv.addObject("alert", "You are not an owner of that entity.");
            }
        } catch (DNSVerifyException e) {
           mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
           response.setStatus(500);
           return mv;
        }

        Document doc = null;
        try {
           DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = builderFactory.newDocumentBuilder();
           doc = builder.parse (in);
        } catch (Exception e) {
           log.info("parse error: " + e.getMessage());
           status = 400;
           mv.addObject("alert", "The posted document was not valid:\n" + e.getMessage());
        }
        if (doc!=null) {
           StringBuffer txt = new StringBuffer("[ Assign to Identity and Access Management. ]\n\nEntity Id: " + id + "\n");
           txt.append("User:      " + session.remoteUser + "\n\nRequesting:\n");
           List<Element> attrs = XMLHelper.getElementsByName(doc.getDocumentElement(), "Add");
           log.debug(attrs.size() + " adds");
           for (int i=0; i<attrs.size(); i++) txt.append("  Add new attribute: " + attrs.get(i).getAttribute("id") + "\n\n");
           attrs = XMLHelper.getElementsByName(doc.getDocumentElement(), "Drop");
           log.debug(attrs.size() + " drops");
           for (int i=0; i<attrs.size(); i++) txt.append("  Drop existing attribute: " + attrs.get(i).getAttribute("id") + "\n\n");
           Element mele = XMLHelper.getElementByName(doc.getDocumentElement(), "Comments");
           if (mele!=null) txt.append("\nComment:\n\n" + mele.getTextContent() + "\n\n"); 
           txt.append("Quick link:\n\n   " + spRegistryUrl + "#a" + id + "\n");

           SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
   /* production to RT system */
           msg.setTo(requestMailTo);
           msg.setSubject("IdP attribute request for " + id);
           if (session.remoteUser.indexOf("@")>0) msg.setFrom(session.remoteUser);
           else msg.setFrom(session.remoteUser + "@uw.edu");
           msg.setText(txt.toString());
           try{
               this.mailSender.send(msg);
           } catch(MailException ex) {
               log.error("sending mail: " + ex.getMessage());            
               status = 500;
               mv.addObject("alert", "Could not complete attribute request (sending mail failed).");
           }

        }
        response.setStatus(status);
        return mv;
    }

    // all attributes 
    @RequestMapping(value="/attr", method=RequestMethod.GET)
    public ModelAndView getAttributes(@RequestParam(value="id", required=false) String id,
            @RequestParam(value="mdid", required=false) String mdid,
            @RequestParam(value="view", required=false) String view,
            HttpServletRequest request,
            HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response);
        if (session==null) return (emptyMV());
        if (session.mv!=null) return (session.mv);

        session.pageType = "attributes";
        session.pageTitle = "Attributes";

        // if (view!=null && view.equals("edit")) session.pageType = "relying-party-attr-edit";
        // RelyingParty rp = null;

        List<Attribute> attributes = filterPolicyManager.getAttributes();

        ModelAndView mv = basicModelAndView(session);

        log.info("returning attrs");

        mv.addObject("attributes", attributes);
        mv.addObject("filterPolicyManager", filterPolicyManager);
        mv.addObject("remoteUser", session.remoteUser);
        return (mv); 
    }


    // update an rp's proxy 
    @RequestMapping(value="/rp/proxy", method=RequestMethod.PUT)
    public ModelAndView putRelyingPartyAttributesZ(@RequestParam(value="id", required=true) String id,
                                                   @RequestParam(value="role", required=false) String role,
                                                   @RequestParam(value="xsrf", required=false) String paramXsrf,
                                                   @RequestParam(value="social_login_flag", required=true) boolean socialActive,
                                                   InputStream in,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {

        RPSession session = processRequestInfo(request, response, false);
        if (session==null) return (emptyMV());

        log.info("PUT update proxy for " + id);
        int status = 200;

        if (session.isBrowser && !(paramXsrf!=null && paramXsrf.equals(session.xsrfCode))) {
            log.info("got invalid xsrf=" + paramXsrf + ", expected+" + session.xsrfCode);
            return emptyMV("invalid session (xsrf)");
        }

        ModelAndView mv = emptyMV("OK dokey");
        try {
            if (!userCanEdit(session, id)) {
                status = 401;
                mv.addObject("alert", "You are not an owner of that entity.");
                response.setStatus(status);
                return mv;
            }
        } catch (DNSVerifyException e) {
            mv.addObject("alert", "Could not verify ownership:\n" + e.getCause());
            response.setStatus(500);
            return mv;
        }


        try {
            Proxy newproxy = new Proxy();
            newproxy.setEntityId(id);
            newproxy.setSocialActive(socialActive);
            proxyManager.updateProxy(newproxy);
            status = 200;
        } catch (ProxyException e) {
            status = 400;
            mv.addObject("alert", "Update of the entity failed:" + e.getMessage());
        }


        SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);
        msg.setTo(mailTo);
        String act = "updated";
        if (status==201) act = "created";
        msg.setSubject("Service provider proxy info " + act + " by " + session.remoteUser);
        msg.setText( "User '" + session.remoteUser + "' " + act + " proxy info '" + id + "'.\nRequest status: " + status + "\n");
        try{
            this.mailSender.send(msg);
        } catch(MailException ex) {
            log.error("sending mail: " + ex.getMessage());
        }

        response.setStatus(status);
        return mv;
    }

    public void setRelyingPartyManager(RelyingPartyManager m) {
        rpManager = m;
    }

    public void setFilterPolicyManager(FilterPolicyManager m) {
        filterPolicyManager = m;
    }

    public void setProxyManager(ProxyManager m) {
        proxyManager = m;
    }


    /* utility */
    private boolean userCanEdit(RPSession session, String entityId)
        throws DNSVerifyException {
        return session.adminRole || dnsVerifier.isOwner(entityId, session.remoteUser, null);
    }

    private boolean isProperEntityId(RPSession session, String id) {
        return (session.adminRole || id.startsWith("http://") || id.startsWith("https://"));
    }

    private long getLongHeader(HttpServletRequest request, String name) {
       try {
          String hdr = request.getHeader(name);
          if (hdr==null) return 0;
          if (hdr.equals("*")) return (-1);
          return Long.parseLong(hdr);
       } catch (NumberFormatException e) {
          return 0;
       }
    }
    private String hostPortFromEntityId(String entityId){
        entityId = entityId.replaceFirst("^https?://", "");
        entityId = entityId.replaceFirst("/.*$", "");
        return entityId;
    }
    private String dnsFromEntityId(String entityid) {
       String dns = entityid;
       if (dns.startsWith("http://")) dns = dns.substring(7);
       if (dns.startsWith("https://")) dns = dns.substring(8);
       int i = dns.indexOf("/");
       if (i>0) dns = dns.substring(0,i);
       i = dns.indexOf(":");
       if (i>0) dns = dns.substring(0,i);
       return dns;
    }
    private String cleanString(String in) {
       if (in==null) return null;
       return in.replaceAll("&", "").replaceAll("<", "").replaceAll(">", "");
    }
    public void setCertRootPath(String path) {
        certRootPath = path;
    }
    public void setBrowserRootPath(String path) {
        browserRootPath = path;
    }

    public void setLoginCookie(String v) {
        loginCookie = v;
    }
    public void setRoleCookie(String v) {
        roleCookie = v;
    }

    public void setLogoutUrl(String v) {
        logoutUrl = v;
    }

    public void setCryptKey(String v) {
        cryptKey = v;
    }

    public void setMailTo(String v) {
        log.debug("mailTo = " + v);
        mailTo = v;
    }
    public void setRequestMailTo(String v) {
        requestMailTo = v;
    }
    public void setAdminGroupName(String v) {
        log.debug("admin group = " + v);
        adminGroupName = v;
    }
    public void setStandardLoginSec(long v) {
        standardLoginSec = v;
    }
    public void setSecureLoginSec(long v) {
        secureLoginSec = v;
    }

    public void setStandardLoginPath(String v) {
        standardLoginPath = v;
    }
    public void setSecureLoginPath(String v) {
        secureLoginPath = v;
    }
    public void setGoogleIdentityProvider(String v) {
        googleIdentityProvider = v;
    }

    public void setMyEntityId(String v) {
        myEntityId = v;
    }
    public void setEppnName(String v) {
        eppnName = v;
    }
    public void setSpRegistryUrl(String v) {
        spRegistryUrl = v;
    }

    public static RelyingPartyManager getRelyingPartyManager() {
       return rpManager;
    }


    /* See if extra login suggested.
     */
/**
    private boolean needMoreAuthn(GwsGroup group, GwsSession session, HttpServletResponse response) {
       if (group.getSecurityLevel()>1 && !session.authn2) {
           log.debug("update needs 2-factor");
           if (session.isBrowser) response.setStatus(402);
           else response.setStatus(401);
           return true;
       }
       return false;
    }
 **/

    public void init() {
       log.info("RelyingPartyController init");
       RPCrypt.init(cryptKey);
       adminGroup = groupManager.getGroup(adminGroupName);
    }

    /* refresh cache groups  */

    @RequestMapping(value="/ws/refresh", method=RequestMethod.GET)
    public ModelAndView getSet(
               HttpServletRequest request,
               HttpServletResponse response) {

       RPSession session = processRequestInfo(request, response);
       response.setStatus(200);
       log.info("refreshing cache groups");
       adminGroup = groupManager.getGroup(adminGroupName);
       return emptyMV();
    }

    /* status  */

    @RequestMapping(value="/status", method=RequestMethod.GET)
    public ModelAndView gettatus(
               HttpServletRequest request,
               HttpServletResponse response) {

       RPSession session = processRequestInfo(request, response);
       response.setStatus(200);
       log.info("status request");
       adminGroup = groupManager.getGroup(adminGroupName);
       return emptyMV();
    }

    // diagnostic
    @RequestMapping(value="/**", method=RequestMethod.GET)
    public ModelAndView homePageStar(HttpServletRequest request, HttpServletResponse response) {
        log.info("Star view");
        return homePage(request, response);
    }


}
