package org.openlmis.web.controller;

import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.BaseJsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.ict4h.atomfeed.server.service.EventFeedService;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.openlmis.core.exception.DataException;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class VendorEventFeedServiceHelper{

  public static String getRecentFeed(EventFeedService eventFeedService, String requestURL, Logger logger, String vendor, String category){
    try {
      Feed feed = eventFeedService.getRecentFeed(new URI(requestURL));
      mapFeedBasedOnVendorAndCategory(feed, vendor, category);
      return new WireFeedOutput().outputString(feed);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad URI", e);
    } catch (Exception e) {
      logger.error("error occurred while getting recent feedgenerator", e);
      throw new RuntimeException("Unexpected error", e); //TODO
    }
  }

  private static void mapFeedBasedOnVendorAndCategory(Feed feed, String vendor, String category) throws IOException {
    if (vendor == null) {
      return;
    }

    String template = "vendorMapping_"+ vendor + "_" + category +".xml";

    Map<String, String> map = createTemplateMap(template);

    List<Entry> feedEntries = feed.getEntries();
    for (Entry entry : feedEntries) {
      List<Content> contentList = entry.getContents();
      for( Content content : contentList){
        String value = content.getValue();
        JsonNode rootNode = convertToTemplate(map, value);
        content.setValue("<![CDATA["+rootNode.toString()+"]]>");
      }
    }

  }

  private static JsonNode convertToTemplate(Map<String, String> map, String value) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree("{\"id\":1,\"code\":\"F10\",\"name\":\"Village Dispensary1\",\"typeId\":1,\"description\":\"IT department\",\"mainPhone\":\"9876234981\",\"fax\":\"fax\",\"address1\":\"A\",\"address2\":\"B\",\"geographicZoneID\":4,\"catchmentPopulation\":333,\"latitude\":22.1,\"longitude\":1.2,\"altitude\":3.3,\"operatedBy\":\"NGO\",\"coldStorageGrossCapacity\":9.9,\"coldStorageNetCapacity\":6.6,\"suppliesOthers\":true,\"hasElectricity\":true,\"hasElectronicSCC\":true,\"hasElectronicDAR\":true,\"active\":true,\"goLiveDate\":1352572200000,\"goDownDate\":1352572200000,\"satelliteFacility\":true,\"satelliteParentID\":null,\"comments\":\"fc\",\"doNotDisplay\":false,\"modifiedDate\":null,\"online\":true,\"vendorSystem\":null,\"gln\":\"G7645\",\"sdp\":true}");
    Iterator<Map.Entry<String,JsonNode>> iterator =  rootNode.getFields();
    ObjectNode returnedNode = new ObjectNode(JsonNodeFactory.instance);
    while(iterator.hasNext()){
      Map.Entry<String, JsonNode> jsonNode = iterator.next();
      String fieldName = jsonNode.getKey();
      String mappedName = map.get(fieldName);
      String textValue = jsonNode.getValue().asText();
      if (mappedName != null) {
        returnedNode.put(mappedName, textValue);
      } else {
        returnedNode.put(fieldName, textValue);
      }
    }
    return returnedNode;
  }

  private static Map<String, String> createTemplateMap(String template) {

    Document document;
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      document = saxBuilder.build(new ClassPathResource(template).getFile());
    } catch (IOException | JDOMException e) {
      throw new DataException(e.getMessage());
    }

    Map<String, String> map = new HashMap<>();
    List<Element> children = document.getRootElement().getChildren();
    for (Element child : children) {
      String openLmisName  = child.getAttributeValue("openlmis-name");
      String vendorName  = child.getAttributeValue("vendor-name");
      map.put(openLmisName,vendorName);
    }

    return map;  //To change body of created methods use File | Settings | File Templates.
  }

  public static String getEventFeed(EventFeedService eventFeedService, String requestURL, int feedNumber, Logger logger, String principalName){
    try {
      Feed feed = eventFeedService.getEventFeed(new URI(requestURL), feedNumber);
      try {
        mapFeedBasedOnVendorAndCategory(feed, principalName, null);
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      return new WireFeedOutput().outputString(feed);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad URI", e);
    } catch (FeedException e) {
      logger.error("error occurred while getting recent feedgenerator", e);
      throw new RuntimeException("Error serializing feed.", e);
    }
  }


}