package org.dc.javatools.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilePath {
	@Value("${storagePath}")
	private static String storagePath;
	@Value("${storageFtpPath}")
	private static String storageFtpPath;

	public static final String RESOURCES_PATH = storagePath;
	public static final String STORAGE_FTP_PATH = storageFtpPath;

}
