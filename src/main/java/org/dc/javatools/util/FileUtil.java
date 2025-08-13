package org.dc.javatools.util;

import com.google.gson.Gson;
import org.dc.javatools.constant.FilePath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h2>※此class即將全面棄用，將依照新設計全面重寫</h2>
 * {@code FileUtil} 是一個專門處理檔案操作的工具類，提供了：
 * <ul>
 *     <li>MultipartFile 的上傳與寫入</li>
 *     <li>檔案名、路徑與副檔名操作</li>
 *     <li>檔案合法性檢查（白名單副檔名）</li>
 *     <li>將 MultipartFile 轉 byte array</li>
 *     <li>從 Spring {@link org.springframework.core.io.Resource} 解析 JSON 資料</li>
 * </ul>
 *
 * <h3>設計考量</h3>
 * <ul>
 *     <li>目前路徑與副檔名白名單寫死在程式中，未來計畫改為可透過 {@code system.properties} 或 {@code application.properties} 配置</li>
 *     <li>檔案寫入方法提供多檔案與單檔案版本，支持自動命名或自定義檔名</li>
 *     <li>提供工具方法解析檔案名稱、主檔名、延伸名，方便上層邏輯使用</li>
 *     <li>提供檢查空檔案列表與空檔案的方法，避免 NullPointerException</li>
 * </ul>
 *
 * <h3>預期重寫方向</h3>
 * <ul>
 *     <li>完全重寫檔案儲存策略，將硬編碼路徑、目錄及副檔名白名單改為可注入的策略或配置</li>
 *     <li>支援更多檔案檢查策略，例如檔案大小、MIME 類型、病毒掃描等</li>
 *     <li>將寫入檔案操作改為可異步或批次化，以提升大檔案或多檔案上傳效能</li>
 *     <li>增加統一異常處理與返回結果封裝，例如 {@code UploadResult} 物件，取代單一 boolean</li>
 *     <li>與 Cloud Storage、FTP 或分布式檔案系統整合，抽象檔案儲存接口</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * List<MultipartFile> files = ...;
 * String path = "uploads/images";
 *
 * // 上傳多檔案
 * List<Boolean> results = FileUtil.writeFiles(files, path);
 *
 * // 檢查單檔案合法性
 * boolean valid = FileUtil.checkFile(file, Arrays.asList("jpg", "png", "gif"));
 *
 * // 取得檔案副檔名
 * String extension = FileUtil.getFileNameExtension(file);
 * }</pre>
 *
 * <h3>執行緒安全性</h3>
 * <ul>
 *     <li>本類別方法多為 static 且不持有可變狀態，除了寫入檔案時的 IO 操作，因此對多執行緒讀操作安全</li>
 *     <li>寫入操作依賴外部檔案系統，同一目錄下同名檔案可能產生覆蓋，需要上層業務控制命名策略</li>
 * </ul>
 *
 * <h3>依賴</h3>
 * <ul>
 *     <li>Spring Web 的 {@link org.springframework.web.multipart.MultipartFile}</li>
 *     <li>Spring Core 的 {@link org.springframework.core.io.Resource}</li>
 *     <li>Gson {@link com.google.gson.Gson}</li>
 *     <li>Lombok {@code @Slf4j}</li>
 * </ul>
 *
 * <p>注意：此工具類目前為硬編碼路徑及白名單，重構後將改為可配置策略，以提升靈活性與可測試性。</p>
 *
 * @author Leo
 * @since 2025.08.14 03:13:56
 */
@Deprecated(forRemoval = false, since = "2025.08.14 03:13:56")
@Slf4j
public class FileUtil {
    private FileUtil() {throw new IllegalStateException("Utility class");}

    private static final Gson OBJECT_MAPPER = new Gson();

    public static <T> T parseResource(Resource resource, Class<T> valueType) throws IOException {
        return OBJECT_MAPPER.fromJson(new InputStreamReader(resource.getInputStream()), valueType);
    }





    /**
     *
     */
    private static final String RESOURCES_PATH = FilePath.RESOURCES_PATH;
    /**
     *
     */
    private static final String STORAGE_PATH = FilePath.STORAGE_FTP_PATH;
    /**
     * TODO 預計接收system.properties或application.properties
     */
    private static final String DIRECTORY_PATH = "PMBACKEND~LA~LA~LA~";
    /**
     * 副檔名白名單
     * TODO 改為傳入使用
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "svg", "gif", "webp");

    /**
     * 多檔案寫入，檔名會依照上傳原檔名自動命名
     * @param files 傳入List&lt;MultipartFile&gt;
     * @param path 自定義路徑
     * @return 個別檔案寫入結果List&lt;Boolean&gt，可依需求搭配List.contains()檢查或List.stream().allMatch(Boolean::booleanValue)返回單一boolean，也可使用for each自行檢查並統計成功/失敗數
     */
    public static List<Boolean> writeFiles(List<MultipartFile> files, String path) {
        //返回單一boolean value，short-circuiting for writeFile
		/*
		return !isNullOrEmpty(files)
				&& files.stream()
				.allMatch(file->writeFile(file, path));
		*/

        //返回單一boolean value
		/*
		return !isNullOrEmpty(files)
				&& files.stream()
				.map(file->writeFile(file, path))
				.allMatch(Boolean::booleanValue);
		return !isNullOrEmpty(files)
				&& files.stream()
				.map(file->writeFile(file, path))
				.collect(Collectors.toList())
				.stream().allMatch(Boolean::booleanValue);
		*/

        //返回List<Boolean>
        return isNullOrEmpty(files) ? Collections.emptyList() :
                files.stream()
                        .map(file->writeFile(file, path))
                        .collect(Collectors.toList());
    }

    /**
     * 多檔案寫入，檔名會依照傳入的fileNames自定義檔名清單命名
     * @param files 傳入List&lt;MultipartFile&gt;
     * @param path 自定義路徑
     * @param fileNames 自定義檔名清單List&lt;String&gt;
     * @return 個別檔案寫入結果List&lt;Boolean&gt;，可依需求搭配List.contains()檢查或List.stream().allMatch(Boolean::booleanValue)返回單一boolean，也可使用for each自行檢查並統計成功/失敗數
     */
    public static List<Boolean> writeFiles(List<MultipartFile> files, String path, List<String> fileNames) {
        //返回單一boolean value，short-circuiting for writeFile
		/*
		if(isNullOrEmpty(files) || fileNames==null || files.size()!=fileNames.size()) {return false;}
		return IntStream.range(0, files.size())
				.allMatch(i->writeFile(files.get(i), path, fileNames.get(i)));
		*/

        //返回單一boolean value
		/*
		if(isNullOrEmpty(files) || fileNames==null || files.size()!=fileNames.size()) {return false;}	//throw new IllegalArgumentException("檔案數量或檔案名稱數量不正確");
		return IntStream.range(0, files.size())
				.mapToObj(i->writeFile(files.get(i), path, fileNames.get(i)))
				.allMatch(Boolean::booleanValue);
		return IntStream.range(0, files.size())
				.mapToObj(i->writeFile(files.get(i), path, fileNames.get(i)))
				.collect(Collectors.toList())
				.stream().allMatch(Boolean::booleanValue);
		*/

        //返回List<Boolean>
        if(isNullOrEmpty(files) || fileNames==null || files.size()!=fileNames.size()) {return Collections.emptyList();}	//throw new IllegalArgumentException("檔案數量或檔案名稱數量不正確");
        return IntStream.range(0, files.size())
                .mapToObj(i->writeFile(files.get(i), path, fileNames.get(i)))
                .collect(Collectors.toList());
    }

    /**
     * 單檔案寫入，檔名會依照上傳原檔名自動命名
     * @param file 傳入MultipartFile
     * @param path 自定義路徑
     * @return 寫入結果boolean
     */
    public static boolean writeFile(MultipartFile file, String path) {
        return writeFile(file, path, getFileName(file));
    }

    /**
     * (待實作)單檔案寫入，檔名會依照傳入的fileName自定義檔名命名
     * @param file 傳入MultipartFile
     * @param path 自定義路徑
     * @param fileName 自定義檔名String
     * @return 寫入結果boolean
     */
    public static boolean writeFile(MultipartFile file, String path, String fileName) {
        //流程：檢查檔案→生成目錄→寫入檔案
        //檢查檔案、檔名
        if(isNullOrEmpty(file) || StringUtil.isNullOrBlank(fileName)) {return false;}
        //生成目錄
        //路徑組成規則：resource path+"/"+path+"/"+fileName
        log.info("storagePath: {}", STORAGE_PATH);
        log.info("resourcesPath: {}", RESOURCES_PATH);
        log.info("path: {}", path);
        log.info("fileName: {}", fileName);

        String fullPath = File.separator+StringUtil.trimSeparator(STORAGE_PATH)
                +File.separator+StringUtil.trimSeparator(RESOURCES_PATH)
                +File.separator+StringUtil.trimSeparator(path);
        //+File.separator+StringUtil.trimSeparator(fileName);
        Path uploadPath;
        try {
            uploadPath=generateFolders(fullPath);
        }catch (IOException ex) {
            log.warn("Exception occurred: {} -> {}", "創建目錄失敗", ex.getMessage());
            //return new UploadResultVo("上傳失敗：創建目錄失敗 (" + ex.getMessage() + ")");
            return false;
        }
        //寫入檔案
        return writeFile(file, uploadPath.resolve(fileName));
    }

    private static Path generateFolders(String path) throws IOException {
        //TODO 待修改路徑組成
        String uploadDirectory = STORAGE_PATH +path;	//String uploadDirectory = File.separator+"Storage"+File.separator+"PMBACKEND~LA~LA~LA~";
        Path uploadPath = Paths.get(uploadDirectory);
        if(!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    private static boolean writeFile(MultipartFile file, Path path) {
        if(true) {return true;}	//TODO 之後正式實裝的時候，把此行拿掉
        log.info("file寫入：{}，目標檔案：{}", getFileName(file), path);
        try {
            file.transferTo(path.toFile());
        } catch (IOException e) {
            log.warn("檔案寫入大失敗");
            return false;
        }

        return true;
    }



    private void inputFile() {}
    private static boolean outputFile(MultipartFile file, Path fullPath) {
        //TODO
        return true;
    }

    public static boolean checkFile(MultipartFile file, List<String> whiteList) {
        //TODO 實作其它檢查
        return checkFileName(file, whiteList);
    }

    private static boolean checkFileName(MultipartFile file, List<String> whiteList) {
        //TODO 實作其它檔名檢查
        return checkFileNameExtension(file, whiteList);
    }

    private static boolean checkFileNameExtension(MultipartFile file, List<String> whiteList) {
        String fileNameExtension = getFileNameExtension(file);
        if(StringUtil.isNullOrBlank(fileNameExtension) || fileNameExtension.contains(".") || fileNameExtension.contains("/") || fileNameExtension.contains("\\")) {return false;}	//簡易防呆，應該要改成更完整的規則
        return whiteList.contains(fileNameExtension);
    }

    /**
     * 將multipartFile轉換為byte stream
     * @param file MultipartFile
     * @return byte[]，如果file為空或轉換失敗，則返回空byte array
     */
    public static byte[] parseBytes(MultipartFile file) {
        byte[] bytes = new byte[0];
        if(isNullOrEmpty(file)) {return bytes;}
        try {
            bytes = file.getBytes();
            log.debug("multipartFile轉byte stream，{}", "轉換成功");
        } catch (IOException ex) {
            log.error("multipartFile轉byte stream，{}", "轉換失敗", ex);
        }
        return bytes;
    }

    public static File toFile(String path) {
        return readFile(path).toFile();
    }



    /**
     * 檢查檔案清單是否為null或空
     * @param files List&lt;MultipartFile&gt;
     * @return 只要list裡至少有一個非空檔案，回傳false
     */
    public static boolean isNullOrEmpty(List<MultipartFile> files) {
        return files==null || files.isEmpty() || files.stream().allMatch(FileUtil::isNullOrEmpty);
        //return files==null || files.isEmpty() || files.stream().allMatch(file->isNullOrEmpty(file));
        //return files==null || files.isEmpty() || files.stream().map(file->isNullOrEmpty(file)).allMatch(Boolean::booleanValue);
    }

    /**
     * 檢查檔案是否為null或空
     * @param file MultipartFile
     * @return 當file為非空檔案，回傳false
     */
    public static boolean isNullOrEmpty(MultipartFile file) {
        return file==null || file.isEmpty();
    }

    /**
     * 獲取上傳原檔路徑+檔名
     * @param file MultipartFile
     * @return 原檔路徑+檔名
     */
    public static String getOriginalFileName(MultipartFile file) {
        return isNullOrEmpty(file) ? "" : file.getOriginalFilename();
    }

    /**
     * 獲取上傳原檔檔名
     * @param file MultipartFile
     * @return 原檔檔名
     */
    public static String getFileName(MultipartFile file) {
        String originalFileName = getOriginalFileName(file);
        return originalFileName.substring(originalFileName.lastIndexOf(File.separator)+1);
    }

    /**
     * 獲取上傳原檔副檔名
     * @param file MultipartFile
     * @return 原檔副檔名
     */
    public static String getFileNameExtension(MultipartFile file) {
        String fileName = getFileName(file);
        return getFileNameExtension(fileName);
    }

    /**
     * 獲取上傳原檔主檔名
     * @param file MultipartFile
     * @return 原檔主檔名
     */
    public static String getFileNameWithoutExtension(MultipartFile file) {
        String fileName = getFileName(file);
        return getFileNameWithoutExtension(fileName);
    }





    public static Path readFile(String fullPath) {
        return Paths.get(fullPath);
    }

    public static String getFileName(Path path) {
        return path==null ? "" : path.getFileName().toString();
    }

    public static String getFileNameExtension(Path path) {
        String fileName = getFileName(path);
        return getFileNameExtension(fileName);
    }

    public static String getFileNameWithoutExtension(Path path) {
        String fileName = getFileName(path);
        return getFileNameWithoutExtension(fileName);
    }

    private static String getFileNameExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        return fileName.substring(lastIndexOfDot==-1 ? fileName.length() : fileName.lastIndexOf(".")+1);
    }

    private static String getFileNameWithoutExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        return fileName.substring(0, lastIndexOfDot==-1 ? fileName.length() : lastIndexOfDot);
    }

}
