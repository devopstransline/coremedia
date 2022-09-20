package de.transline.labs.translation.tlc.facade;

import org.springframework.core.io.Resource;
import java.util.Set;
import java.io.InputStream;
import java.io.IOException;

public interface TLCRestClient<T> {

  T getOrder(String orderId);

  String createOrder(String title, String description, String sourceLanguage, Set<String> targetLanguages);

  void uploadFile(String orderId, Resource resource);

  void finishUpload(String orderId);

  InputStream downLoadFile(String downloadUrl) throws IOException;
}
