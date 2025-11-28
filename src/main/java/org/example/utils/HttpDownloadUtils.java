package org.example.utils;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HttpDownloadUtils {
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().readTimeout(600, TimeUnit.SECONDS).build();
    private static final String URL = "https://api.textin.com/ai/service/v1/bill_recognize_v2";

    private static Logger logger = LogManager.getLogger(HttpDownloadUtils.class);


   public static String getreceiptByBase64File(String url,String token) throws IOException {
       MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
       RequestBody body = RequestBody.create(mediaType, "url="+ URLEncoder.encode(url, "UTF-8"));
       Request request = new Request.Builder()
               .url("https://aip.baidubce.com/rest/2.0/ocr/v1/multiple_invoice")
               .method("POST", body)
               .addHeader("Content-Type", "application/x-www-form-urlencoded")
               .addHeader("Accept", "application/json")
               .addHeader("Authorization", "Bearer "+token)
               .build();
       Response response = HTTP_CLIENT.newCall(request).execute();
       String result =response.body().string();
       return result;
   }

    public static String saveByUrl(String url, String filePath, String fileName)  {
        logger.info("下载url:"+url);
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
               logger.warn("Failed to download file: " + response);
            }

            // 确保文件路径存在
            File directory = new File(filePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 构造完整的文件路径
            String fullFilePath = filePath + File.separator + fileName;
            if (filePath.endsWith("/") || filePath.endsWith("\\")) {
                fullFilePath = filePath + fileName;
            }

            // 写入文件
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new IOException("Response body is null");
                }

                try (InputStream inputStream = responseBody.byteStream();
                     FileOutputStream fileOutputStream = new FileOutputStream(fullFilePath)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            return fullFilePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
   public static String getreceiptTextIn(String url,String appId,String secretCode) throws IOException {
       MediaType contentType = MediaType.get("text/plain; charset=utf-8");
       RequestBody body = RequestBody.create(url, contentType);
       Request request = new Request.Builder()
               .url(URL)
               .addHeader("x-ti-app-id", appId)
               .addHeader("x-ti-secret-code", secretCode)
               .addHeader("Content-Type", contentType.toString())
               .post(body)
               .build();

       try (Response response = HTTP_CLIENT.newCall(request).execute()) {
           if (!response.isSuccessful()) {
               throw new IOException("请求失败，HTTP 状态码: " + response.code());
           }

           String result =response.body().string();
           System.out.println(result);
           return result;
       }

   }

 /*   *//**
     * 根据URL下载文件并转换为Base64编码
     *
     * @param fileUrl 文件URL地址
     * @return Base64编码字符串
     * @throws IOException IO异常
     *//*
    public static String downloadFileToBase64(String fileUrl) throws IOException {
        return downloadFileToBase64(fileUrl, 10000, 50000);
    }*/

    /**
     * 根据URL下载文件并转换为Base64编码（可设置超时时间）
     *
     * @param fileUrl 文件URL地址
     * @param connectTimeout 连接超时时间（毫秒）
     * @param readTimeout 读取超时时间（毫秒）
     * @return Base64编码字符串
     * @throws IOException IO异常
     */
    public static String downloadFileToBase64(String fileUrl, int connectTimeout, int readTimeout)
            throws IOException {

        HttpURLConnection connection = null;
        ByteArrayOutputStream outputStream = null;

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            // 检查响应状态码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file. HTTP response code: " + responseCode);
            }

            // 读取文件内容到字节数组输出流
            outputStream = new ByteArrayOutputStream();
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 将字节数组转换为Base64编码
            byte[] fileBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(fileBytes);

        } finally {
            // 关闭资源
            if (connection != null) {
                connection.disconnect();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    /**
     * 直接从URL下载文件并保存到指定路径，同时返回Base64编码
     *
     * @param fileUrl 文件URL地址
     * @param savePath 本地保存路径（可选）
     * @return Base64编码字符串
     * @throws IOException IO异常
     */
    public static String downloadFileToBase64AndSave(String fileUrl, String savePath) throws IOException {
        HttpURLConnection connection = null;
        ByteArrayOutputStream outputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            // 检查响应状态码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file. HTTP response code: " + responseCode);
            }

            // 创建输出流
            outputStream = new ByteArrayOutputStream();
            fileOutputStream = new FileOutputStream(savePath);

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
            // 将字节数组转换为Base64编码
            byte[] fileBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(fileBytes);

        } finally {
            // 关闭资源
            if (connection != null) {
                connection.disconnect();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    public static void main(String[] args) {
        saveByUrl("http://wukong-file-im-zjk.oss-cn-zhangjiakou.aliyuncs.com/ddmedia%2FiwEcAqNqcGcDAQTRBDgF0QeABrCZFBC17yoUfwkBEoShRGwAB9IS8pg_CAAJomltCgAL0gAPMng.jpg?x-oss-access-key-id=LTAI5tHAVmgnLXFMYxv2BgEv&x-oss-expires=1764317822&x-oss-signature=rn2zRnSoF%2BzVWxPwgK3tbQW%2Bk1E7Z87N1d%2B1j8okGZc%3D&x-oss-signature-version=OSS2","C:/invoice","自友_30530302231009D082372.jpg");
    }
}
