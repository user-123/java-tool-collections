package org.dc.javatools.util;

import jakarta.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.dc.javatools.exception.IllegalIdException;
import org.dc.javatools.util.validation.asserts.ObjectAssert;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ImageUtil {

    private ImageUtil() {throw new IllegalStateException("Utility class");}

    @Deprecated
    public static boolean resizeImageToJpg(String inputPath, String outputPath, int targetWidth, int targetHeight) {
        return resizeImage(inputPath, outputPath, targetWidth, targetHeight, "jpg");
    }

    public static boolean resizeImage(String inputPath, String outputPath, int targetWidth, int targetHeight) {
        String originalFormat = FileUtil.getFileNameExtension(FileUtil.readFile(inputPath));
        return !StringUtil.isNullOrBlank(originalFormat) && resizeImage(inputPath, outputPath, targetWidth, targetHeight, originalFormat);
    }

    public static boolean resizeImage(MultipartFile file, String outputPath, int targetWidth, int targetHeight) {
        String originalFormat = FileUtil.getFileNameExtension(file);
        return !StringUtil.isNullOrBlank(originalFormat) && resizeImage(file, outputPath, targetWidth, targetHeight, originalFormat);
    }

    public static boolean resizeImage(String inputPath, String outputPath, int targetWidth, int targetHeight, String imgFormat) {
        if(isIllegalResolution(targetWidth, targetHeight)) {return false;}
        //讀取檔案
        BufferedImage originalImage = readImage(inputPath);
        return resizeImage(originalImage, outputPath, targetWidth, targetHeight, imgFormat);
    }

    public static boolean resizeImage(MultipartFile file, String outputPath, int targetWidth, int targetHeight, String imgFormat) {
        if(isIllegalResolution(targetWidth, targetHeight)) {return false;}
        //讀取檔案
        BufferedImage originalImage = readImage(file);
        return resizeImage(originalImage, outputPath, targetWidth, targetHeight, imgFormat);
    }

    public static boolean resizeImage(BufferedImage originalImage, String outputPath, int targetWidth, int targetHeight, String imgFormat) {
        if(isNullOrEmpty(originalImage)) {return false;}
        //檢查原圖大小與目標大小
        if(isSameSize(originalImage, targetWidth, targetHeight)) {
            log.debug("圖片原始解析度與設定值一致，不進行resize");
            return true;
        }

        //渲染圖片
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D graphics = targetImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        boolean renderResult = graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        //檢查渲染程序是否成功啟動
        if(!renderResult) {
            log.error("啟動圖像繪製，{}", "啟動失敗");
            return false;
        }

        //寫入檔案
        boolean writeResult = false;
        try {
            writeResult = ImageIO.write(targetImage, imgFormat, Paths.get(outputPath).toFile());
            log.debug("寫入圖片檔案，{}", "寫入成功");
        } catch (IOException ex) {
            log.error("寫入圖片檔案，{}", "寫入錯誤", ex);
        }
        return writeResult;
    }

    public static BufferedImage readImage(MultipartFile multipartFile) {
        byte[] bytes = FileUtil.parseBytes(multipartFile);
        BufferedImage bufferedImage = null;
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            bufferedImage = ImageIO.read(inputStream);
            log.debug("讀取圖片檔案，{}", "讀取成功");
        } catch (IOException ex) {
            log.error("讀取圖片檔案，{}", "讀取失敗", ex);
        }
        return bufferedImage;
    }

    public static BufferedImage readImage(InputStream inputStream) {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(inputStream);
            log.debug("讀取圖片檔案，{}", "讀取成功");
        } catch (IOException ex) {
            log.error("讀取圖片檔案，{}", "讀取失敗", ex);
            log.debug("{}", new Object() {}.getClass().getEnclosingMethod().getName(), ex);
        }
        return bufferedImage;
    }

    public static BufferedImage readImage(String inputPath) {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(FileUtil.toFile(inputPath));
            log.debug("讀取圖片檔案，{}", "讀取成功");
        } catch (IOException ex) {
            log.error("讀取圖片檔案，{}", "讀取失敗", ex);
        }
        return bufferedImage;
    }

    private static boolean isNullOrEmpty(BufferedImage bufferedImage) {
        return bufferedImage==null || bufferedImage.getWidth()==0 || bufferedImage.getHeight()==0;
    }

    private static boolean isIllegalResolution(int width, int height) {
        return width<0 || height<0;
    }

    private static boolean isSameSize(BufferedImage image, int targetWidth, int targetHeight) {
        return image.getWidth()==targetWidth && image.getHeight()==targetHeight;
    }

    /**
     * @param file
     * @param width 單位 px
     * @param height 單位 px
     * @param type  0:等於  1:小於  2:大於
     * @return
     */
    @Deprecated(forRemoval = true)
    public static boolean checkImageRatio(MultipartFile file, Integer width, Integer height, int type) {
        COMPARISON_OPTION comparisonOption = switch (type) {
            case 0 -> COMPARISON_OPTION.EQUAL;
            case 1 -> COMPARISON_OPTION.LESSER;
            case 2 -> COMPARISON_OPTION.GREATER;
            default -> null;
        };
        return checkImageSize(file, width, height, comparisonOption);
    }

    public static boolean checkImageSize(MultipartFile file, int targetWidth, int targetHeight, COMPARISON_OPTION comparisonOption) {
        return checkImageSize(readImage(file), targetWidth, targetHeight, comparisonOption);
    }

    public static boolean checkImageSize(InputStream inputStream, int targetWidth, int targetHeight, COMPARISON_OPTION comparisonOption) {
        return checkImageSize(readImage(inputStream), targetWidth, targetHeight, comparisonOption);
    }

    @Getter
    @Accessors(fluent = true)
    @ToString
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public enum COMPARISON_OPTION {
        GREATER(1), EQUAL(0), LESSER(-1);
        private int option;



        private static final Map<Integer, COMPARISON_OPTION> codeToEnum = Map.copyOf(
                Arrays.stream(COMPARISON_OPTION.values())
                        .collect(Collectors.toMap(COMPARISON_OPTION::option, Function.identity()))
        );

        public static @Nonnull COMPARISON_OPTION fromCode(int code) {
            COMPARISON_OPTION comparisonOption = codeToEnum.get(code);
            ObjectAssert.isNotNull(comparisonOption, IllegalIdException.class, "錯誤的comparison option code: %s".formatted(code));
            return comparisonOption;
        }
    }

    public static boolean checkImageSize(BufferedImage image, int targetWidth, int targetHeight, COMPARISON_OPTION comparisonOption) {
        return !isNullOrEmpty(image) && !isIllegalResolution(targetWidth, targetHeight) && comparisonOption!=null && switch (comparisonOption) {
            case EQUAL -> isSameSize(image, targetWidth, targetHeight);
            case GREATER -> image.getWidth()>targetWidth && image.getHeight()>targetHeight;
            case LESSER -> image.getWidth()<targetWidth && image.getHeight()<targetHeight;
        };
    }

}
