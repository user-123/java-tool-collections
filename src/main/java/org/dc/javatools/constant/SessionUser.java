package org.dc.javatools.constant;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@ToString
public class SessionUser implements Serializable {
    /**
     * {@link SessionKey#SESSION_USER}引用此常數值
     */
    public static final String SESSION_KEY = "SESSION_USER";
    private static final long serialVersionUID = -5038573912915784962L;

}
