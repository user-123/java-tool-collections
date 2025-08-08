package org.dc.javatools.dao;

import org.dc.javatools.util.StringUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public abstract class CommonDao {
    protected final JdbcOperations jdbcOperations;
    protected final NamedParameterJdbcOperations namedParameterJdbcOperations;
    protected final org.slf4j.Logger log;
    //protected final org.apache.logging.log4j.Logger log2;

    public CommonDao(JdbcOperations jdbcOperations, NamedParameterJdbcOperations namedParameterJdbcOperations) {
        this.log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        //this.log2 = org.apache.logging.log4j.LogManager.getLogger(this.getClass());

        /*
        if(jdbcOperations==null && namedParameterJdbcOperations==null) {throw new IllegalArgumentException("jdbcOperations=={} && namedParameterJdbcOperations=={}");}
        this.jdbcOperations = jdbcOperations==null ? namedParameterJdbcOperations.getJdbcOperations() : jdbcOperations;
        this.namedParameterJdbcOperations = namedParameterJdbcOperations==null ? findNamedParameterJdbcOperations(jdbcOperations) : namedParameterJdbcOperations;
        */

        //新手友善版
        if (jdbcOperations != null && namedParameterJdbcOperations != null) {
            this.jdbcOperations = jdbcOperations;
            this.namedParameterJdbcOperations = namedParameterJdbcOperations;
        } else if (jdbcOperations != null) {    //注意!!隱含的邏輯為jdbcOperations!=null && namedParameterJdbcOperations==null
            this.jdbcOperations = jdbcOperations;
            this.namedParameterJdbcOperations = findNamedParameterJdbcOperations(jdbcOperations);
        } else if (namedParameterJdbcOperations != null) {    //注意!!隱含的邏輯為namedParameterJdbcOperations!=null && jdbcOperations==null
            this.jdbcOperations = namedParameterJdbcOperations.getJdbcOperations();
            this.namedParameterJdbcOperations = namedParameterJdbcOperations;
        } else {
            throw new IllegalStateException("jdbcOperations==null && namedParameterJdbcOperations==null");
        }


        log.trace("jdbc實例相同：{}", this.jdbcOperations == (this.namedParameterJdbcOperations != null ? this.namedParameterJdbcOperations.getJdbcOperations() : null));
    }

    public CommonDao(JdbcOperations jdbcOperations) {
        this(jdbcOperations, null);
    }

    public CommonDao(NamedParameterJdbcOperations namedParameterJdbcOperations) {
        this(null, namedParameterJdbcOperations);
    }

    @Autowired
    private ApplicationContext applicationContext;

    private NamedParameterJdbcOperations findNamedParameterJdbcOperations(JdbcOperations jdbcOperations) {
        if (applicationContext == null) {  //TODO 修正此問題；方案1，取消final，但應該要將filed property改為private；方案2，調整初始化順序流程
            log.warn("applicationContext尚未完成初始化，{}無法獲取bean，namedParameterJdbcOperations無法使用", this.getClass().getSimpleName());
            return null;
        }
        //從容器中取出所有namedParameterJdbcOperations bean檢查其內含的jdbcOperations實例是否為傳入的jdbcOperations；這樣可以在傳入namedParameterJdbcOperations缺失的情況下拿到bean
        Map<String, NamedParameterJdbcOperations> beans = applicationContext.getBeansOfType(NamedParameterJdbcOperations.class);
        for (NamedParameterJdbcOperations namedParameterJdbcOperations : beans.values()) {
            if (jdbcOperations.equals(namedParameterJdbcOperations.getJdbcOperations())) {
                return namedParameterJdbcOperations;
            }
        }
        log.warn("注意!!!!未找到namedParameterJdbcOperations實例，使用new instance，請務必確認是否有正確定義namedParameterJdbcOperations的bean");
        return new NamedParameterJdbcTemplate(jdbcOperations);
    }


    /**
     * 新增一筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return 新增的行數量，若是錯誤返回-1
     */
    public int createOne(String sqlNativeQuery, List<Object> arguments) {
        return writeOne(sqlNativeQuery, arguments, "新增");
    }

    /**
     * 新增多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param argumentsList  搭配語句的argument，須按照語句所需參數順序排列(多組arguments)
     * @return int array紀錄每一組arguments搭配語句執行所新增的行數量，若是錯誤返回空int array
     */
    public int[] createMultiple(String sqlNativeQuery, List<Object[]> argumentsList) {
        return writeMultiple(sqlNativeQuery, argumentsList, "新增");
    }

    /**
     * 讀取一筆資料，若是資料不存在，會catch住，並return null，須在接到的時候做null check
     *
     * @param entityClass    return的po型別
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return {@code entityClass}指定的Po Object，若是錯誤返回null
     */
    public <Po> Po readOne(Class<Po> entityClass, String sqlNativeQuery, List<Object> arguments) {
        return readOne(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取一筆資料，若是資料不存在，會catch住，並return null，須在接到的時候做null check
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param entityClass    return的po型別
     * @return {@code entityClass}指定的Po Object，若是錯誤返回null
     */
    public <Po> Po readOne(String sqlNativeQuery, List<Object> arguments, Class<Po> entityClass) {
        return readOne(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取一筆資料，若是資料不存在，會catch住，並return null，須在接到的時候做null check
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param rowMapper      解析sql回傳的row mapper
     * @return {@code RowMapper<Po>}指定的Po Object，若是錯誤返回null
     */
    public <Po> Po readOne(String sqlNativeQuery, List<Object> arguments, RowMapper<Po> rowMapper) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        Po result = null;
        try {
            result = jdbcOperations.queryForObject(sqlNativeQuery, rowMapper, arguments.toArray());
            log.info("讀取單筆結果：{}", result);
        } catch (EmptyResultDataAccessException ex) {
            log.info("SQL查無資料");
        } catch (IncorrectResultSizeDataAccessException ex) {
            log.error("資料不唯一或無效");
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 讀取多筆資料
     *
     * @param entityClass    return的po型別
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return {@code entityClass}指定的Po Object組成的{@code RowMapper<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(Class<Po> entityClass, String sqlNativeQuery, List<Object> arguments) {
        return readMultiple(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param entityClass    return的po型別
     * @return {@code entityClass}指定的Po Object組成的{@code List<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(String sqlNativeQuery, List<Object> arguments, Class<Po> entityClass) {
        return readMultiple(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param rowMapper      解析sql回傳的row mapper
     * @return {@code RowMapper<Po>}指定的Po Object組成的{@code List<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(String sqlNativeQuery, List<Object> arguments, RowMapper<Po> rowMapper) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        List<Po> result = Collections.emptyList();
        try {
            result = jdbcOperations.query(sqlNativeQuery, rowMapper, arguments.toArray());
            log.info("讀取多筆結果：{}{}", result.size(), "筆");
            log.trace("讀取多筆結果：{}", result);
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }


    public <T> List<T> getEmployeesByDepartment(String department) {
        String sql = "SELECT * FROM Employee WHERE department = :department";
        SqlParameterSource parameters = new MapSqlParameterSource(); //SqlParameterSource
        ((MapSqlParameterSource) parameters).addValue("department", department);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = (NamedParameterJdbcTemplate) namedParameterJdbcOperations;
        return namedParameterJdbcTemplate.query(sql, parameters, new BeanPropertyRowMapper<>());
    }


    public <Po> Po readOne(Class<Po> entityClass, String sqlNativeQuery, Map<String, Object> arguments) {
        return readOne(sqlNativeQuery, new MapSqlParameterSource(arguments), entityClass);
    }

    public <Po> Po readOne(String sqlNativeQuery, Map<String, Object> arguments, Class<Po> entityClass) {
        return readOne(sqlNativeQuery, new MapSqlParameterSource(arguments), entityClass);
    }

    public <Po> Po readOne(Class<Po> entityClass, String sqlNativeQuery, SqlParameterSource arguments) {
        return readOne(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    public <Po> Po readOne(String sqlNativeQuery, SqlParameterSource arguments, Class<Po> entityClass) {
        return readOne(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    public <Po> Po readOne(String sqlNativeQuery, Map<String, Object> arguments, RowMapper<Po> rowMapper) {
        return readOne(sqlNativeQuery, new MapSqlParameterSource(arguments), rowMapper);
    }

    /**
     * 讀取一筆資料，若是資料不存在，會catch住，並return null，須在接到的時候做null check
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      XXXXX
     * @param rowMapper      解析sql回傳的row mapper
     * @return {@code RowMapper<Po>}指定的Po Object，若是錯誤返回null
     */
    public <Po> Po readOne(String sqlNativeQuery, SqlParameterSource arguments, RowMapper<Po> rowMapper) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        Po result = null;
        try {
            result = namedParameterJdbcOperations.queryForObject(sqlNativeQuery, arguments, rowMapper);
            log.info("讀取單筆結果：{}", result);
        } catch (EmptyResultDataAccessException ex) {
            log.info("SQL查無資料");
        } catch (IncorrectResultSizeDataAccessException ex) {
            log.error("資料不唯一或無效");
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 讀取多筆資料
     *
     * @param entityClass    return的po型別
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return {@code entityClass}指定的Po Object組成的{@code List<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(Class<Po> entityClass, String sqlNativeQuery, SqlParameterSource arguments) {
        return readMultiple(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param entityClass    return的po型別
     * @return entityClass指定的Po Object組成的{@code List<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(String sqlNativeQuery, SqlParameterSource arguments, Class<Po> entityClass) {
        return readMultiple(sqlNativeQuery, arguments, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 讀取多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param rowMapper      解析sql回傳的row mapper
     * @return {@code RowMapper<Po>}指定的Po Object組成的{@code List<Po>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Po> List<Po> readMultiple(String sqlNativeQuery, SqlParameterSource arguments, RowMapper<Po> rowMapper) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        List<Po> result = Collections.emptyList();
        try {
            result = namedParameterJdbcOperations.query(sqlNativeQuery, arguments, rowMapper);
            log.info("讀取多筆結果：{}{}", result.size(), "筆");
            log.trace("讀取多筆結果：{}", result);
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }


    /**
     * 修改一筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return 更新的行數量，若是錯誤返回-1
     */
    public int updateOne(String sqlNativeQuery, List<Object> arguments) {
        return writeOne(sqlNativeQuery, arguments, "更新");
    }

    /**
     * 修改多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param argumentsList  搭配語句的argument，須按照語句所需參數順序排列(多組arguments)
     * @return int array紀錄每一組arguments搭配語句執行所更新的行數量，若是錯誤返回空int array
     */
    public int[] updateMultiple(String sqlNativeQuery, List<Object[]> argumentsList) {
        return writeMultiple(sqlNativeQuery, argumentsList, "更新");
    }

    /**
     * 刪除一筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @return 刪除的行數量，若是錯誤返回-1
     */

    public int deleteOne(String sqlNativeQuery, List<Object> arguments) {
        return writeOne(sqlNativeQuery, arguments, "刪除");
    }

    /**
     * 刪除多筆資料
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param argumentsList  搭配語句的argument，須按照語句所需參數順序排列(多組arguments)
     * @return int array紀錄每一組arguments搭配語句執行所刪除的行數量，若是錯誤返回空int array
     */
    public int[] deleteMultiple(String sqlNativeQuery, List<Object[]> argumentsList) {
        return writeMultiple(sqlNativeQuery, argumentsList, "刪除");
    }

    /**
     * 讀取單一值，若是資料不存在，會catch住，並return null，須在接到的時候做null check
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param langClass      單一值的型別
     * @return langClass指定的class type Object，若是錯誤返回null
     */
    public <Lang> Lang queryOne(String sqlNativeQuery, List<Object> arguments, Class<Lang> langClass) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        Lang result = null;
        try {
            result = jdbcOperations.queryForObject(sqlNativeQuery, langClass, arguments.toArray());
            log.info("請求單筆結果：{}", result);
        } catch (EmptyResultDataAccessException ex) {
            log.info("SQL查無資料");
        } catch (IncorrectResultSizeDataAccessException ex) {
            log.error("資料不唯一或無效");
        } catch (IncorrectResultSetColumnCountException ex) {
            log.error("資料檢索失敗，請確認sql語句為讀取單一值或傳入的{}為java.lang.* class", langClass.getName());
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 讀取單一column值
     *
     * @param sqlNativeQuery 原生SQL語句
     * @param arguments      搭配語句的argument，須按照語句所需參數順序排列
     * @param langClass      單一column值的型別
     * @return langClass指定的class type Object組成的{@code List<langClass>}，若是錯誤返回空list({@link Collections#EMPTY_LIST})
     */
    public <Lang> List<Lang> queryMultiple(String sqlNativeQuery, List<Object> arguments, Class<Lang> langClass) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        List<Lang> result = Collections.emptyList();
        try {
            result = jdbcOperations.queryForList(sqlNativeQuery, langClass, arguments.toArray());
            log.info("請求多筆結果：{}", result);
        } catch (IncorrectResultSetColumnCountException ex) {
            log.error("資料檢索失敗，請確認sql語句為讀取單一column值或傳入的{}為java.lang.* class", langClass.getName());
        } catch (DataAccessException ex) {
            log.error("讀取資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 儲存一筆資料(包含新增、修改)(未實作)
     */
    @Deprecated
    public void saveOne(String sqlNativeQuery, List<Object> arguments) {
    }

    /**
     * 儲存多筆資料(包含新增、修改)(未實作)
     */
    @Deprecated
    public void saveMultiple(String sqlNativeQuery, List<Object[]> argumentsList) {
    }

    private int writeOne(String sqlNativeQuery, List<Object> arguments, String logFlag) {
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, arguments);
        int result = -1;
        try {
            result = jdbcOperations.update(sqlNativeQuery, arguments.toArray());
            log.info("{}單筆結果：{}{}", logFlag, result, "筆");
        } catch (DataAccessException ex) {
            log.error("寫入資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    private int[] writeMultiple(String sqlNativeQuery, List<Object[]> argumentsList, String logFlag) {
        String argsList = argumentsListToString(argumentsList);
        log.info("{}===[sqlNativeQuery: {}, arguments = {}]===", this.getClass().getName(), sqlNativeQuery, argsList);
        int[] result = new int[0];
        try {
            result = jdbcOperations.batchUpdate(sqlNativeQuery, argumentsList);
            log.info("{}多筆結果：{}{}", logFlag, Arrays.toString(result), "筆");
        } catch (DataAccessException ex) {
            log.error("寫入資料庫錯誤，{}", ex.getMessage(), ex);
        }
        return result;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter(AccessLevel.PROTECTED)
    protected enum ARGUMENT_LIST_TYPE {
        IMMUTABLE(false), MUTABLE(true);
        private boolean isMutable;
    }

    /**
     * 將傳入參數組成參數list；注意此回傳的為immutable list；若是有追加argument需求，則呼叫generateArguments(ARGUMENT_LIST_TYPE.MUTABLE, Object...)方法
     *
     * @param arguments
     * @return 參數list，{@code List<Object>}(immutable array list)
     */
    protected List<Object> generateArguments(Object... arguments) {
        return generateArguments(ARGUMENT_LIST_TYPE.IMMUTABLE, arguments);
    }

    //    protected Map<String, ?> generateArguments(Object[]... arguments) {
//        return generateArguments(ARGUMENT_LIST_TYPE.IMMUTABLE, arguments);
//    }
    protected Map<String, ?> generateArguments(Map<String, ?> argumentsMap) {
        return generateArguments(ARGUMENT_LIST_TYPE.IMMUTABLE, argumentsMap);
    }

    /**
     * 若是有追加argument需求，可呼叫此方法，自行控制傳回的list是否可變(mutable)
     *
     * @param argumentListType ARGUMENT_LIST_TYPE.MUTABLE：傳回mutable list；ARGUMENT_LIST_TYPE.IMMUTABLE：傳回immutable list
     * @param arguments
     * @return 參數list，{@code List<Object>}(mutable/immutable array list)
     */
    protected List<Object> generateArguments(ARGUMENT_LIST_TYPE argumentListType, Object... arguments) {
        return argumentListType == null || !argumentListType.isMutable() ? Arrays.asList(arguments) : generateArguments(Arrays.asList(arguments));
    }

    protected Map<String, ?> generateArguments(ARGUMENT_LIST_TYPE argumentListType, Object[]... arguments) {
        //SqlParameterSource parameters = new MapSqlParameterSource();
        //for(Object[] argument : arguments) {
        //    ((MapSqlParameterSource) parameters).addValue((String)argument[0], argument[1]);
        //}
        Map<String, Object> argumentsMap = new HashMap<>(arguments.length * 4 / 3 + 1);
        for (Object[] argument : arguments) {
            argumentsMap.put((String) argument[0], argument[1]);
        }
        return generateArguments(argumentListType, argumentsMap);
    }

    protected Map<String, ?> generateArguments(ARGUMENT_LIST_TYPE argumentListType, Map<String, ?> argumentsMap) {
        return argumentListType == null || !argumentListType.isMutable() ? Collections.unmodifiableMap(argumentsMap) : argumentsMap;
    }

    /**
     * 若是有追加argument需求，可將generateArguments(Object...)返回的list傳入此方法，將返回mutable list
     *
     * @param argumentList
     * @return 參數list，{@code List<Object>}(mutable array list)
     */
    protected <T> List<T> generateArguments(List<T> argumentList) {
        return new ArrayList<>(argumentList);
    }

    /**
     * 生成參數組清單的string
     *
     * @param argumentsList
     * @return {[a, b, c, ...], [array2], [array3]}
     */
    private String argumentsListToString(List<Object[]> argumentsList) {
        return argumentsList.stream().map(Arrays::toString).collect(Collectors.joining(", ", "{", "}"));
    }

    /**
     * 判定SQL是否使用named parameter
     *
     * @param sqlNativeQuery 原生SQL語句
     * @return SQL語句是否使用named parameter
     */
    private boolean isNamedParameterSqlQuery(String sqlNativeQuery) {
        if (StringUtil.isNullOrBlank(sqlNativeQuery)) {
            throw new IllegalArgumentException(String.format("SQL語句為空白：{}", sqlNativeQuery));
        }
        boolean hasNamedParameter = sqlNativeQuery.contains(":"), hasPlaceholder = sqlNativeQuery.contains("?");
        //邏輯：如果同時有":argName"和"?"，噴錯；如果只有":argName"，則為namedParameterSqlQuery；其餘視為placeholder(包含沒有查詢參數的語句，即hasNamedParameter和hasPlaceholder均為false)
        if (hasNamedParameter && hasPlaceholder) {
            throw new IllegalArgumentException(String.format("SQL語句佔位符有問題：{}", sqlNativeQuery));
        }
        return hasNamedParameter;
    }
}
