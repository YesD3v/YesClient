package com.yesclient.altmanager;
import java.awt.FileDialog;
import java.awt.Frame;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

 




public class CookieAltManager {

    public static class AltAccount {
        public String username;
        public String uuid;
        public Map<String, String> cookies;
         
        public String microsoftRefreshToken = "";
        public boolean valid = true;
        public String banStatus = "UNKNOWN";    
        public long banExpiry = 0;     

        public AltAccount(String username, String uuid, Map<String, String> cookies) {
            this.username = username;
            this.uuid = uuid;
            this.cookies = cookies;
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public boolean isBanned() {
            if ("TEMP_BANNED".equals(banStatus) && banExpiry > 0 && System.currentTimeMillis() >= banExpiry) {
                 
                banStatus = "UNBANNED";
                banExpiry = 0;
            }
            return "BANNED".equals(banStatus) || "TEMP_BANNED".equals(banStatus);
        }

        public String getBanStatus() {
            if ("TEMP_BANNED".equals(banStatus) && banExpiry > 0 && System.currentTimeMillis() >= banExpiry) {
                banStatus = "UNBANNED";
                banExpiry = 0;
            }
            return banStatus;
        }

        public long getBanExpiry() {
            return banExpiry;
        }

        public void setBan(String status, long expiry) {
            this.banStatus = status;
            this.banExpiry = expiry;
        }

        public void autoUnban() {
            this.banStatus = "UNBANNED";
            this.banExpiry = 0;
        }
    }

    public enum Status {
        IDLE("\u00A77Idle"),
        PARSING("\u00A7eParsing cookies..."),
        OAUTH("\u00A7eMicrosoft OAuth..."),
        MICROSOFT_LOGIN("\u00A7eComplete Microsoft sign-in..."),
        XBL("\u00A7eXbox Live..."),
        XSTS("\u00A7eXSTS Auth..."),
        MC_AUTH("\u00A7eMinecraft Auth..."),
        PROFILE("\u00A7eLoading profile..."),
        SUCCESS("\u00A7aSuccess!"),
        FAILED("\u00A7cFailed!");

        private final String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static CookieAltManager instance;
    private final Minecraft mc = Minecraft.getMinecraft();
     
    private static final String MICROSOFT_CLIENT_ID = "00000000402b5328";
    private static final String MICROSOFT_DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
    private static final String MICROSOFT_TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String MICROSOFT_XBOX_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private volatile String lastError = "";
    private volatile Status status = Status.IDLE;
    private final List<AltAccount> accounts = new ArrayList<>();
    private final File altsFile;
    private String lastAccountUuid = "";
    private Map<String, String> sessionCookies = new HashMap<>();
    private volatile boolean microsoftLoginInProgress;
    private volatile boolean refreshRequiresInteractiveSignIn;
    private volatile long accountReloginGeneration;
    private volatile String microsoftUserCode = "";
    private volatile String microsoftVerificationUri = "https://www.microsoft.com/link";

    public CookieAltManager() {
        this.altsFile = new File(new File(mc.mcDataDir, "yesclient"), "alts.json");
        load();
    }

    public static CookieAltManager getInstance() {
        if (instance == null) {
            instance = new CookieAltManager();
        }
        return instance;
    }

    public Map<String, String> parseCookieFile(File file) {
        Map<String, String> cookies = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                if (line.contains("\t")) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 7) {
                        String name = parts[5].trim();
                        String value = parts[6].trim();
                        if (!name.isEmpty()) {
                            cookies.put(name, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            lastError = "Failed to read file: " + e.getMessage();
            e.printStackTrace();
        }

        return cookies;
    }

    private String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0)
                sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private void updateCookiesFromResponse(HttpURLConnection conn) {
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                String[] parts = cookie.split(";")[0].split("=", 2);
                if (parts.length == 2) {
                    sessionCookies.put(parts[0], parts[1]);
                }
            }
        }
    }

    public boolean addAccountFromCookies(File cookieFile) {
        try {
            status = Status.PARSING;
            Map<String, String> cookies = parseCookieFile(cookieFile);
            sessionCookies = new HashMap<>(cookies);

            if (cookies.isEmpty()) {
                status = Status.FAILED;
                lastError = "No cookies parsed!";
                return false;
            }

            System.out.println("[CookieAlt] Found " + cookies.size() + " cookies");

             
            status = Status.OAUTH;
            String msToken = getMicrosoftToken();
            if (msToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got MS token (length: " + msToken.length() + ")");

             
            status = Status.XBL;
            String xblToken = getXboxLiveToken(msToken);
            if (xblToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got XBL token");

             
            status = Status.XSTS;
            String[] xstsData = getXSTSToken(xblToken);
            if (xstsData == null) {
                status = Status.FAILED;
                return false;
            }
            String xstsToken = xstsData[0];
            String userHash = xstsData[1];
            System.out.println("[CookieAlt] Got XSTS token");

             
            status = Status.MC_AUTH;
            String accessToken = getMinecraftToken(xstsToken, userHash);
            if (accessToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got MC token");

             
            status = Status.PROFILE;
            String[] profileData = getMinecraftProfile(accessToken);
            if (profileData == null) {
                status = Status.FAILED;
                return false;
            }
            String uuid = profileData[0];
            String username = profileData[1];
            System.out.println("[CookieAlt] Profile: " + username);

             
            AltAccount account = new AltAccount(username, uuid, sessionCookies);
            addAccount(account);

            status = Status.SUCCESS;
            return true;

        } catch (Exception e) {
            status = Status.FAILED;
            lastError = "Error: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

     




    public synchronized void startMicrosoftLogin() {
        if (microsoftLoginInProgress) {
            return;
        }

        cancelRequested = false;
        microsoftLoginInProgress = true;
        microsoftUserCode = "";
        microsoftVerificationUri = "https://www.microsoft.com/link";
        lastError = "";
        status = Status.MICROSOFT_LOGIN;

        Thread loginThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DeviceCode deviceCode = requestMicrosoftDeviceCode();
                    microsoftUserCode = deviceCode.userCode;
                    microsoftVerificationUri = deviceCode.verificationUri;

                    openMicrosoftVerificationPage(deviceCode.verificationUri, deviceCode.verificationUriComplete,
                            deviceCode.userCode);

                    String[] tokens = pollMicrosoftToken(deviceCode);
                    if (tokens == null) {
                        status = Status.FAILED;
                        return;
                    }

                    completeMicrosoftLogin(tokens[0], tokens[1], null);
                } catch (Exception e) {
                    status = Status.FAILED;
                    lastError = "Microsoft login error: " + e.getMessage();
                    e.printStackTrace();
                } finally {
                    microsoftLoginInProgress = false;
                }
            }
        }, "YesClient-MicrosoftLogin");
        loginThread.setDaemon(true);
        loginThread.start();
    }

     
    public synchronized boolean startAccountRelogin(final AltAccount account) {
        if (account == null || microsoftLoginInProgress
                || (status != Status.IDLE && status != Status.SUCCESS && status != Status.FAILED)) {
            return false;
        }

        final long loginGeneration = ++accountReloginGeneration;
        cancelRequested = false;
        microsoftLoginInProgress = true;
        microsoftUserCode = "";
        microsoftVerificationUri = "https://www.microsoft.com/link";
        lastError = "";
        status = Status.MICROSOFT_LOGIN;

        Thread loginThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isAccountReloginCancelled(loginGeneration)) return;
                    setLoginStatus(Status.MICROSOFT_LOGIN, loginGeneration);
                    DeviceCode deviceCode = requestMicrosoftDeviceCode();
                    if (isAccountReloginCancelled(loginGeneration)) return;
                    microsoftUserCode = deviceCode.userCode;
                    microsoftVerificationUri = deviceCode.verificationUri;
                    openMicrosoftVerificationPage(deviceCode.verificationUri,
                            deviceCode.verificationUriComplete, deviceCode.userCode);

                    String[] tokens = pollMicrosoftToken(deviceCode, loginGeneration);
                    if (isAccountReloginCancelled(loginGeneration)) return;
                    if (tokens == null) {
                        failAccountRelogin(account, getLastError(), loginGeneration);
                        return;
                    }
                    completeMicrosoftLogin(tokens[0], tokens[1], account, loginGeneration);
                } catch (Exception e) {
                    if (!isAccountReloginCancelled(loginGeneration)) {
                        failAccountRelogin(account, "Microsoft sign-in error: " + e.getMessage(), loginGeneration);
                    }
                } finally {
                    if (accountReloginGeneration == loginGeneration) {
                        microsoftLoginInProgress = false;
                    }
                }
            }
        }, "YesClient-MicrosoftRelogin");
        loginThread.setDaemon(true);
        loginThread.start();
        return true;
    }

    private boolean isAccountReloginCancelled(long loginGeneration) {
        return cancelRequested || accountReloginGeneration != loginGeneration;
    }

    private void failAccountRelogin(AltAccount account, String message, long loginGeneration) {
        synchronized (this) {
            if (isAccountReloginCancelled(loginGeneration)) return;
            account.valid = false;
            save();
            setFailed(message == null || message.isEmpty() ? "Microsoft sign-in failed" : message);
        }
    }

    private DeviceCode requestMicrosoftDeviceCode() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(MICROSOFT_DEVICE_CODE_URL).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "YesClient/1.0");
        conn.setDoOutput(true);

        String body = "client_id=" + encode(MICROSOFT_CLIENT_ID)
                + "&scope=" + encode(MICROSOFT_XBOX_SCOPE)
                + "&response_type=device_code";
        try (OutputStream output = conn.getOutputStream()) {
            output.write(body.getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        String response = readResponse(conn);
        if (code != 200) {
            throw new IOException("Microsoft device code failed (" + code + ")");
        }

        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        String userCode = json.has("user_code") ? json.get("user_code").getAsString() : "";
        String verificationUri = json.has("verification_uri")
                ? json.get("verification_uri").getAsString()
                : "https://www.microsoft.com/link";
        String verificationUriComplete = json.has("verification_uri_complete")
                ? json.get("verification_uri_complete").getAsString()
                : "";
        if (userCode.isEmpty()) {
            throw new IOException("Microsoft did not return a sign-in code");
        }

        int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 900;
        int interval = json.has("interval") ? json.get("interval").getAsInt() : 5;
        return new DeviceCode(userCode, json.get("device_code").getAsString(), verificationUri,
                verificationUriComplete, expiresIn, Math.max(5, interval));
    }

    private String[] pollMicrosoftToken(DeviceCode deviceCode) throws Exception {
        return pollMicrosoftToken(deviceCode, -1L);
    }

    private String[] pollMicrosoftToken(DeviceCode deviceCode, long loginGeneration) throws Exception {
        long expiresAt = System.currentTimeMillis() + deviceCode.expiresIn * 1000L;
        int interval = deviceCode.interval;

        while (System.currentTimeMillis() < expiresAt && microsoftLoginInProgress
                && !isLoginCancelled(loginGeneration)) {
            Thread.sleep(interval * 1000L);

            HttpURLConnection conn = (HttpURLConnection) new URL(MICROSOFT_TOKEN_URL).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "YesClient/1.0");
            conn.setDoOutput(true);

            String body = "client_id=" + encode(MICROSOFT_CLIENT_ID)
                    + "&device_code=" + encode(deviceCode.deviceCode)
                    + "&grant_type=" + encode("urn:ietf:params:oauth:grant-type:device_code");
            try (OutputStream output = conn.getOutputStream()) {
                output.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);
            JsonObject json = response.isEmpty() ? new JsonObject() : new JsonParser().parse(response).getAsJsonObject();

            if (code == 200 && json.has("access_token")) {
                String accessToken = json.get("access_token").getAsString();
                String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : "";
                return new String[] { accessToken, refreshToken };
            }

            String error = json.has("error") ? json.get("error").getAsString() : "";
            if ("authorization_pending".equals(error)) {
                continue;
            }
            if ("slow_down".equals(error)) {
                interval += 5;
                continue;
            }
            if ("authorization_declined".equals(error) || "access_denied".equals(error)) {
                lastError = "Microsoft sign-in was cancelled";
            } else if ("expired_token".equals(error)) {
                lastError = "Microsoft sign-in code expired";
            } else {
                lastError = "Microsoft token request failed";
            }
            return null;
        }

        if (isLoginCancelled(loginGeneration)) return null;
        if (lastError.isEmpty()) {
            lastError = "Microsoft sign-in timed out";
        }
        return null;
    }

    private void openMicrosoftVerificationPage(String verificationUri, String verificationUriComplete,
            String userCode) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(userCode), null);
        } catch (Exception ignored) {
             
        }

        try {
            String loginUri = verificationUriComplete;
            if (loginUri == null || loginUri.isEmpty()) {
                String separator = verificationUri.indexOf('?') >= 0 ? "&" : "?";
                 
                 
                 
                loginUri = verificationUri + separator + "otc=" + encode(userCode);
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(loginUri));
            } else {
                String osName = System.getProperty("os.name", "").toLowerCase();
                String[] command = osName.contains("win")
                        ? new String[] { "rundll32", "url.dll,FileProtocolHandler", loginUri }
                        : osName.contains("mac")
                                ? new String[] { "open", loginUri }
                                : new String[] { "xdg-open", loginUri };
                Runtime.getRuntime().exec(command);
            }
        } catch (Exception e) {
            lastError = "Open " + verificationUri + " and enter the code shown in YesClient";
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static final class DeviceCode {
        private final String userCode;
        private final String deviceCode;
        private final String verificationUri;
        private final String verificationUriComplete;
        private final int expiresIn;
        private final int interval;

        private DeviceCode(String userCode, String deviceCode, String verificationUri, String verificationUriComplete,
                int expiresIn, int interval) {
            this.userCode = userCode;
            this.deviceCode = deviceCode;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete;
            this.expiresIn = expiresIn;
            this.interval = interval;
        }
    }

    private String getMicrosoftToken() {
        try {
             
            String oauthUrl = "https://login.live.com/oauth20_authorize.srf?client_id=000000004C12AE6F&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL&response_type=token";

            HttpURLConnection conn = createConnection(oauthUrl, "GET");
            int code = conn.getResponseCode();
            updateCookiesFromResponse(conn);

            String location = conn.getHeaderField("Location");
            System.out.println("[CookieAlt] OAuth initial: " + code);

             
            if (location != null && location.contains("access_token=")) {
                System.out.println("[CookieAlt] Direct token redirect!");
                String token = extractFromUrl(location, "access_token");
                String decoded = URLDecoder.decode(token, "UTF-8");
                System.out.println("[CookieAlt] Extracted access_token. Starts with: "
                        + (decoded.length() > 10 ? decoded.substring(0, 10) : decoded));
                return decoded;
            }

             
            if (code == 302 && location != null) {
                System.out.println("[CookieAlt] Following redirect...");
                conn = createConnection(location, "GET");
                code = conn.getResponseCode();
                updateCookiesFromResponse(conn);
                location = conn.getHeaderField("Location");

                if (location != null && location.contains("access_token=")) {
                    return extractFromUrl(location, "access_token");
                }
            }

             
            if (code == 200) {
                String body = readResponse(conn);
                System.out.println("[CookieAlt] Got form page, length: " + body.length());

                 
                Map<String, String> inputs = extractAllHiddenInputs(body);
                System.out.println("[CookieAlt] Extracted " + inputs.size() + " form inputs");

                 
                for (String key : inputs.keySet()) {
                    if (key.equals("PPFT")) {
                        System.out.println("[CookieAlt] Found PPFT: " + inputs.get(key).length() + " chars");
                    } else {
                         
                        System.out.println("[CookieAlt] Input: " + key);
                    }
                }

                 
                String postUrl = extractWithRegex(body, "urlPost\"?\\s*[:\"]\\s*\"?(https?://[^\"']+)");
                if (postUrl == null) {
                    postUrl = extractWithRegex(body, "urlPost\\s*[:=]\\s*['\"]([^'\"]+)");
                }

                if (postUrl == null || !postUrl.startsWith("http")) {
                    lastError = "No form POST URL found";
                    return null;
                }

                System.out.println("[CookieAlt] Post URL: " + postUrl.substring(0, Math.min(60, postUrl.length())));

                 
                return submitForm(postUrl, inputs, 0);
            }

            lastError = "OAuth failed: " + code;
            return null;

        } catch (Exception e) {
            lastError = "OAuth error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> extractAllHiddenInputs(String html) {
        Map<String, String> inputs = new HashMap<>();

         
        Pattern inputPattern = Pattern.compile("<input[^>]+>", Pattern.CASE_INSENSITIVE);
        Matcher m = inputPattern.matcher(html);
        while (m.find()) {
            String tag = m.group();
            String name = extractWithRegex(tag, "name=['\"]?([^'\"]+)['\"]?");
            String value = extractWithRegex(tag, "value=['\"]?([^'\"]*)['\"]?");

            if (name != null) {
                inputs.put(name, value != null ? value : "");
            }
        }

         
         
         
        String sftTag = extractWithRegex(html, "sFTTag\\\"?\\s*:\\s*\\\"((?:[^\\\\\\\"]|\\\\.)*)\\\"");
        if (sftTag != null) {
             
            String unescaped = sftTag.replace("\\\"", "\"").replace("\\/", "/");
            Matcher m2 = inputPattern.matcher(unescaped);
            while (m2.find()) {
                String tag = m2.group();
                String name = extractWithRegex(tag, "name=['\"]?([^'\"]+)['\"]?");
                String value = extractWithRegex(tag, "value=['\"]?([^'\"]*)['\"]?");
                if (name != null) {
                    inputs.put(name, value != null ? value : "");
                }
            }
        }
        return inputs;
    }

    private String submitForm(String postUrl, Map<String, String> inputs, int depth) {
        if (depth > 5) {
            System.out.println("[CookieAlt] Max form recursion reached aborting.");
            lastError = "Max form recursion reached";
            return null;
        }

        try {
            System.out
                    .println("[CookieAlt] Submitting form (depth " + depth + ") with " + inputs.size() + " params...");

            HttpURLConnection conn = createConnection(postUrl, "POST");
            conn.setDoOutput(true);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                if (postData.length() > 0)
                    postData.append("&");
                postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                postData.append("=");
                postData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            byte[] postBytes = postData.toString().getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postBytes);
            }

            int code = conn.getResponseCode();
            updateCookiesFromResponse(conn);
            String location = conn.getHeaderField("Location");

            System.out.println("[CookieAlt] Form POST code: " + code);

             
            if (location != null) {
                System.out.println("[CookieAlt] Redirect: " + location.substring(0, Math.min(80, location.length())));
                if (location.contains("access_token=")) {
                    return extractFromUrl(location, "access_token");
                }

                 
                if (code == 302) {
                    return followRedirectChain(location, 5);
                }
            }

             
            if (code == 200) {
                String body = readResponse(conn);

                 
                String token = extractValue(body, "access_token", "=", "&");
                if (token != null) {
                    return URLDecoder.decode(token, "UTF-8");
                }

                 
                String newPostUrl = extractWithRegex(body, "urlPost\"?\\s*[:\"]\\s*\"?(https?://[^\"']+)");
                if (newPostUrl == null) {
                    newPostUrl = extractWithRegex(body, "urlPost\\s*[:=]\\s*['\"]([^'\"]+)");
                }

                if (newPostUrl != null && newPostUrl.startsWith("http")) {
                     
                     
                    if (newPostUrl.equals(postUrl)) {
                        System.out.println("[CookieAlt] Loop detected: new form has same URL as previous POST.");
                         
                        if (body.contains("name=\"passwd\"") || body.contains("type=\"password\"")) {
                        }

                        lastError = "Login requires password - cookies may be expired";
                        return null;
                    }

                    System.out.println("[CookieAlt] Found another form, submitting...");
                    Map<String, String> newInputs = extractAllHiddenInputs(body);
                    return submitForm(newPostUrl, newInputs, depth + 1);
                }

                 
                int idx = body.indexOf("access_token");
                if (idx == -1)
                    idx = body.indexOf("error");
                if (idx == -1)
                    idx = 0;
                System.out.println(
                        "[CookieAlt] Body snippet: " + body.substring(idx, Math.min(idx + 150, body.length())));
            }

            lastError = "Form submission failed: " + code;
            return null;

        } catch (Exception e) {
            lastError = "Form error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String followRedirectChain(String url, int maxRedirects) {
        try {
            for (int i = 0; i < maxRedirects; i++) {
                if (url.contains("access_token=")) {
                    return extractFromUrl(url, "access_token");
                }

                HttpURLConnection conn = createConnection(url, "GET");
                int code = conn.getResponseCode();
                updateCookiesFromResponse(conn);
                String location = conn.getHeaderField("Location");

                System.out.println("[CookieAlt] Redirect " + (i + 1) + ": " + code);

                if (location == null) {
                    if (code == 200) {
                        String body = readResponse(conn);
                        String token = extractValue(body, "access_token", "=", "&");
                        if (token != null) {
                            return URLDecoder.decode(token, "UTF-8");
                        }
                    }
                    break;
                }

                url = location;
            }
            lastError = "Redirect chain exhausted without token";
            return null;
        } catch (Exception e) {
            lastError = "Redirect error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private HttpURLConnection createConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Cookie", buildCookieHeader(sessionCookies));
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private String extractValue(String text, String marker, String start, String end) {
        int markerIdx = text.indexOf(marker);
        if (markerIdx == -1)
            return null;

        int startIdx = text.indexOf(start, markerIdx + marker.length());
        if (startIdx == -1)
            return null;
        startIdx += start.length();

        int endIdx = text.indexOf(end, startIdx);
        if (endIdx == -1)
            return null;

        return text.substring(startIdx, endIdx);
    }

    private String extractWithRegex(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractFromUrl(String url, String param) {
        try {
            int start = url.indexOf(param + "=");
            if (start == -1)
                return null;
            start += param.length() + 1;

            int end = url.indexOf("&", start);
            if (end == -1)
                end = url.indexOf("#", start);
            if (end == -1)
                end = url.length();

            return URLDecoder.decode(url.substring(start, end), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private String[] refreshMicrosoftToken(String refreshToken) {
        return refreshMicrosoftToken(refreshToken, -1L);
    }

    private String[] refreshMicrosoftToken(String refreshToken, long loginGeneration) {
        synchronized (this) {
            if (isLoginCancelled(loginGeneration)) return null;
            refreshRequiresInteractiveSignIn = false;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(MICROSOFT_TOKEN_URL).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "YesClient/1.0");
            conn.setDoOutput(true);

            String body = "client_id=" + encode(MICROSOFT_CLIENT_ID)
                    + "&refresh_token=" + encode(refreshToken)
                    + "&grant_type=refresh_token"
                    + "&scope=" + encode(MICROSOFT_XBOX_SCOPE);
            try (OutputStream output = conn.getOutputStream()) {
                output.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);
            if (code != 200) {
                String oauthError = "";
                try {
                    JsonObject errorJson = new JsonParser().parse(response).getAsJsonObject();
                    if (errorJson.has("error")) oauthError = errorJson.get("error").getAsString();
                } catch (Exception ignored) {
                }
                synchronized (this) {
                    if (isLoginCancelled(loginGeneration)) return null;
                    refreshRequiresInteractiveSignIn = "invalid_grant".equals(oauthError);
                    lastError = refreshRequiresInteractiveSignIn
                            ? "Microsoft refresh token expired or was revoked"
                            : "Microsoft refresh failed (" + code + ")";
                }
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            if (!json.has("access_token")) {
                synchronized (this) {
                    if (!isLoginCancelled(loginGeneration)) {
                        lastError = "Microsoft refresh returned no access token";
                    }
                }
                return null;
            }
            String nextRefreshToken = json.has("refresh_token")
                    ? json.get("refresh_token").getAsString()
                    : refreshToken;
            return new String[] { json.get("access_token").getAsString(), nextRefreshToken };
        } catch (Exception e) {
            synchronized (this) {
                if (!isLoginCancelled(loginGeneration)) {
                    lastError = "Microsoft refresh error: " + e.getMessage();
                }
            }
            return null;
        }
    }

     
    private boolean completeMicrosoftLogin(String msToken, String refreshToken, AltAccount existingAccount) {
        return completeMicrosoftLogin(msToken, refreshToken, existingAccount, -1L);
    }

    private boolean completeMicrosoftLogin(String msToken, String refreshToken, AltAccount existingAccount,
            long loginGeneration) {
        try {
            if (isLoginCancelled(loginGeneration)) return false;
            setLoginStatus(Status.XBL, loginGeneration);
            String xblToken = getXboxLiveTokenForMicrosoft(msToken);
            if (isLoginCancelled(loginGeneration)) return false;
            if (xblToken == null) {
                setLoginStatus(Status.FAILED, loginGeneration);
                return false;
            }

            setLoginStatus(Status.XSTS, loginGeneration);
            String[] xstsData = getXSTSToken(xblToken);
            if (isLoginCancelled(loginGeneration)) return false;
            if (xstsData == null) {
                setLoginStatus(Status.FAILED, loginGeneration);
                return false;
            }

            setLoginStatus(Status.MC_AUTH, loginGeneration);
            String accessToken = getMinecraftToken(xstsData[0], xstsData[1]);
            if (isLoginCancelled(loginGeneration)) return false;
            if (accessToken == null) {
                setLoginStatus(Status.FAILED, loginGeneration);
                return false;
            }

            setLoginStatus(Status.PROFILE, loginGeneration);
            String[] profileData = getMinecraftProfile(accessToken);
            if (isLoginCancelled(loginGeneration)) return false;
            if (profileData == null) {
                setLoginStatus(Status.FAILED, loginGeneration);
                return false;
            }

            String username = profileData[1];
            String uuid = profileData[0];
            if (existingAccount != null && existingAccount.uuid != null && !existingAccount.uuid.isEmpty()
                    && !normalizeUuid(existingAccount.uuid).equals(normalizeUuid(uuid))) {
                synchronized (this) {
                    if (isLoginCancelled(loginGeneration)) return false;
                    existingAccount.valid = false;
                    save();
                    setFailed("Signed into a different Microsoft account");
                }
                return false;
            }
            synchronized (this) {
                if (isLoginCancelled(loginGeneration)) return false;
                setSession(new Session(username, uuid, accessToken, "mojang"));

                AltAccount account = existingAccount != null
                        ? existingAccount
                        : new AltAccount(username, uuid, new HashMap<String, String>());
                account.username = username;
                account.uuid = uuid;
                account.cookies = new HashMap<String, String>();
                account.microsoftRefreshToken = refreshToken != null ? refreshToken : "";
                account.valid = true;
                addAccount(account);
                setLastAccount(account);

                status = Status.SUCCESS;
                return true;
            }
        } catch (Exception e) {
            if (isLoginCancelled(loginGeneration)) return false;
            synchronized (this) {
                if (isLoginCancelled(loginGeneration)) return false;
                status = Status.FAILED;
                lastError = "Microsoft authentication error: " + e.getMessage();
                if (existingAccount != null) {
                    existingAccount.valid = false;
                    save();
                }
            }
            return false;
        }
    }

    private void setLoginStatus(Status next, long loginGeneration) {
        synchronized (this) {
            if (!isLoginCancelled(loginGeneration)) {
                status = next;
            }
        }
    }

    private boolean isLoginCancelled(long loginGeneration) {
        return cancelRequested || (loginGeneration >= 0L && accountReloginGeneration != loginGeneration);
    }

    private String getXboxLiveToken(String msToken) {
        return getXboxLiveToken(msToken, "t=");
    }

     





    private String getXboxLiveTokenForMicrosoft(String msToken) {
        String[] prefixes = { "", "d=", "t=" };
        for (String prefix : prefixes) {
            String token = getXboxLiveToken(msToken, prefix);
            if (token != null) {
                return token;
            }
        }
        lastError = "Xbox Live rejected the Microsoft access token";
        return null;
    }

    private String getXboxLiveToken(String msToken, String ticketPrefix) {
        try {
            URL url = new URL("https://user.auth.xboxlive.com/user/authenticate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("x-xbl-contract-version", "1");
            conn.setDoOutput(true);

             
             
             
            String rpsTicket = ticketPrefix + msToken;

            JsonObject properties = new JsonObject();
            properties.addProperty("AuthMethod", "RPS");
            properties.addProperty("SiteName", "user.auth.xboxlive.com");
            properties.addProperty("RpsTicket", rpsTicket);

            JsonObject reqCtx = new JsonObject();
            reqCtx.add("Properties", properties);
            reqCtx.addProperty("RelyingParty", "http://auth.xboxlive.com");
            reqCtx.addProperty("TokenType", "JWT");

            String body = reqCtx.toString();
            byte[] bodyBytes = body.getBytes("UTF-8");

            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
             
            conn.setRequestProperty("User-Agent", "YesClient/1.0");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int code = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            String response = readResponse(conn);

            if (code != 200) {
                System.out.println("[CookieAlt] XBL failed: " + code + " " + msg);
                System.out.println("[CookieAlt] XBL Response Body: " + response);
                lastError = "XBL failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            return json.get("Token").getAsString();

        } catch (Exception e) {
            lastError = "XBL error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String[] getXSTSToken(String xblToken) {
        try {
            URL url = new URL("https://xsts.auth.xboxlive.com/xsts/authorize");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String body = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                    + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "XSTS failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            String token = json.get("Token").getAsString();
            String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs")
                    .getAsString();
            return new String[] { token, uhs };

        } catch (Exception e) {
            lastError = "XSTS error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String getMinecraftToken(String xstsToken, String userHash) {
        try {
            URL url = new URL("https://api.minecraftservices.com/authentication/login_with_xbox");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String body = "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\",\"ensureLegacyEnabled\":true}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "MC auth failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            return json.get("access_token").getAsString();

        } catch (Exception e) {
            lastError = "MC token error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String[] getMinecraftProfile(String accessToken) {
        try {
            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "Profile failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            String uuid = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            return new String[] { uuid, name };

        } catch (Exception e) {
            lastError = "Profile error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            System.out.println("[CookieAlt] IOException reading InputStream (expected for 400+): " + e.getMessage());
            is = conn.getErrorStream();
            if (is == null) {
                System.out.println("[CookieAlt] ErrorStream is NULL!");
            } else {
                System.out.println("[CookieAlt] Got ErrorStream");
            }
        }
        if (is == null)
            return "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private void setSession(Session session) {
        try {
            Field sessionField = null;
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    sessionField = f;
                    break;
                }
            }

            if (sessionField != null) {
                sessionField.setAccessible(true);
                sessionField.set(mc, session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCurrentUsername() {
        return mc.getSession().getUsername();
    }

    public void pickFileAndLogin() {
        new Thread(() -> {
            try {
                 
                Frame parent = null;
                try {
                } catch (Exception e) {
                }

                FileDialog dialog = new FileDialog(parent, "Select Cookie File",
                        FileDialog.LOAD);
                dialog.setFile("*.txt");
                dialog.setVisible(true);

                String dir = dialog.getDirectory();
                String file = dialog.getFile();

                if (dir != null && file != null) {
                    File selectedFile = new File(dir, file);
                    System.out.println("[CookieAlt] Selected: " + selectedFile.getAbsolutePath());

                    mc.addScheduledTask(() -> {
                        if (mc.thePlayer != null)
                            mc.thePlayer.addChatMessage(
                                    new net.minecraft.util.ChatComponentText("\u00A7e[CookieAlt] Logging in..."));
                    });

                    boolean success = addAccountFromCookies(selectedFile);

                    mc.addScheduledTask(() -> {
                        if (success) {
                            String msg = "\u00A7a[CookieAlt] Login successful! Logged in as: " + getCurrentUsername();
                            System.out.println(msg);
                            if (mc.thePlayer != null)
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
                        } else {
                            String msg = "\u00A7c[CookieAlt] Login failed: " + getLastError();
                            System.out.println(msg);
                            if (mc.thePlayer != null)
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String getLastError() {
        return lastError != null ? lastError : "";
    }

    public Status getStatus() {
        return status;
    }

    public boolean isMicrosoftLoginInProgress() {
        return microsoftLoginInProgress;
    }

    public String getMicrosoftUserCode() {
        return microsoftUserCode;
    }

    public String getMicrosoftVerificationUri() {
        return microsoftVerificationUri;
    }

    private volatile boolean cancelRequested;

    public void setStatus(Status status) {
        if (cancelRequested) return;
        this.status = status;
        if (status != Status.FAILED) {
            this.lastError = "";
        }
    }

    public synchronized void cancelLogin() {
        accountReloginGeneration++;
        this.microsoftLoginInProgress = false;
        this.cancelRequested = true;
        this.status = Status.IDLE;
        this.lastError = "Cancelled by user";
    }

    public void setFailed(String message) {
        this.status = Status.FAILED;
        this.lastError = message;
    }

    public void prepareAccountLogin() {
        cancelRequested = false;
        status = Status.IDLE;
        lastError = "";
    }

    public List<AltAccount> getAccounts() {
        return accounts;
    }

    public void addAccount(AltAccount account) {
        if (account == null) return;
        accounts.removeIf(a -> a == account || (a.uuid != null && account.uuid != null
                && a.uuid.equalsIgnoreCase(account.uuid)));
        accounts.add(account);
        save();
    }

    public void removeAccount(AltAccount account) {
        accounts.remove(account);
        if (account != null && account.uuid != null && lastAccountUuid != null
                && normalizeUuid(account.uuid).equals(normalizeUuid(lastAccountUuid))) {
            lastAccountUuid = "";
        }
        save();
    }

    public void save() {
        try {
            if (!altsFile.getParentFile().exists()) {
                altsFile.getParentFile().mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty("lastAccountUuid", lastAccountUuid == null ? "" : lastAccountUuid);
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (AltAccount acc : accounts) {
                JsonObject accJson = new JsonObject();
                accJson.addProperty("username", acc.username);
                accJson.addProperty("uuid", acc.uuid);
                accJson.addProperty("valid", acc.valid);
                accJson.addProperty("banStatus", acc.banStatus != null ? acc.banStatus : "UNKNOWN");
                accJson.addProperty("banExpiry", acc.banExpiry);
                accJson.addProperty("microsoftRefreshToken",
                        acc.microsoftRefreshToken != null ? acc.microsoftRefreshToken : "");

                JsonObject cookieJson = new JsonObject();
                if (acc.cookies != null) {
                    for (Map.Entry<String, String> entry : acc.cookies.entrySet()) {
                        cookieJson.addProperty(entry.getKey(), entry.getValue());
                    }
                }
                accJson.add("cookies", cookieJson);
                array.add(accJson);
            }
            json.add("accounts", array);

            try (FileWriter writer = new FileWriter(altsFile)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!altsFile.exists())
            return;
        try (Reader reader = new FileReader(altsFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            lastAccountUuid = json.has("lastAccountUuid") && !json.get("lastAccountUuid").isJsonNull()
                    ? json.get("lastAccountUuid").getAsString() : "";
            if (json.has("accounts")) {
                com.google.gson.JsonArray array = json.getAsJsonArray("accounts");
                accounts.clear();
                for (com.google.gson.JsonElement el : array) {
                    JsonObject accJson = el.getAsJsonObject();
                    String username = accJson.get("username").getAsString();
                    String uuid = accJson.has("uuid") ? accJson.get("uuid").getAsString() : "";
                    boolean valid = accJson.has("valid") ? accJson.get("valid").getAsBoolean() : true;

                    String banStatus = accJson.has("banStatus") ? accJson.get("banStatus").getAsString() : "UNKNOWN";
                    long banExpiry = accJson.has("banExpiry") ? accJson.get("banExpiry").getAsLong() : 0;
                    String microsoftRefreshToken = accJson.has("microsoftRefreshToken")
                            ? accJson.get("microsoftRefreshToken").getAsString()
                            : "";

                    Map<String, String> cookies = new HashMap<>();
                    JsonObject cookieJson = accJson.has("cookies") && accJson.get("cookies").isJsonObject()
                            ? accJson.getAsJsonObject("cookies")
                            : null;
                    if (cookieJson != null) {
                        for (Map.Entry<String, com.google.gson.JsonElement> entry : cookieJson.entrySet()) {
                            cookies.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                    AltAccount acc = new AltAccount(username, uuid, cookies);
                    acc.microsoftRefreshToken = microsoftRefreshToken;
                    acc.valid = valid;
                    acc.banStatus = banStatus;
                    acc.banExpiry = banExpiry;
                    accounts.add(acc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loginWithAccount(AltAccount account) {
        cancelRequested = false;
        status = Status.IDLE;
        sessionCookies = account.cookies != null ? new HashMap<>(account.cookies) : new HashMap<String, String>();

        try {
             
             
            if (account.microsoftRefreshToken != null && !account.microsoftRefreshToken.isEmpty()) {
                status = Status.OAUTH;
                String[] tokens = refreshMicrosoftToken(account.microsoftRefreshToken);
                if (tokens == null) {
                    status = Status.FAILED;
                    account.valid = false;
                    save();
                    return false;
                }
                return completeMicrosoftLogin(tokens[0], tokens[1], account);
            }

             
            status = Status.OAUTH;
            String msToken = getMicrosoftToken();
            if (msToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

             
            status = Status.XBL;
            String xblToken = getXboxLiveToken(msToken);
            if (xblToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

             
            status = Status.XSTS;
            String[] xstsData = getXSTSToken(xblToken);
            if (xstsData == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }
            String xstsToken = xstsData[0];
            String userHash = xstsData[1];

             
            status = Status.MC_AUTH;
            String accessToken = getMinecraftToken(xstsToken, userHash);
            if (accessToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

             
            status = Status.PROFILE;
            String[] profileData = getMinecraftProfile(accessToken);
            if (profileData == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }
            String uuid = profileData[0];
            String username = profileData[1];

             
            Session session = new Session(username, uuid, accessToken, "mojang");
            setSession(session);

             
            account.username = username;
            account.uuid = uuid;
            account.cookies = new HashMap<>(sessionCookies);
            account.valid = true;
            setLastAccount(account);
            save();

            status = Status.SUCCESS;
            return true;

        } catch (Exception e) {
            status = Status.FAILED;
            account.valid = false;
            save();
            lastError = "Error: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    public AltAccount getLastAccount() {
        if (lastAccountUuid == null || lastAccountUuid.isEmpty()) return null;
        String wanted = normalizeUuid(lastAccountUuid);
        for (AltAccount account : accounts) {
            if (account.uuid != null && normalizeUuid(account.uuid).equals(wanted)) return account;
        }
        return null;
    }

    public boolean isCurrentAccount(AltAccount account) {
        if (account == null) return false;
        try {
            if (mc.getSession().getProfile() != null && mc.getSession().getProfile().getId() != null
                    && account.uuid != null && !account.uuid.isEmpty()) {
                return normalizeUuid(mc.getSession().getProfile().getId().toString())
                        .equals(normalizeUuid(account.uuid));
            }
        } catch (Exception ignored) {
        }
        return account.username != null && account.username.equalsIgnoreCase(mc.getSession().getUsername());
    }

    public void clearLastAccount(AltAccount account) {
        if (account == null || account.uuid == null || lastAccountUuid == null) return;
        if (normalizeUuid(account.uuid).equals(normalizeUuid(lastAccountUuid))) {
            lastAccountUuid = "";
            save();
        }
    }

    private void setLastAccount(AltAccount account) {
        if (account == null || account.uuid == null || account.uuid.isEmpty()) return;
        lastAccountUuid = account.uuid;
        save();
    }

    private static String normalizeUuid(String uuid) {
        return uuid == null ? "" : uuid.replace("-", "").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
