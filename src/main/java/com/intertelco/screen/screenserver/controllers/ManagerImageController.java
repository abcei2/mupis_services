package com.intertelco.screen.screenserver.controllers;

import com.intertelco.screen.screenserver.services.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

@RestController
@RequestMapping("/rest/images")
public class ManagerImageController {

    private static final Logger logger = LoggerFactory.getLogger(ManagerImageController.class);

    private static String UserDB="postgres";
    private static String PassDB="Screen2021";
    
    @Value("${file.upload-dir}")
    private String pathImage;

    @Autowired
    private FileStorageService fileStorageService;

    @CrossOrigin
    @PostMapping("/uploadFile/{media_id}")
    public String uploadFile(@RequestParam("file") MultipartFile file,@PathVariable int media_id) throws Exception {
        String[] available_image_extentions={"jpg","JPEG","jpeg","png","PNG"};
        String[] available_gif_extentions={"gif", "GIF"};
        String[] available_video_extentions={"avi","AVI","mp4","MP4"};
        String type_file="none";

        String fileName = fileStorageService.storeFile(file);
        String[] fileName_split=fileName.split("[.]");
        System.out.println(fileName.length());
        if(fileName_split.length==2){
            String file_extention=fileName_split[1];


            if (Arrays.asList(available_image_extentions).contains(file_extention)){
                type_file="jpg";
            }
            else if(Arrays.asList(available_gif_extentions).contains(file_extention)){
                type_file="gif";

            }else if(Arrays.asList(available_video_extentions).contains(file_extention)){
                type_file="mp4";
            }else{                
                return "File extention is not availabe";
            }

            
            JSONObject multimedia_json=agregar_multimedia("asd","asda",true,-1);
            JSONArray multimedia_array=multimedia_json.getJSONArray("media");
            
            System.out.println(fileName);
            for (int kk=0 ; kk<multimedia_array.length() ; kk++) {

                System.out.println(multimedia_array.getJSONObject(kk).getString("multimedia_name"));
                if(multimedia_array.getJSONObject(kk).getString("multimedia_name").equals(fileName)){
                    return "there are already a file with name: "+fileName;
                }
            }


            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/rest/images/downloadFile/")
            .path(fileName).toUriString();



            agregar_multimedia(fileName, type_file,false,media_id);
            return "Image Uploaded to server";
            
        }else{

            return "Filename couldn't have spaces, dots, or special characters";
        }
       
            
       
    }

    


    @CrossOrigin
    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @CrossOrigin
    @RequestMapping(value = "/{image}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public void getImage(HttpServletResponse response, @PathVariable String image) throws IOException {
        File file = new File(pathImage + "/" + image);

        System.out.println("GETTING IMAGE!");
        long millis=System.currentTimeMillis();
        java.util.Date date=new java.util.Date(millis);
        System.out.println(date);
        System.out.println(image);
        if (file.isFile()) {
            InputStream imgFile = new FileInputStream(file.getPath());
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            StreamUtils.copy(imgFile, response.getOutputStream());
        }
    }

    @CrossOrigin
    @GetMapping("/remove_media/{nameImage}")
    public String RemoveImage(@PathVariable String nameImage) throws Exception {

        File temp;
        temp = new File(pathImage + "/" + nameImage);

        boolean exists = temp.exists();
        if (exists) {
            if (temp.isDirectory()) {

                System.out.println("File is a directory");
                return "File is a directory";
            }

            System.out.println(executeCommand("rm " + pathImage + "/" + nameImage));
            exists = temp.exists();

            if (!exists) {
                System.out.println("File removed");
                return "File removed";
            } else {

                System.out.println("Some king of internal problem happend");
                System.out.println("Try to give permissions to " + pathImage);
                return "Some king of internal problem happend\nTry to give permissions to " + pathImage;
            }
        } else {
            System.out.println("File doesn't exists");
            return "File doesn't exists";
        }

    }

    @GetMapping()
    public List<String> getNameImage() {
        System.out.println("GETTING IMAGE!");
        long millis=System.currentTimeMillis();
        java.util.Date date=new java.util.Date(millis);
        System.out.println(date);
        File file = new File(pathImage);
        if (file.isDirectory()) {
            return Stream.of(file.list()).collect(Collectors.toList());
            
        }
        return null;
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

    public JSONObject agregar_multimedia(String media_name, String media_type,Boolean get_media_names,int media_id) throws JSONException, SQLException {
        Connection c = null;
        Statement stmt = null;

        JSONObject json=new JSONObject();
        try
        {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/pantallas",
                        UserDB, PassDB);
            c.setAutoCommit(false);
            if(get_media_names){
                stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery("select name as multimedia_name, type as multimedia_type from multimedia;");

                JSONArray json_array_media=new JSONArray();
                while (rs.next()) {
                    
                    
                    JSONObject json_media = new JSONObject();
                    json_media.put("multimedia_name", rs.getString("multimedia_name"));
                    json_media.put("multimedia_type", rs.getString("multimedia_type"));
                    json_array_media.put(json_media);
    
                }
                json.put("media", json_array_media);

                rs.close();
                stmt.close();
                c.close();
            }
            else{
                stmt = c.createStatement();
                String sql="asd";
                if(media_id==-1)
                    sql = "insert into multimedia (name,type) values('"+media_name+"','"+media_type+"');";
                else
                    sql = "insert into multimedia (id, name,type) values('"+media_id+"','"+media_name+"','"+media_type+"');";
                
                System.out.println(sql);
                stmt.executeUpdate(sql);
                
                stmt.close();
                c.commit();
                json.put("message", "inserted");
            }
            
            c.close();
        }catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
            json.put("message", e.getClass().getName() + ": " + e.getMessage());
        }

        return json;

    }


    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
