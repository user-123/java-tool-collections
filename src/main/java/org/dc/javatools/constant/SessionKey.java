package org.dc.javatools.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
@AllArgsConstructor
public enum SessionKey {    //目前valueType尚未完全實作
    UNKNOWN("", void.class),
    SESSION_DEVICE("SESSION_DEVICE", String.class),
    SESSION_PAGE("SESSION_PAGE", String.class),
    SESSION_FROM_PAGE("SESSION_FROM_PAGE", String.class),
    /**
     * @see SessionUser#SESSION_KEY
     */
    SESSION_USER(SessionUser.SESSION_KEY, SessionUser.class),
    SESSION_ACTION("SESSION_ACTION", String.class),
    SESSION_CAMPAIGN_MAP("SESSION_CAMPAIGN_MAP", String.class),


    /**
     * 用戶申裝狀態
     */
    USER_RENT_SATUS("USER_RENT_SATUS", String.class),
    APP_MOBILE("appmobile", String.class),
    DEVICE_ID("deviceId", String.class),
    /**
     * 申裝or下載後清除
     */
    NT_TRACK_MESSAGE("nttm", String.class),
    /**
     * 首次訪問頁面後清除
     */
    NT_TRACK_MESSAGE_VISIT("nttm_visit", String.class),
    LOGIN_PHONE_NUMBER("loginPhoneNumber", String.class),
    BLACK("black", String.class),
    AIRECORD_MSG("msg", String.class),
    REDEEM_OFFER_PID("redeem_offer_pid", String.class),
    ;

    private final String key;
    @SuppressWarnings("rawtypes")
    private final Class valueType;    //TODO type token擴展以支持泛型


    private static final Map<String, SessionKey> codeToEnum;

    static {
        codeToEnum = new HashMap<>(values().length * 4 / 3 + 1);
        for (SessionKey sessionKey : values()) {
            codeToEnum.put(sessionKey.getKey(), sessionKey);
        }
    }

    public static SessionKey fromCode(String code) {
        return codeToEnum.getOrDefault(code, SessionKey.UNKNOWN);
    }
}
