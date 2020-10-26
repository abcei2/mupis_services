package com.intertelco.screen.screenserver.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class Screen {

    @Id
    private String id;
    private String topic;
    private Date update;
    private Integer countImg;

    public Screen(){}

    public Screen(String id, String topic, Date update, Integer countImg) {
        this.id = id;
        this.topic = topic;
        this.update = update;
        this.countImg = countImg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Date getUpdate() {
        return update;
    }

    public void setUpdate(Date update) {
        this.update = update;
    }

    public Integer getCountImg() {
        return countImg;
    }

    public void setCountImg(Integer countImg) {
        this.countImg = countImg;
    }

    @Override
    public String toString() {
        return "Screen{" +
                "id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", update=" + update +
                ", countImg=" + countImg +
                '}';
    }
}
