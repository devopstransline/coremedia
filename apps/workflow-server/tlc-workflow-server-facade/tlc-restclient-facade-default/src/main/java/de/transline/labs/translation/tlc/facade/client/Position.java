package de.transline.labs.translation.tlc.facade.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Created by aga on 26.01.2022.
 */
public class Position {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("target_lang")
    private String targetLanguage;

    @JsonProperty("external_target_lang")
    private String externalTargetLanguage;

    @JsonProperty("delivery")
    private Map<String, String> delivery;

    public Position() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetLanguage() {
      return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
      this.targetLanguage = targetLanguage;
    }

    public String getExternalTargetLanguage() {
      return externalTargetLanguage;
    }

    public void setExternalTargetLanguage(String externalTargetLanguage) {
      this.externalTargetLanguage = externalTargetLanguage;
    }

    public Map<String, String> getDelivery() {
        return delivery;
    }

    public void setDelivery(Map<String, String> delivery) {
        this.delivery = delivery;
    }
}
