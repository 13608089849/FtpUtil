package com.zy.ftputil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * 
 * @author zy 2017/5/19
 * 
 */
public class FtpUtil {
	private final static int BUFF_SIZE = 1024 * 1024;
	private String ipAddress = "", user = "", password = "", remotePath = "", localPath = "";
	private int port, CONNECTION_TIME_OUT = 10000;

	public void SetParams(int port, String ipAddress, String user, String password, String remotePath,
			String localPath) {
		this.port = port;
		this.ipAddress = ipAddress;
		this.user = user;
		this.password = password;
		this.remotePath = remotePath;
		this.localPath = localPath;
	}

	public void SetConnectionTimeOut(int CONNECTION_TIME_OUT) {
		this.CONNECTION_TIME_OUT = CONNECTION_TIME_OUT;
	}

	private FTPClient CreateFTPClient() {
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(ipAddress, port);
			ftpClient.login(user, password);
			int reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				System.out.println("Information error.");
				ftpClient.disconnect();
				return null;
			}
			ftpClient.setBufferSize(BUFF_SIZE);
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setConnectTimeout(CONNECTION_TIME_OUT);
			ftpClient.setControlEncoding("GBK");
			return ftpClient;
		} catch (ConnectException e) {
			// TODO: handle exception
			System.out.println("Connection timed out.");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Fail to connect.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void Download() {
		FTPClient ftpClient = CreateFTPClient();
		if (ftpClient != null)
			DownloadFileFromFtp(ftpClient);
	}

	public void Upload() {
		FTPClient ftpClient = CreateFTPClient();
		if (ftpClient != null)
			UploadFileFromFtp(ftpClient);
	}

	private void DownloadFileFromFtp(FTPClient ftpClient) {
		try {
			if (ftpClient.changeWorkingDirectory(remotePath)) {
				ScanFtpFileList(remotePath, ftpClient);
			} else {
				System.out.println("WorkingDirectory doesn't exist.");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				ftpClient.logout();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void UploadFileFromFtp(FTPClient ftpClient) {
		try {
			if (!ftpClient.changeWorkingDirectory(remotePath))
				System.out.println("WorkingDirectory doesn't exist.");
			ScanLocalFileList(localPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				ftpClient.logout();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void ScanFtpFileList(String path, FTPClient ftpClient) {
		try {
			FTPFile[] files = ftpClient.listFiles();
			if (files != null && files.length > 0) {
				System.out.println("Find " + files.length + " files.");
				for (FTPFile file : files) {
					if (file.isFile()) {
						if (file.getName().toString().toUpperCase().endsWith("ZIP")) {
							new DownloadThread(file.getName(), file.getSize()).start();
						}
					}
				}
			} else
				System.out.println("No file found");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ScanLocalFileList(String path) {
		File[] files = new File(path).listFiles();
		if (files != null && files.length > 0) {
			System.out.println("Find " + files.length + " files.");
			for (File file : files) {
				if (file.isFile()) {
					if (file.getName().toString().toUpperCase().endsWith("ZIP")) {
						new UploadThread(file.getAbsolutePath(), file.getName(), file.length()).start();
					}
				}
			}
		} else
			System.out.println("No file found");
	}

	private class DownloadThread extends Thread {
		private String name;
		private long size;

		public DownloadThread(String name, long size) {
			this.name = name;
			this.size = size;
		}

		@Override
		public void run() {
			FTPClient ftpClient = CreateFTPClient();
			if (ftpClient == null)
				return;
			try {
				ftpClient.changeWorkingDirectory(remotePath);
				ftpClient.setBufferSize(BUFF_SIZE);
				ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				String localpath = localPath + File.separator + name;
				File localfile = new File(localpath);
				if (!localfile.exists()) {
					try {
						localfile.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else if (localfile.length() == this.size) {
					System.out.println(localfile.getName() + " exists! ");
					return;
				}
				try {
					OutputStream outputStream = new FileOutputStream(localfile);
					ftpClient.retrieveFile(new String(name.getBytes(System.getProperty("file.encoding")), "ISO-8859-1"),
							outputStream);
					outputStream.close();
					ftpClient.logout();
					if (ftpClient.isConnected()) {
						try {
							ftpClient.disconnect();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (localfile.length() != this.size)
					localfile.delete();
				else
					System.out.println("Download file successfully: " + localfile.getName());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class UploadThread extends Thread {
		private String filePath;
		private String name;
		private long size;

		public UploadThread(String filePath, String name, long size) {
			this.filePath = filePath;
			this.name = name;
			this.size = size;
		}

		@Override
		public void run() {
			FTPClient ftpClient = CreateFTPClient();
			if (ftpClient == null)
				return;
			boolean success = false;
			try {
				ftpClient.setBufferSize(BUFF_SIZE);
				ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				InputStream inputStream = new FileInputStream(new File(filePath));
				if (!ftpClient.changeWorkingDirectory(remotePath)) {
					if (!ftpClient.makeDirectory(remotePath)) {
						inputStream.close();
						return;
					}
					ftpClient.changeWorkingDirectory(remotePath);
				}
				success = ftpClient.storeFile(
						new String(name.getBytes(System.getProperty("file.encoding")), "ISO-8859-1"), inputStream);
				inputStream.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					ftpClient.logout();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (ftpClient.isConnected()) {
					try {
						ftpClient.disconnect();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (!success) {
				try {
					ftpClient.deleteFile(name);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				System.out.println("Upload successfully: " + name);
		}
	}
}
