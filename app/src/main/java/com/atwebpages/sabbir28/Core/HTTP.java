package com.atwebpages.sabbir28.Core;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HTTP {

    public static class Response {
        public int code;
        public String body;

        public Response(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    // ===== GET request (for verify with token) =====
    public static Response get(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new Response(responseCode, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(-1, e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    // ===== POST request with JSON body (login) =====
    public static Response postJson(String urlString, String jsonBody) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            try (DataOutputStream os = new DataOutputStream(connection.getOutputStream())) {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            return new Response(responseCode, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(-1, e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    // ===== POST request with multipart/form-data (register/edit with image) =====
    public static Response postMultipart(String urlString, Map<String, String> params, String fileParam, File file) {
        HttpURLConnection connection = null;
        String boundary = "----Boundary" + System.currentTimeMillis();
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());

            // Text params
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                    out.writeBytes(entry.getValue() + "\r\n");
                }
            }

            // File param
            if (file != null && file.exists()) {
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"" + fileParam + "\"; filename=\"" + file.getName() + "\"\r\n");
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
                fis.close();
                out.writeBytes("\r\n");
            }

            out.writeBytes("--" + boundary + "--\r\n");
            out.flush();
            out.close();

            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            return new Response(responseCode, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(-1, e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}