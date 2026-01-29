//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.kingdee.bos.webapi.sdk;

import com.kingdee.bos.webapi.entity.*;
import com.kingdee.bos.webapi.utils.Base64Utils;
import com.kingdee.bos.webapi.utils.MD5Utils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ApiRequester {
    public static final ThreadLocal<String> K3CloudContext = new InheritableThreadLocal<String>();
    K3CloudCookieStore cookieStore;
    IdentifyInfo identify;
    protected int connectTimeout = 120;
    protected int connectionRequrestTimeout = 120;
    protected int socketTimeout = 180;
    protected String uri;

    ApiRequester(String uri) {
        this.uri = uri;
    }

    public K3CloudCookieStore getCookieStore() {
        return this.cookieStore;
    }

    public void setCookieStore(K3CloudCookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public IdentifyInfo getIdentify() {
        return this.identify;
    }

    public void setIdentify(IdentifyInfo identify) {
        this.identify = identify;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getConnectionRequrestTimeout() {
        return this.connectionRequrestTimeout;
    }

    public void setConnectionRequrestTimeout(int connectionRequrestTimeout) {
        this.connectionRequrestTimeout = connectionRequrestTimeout;
    }

    public int getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String postJson(RequestBodyObject requestBody) throws Exception {
        try {
            return this.postJson(this.uri, requestBody, this.connectTimeout, this.connectionRequrestTimeout, this.socketTimeout);
        } catch (IOException var3) {
            throw var3;
        }
    }

    protected String postJson(String uri, RequestBodyObject json, int connectTimeout, int connectionRequestTimeout, int socketTimeout) throws Exception {
        HttpRequester req = new HttpRequester(uri, this.buildHeader(this.getUrlPath(uri)), json, connectTimeout, connectionRequestTimeout);
        String body = req.post();
        this.getCookieFromReq(req);
        return body;
    }

    protected void getCookieFromReq(HttpRequester req) {
        if (req.getRepoCookies() != null && this.cookieStore != null) {
            Iterator var2 = req.getRepoCookies().iterator();

            while(var2.hasNext()) {
                Cookie c = (Cookie)var2.next();
                this.cookieStore.getCookies().put(c.getName(), c);
                if (c.getName().equals("kdservice-sessionid")) {
                    this.cookieStore.setSID(c.getValue());
                }
            }
        }

    }

    String getUrlPath(String url) {
        if (url.startsWith("http")) {
            int index = url.indexOf("/", 10);
            return index > -1 ? url.substring(index) : url;
        } else {
            return url;
        }
    }

    protected HashMap<String, String> buildHeader(String path) throws IOException {
        HashMap<String, String> header = new HashMap();

        try {
            String cookieHD;
            if (this.identify != null) {
                cookieHD = "";
                String apigwSec = "";
                String[] arr = this.identify.getAppId().split("_");
                if (arr.length >= 2) {
                    cookieHD = arr[0];
                    apigwSec = this.decodeSec(arr[1]);
                } else if (arr.length == 1) {
                    cookieHD = arr[0];
                }

                header.put("X-Api-ClientID", cookieHD);
                header.put("X-Api-Auth-Version", "2.0");
                Date date = new Date();
                Timestamp ts = new Timestamp(date.getTime());
                String tsVal = Long.valueOf(ts.getTime()).toString();
                header.put("x-api-timestamp", tsVal);
                String nonceVal = Long.valueOf(ts.getTime()).toString();
                header.put("x-api-nonce", nonceVal);
                header.put("x-api-signheaders", "X-Api-TimeStamp,X-Api-Nonce");
                String urlPath = URLEncoder.encode(path, "UTF-8");
                String context = String.format("POST\n%s\n\nx-api-nonce:%s\nx-api-timestamp:%s\n", urlPath, nonceVal, tsVal);
                if (apigwSec != "") {
                    header.put("X-Api-Signature", MD5Utils.hashMAC(context, apigwSec));
                } else {
                    header.put("X-Api-Signature", "");
                }
                String userName= K3CloudContext.get();
                String userId=this.identify.getUserName();
                if(userName != null) {
                    userId= userName;
                }
                header.put("X-Kd-Appkey", this.identify.getAppId());
                String data = String.format("%s,%s,%s,%s", this.identify.getdCID(),userId, this.identify.getlCID(), this.identify.getOrgNum());
                header.put("X-Kd-Appdata", Base64Utils.encodingToBase64(data.getBytes("UTF-8")));
                header.put("X-Kd-Signature", MD5Utils.hashMAC(this.identify.getAppId() + data, this.identify.getAppSecret()));
            }

            if (this.cookieStore != null) {
                if (this.cookieStore.getSID() != null) {
                    header.put("SID", this.cookieStore.getSID());
                }

                if (this.cookieStore.getCookies().size() > 0) {
                    cookieHD = String.format("Theme=standard");

                    Map.Entry cookie;
                    for(Iterator var14 = this.cookieStore.getCookies().entrySet().iterator(); var14.hasNext(); cookieHD = cookieHD + "; " + ((Cookie)cookie.getValue()).toString()) {
                        cookie = (Map.Entry)var14.next();
                    }

                    header.put("Cookie", cookieHD);
                }
            }
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        return header;
    }

    String decodeSec(String sec) throws IOException {
        byte[] buffer = Base64Utils.decodingFromBase64(sec);
        buffer = this.xOrSec(buffer);
        return Base64Utils.encodingToBase64(buffer);
    }

    byte[] xOrSec(byte[] buffer) {
        String seckey = "0054f397c6234378b09ca7d3e5debce7";
        byte[] pwd = null;

        try {
            pwd = seckey.getBytes("UTF-8");
        } catch (UnsupportedEncodingException var5) {
            var5.printStackTrace();
        }

        for(int i = 0; i < buffer.length; ++i) {
            buffer[i] ^= pwd[i];
        }

        return buffer;
    }
}
