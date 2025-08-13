package org.dc.javatools.util;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.file.util.BaseFileSystem;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 此util邏輯僅針對低負載設計，預期全局僅使用單一client，且不考慮channel複用，僅複用ssh client，session複用則交由調用方決定；<br />
 * 為了節省資源，若是沒有正在連線的session，預設自動關閉client，可經由{@link #stopClientWhenIdle(boolean)}調整設定。<br />
 * {@link SftpUtil}.{@link SftpUtil#createConnection(String, int, String, String)}新建session時，自動實例化ssh client；<br />
 * 關閉session時務必呼叫{@link SftpUtil}.{@link #disconnect(ClientSession)}<br />
 * TODO 待處理，在undeploy時呼叫{@link #closeClient()}以避免leak
 */
public class SftpUtil {
	private static SshClient client;
	private static final Set<ClientSession> activeSessions = new HashSet<>();
	private static boolean wouldStopClientWhenIdle = true;
	private static final Logger log = LoggerFactory.getLogger(SftpUtil.class);

	private SftpUtil() {throw new IllegalStateException("Utility class");}

	private static synchronized void initClient() {
		if(isClientAlive()) {return;}
		startClient();
	}

	/**
	 * 設定閒置時(當沒有正在連線的session時)，關閉ssh client
	 * @param enableStop {@code true}啟用，{@code false}停用
	 */
	public static void stopClientWhenIdle(boolean enableStop) {
		wouldStopClientWhenIdle = enableStop;
	}

	/**
	 * 建立連線
	 * @param server remote server ip
	 * @param port
	 * @param user
	 * @param pwd
	 * @return 返回session，型別為{@code ClientSession}
	 * @throws IOException
	 */
	public static ClientSession createConnection(String server, int port, String user, String pwd) throws IOException {
		initClient();
		try{
			ClientSession session = client.connect(user, server, port).verify(5, TimeUnit.SECONDS).getSession();
			session.addPasswordIdentity(pwd);
			session.auth().verify();
			activeSessions.add(session);
			return session;
		}catch (IOException ex){
			log.error("SFTP連線錯誤，ip：{}:{}，帳號：{}", server, port, user);
			throw (IOException)ex.fillInStackTrace();
		}
	}

	/**
	 * 斷開連線
	 * @param session
	 */
	public static void disconnect(ClientSession session) {
		closeSession(session);
		cleanUpInactiveSessions();
		if(wouldStopClientWhenIdle) {stopClient();}
	}

	public static void uploadFile(String server, int port, String user, String pwd, String localFilePath, String remoteFilePath) throws IOException {
		uploadFile(server, port, user, pwd, Paths.get(localFilePath), Paths.get(remoteFilePath));
	}

	public static void downloadFile(String server, int port, String user, String pwd, String remoteFilePath, String localFilePath) throws IOException {
		downloadFile(server, port, user, pwd, Paths.get(remoteFilePath), Paths.get(localFilePath));
	}

	public static void uploadFile(String server, int port, String user, String pwd, Path localFilePath, Path remoteFilePath) throws IOException {
		copyFile(server, port, user, pwd, localFilePath, remoteFilePath, false);
	}

	public static void downloadFile(String server, int port, String user, String pwd, Path remoteFilePath, Path localFilePath) throws IOException {
		copyFile(server, port, user, pwd, remoteFilePath, localFilePath, true);
	}

	public static void uploadFile(ClientSession session, String localFilePath, String remoteFilePath) throws IOException {
		uploadFile(session, Paths.get(localFilePath), Paths.get(remoteFilePath));
	}

	public static void downloadFile(ClientSession session, String remoteFilePath, String localFilePath) throws IOException {
		downloadFile(session, Paths.get(remoteFilePath), Paths.get(localFilePath));
	}

	public static void uploadFile(ClientSession session, Path localFilePath, Path remoteFilePath) throws IOException {
		copyFile(session, localFilePath, remoteFilePath, false);
	}

	public static void downloadFile(ClientSession session, Path remoteFilePath, Path localFilePath) throws IOException {
		copyFile(session, remoteFilePath, localFilePath, true);
	}

	public static void uploadFiles(ClientSession session, List<Path> localFilePaths, List<Path> remoteFilePaths) throws IOException {
		copyFiles(session, localFilePaths, remoteFilePaths, false);
	}

	public static void downloadFiles(ClientSession session, List<Path> remoteFilePaths, List<Path> localFilePaths) throws IOException {
		copyFiles(session, localFilePaths, remoteFilePaths, true);
	}

	private static void copyFiles(ClientSession session, List<Path> sourceFilePaths, List<Path> destinationFilePaths, final boolean isSourceFromRemote) throws IOException {
		if(!isSessionAlive(session)) {throw new IOException(String.format("SFTP 檔案上傳錯誤，錯誤原因：%s", "session尚未實例化或已關閉"));}
		try(SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(session)) {
			copyFiles(sftpFileSystem, sourceFilePaths, destinationFilePaths, isSourceFromRemote);
		}
	}

	private static void copyFiles(BaseFileSystem<SftpPath> sftpFileSystem, List<Path> sourceFilePaths, List<Path> destinationFilePaths, final boolean isSourceFromRemote) throws IOException {
		if(sourceFilePaths==null || destinationFilePaths==null || sourceFilePaths.isEmpty() || destinationFilePaths.isEmpty() || sourceFilePaths.size()!=destinationFilePaths.size()) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < sourceFilePaths.size(); i++) {
			copyFile(sftpFileSystem, sourceFilePaths.get(i), destinationFilePaths.get(i), isSourceFromRemote);
		}
	}

	private static void copyFile(String server, int port, String user, String pwd, Path sourceFilePath, Path destinationFilePath, boolean isDownload) throws IOException {
		ClientSession session = null;
		try {
			copyFile(session=createConnection(server, port, user, pwd), sourceFilePath, destinationFilePath, isDownload);
		}finally {
			closeSession(session);
		}
	}

	/**
	 * upload和download抽象成來源檔案&目標檔案的通用邏輯；
	 * 下載呼叫{@code copyFile(session, sourceFilePath, destinationFilePath, true)}，
	 * 上傳呼叫{@code copyFile(session, sourceFilePath, destinationFilePath, false)}
	 * @param session
	 * @param sourceFilePath 來源檔案路徑
	 * @param destinationFilePath 目標檔案路徑
	 * @param isSourceFromRemote 下載：{@code true}，上傳：{@code false}；別名：{@code isDownload}
	 * @throws IOException
	 * @implNote {@link #copyFile(BaseFileSystem, Path, Path, boolean)}
	 * @see #copyFile(BaseFileSystem, Path, Path, boolean)
	 */
	private static void copyFile(ClientSession session, Path sourceFilePath, Path destinationFilePath, final boolean isSourceFromRemote) throws IOException {
		if(!isSessionAlive(session)) {throw new IOException(String.format("SFTP 檔案%s錯誤，錯誤原因：%s", isSourceFromRemote ? "下載" : "上傳", "session尚未實例化或已關閉"));}

		try(SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(session)) {
			copyFile(sftpFileSystem, sourceFilePath, destinationFilePath, isSourceFromRemote);
		}catch (IOException ex){
			log.error("SFTP 檔案{}錯誤，remote ip：{}，錯誤原因：{}", isSourceFromRemote ? "下載" : "上傳", session.getConnectAddress(), ex.getMessage(), ex);
			throw (IOException)ex.fillInStackTrace();
		}
	}

	/**
	 * 最底層實現來源檔案&目標檔案的複製通用邏輯
	 * 下載呼叫{@code copyFile(session, sourceFilePath, destinationFilePath, true)}
	 * 上傳呼叫{@code copyFile(session, sourceFilePath, destinationFilePath, false)}
	 * @param sftpFileSystem
	 * @param sourceFilePath 來源檔案路徑
	 * @param destinationFilePath 目標檔案路徑
	 * @param isSourceFromRemote 下載：{@code true}，上傳：{@code false}；別名：{@code isDownload}
	 * @throws IOException
	 * @implNote 利用sftpFileSystem抽換remoteFilePath(可能為sourceFilePath或destinationFilePath，由{@code isSourceFromRemote}決定)
	 */
	private static void copyFile(BaseFileSystem<SftpPath> sftpFileSystem, Path sourceFilePath, Path destinationFilePath, final boolean isSourceFromRemote) throws IOException {
		if(!isFileSystemAlive(sftpFileSystem)) {throw new IOException(String.format("SFTP 檔案上傳錯誤，錯誤原因：%s", "fileSystem尚未實例化或已關閉"));}
		if(sourceFilePath==null || destinationFilePath==null) {throw new IllegalArgumentException("路徑為空!!!!");}

		//準備參數，這邊同時存在兩個概念；一組為"來源&目標"，另一組為"本機&遠端"；TODO 處理getParent() return null的case
		//Path localFilePath = isSourceFromRemote ? destinationFilePath : sourceFilePath;
		Path remoteFilePath = isSourceFromRemote ? sourceFilePath : destinationFilePath;

		//將path的provider替換為org.apache.sshd.common.file.util.BasePath(附帶：若是遠端目錄為相對路徑，則能將路徑轉為絕對路徑)
		remoteFilePath = sftpFileSystem.getDefaultDir().resolve(remoteFilePath.toString()); //此處必須使用toString，利用傳入string以更換path的provider

		sourceFilePath = (isSourceFromRemote ? remoteFilePath : sourceFilePath).normalize();
		destinationFilePath = (isSourceFromRemote ? destinationFilePath : remoteFilePath).normalize();
		try {
			//若是目標目錄不存在，則建立目標目錄
			Files.createDirectories(destinationFilePath.getParent());    //TODO 處理getParent() return null的case
			//將檔案從來源複製到目標
			Files.copy(sourceFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
			//FIXME 擴展此底層功能&開口，將刪除source檔的開關boolean由外部傳入
			boolean removeSourceFile = true;
			if(removeSourceFile) {Files.delete(sourceFilePath);}
		}catch (IOException ex){
			log.error("SFTP 檔案{}錯誤，錯誤原因：{}", isSourceFromRemote ? "下載" : "上傳", ex.getMessage(), ex);
			throw (IOException)ex.fillInStackTrace();
		}
	}

	public static List<String> listRemoteFilesAsStrings(ClientSession session, String remoteDirPath) throws IOException {
		final List<Path> paths = listRemoteFiles(session, remoteDirPath);
		final List<String> stringOfPaths = paths.stream().map(Path::toString).collect(Collectors.toList());
		log.info("遠端目錄[{}]，檔案清單：{}", remoteDirPath, stringOfPaths);
		return stringOfPaths;
	}

	private static List<Path> listRemoteFiles(ClientSession session, String remoteDirPath) throws IOException {
		if(!isSessionAlive(session)) {throw new IOException("SFTP session 尚未連線或已關閉");}

		//建立 SFTP 檔案系統並讀取遠端目錄
		try (SftpFileSystem sftpFileSystem = SftpClientFactory.instance().createSftpFileSystem(session)) {
			Path remotePath = sftpFileSystem.getDefaultDir().resolve(remoteDirPath).normalize();    //解析遠端路徑

			//確認遠端目錄存在
			if(!Files.exists(remotePath) || !Files.isDirectory(remotePath)) {throw new IOException("遠端目錄不存在或不是目錄: " + remoteDirPath);}

			//遍歷目錄中的檔案並加入清單
			try (Stream<Path> pathStream = Files.list(remotePath)) {
				List<Path> paths = pathStream.collect(Collectors.toList());
				log.info("遠端目錄[{}]包含[{}]個檔案/資料夾", remoteDirPath, paths.size());
				return paths;
			}
		}
	}

	private static void startClient() {
		client = startClient(client);
	}

	private static SshClient startClient(SshClient client) {
		if(client!=null) {
			try {
				client.start();
				log.trace("啟動client");
				return client;
			}catch (IllegalStateException ex) {
				log.trace("", ex);
			}
		}
		client = SshClient.setUpDefaultClient();
		client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
		client.start();
		log.trace("實例化並啟動client");
		return client;
	}

	private static void stopClient() {
		if(activeSessions.isEmpty()) {stopClient(client);}
	}

	private static void stopClient(SshClient client) {
		if(!isClientAlive(client)) {return;}
		client.stop();
	}

	public static synchronized void closeClient() throws IOException {
		closeClient(client);
		client = null;
	}

	private static void closeClient(SshClient client) throws IOException {
		if(client!=null) {client.close();}
	}

	private static boolean isClientAlive() {
		return isClientAlive(client);
	}

	private static boolean isClientAlive(SshClient client) {
		return client!=null && client.isOpen();
	}

	private static void closeSession(ClientSession session) {
		if(session==null) {return;}
		try {
			session.close();
			log.info("SFTP連線斷開");
		} catch (IOException e) {
			log.error("SFTP連線斷開錯誤");
		}
		activeSessions.remove(session);
	}

	private static void closeFileSystem(FileSystem fileSystem) {
		if(fileSystem==null) {return;}
		try {
			fileSystem.close();
			log.info("channel關閉");
		} catch (IOException e) {
			log.error("channel關閉錯誤");
		}
	}

	private static void cleanUpInactiveSessions() {
		activeSessions.removeIf(session -> !isSessionAlive(session));
	}

	private static boolean isSessionAlive(ClientSession session) {
		return session!=null && session.isOpen();
	}

	private static boolean isFileSystemAlive(FileSystem fileSystem) {
		return fileSystem!=null && fileSystem.isOpen();
	}

	private static List<Path> toPaths(List<String> paths) {
		return paths.stream().map(Paths::get).collect(Collectors.toList());
	}

}
