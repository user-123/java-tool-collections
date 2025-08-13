package org.dc.javatools.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.dc.javatools.constant.SessionKey;
import org.dc.javatools.constant.SessionUser;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Objects;

/**
 * 放置與servlet相關的封裝方法
 */
public class ServletUtil {    //TODO cookie name是否需要列舉管理

    private ServletUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static RequestAttributes getRequestAttributes() {
        return RequestContextHolder.currentRequestAttributes();
    }

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) getRequestAttributes()).getRequest();
    }

    public static HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) getRequestAttributes()).getResponse();
    }

    private static Cookie[] getCookies() {
        HttpServletRequest request = getRequest();
        Cookie[] cookies = request != null ? request.getCookies() : null;
        return cookies != null ? cookies : new Cookie[0];
    }

    /**
     * 讀取cookie
     *
     * @param name cookie的name(/key)
     * @return {@link Cookie}
     */
    public static Cookie getCookie(String name) {
        if (StringUtil.isNullOrEmpty(name)) {
            return null;
        }
        Cookie[] cookies = getCookies();
        return Arrays.stream(cookies).filter(cookie -> name.equals(cookie.getName())).findFirst().orElse(null);
    }

    /**
     * 讀取cookie
     *
     * @param name cookie的name(/key)
     * @return {@link String}
     */
    public static String getCookieValue(String name) {
        Cookie cookie = getCookie(name);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * TODO value實現object serialization
     *
     * @param name
     * @param value
     * @param maxAge
     * @param path
     */
    private static void setCookie(String name, Object value, Integer maxAge, String path) {
        setCookie(name, value.toString(), maxAge, path);
    }

    /**
     * 設定cookie
     *
     * @param name   cookie的name(/key)
     * @param value  cookies的value
     * @param maxAge
     * @param path
     * @return void
     */
    public static void setCookie(String name, String value, Integer maxAge, String path) {
        if (StringUtil.isNullOrEmpty(name)) {
            return;
        }
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(StringUtil.isNotNullAndNotBlank(path) ? path : "/");
        if (maxAge != null) {
            cookie.setMaxAge(maxAge);
        }
        setCookie(cookie);
    }

    public static void setCookie(Cookie cookie) {
        HttpServletResponse response;
        if ((response = getResponse()) == null) {
            return;
        }
        response.addCookie(cookie);
    }

    public static HttpSession getSession(boolean createNewIfNotExist) {
        HttpServletRequest httpServletRequest = getRequest();
        return httpServletRequest != null ? httpServletRequest.getSession(createNewIfNotExist) : null;
    }

    public static HttpSession getSession() {
        return getSession(true);
    }

    /**
     * 取得當前session是否為新建立，若為新建立則回傳true，否則回傳false<br />
     * 此方法在session新建立(被重設或第一次生成)時會回傳true，但僅在該request生命週期內(該request到response結束前)為true，
     * 在下一次request以後將會回傳false，直到下一次session被重設為新建立為止
     *
     * @return boolean 是否為新建立的session
     */
    public static boolean isNewCreatedSession() {
        return getSession().isNew();
    }

    public static String getSessionId() {
        HttpSession session = getSession();
        return session != null ? session.getId() : null;
    }

    /**
     * 建議維護{@link SessionKey}以新增/修改session attribute key，並改調用{@link #getSessionAttribute(SessionKey)}
     *
     * @see SessionKey
     * @see #getSessionAttribute(SessionKey)
     * @see #setSessionAttribute(SessionKey, Object)
     * @see #setSessionAttribute(String, Object)
     * @deprecated 為避免session attribute混亂，應實現session attribute key標準化以利統一管理，並將此方法改為private method
     */
    @Deprecated(since = "2024.11.15")
    public static Object getSessionAttribute(String key) {
        HttpSession session = getSession();
        return session != null ? session.getAttribute(key) : null;
    }

    private static <T> T getSessionAttribute(String key, Class<T> valueType) {
        Object value = getSessionAttribute(key);
		/*
		if(value!=null || !valueType.isPrimitive()) {return (T)getSessionAttribute(key);}
		return switch (valueType.getName()) {	//primitive type返回預設值
			case "boolean" -> (T)Boolean.FALSE;
			case "byte" -> (T)Byte.valueOf((byte)0);
			case "short" -> (T)Short.valueOf((short)0);
			case "int" -> (T)Integer.valueOf(0);
			case "long" -> (T)Long.valueOf(0L);
			case "float" -> (T)Float.valueOf(0.0f);
			case "double" -> (T)Double.valueOf(0.0);
			case "char" -> (T)Character.valueOf('\u0000');
			default -> throw new IllegalStateException("Unexpected primitive type: " + valueType);
		};
		*/
        if (value != null || !valueType.isPrimitive()) {
            return valueType.cast(value);
        }
        return switch (valueType.getName()) {    //primitive type返回預設值
            case "boolean" -> valueType.cast(Boolean.FALSE);
            case "byte" -> valueType.cast((byte) 0);
            case "short" -> valueType.cast((short) 0);
            case "int" -> valueType.cast(0);
            case "long" -> valueType.cast(0L);
            case "float" -> valueType.cast(0.0f);
            case "double" -> valueType.cast(0.0d);
            case "char" -> valueType.cast('\u0000');
            default -> throw new IllegalStateException("Unexpected primitive type: " + valueType);
        };
    }

    /**
     * 在session裡新增session attribute；<br />若要新增/修改session attribute key，請在{@link SessionKey}新增/修改
     *
     * @param key 傳入定義好的{@link SessionKey}作為key進行查詢
     * @return {@link T} session attribute value，自動轉型為{@link SessionKey}裡面定義的value type；若是session不存在或沒有該key的session attribute，則回傳{@code null}
     * @apiNote 目前 實作/支持 "半殘的"泛型類型；但在外部若是使用鏈式呼叫，因為型別推斷的事發生在執行期間，所以無法直接鏈式調用，必須先宣告一個對應型別的物件，再進行呼叫物件方法
     */
    public static <T> T getSessionAttribute(SessionKey key) {
        Objects.requireNonNull(key);
        Class<T> valueType = key.getValueType();
        return getSessionAttribute(key.getKey(), valueType);
    }

    /**
     * 建議維護{@link SessionKey}以新增/修改session attribute key，並改調用{@link #setSessionAttribute(SessionKey, Object)}
     *
     * @see SessionKey
     * @see #setSessionAttribute(SessionKey, Object)
     * @see #getSessionAttribute(String)
     * @see #getSessionAttribute(SessionKey)
     * @deprecated 為避免session attribute混亂，應實現session attribute key標準化以利統一管理，並將此方法改為private method
     */
    @Deprecated(since = "2024.11.15")
    public static void setSessionAttribute(String key, Object value) {
        getSession().setAttribute(key, value);
    }

    /**
     * 在session裡新增session attribute；<br />若要新增/修改session attribute key，請在{@link SessionKey}裡新增/修改
     *
     * @param key   傳入定義好的{@link SessionKey}作為key
     * @param value 傳入{@link Object}作為value
     * @return void
     */
    public static void setSessionAttribute(SessionKey key, Object value) {
        setSessionAttribute(key != null ? key.getKey() : null, value);
    }

    private static void removeSessionAttribute(String key) {
        getSession().removeAttribute(key);
    }

    public static void removeSessionAttribute(SessionKey key) {
        removeSessionAttribute(key.getKey());
    }


    //TODO 將以下將by case的method抽離

    public static SessionUser getSessionUser() {
        return getSessionAttribute(SessionKey.SESSION_USER);
    }

    public static void setSessionUser(SessionUser user) {
        setSessionAttribute(SessionKey.SESSION_USER, user);
    }

    public static String getSessionFromPage() {
        return getSessionAttribute(SessionKey.SESSION_FROM_PAGE);
    }

    public static void setSessionFromPage(String pageId) {
        setSessionAttribute(SessionKey.SESSION_FROM_PAGE, pageId);
    }

    public static void setSessionPage(String pageId) {
        setSessionAttribute(SessionKey.SESSION_PAGE, pageId);
    }

    public static String getSessionPage() {
        return getSessionAttribute(SessionKey.SESSION_PAGE);
    }

    public static String getAppMobile() {
        return getSessionAttribute(SessionKey.APP_MOBILE);
    }

    public static String getDeviceId() {
        return getSessionAttribute(SessionKey.DEVICE_ID);
    }


    // HttpServlet
    public static String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNullOrBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtil.isNullOrBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtil.isNullOrBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtil.isNullOrBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtil.isNullOrBlank(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

}
