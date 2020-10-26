package com.intertelco.screen.screenserver.services;

import com.intertelco.screen.screenserver.jpa.entities.Screen;
import com.intertelco.screen.screenserver.jpa.repositories.ScreenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;

@Service
public class KafkaServiceImpl implements KafkaService {

    String ip_screen = "192.168.0.111";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.dst}")
    private String topicName;

    @Override
    public void sendMessage(String message, String topicDst) {
        kafkaTemplate.send(topicDst, message);
    }

    @Autowired
    private ScreenRepository screenRepository;

    @Override
    @KafkaListener(topics = "${kafka.topic.own}", autoStartup = "${listen.auto.start}")
    public void listenWithHeaders(@Payload String message) {
        System.out.println("Received Message: " + message);
        try {
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.getString("TIPO").equals("3")) {
                //escribir en base de datos
                update("progress=" + jsonObject.getString("PROGRESO"), ip_screen);

                System.out.println(jsonObject.getInt("PROGRESO"));

                if (jsonObject.getInt("PROGRESO")==100)
                {
                    update( "estado=0", ip_screen);
                }
                else {update( "estado=1", ip_screen);}
            }
        } catch (JSONException err) {
            if(message.contains("screen")){
                String idScreen = message.split("screen:")[1];
                Screen screen = new Screen(idScreen,idScreen,new Date(),0);
                screenRepository.save(screen);
            }else if(message.contains("response")){
                System.out.println("Received Message: " + message);
            }
        }
    }

    @Override
    public void sendMessageToAll(String message) {
        kafkaTemplate.send(topicName, message);
    }

    public static void update(String comando, String destino) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://192.168.0.31:5432/pantallas",
                            "postgres", "postgres");
            c.setAutoCommit(false);
            //System.out.println("Opened database successfully");

                // ******* MODIFICAR DATOS ************

                stmt = c.createStatement();
                String sql = "UPDATE pantalla SET " + comando + ", last_access= CURRENT_TIMESTAMP WHERE ip='" + destino + "';";
                stmt.executeUpdate(sql);
                stmt.close();
                c.commit();
                c.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}