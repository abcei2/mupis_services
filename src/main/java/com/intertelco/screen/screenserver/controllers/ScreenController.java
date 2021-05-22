package com.intertelco.screen.screenserver.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.gif.GifControlDirectory;
import com.intertelco.screen.screenserver.jpa.repositories.ScreenRepository;
import com.intertelco.screen.screenserver.services.KafkaService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import onbon.bx06.Bx6GEnv;
import onbon.bx06.Bx6GException;
import onbon.bx06.Bx6GScreen;
import onbon.bx06.Bx6GScreenClient;
import onbon.bx06.area.TextCaptionBxArea;
import onbon.bx06.area.page.ImageFileBxPage;
import onbon.bx06.file.BxFileWriterListener;
import onbon.bx06.file.ProgramBxFile;
import onbon.bx06.series.Bx6Q;



@RestController
@RequestMapping("/rest/screens")
public class ScreenController {

    private static String state = "vacio";
    private String UserDB="postgres";
    private String PassDB="Screen2021";


    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private KafkaService kafkaService;

    @Value("${file.upload-dir}")
    private String pathImage;




    
    ////////////////////////////////////
    ////// CONTROL SCREEN
    ////////////////////////////////////
    @CrossOrigin
    @GetMapping("/delete_screen/{screen_id}/{remote_screen_id}")
    public String delete_screen(@PathVariable int screen_id,@PathVariable int remote_screen_id) throws Exception {
        JSONObject screen_params = get_screen_params(screen_id,"5432",false);
        if(screen_params.getInt("estado")==1){
            return "Screen is bussy";
        }

        String screen_ip=screen_params.getString("ip");
        if (screen_params.getBoolean("is_server_ip")) {

            String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
            "/rest/screens/delete_screen/"+remote_screen_id+"/0";

            System.out.println(url);
            System.out.println(ServiceRestFull(url));
            return "Remotely screen delete";
        }
        else{
            update("eliminar", "ok", screen_id, screen_ip);
            return "Localy screen delete";
        }

    }

    @CrossOrigin
    @GetMapping("/turn_on_screen/{screen_id}/{remote_screen_id}")
    public String doTurnOnScreen(@PathVariable int screen_id,@PathVariable int remote_screen_id) throws Exception {
       
        JSONObject screen_params = get_screen_params(screen_id,"5432",false);
        if(screen_params.getInt("estado")==1){
            return "Screen is bussy";
        }

        String screen_ip=screen_params.getString("ip");
        if (screen_params.getBoolean("is_server_ip")) {
            String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
            "/rest/screens/turn_on_screen/"+remote_screen_id+"/0";
            String response1=ServiceRestFull(url);
            System.out.println(url);
            System.out.println(response1);
            return response1;
        } else {
            String response1=TURN_ON_SCREEN(screen_ip,screen_id);
            System.out.println(response1);
            return response1;
        }

    }

    @CrossOrigin
    @GetMapping("/turn_off_screen/{screen_id}/{remote_screen_id}")
    public String doTurnOffScreen(@PathVariable int screen_id,@PathVariable int remote_screen_id) throws Exception {
       
        JSONObject screen_params = get_screen_params(screen_id,"5432",false);
        if(screen_params.getInt("estado")==1){
            return "Screen is bussy";
        }

        String screen_ip=screen_params.getString("ip");
        if (screen_params.getBoolean("is_server_ip")) {
            String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
            "/rest/screens/turn_off_screen/"+remote_screen_id+"/0";
            String response1=ServiceRestFull(url);
            System.out.println(url);
            System.out.println(response1);
            return response1;
        } else {
            String response1=TURN_OFF_SCREEN(screen_ip,screen_id);
            System.out.println(response1);
            return response1;
        }
    }

    @CrossOrigin
    @GetMapping("/clear_screen/{screen_id}/{remote_screen_id}")
    public String clear_screen(@PathVariable int screen_id,@PathVariable int remote_screen_id) throws Exception {
      //BORRAR TODOS LOS PROGRAMAS DE LA PANTALLA

      JSONObject Respuesta = new JSONObject();
      JSONObject screen_params = get_screen_params(screen_id,"5432",false);
      if(screen_params.getInt("estado")==1){
        Respuesta.put("state",400);
        Respuesta.put("message", "Screen is bussy, try again in few seconds");
        return "bussy";
      }

      String screen_ip=screen_params.getString("ip");
      if (screen_params.getBoolean("is_server_ip"))
      {
        String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
        "/rest/screens/clear_screen/"+remote_screen_id+"/0";
        String response1=ServiceRestFull(url);
        System.out.println(url);
        System.out.println(response1);
        Respuesta.put("state",200);
        Respuesta.put("message", response1);
        return response1;
      }
      else
      {
        Bx6GEnv.initial("log.properties", 30000);
        Bx6GScreenClient screen = new Bx6GScreenClient("MyScreen", new Bx6Q());
  
        if (!screen.connect(screen_ip, 5005)) {
            
            System.out.println("\n\n\nconnect failed\n\n\n");
            Respuesta.put("state",400);
            Respuesta.put("message", "Failed to connect controller card screen");
            return "no_conection";
        } else {
            System.out.println("\n\n\nconnect stablished\n\n\n");
            screen.deletePrograms();
            screen.disconnect();
            
            Respuesta.put("state",200);
            Respuesta.put("message", "Cleared screen");
            return "done";
        }  
      }
    }

    @CrossOrigin
    @GetMapping("/change_brightness/{screen_id}/{value}/{remote_screen_id}")
    public String doChangeBrightness(@PathVariable int screen_id, @PathVariable int value,
    @PathVariable int remote_screen_id) throws Exception {
        if (value > 0 && value <= 16) {

            JSONObject screen_params = get_screen_params(screen_id,"5432",false);
            if(screen_params.getInt("estado")==1){
                return "Screen is bussy";
            }


            String screen_ip=screen_params.getString("ip");
            if (screen_params.getBoolean("is_server_ip")) {
           
                String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
                            "/rest/screens/change_brightness/"+remote_screen_id+"/"+value+"/0";
                
                System.out.println(url);
                System.out.println(ServiceRestFull(url));
                return "BRIGHTNESS CHANGE";
            }else{

                Bx6GEnv.initial("log.properties", 30000);
                Bx6GScreenClient screen = new Bx6GScreenClient("MyScreen", new Bx6Q());

                if (!screen.connect(screen_ip, 5005)) {
                    System.out.println("\n\n\nconnect failed\n\n\n");
                } else {
                    System.out.println("\n\n\nconnect stablished\n\n\n");


                    screen.manualBrightness((byte) value);
                    screen.disconnect();

                    Connection c = null;
                    Statement stmt = null;
                    try {
                        Class.forName("org.postgresql.Driver");
                        c = DriverManager
                                .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
                        c.setAutoCommit(false);
            
                        stmt = c.createStatement();
                        String query="update pantalla set brightness = "+Integer.toString(value)+" where id = "+Integer.toString(screen_id)+";";
                        System.out.println(query);
                        System.out.println(stmt.executeUpdate(query));
            
                        c.commit();
                        stmt.close();
                        c.close();
            
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e.getClass().getName() + ": " + e.getMessage());
                        System.exit(0);
                    }
                }
                System.out.println("\n\n\ndisconected\n\n\n");
                return "BRIGHTNESS CHANGE";
            }

        } else {
            return "VALUES MUST BE BETWEEN 1 AND 16";
        }
    }

    private String TURN_OFF_SCREEN(String ip_screen,int screen_id) throws Exception {
        Bx6GEnv.initial("log.properties", 30000);
        Bx6GScreenClient screen = new Bx6GScreenClient("MyScreen", new Bx6Q());
        if (!screen.connect(ip_screen, 5005)) {
            System.out.println("\n\n\nconnect failed\n\n\n");
            return "no_conection";
        } else {

            System.out.println("\n\n\nconnect stablished \n\n\n");
            screen.turnOff();
            screen.disconnect();
            Connection c = null;
            Statement stmt = null;
            try {
                Class.forName("org.postgresql.Driver");
                c = DriverManager
                        .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                        UserDB, PassDB);
                c.setAutoCommit(false);
    
                stmt = c.createStatement();
                String query="update pantalla set is_on = 0 where id = "+Integer.toString(screen_id)+";";
                System.out.println(query);
                System.out.println(stmt.executeUpdate(query));
    
                c.commit();
                stmt.close();
                c.close();
    
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
            return "off";
        }
    }

    private String TURN_ON_SCREEN(String ip_screen,int screen_id) throws Exception {

        Bx6GEnv.initial("log.properties", 30000);
        Bx6GScreenClient screen = new Bx6GScreenClient("MyScreen", new Bx6Q());
        if (!screen.connect(ip_screen, 5005)) {
            System.out.println("\n\n\nconnect failed\n\n\n");
            return "no_conection";
        } else {

            System.out.println("\n\n\nconnect stablished \n\n\n");
            screen.turnOn();
            screen.disconnect();
            Connection c = null;
            Statement stmt = null;
            try {
                Class.forName("org.postgresql.Driver");
                c = DriverManager
                        .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
                c.setAutoCommit(false);
    
                stmt = c.createStatement();
                String query="update pantalla set is_on = 1 where id = "+Integer.toString(screen_id)+";";
                System.out.println(query);
                System.out.println(stmt.executeUpdate(query));
    
                c.commit();
                stmt.close();
                c.close();
    
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
            return "on";
        }
        
    }

    @CrossOrigin
    @GetMapping("/send_image_to_remote/{multimedia_id}/{screen_id}")
    public String SendToRemote(@PathVariable int multimedia_id,@PathVariable int screen_id) throws Exception {
        
        Connection c = null;
        Statement stmt = null;

        try
        {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "select name as multimedia_name, "+
            "type as multimedia_type from multimedia where id="+multimedia_id+";";
            ResultSet rs = stmt.executeQuery(sql);
            String nameImage="no";
            String type="1";
            if (rs.next()) {
                nameImage=rs.getString("multimedia_name");
                type=rs.getString("multimedia_type");
            

            }
            
            stmt.close();
            c.close();
            JSONObject screen_params = get_screen_params(screen_id,"5432",false);

            //EN ESTE CASO NO IMPORTA PONER LA PANTALLA EN BUSSY, YA QUE
            //EL ESTADO SE LEE DIRECTAMENTE DE LA BASE DE DATOS REMOTA DONDE LA IP 
            //ES DIFERENTE A localhost
            //BUSSYYY HAY QUE ESPERAR QUE TERMINE ESTE SERVICIO ANTES QUE NADA

            JSONObject remote_screen_param = get_screen_params(-1,screen_params.getString("db_port"),true);
            if(remote_screen_param.has("ERROR")){

                return "no_conection_to_remote";
            }

            JSONArray remote_media = remote_screen_param.getJSONArray("media");
            System.out.println(remote_media.length());

           
            Boolean already_uploaded=false;
            for (int kk=0 ; kk<remote_media.length() ; kk++) {
                String media_name=remote_media.getJSONObject(kk).getString("multimedia_name");
                int media_id=remote_media.getJSONObject(kk).getInt("multimedia_id");
                if(nameImage.equals(media_name) && media_id==multimedia_id){
                    already_uploaded=true;
                    break;
                }
            }
            
            if(!already_uploaded)
            {
                String aux_url="http://localhost:"+Integer.toString(screen_params.getInt("service_port"))+"/rest/images/uploadFile/"+Integer.toString(multimedia_id);
                System.out.println(aux_url);
                sendPost(aux_url,pathImage + "/" + nameImage);
                System.out.println("UPLOADED IMAGE");
            }else{

                System.out.println("IMAGE ALREADY UPLOADED TO REMOTELLY SCREEN SERVER");
            }

            return "now is free";
        }catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            return "internal_error";
        }
        
        


    }
    ////////////////////////////////////
    ////// SEND IMAGE TO SCREEN
    ////////////////////////////////////

    @CrossOrigin
    @GetMapping("/send/{nameImage}/{image_duration}/{type}/{program_id}/{screen_id}/{multimedia_id}/{remote_screen_id}")
    public String doSendImageToNode(@PathVariable String nameImage, @PathVariable int image_duration,
                                    @PathVariable String type, @PathVariable int program_id, 
                                    @PathVariable int screen_id, @PathVariable int multimedia_id,
                                    @PathVariable int remote_screen_id)
                                    throws Exception, JSONException {
        
        ///PONBER VARIABLE DE BASE DE DATOS ESTADO=1  OCUPADAAA
        //SELECT PARA SABER SI LA PAANTALLA ID_SCREEN ESTÁ OCUPADA
        //Boolean getMedia: 
        //false, para no retornar la multimedia que hay en el servidor
        //true, para retornar la multimedia del servidor local o de los tuneleados por ssh (cambiando el puerto)
        JSONObject screen_params = get_screen_params(screen_id,"5432",false);

        if(screen_params.getInt("estado")==1){
            return "bussy";
        }
        String screen_ip=screen_params.getString("ip");
        if(screen_params.getBoolean("is_server_ip")){
            //EN ESTE CASO NO IMPORTA PONER LA PANTALLA EN BUSSY, YA QUE
            //EL ESTADO SE LEE DIRECTAMENTE DE LA BASE DE DATOS REMOTA DONDE LA IP 
            //ES DIFERENTE A localhost
            //BUSSYYY HAY QUE ESPERAR QUE TERMINE ESTE SERVICIO ANTES QUE NADA

            JSONObject remote_screen_param = get_screen_params(remote_screen_id,screen_params.getString("db_port"),true);
            if(remote_screen_param.has("ERROR")){

                return "no_conection_remote";
            }
            if(remote_screen_param.getInt("estado")==1){
                return "bussy_remote";
            }

            JSONArray remote_media = remote_screen_param.getJSONArray("media");
            System.out.println(remote_media.length());

            Boolean already_uploaded=false;
           
            for (int kk=0 ; kk<remote_media.length() ; kk++) {
                String media_name=remote_media.getJSONObject(kk).getString("multimedia_name");
                int media_id=remote_media.getJSONObject(kk).getInt("multimedia_id");
                if(nameImage.equals(media_name) && media_id==multimedia_id){
                    already_uploaded=true;
                    break;
                }
            }
            
            if(!already_uploaded)
            {
                String aux_url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+"/rest/images/uploadFile/"+Integer.toString(multimedia_id);
                System.out.println(aux_url);
                sendPost(aux_url,pathImage + "/" + nameImage);
                System.out.println("UPLOADED IMAGE");
            }else{

                System.out.println("IMAGE ALREADY UPLOADED TO REMOTELLY SCREEN SERVER");
            }
            //LOCALHOST INDICA Q LA PANTALLA ESTA RUTEADA
            //LA PANTALLA ESTÁ RUTEADA POR SSH, EN ESTE CASO SE LLAMA EL SERVICIO RUTEADO.
            //EL SERVICIO ES EXACTAMENTE IGUAL A ESTE SOLO QUE EN SU BASE DE DATOS LAS IP
            //SON DIFERENTE A LOCALHOST Y EL remote_screen_id pasa ser el screen_id EL ID DE LA CARA Q VAMOS A CAMBIAR
          
            String url="http://"+screen_ip+":"+Integer.toString(screen_params.getInt("service_port"))+
            "/rest/screens/send/"+nameImage+"/"+image_duration+"/"+type+"/"+program_id+
            "/"+remote_screen_id+"/"+multimedia_id+"/-1";
            String response1=ServiceRestFull(url); 
            System.out.println(url);
            System.out.println(response1);

            return response1;
        }
        else{

            if(screen_params.getInt("alto")>0 && screen_params.getInt("ancho")>0){
             
                update("actualizar", "estado=1", -1, screen_params.getString("ip"));         
                    // update("actualizar", "estado=1", screen_id, screen_params.getString("ip"));
                String To_Return = "OK";
                System.out.println(executeCommand("rm -r " + pathImage + "/TEMP"+Integer.toString(screen_id)));
                System.out.println(executeCommand("mkdir " + pathImage + "/TEMP"+Integer.toString(screen_id)));
                if (type.equals("VIDEO")) {
                    image_duration = 1;
                    System.out.println("ffmpeg -i " + pathImage + "/" + nameImage + " -vf fps=20 -ss 00:00:00 -t 00:00:10 -start_number 0 " + pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME%d.jpg");
                    System.out.println(executeCommand("ffmpeg -i " + pathImage + "/" + nameImage + " -vf fps=20 -ss 00:00:00 -t 00:00:10 -start_number 0 " + pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME%d.jpg"));
                    To_Return=PUTSCREEN(remote_screen_id,image_duration, new int[]{1}, screen_ip, program_id, nameImage,screen_params, screen_id, multimedia_id);
                }
                else{

                    System.out.println("convert -coalesce " + pathImage + "/" + nameImage + " " + pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME%d.jpg");
                    System.out.println(executeCommand("convert -coalesce " + pathImage + "/" + nameImage + " " + pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME%d.jpg"));
                    int[] timeFrames=new int[]{1};

                    if (type.equals("IMAGES")) 
                        timeFrames=new int[]{1};
                    else if(type.equals("GIFS")) 
                        timeFrames=getGifAnimatedTimeLength(pathImage + nameImage);
                    else
                        return "INVALID OPTIONS";
                    To_Return=PUTSCREEN(remote_screen_id,image_duration/10, timeFrames,
                    screen_ip, program_id, nameImage, screen_params, 
                    screen_id, multimedia_id);
                    
                }
                return To_Return;
            }else{

                return "WRONG ID";
            }
        }
    }

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private String PUTSCREEN(int remote_screen_id, int image_duration, int[] timeFrames, String screen_ip, int program_id, String nameImage, JSONObject shape, int screen_id, int multimedia_id) throws Exception {

        int program = program_id;
        int count = new File(pathImage + "/TEMP"+Integer.toString(screen_id)+"/").list().length;
        System.out.println( new File(pathImage + "/TEMP"+Integer.toString(screen_id)+"/").list().length);
        
        if (count > 0) {
            Bx6GEnv.initial("log.properties", 30000);
            Bx6GScreenClient screen = new Bx6GScreenClient("MyScreen", new Bx6Q());
            ProgramBxFile p = new ProgramBxFile(program, screen.getProfile());

            System.out.println("\n\n\n"+screen_ip+"\n\n\n");
            if (!screen.connect(screen_ip, 5005)) {
                System.out.println("\n\n\nconnect failed\n\n\n");

                update("actualizar", "estado=0", -1, screen_ip);

                return "connect failed";
            } else {
                System.out.println("\n\n\nconnect stablished\n\n\n");
            }

            screen.turnOff();
            Thread.sleep(100);

            if(shape.getInt("tipo_pantalla")==2){
                screen.deletePrograms();
            }else{
                screen.unlockProgram(program);
                screen.deleteProgram(program);
            }

            int widthP = shape.getInt("ancho");
            int heightP = shape.getInt("alto");

            p.setFrameShow(false);
            p.setProgramTimeSpan(10);
            TextCaptionBxArea dAreaContent = new TextCaptionBxArea(0, 0, widthP, heightP, screen.getProfile());
         
            if (count == 1) {
               ImageFileBxPage aux = new ImageFileBxPage(pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME" + String.valueOf(0) + ".jpg");
               dAreaContent.addPage(aux);
            }else{
                for (int i = 0; i < count; i++) { // 4FRAME
                    ImageFileBxPage aux = new ImageFileBxPage(pathImage + "/TEMP"+Integer.toString(screen_id)+"/FRAME" + String.valueOf(i) + ".jpg");
                    if(image_duration==0)
                        aux.setStayTime(timeFrames[i]);
                    else{
                        aux.setStayTime(image_duration);
                    }
                    dAreaContent.addPage(aux);
                }
            }

            p.addArea(dAreaContent);
            final int[] finish = new int[1];

            screen.writeProgramAsync(p, new BxFileWriterListener<Bx6GScreen>() {
                @Override
                public void fileWriting(Bx6GScreen bx6GScreen, String s, int i) {
                    System.out.println("Writing file "+s+" "+Integer.toString(i));

                }

                @Override
                public void fileFinish(Bx6GScreen bx6GScreen, String s, int i) {

                }

                @Override
                public void progressChanged(Bx6GScreen bx6GScreen, String s, int i, int i1) {

                    finish[0] = i * 100 / i1;
              
                    add_media_progress( finish[0], multimedia_id, screen_id,program_id);
                }

                @Override
                public void cancel(Bx6GScreen bx6GScreen, String s, Bx6GException e) {
                   
                    add_media_progress( 0, multimedia_id, screen_id,program_id);
                
                    System.out.println("CANCELED!");
                    screen.disconnect();
                    ///PONBER VARIABLE DE BASE DE DATOS ESTADO=0  DESOCUPADAAA                 
                    
                    update("actualizar", "estado=0", -1, screen_ip);
                        
                    System.out.println(executeCommand("rm -r " + pathImage + "/TEMP"+Integer.toString(screen_id)));
                    // update("actualizar", "estado=0", screen_id, screen_ip);
                }

                @Override
                public void done(Bx6GScreen bx6GScreen) {
                    System.out.println("\n");
                    System.out.println("DONEE!");
                    screen.turnOn();
                    screen.disconnect();

                    ///PONBER VARIABLE DE BASE DE DATOS ESTADO=0  DESOCUPADAAA
                    update("actualizar", "estado=0", -1, screen_ip);
                   
                    add_media_progress( 100, multimedia_id, screen_id,program_id);

                    System.out.println(executeCommand("rm -r " + pathImage + "/TEMP"+Integer.toString(screen_id)));
                    System.out.println("FINISH");
                }
                
            });

            multimedia_actualizada(Integer.toString(screen_id),Integer.toString(program_id), Integer.toString(multimedia_id));
            Thread.sleep(100);
        } else {
            System.out.println("NO FILES TO UPLOAD ON SCREEN. TRYY SEND MESSAGE");
            System.out.println("FINISH");
            return "no_files_in_media";
        }
        return "image_loading_on_screen";
    }

    private String executeCommand(String command) {

        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";

            printStream(p.getErrorStream(), "ERROR");
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    public static void printStream(InputStream is, String type) {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
                System.out.println(type + ">" + line);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static int[] getGifAnimatedTimeLength(String url) throws ImageProcessingException, IOException, MetadataException {
        InputStream inputStream = new FileInputStream(url);
        Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
        List<GifControlDirectory> gifControlDirectories =
                (List<GifControlDirectory>) metadata.getDirectoriesOfType(GifControlDirectory.class);

        int[] timeLength = new int[gifControlDirectories.size()];
        int n = 0;
        if (gifControlDirectories.size() == 1) { // Do not read delay of static GIF files with single frame.
        } else if (gifControlDirectories.size() >= 1) {
            for (GifControlDirectory gifControlDirectory : gifControlDirectories) {
                if (gifControlDirectory.hasTagName(GifControlDirectory.TAG_DELAY)) {
                    timeLength[n] = gifControlDirectory.getInt(GifControlDirectory.TAG_DELAY);
                    //System.out.println(timeLength[n]);
                    n++;
                }
            }
        }
        return timeLength;
    }

    public JSONObject get_screen_params(int screen_id,String postgres_port, Boolean get_media) throws JSONException, SQLException {

        Connection c = null;
        Statement stmt = null;
        try {
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:"+postgres_port+"/pantallas",
                            UserDB, PassDB);
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql="select pantalla.estado as estado, "
            + "pantalla.ip as ip, pantalla.db_port as db_port, pantalla.service_port as service_port, "
            + "pantalla.is_server_ip as is_server_ip, "
            + "pantalla.tipo_pantalla as tipo_pantalla,tipo, pantalla.ancho, pantalla.alto from "
            + "tipo_pantalla inner join pantalla on tipo_pantalla.id=pantalla.tipo_pantalla where pantalla.id="+screen_id+";";
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println(sql);
            JSONObject json = new JSONObject();
            if (rs.next()) {
                
                
                System.out.println("DONEEE");

                json.put("tipo_pantalla", rs.getInt("tipo_pantalla"));
                json.put("ancho", rs.getInt("ancho"));
                json.put("alto", rs.getInt("alto"));
                json.put("ip", rs.getString("ip"));
                json.put("db_port", rs.getString("db_port"));
                json.put("service_port", rs.getString("service_port"));
                json.put("estado", rs.getInt("estado"));
                json.put("is_server_ip", rs.getBoolean("is_server_ip"));
            }else{
                json.put("error","fail to get screen params");
            }

            if(get_media)
            {    
                rs = stmt.executeQuery("select id as multimedia_id, "+
                    "name as multimedia_name, type as multimedia_type from multimedia;");

                JSONArray json_array_media=new JSONArray();
                while (rs.next()) {
                    
                    
                    JSONObject json_media = new JSONObject();
                    json_media.put("multimedia_id", rs.getInt("multimedia_id"));
                    json_media.put("multimedia_name", rs.getString("multimedia_name"));
                    json_media.put("multimedia_type", rs.getString("multimedia_type"));
                    json_array_media.put(json_media);
    
                }
                json.put("media", json_array_media);
            }
            rs.close();
            stmt.close();
            c.close();
            return(json);
            

        } catch (PSQLException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            JSONObject json = new JSONObject();
            json.put("ERROR",e.getClass().getName() + ": " + e.getMessage());

            return(json);
        }

    }
    public void add_media_progress(int progress, int multimedia_id,int screen_id, int program_id){
        Connection c = null;
        Statement stmt = null;
        try
        {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "UPDATE pantalla_multimedia SET progress="+progress+" WHERE id_programa='"+program_id+"' and id_multimedia= " + multimedia_id + " and id_pantalla=" + screen_id + ";";
            System.out.println(sql);
            stmt.executeUpdate(sql);
            c.commit();
            c.close();
            stmt.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

    }
 
    public void multimedia_actualizada(String screen_id,String program_id,String multimedia_id) {

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String query="update pantalla_multimedia"+
            " SET id_multimedia="+multimedia_id+
            ", add_date=current_timestamp"+
            " where id_pantalla="+screen_id+
            " and id_programa='"+program_id+"';";
            System.out.println(query);
            System.out.println(stmt.executeUpdate(query));

            c.commit();
            stmt.close();
            c.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

    }

    public static void update(String modo, String comando, int screen_id, String screen_ip) {

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                            UserDB, PassDB);
            c.setAutoCommit(false);
            //System.out.println("Opened database successfully");

            if (modo == "actualizar") {

                // ******* MODIFICAR DATOS ************

                stmt = c.createStatement();
                String sql ="sdaf";
                if(screen_id==-1){
                    sql = "UPDATE pantalla SET " + comando + ", last_access= CURRENT_TIMESTAMP;";

                }else{
                    sql = "UPDATE pantalla SET " + comando + ", last_access= CURRENT_TIMESTAMP WHERE id='" + screen_id + "';";
                }
                
                stmt.executeUpdate(sql);
                c.commit();
                stmt.close();
                c.close();
            }
            if (modo == "multimedia_actualizada") {
                
                //******** LEER DATOS *****************

                stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery("update pantalla_multimedia"+
                " SET id_multimedia="+121+" where id_pantalla=1 and id_programa='"+2+"';");
                while (rs.next()) {
                    state = rs.getString("estado");
                    System.out.println("ESTADO = " + state);
                    System.out.println();
                }
                rs.close();
                stmt.close();
                c.close();
            }


           if (modo == "leer") {

               //******** LEER DATOS *****************

               stmt = c.createStatement();
               ResultSet rs = stmt.executeQuery("SELECT * FROM pantalla;");
               while (rs.next()) {
                   state = rs.getString("estado");
                   System.out.println("ESTADO = " + state);
                   System.out.println();
               }
               rs.close();
               stmt.close();
               c.close();
           }

       

           if (modo == "eliminar") {

               // ******* ELIMINAR DATOS ************

               stmt = c.createStatement();

               String sql="SELECT * FROM pantalla where id= '"+screen_id+"';";
               ResultSet rs = stmt.executeQuery(sql);
               String id1="";
               while (rs.next()) {
                   id1 = rs.getString("id");
               }

               sql = "DELETE FROM pantalla where id= '"+screen_id+"';";
               stmt.executeUpdate(sql);

               sql = "DELETE FROM pantalla_multimedia where id_pantalla= "+id1+";";
               stmt.executeUpdate(sql);

               stmt.close();
               c.commit();
               c.close();
           }

           if (modo == "modificar") {

               // ******* MODIFICAR DATOS ************

               stmt = c.createStatement();

               String sql="SELECT * FROM pantalla where id= '"+screen_id+"';";
               ResultSet rs = stmt.executeQuery(sql);
               String id1="";
               while (rs.next()) {
                   id1 = rs.getString("id");
               }

               sql = "DELETE FROM pantalla_multimedia where id_pantalla= "+id1+";";
               stmt.executeUpdate(sql);

               int num=Integer.valueOf(comando);
               for (int i=1; i<=num; i++) {
                   sql = "INSERT INTO pantalla_multimedia (id_pantalla, add_date, id_programa) "
                           + "VALUES ('" + id1+ "',CURRENT_TIMESTAMP, '"+i+"');";
                   stmt.executeUpdate(sql);
               }

               stmt.close();
               c.commit();
               c.close();
           }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static String ServiceRestFull(String uri2){
        String uri =uri2;
        String aux;
        URL url = null;
        boolean connectionFlag=false;
        try {
            connectionFlag=true;
            url = new URL(uri);
        } catch (MalformedURLException e) {
            connectionFlag=true;
            e.printStackTrace();
        }
        HttpURLConnection connection =
                null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connectionFlag=true;
        } catch (IOException e) {
            connectionFlag=true;
            e.printStackTrace();
        }
        try {
            connection.setRequestMethod("GET");
            connectionFlag=true;
        } catch (ProtocolException e) {
            e.printStackTrace();
            connectionFlag=true;
        }
        connection.setRequestProperty("Accept", "*/*");

        try {
            InputStream xml = connection.getInputStream();
            aux=convertStreamToString(xml);
            connectionFlag=true;
            return aux;
        } catch (IOException e) {
            connection.disconnect();
            e.printStackTrace();
            connectionFlag=false;
            return "error_service";
        }
    }

    private void sendPost(String url,String path_to_file) throws Exception {
        File file = new File(path_to_file);  
        FileInputStream fis = null;
        try {
			fis = new FileInputStream(file);
			CloseableHttpClient httpClient = HttpClients.createDefault();

			// server back-end URL
            HttpPost httppost = new HttpPost(url);
			MultipartEntity entity = new MultipartEntity();
			// set the file input stream and file name as arguments
			entity.addPart("file", new InputStreamBody(fis, file.getName()));
			httppost.setEntity(entity);
			// execute the request
            HttpResponse response = httpClient.execute(httppost);
            System.out.println("IMAGE UPLOADED");
        } catch (ClientProtocolException e) {
			System.err.println("Unable to make connection");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Unable to read file");
			e.printStackTrace();
		} finally {
			try {
				if (fis != null) fis.close();
			} catch (IOException e) {}
		}
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}