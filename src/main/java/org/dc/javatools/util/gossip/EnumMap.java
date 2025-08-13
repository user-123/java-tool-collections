package org.dc.javatools.util.gossip;

import java.util.Map;

public final class EnumMap {

    private EnumMap() {
        throw new IllegalStateException("Utility class");
    }

    public static <K, V extends Enum<V>> Map<K, V> getMap(Enum<V> enumInstance, Class<K> keyType, Class<V> valueType) {
        //TODO 待完善
		/*
		當前思路：
			理想作法：傳入參數給定以哪個field property作為map key
			簡單作法：預期傳入的enum格式均為 ENUM(int, String)，field properties name均為value和description，這樣只要簡單get或reflaction即可(意味著強制使用value作為key)
		*/
        return null;
    }
}
