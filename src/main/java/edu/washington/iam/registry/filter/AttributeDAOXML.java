package edu.washington.iam.registry.filter;

import edu.washington.iam.registry.exception.AttributeException;
import edu.washington.iam.registry.exception.AttributeNotFoundException;
import edu.washington.iam.tools.XMLHelper;
import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AttributeDAOXML implements AttributeDAO {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ReentrantReadWriteLock locker = new ReentrantReadWriteLock();

  private List<Attribute> attributes;
  private String attributeUri;
  private String attributeSourceName;

  public void setAttributeUri(String v) {
    attributeUri = v;
    attributeSourceName = attributeUri.replaceFirst("file:", "");
  }

  private int attributeRefresh = 0; // seconds

  public void setAttributeRefresh(int i) {
    attributeRefresh = i;
  }

  Thread reloader = null;
  private long modifyTime = 0; // for the attrs

  @Override
  public List<Attribute> getAttributes() {
    return attributes;
  }

  // find an attribute
  @Override
  public Attribute getAttribute(String id) throws AttributeNotFoundException {
    for (int i = 0; i < attributes.size(); i++) {
      if (attributes.get(i).getId().equals(id)) return attributes.get(i);
    }
    throw new AttributeNotFoundException();
  }

  private void loadAttributes() {
    attributes = new Vector();
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(false);
    Document doc;

    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      doc = builder.parse(attributeUri);
    } catch (Exception e) {
      log.error("parse issue: " + e);
      return;
    }
    // update the timestamp
    File f = new File(attributeSourceName);
    modifyTime = f.lastModified();
    log.debug("attr load " + f.getName() + ": time = " + modifyTime);

    List<Element> list = XMLHelper.getElementsByName(doc.getDocumentElement(), "Attribute");
    log.info("found " + list.size());

    for (int i = 0; i < list.size(); i++) {
      Element fpe = list.get(i);
      try {
        attributes.add(new Attribute(fpe));
      } catch (AttributeException e) {
        log.error("load of element failed: " + e);
      }
    }
  }

  // attribute reloader
  class AttributeReloader extends Thread {

    public void run() {
      log.debug("attr reloader running: interval = " + attributeRefresh);

      while (true) {
        log.debug("reloader checking...");
        File f = new File(attributeSourceName);
        if (f.lastModified() > modifyTime) {
          // reload the attributes
          log.debug("reload starting for " + attributeUri);
          locker.writeLock().lock();
          try {
            loadAttributes();
          } catch (Exception e) {
            log.error("reload errro: " + e);
          }
          locker.writeLock().unlock();
          log.debug("reload completed, time now " + modifyTime);
        }
        try {
          if (isInterrupted()) {
            log.info("interrupted during processing");
            break;
          }
          Thread.sleep(attributeRefresh * 1000);
        } catch (InterruptedException e) {
          log.info("sleep interrupted");
          break;
        }
      }
    }
  }

  @PostConstruct
  public void init() {
    loadAttributes();

    // start attribute list refresher
    if (attributeRefresh > 0) {
      reloader = new Thread(new AttributeReloader());
      reloader.start();
    }
  }

  @PreDestroy
  public void cleanup() {
    if (reloader != null) reloader.interrupt();
  }
}
