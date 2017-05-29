package com.example.zy;

/**
 * Created by ZengYu on 2017/5/29.
 */

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketException;

public class FtpUtil {
    private final static int BUFF_SIZE = 1024 * 1024;
    private String ipAddress = "", user = "", password = "", remotePath = "", localPath = "";
    private int port = 21, CONNECT_TIME_OUT = 60000, DATA_TIME_OUT = 60000;

    /**
     * 设置FTP客户端全局参数
     *
     * @param port 端口，默认为21
     * @param ipAddress IP地址
     * @param user 用戶名
     * @param password 密碼
     * @param remotePath 远程地址
     * @param localPath 本地地址
     *
     */
    public void SetParams(int port, String ipAddress, String user, String password, String remotePath,
                          String localPath) {
        this.port = port;
        this.ipAddress = ipAddress;
        this.user = user;
        this.password = password;
        this.remotePath = remotePath;
        this.localPath = localPath;
    }

    /**
     * 设置连接超时
     *
     * @param CONNECT_TIME_OUT 连接超时，默认为一分钟
     *
     */
    public void SetConnectTimeOut(int CONNECT_TIME_OUT) {
        this.CONNECT_TIME_OUT = CONNECT_TIME_OUT;
    }

    /**
     * 设置传输超时
     *
     * @param DATA_TIME_OUT 传输超时，默认为一分钟
     *
     */
    public void SetDataTimeOut(int DATA_TIME_OUT) {
        this.DATA_TIME_OUT = DATA_TIME_OUT;
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
            ftpClient.setConnectTimeout(CONNECT_TIME_OUT);
            ftpClient.setDataTimeout(DATA_TIME_OUT);
            ftpClient.setControlEncoding("GBK");
            return ftpClient;
        } catch (ConnectException e) {
            System.out.println("Connection timed out.");
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Fail to connect.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载接口
     */
    public void Download() {
        FTPClient ftpClient = CreateFTPClient();
        if (ftpClient != null)
            DownloadFileFromFtp(ftpClient, remotePath, localPath);
    }

    /**
     * 上传接口
     */
    public void Upload() {
        FTPClient ftpClient = CreateFTPClient();
        if (ftpClient != null)
            UploadFileFromFtp(ftpClient, remotePath, localPath);
    }

    private void DownloadFileFromFtp(FTPClient ftpClient, String remotepath, String localpath) {
        try {
            File localfile = new File(localpath);
            if (!localfile.exists()) {
                localfile.mkdirs();
                System.out.println("Make directory: " + localfile.getAbsolutePath());
            }
            if (!ftpClient.changeWorkingDirectory(remotepath)) {
                String parentpath = remotepath.substring(0, remotepath.lastIndexOf(File.separator));
                String filename = remotepath.substring(remotepath.lastIndexOf(File.separator) + 1);
                FTPClient parentClient = CreateFTPClient();
                parentClient.changeWorkingDirectory(parentpath);
                if (parentClient != null) {
                    FTPFile[] childFile = parentClient.listFiles();
                    for (FTPFile ftpFile : childFile) {
                        if (ftpFile.getName().equals(filename)) {
                            new DownloadHandle(filename, ftpFile.getSize(), parentpath, localpath).run();
                            break;
                        }
                    }
                }
            } else
                ScanFtpFileList(ftpClient, remotepath, localpath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isAvailable())
                    ftpClient.logout();
            } catch (IOException e1) {
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

    private void UploadFileFromFtp(FTPClient ftpClient, String remotepath, String localpath) {
        try {
            if (!ftpClient.changeWorkingDirectory(remotepath)) {
                System.out.println("WorkingDirectory doesn't exist.");
                ftpClient.makeDirectory(remotepath);
                ftpClient.changeWorkingDirectory(remotepath);
            }
            ScanLocalFileList(ftpClient, remotepath, localpath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isAvailable())
                    ftpClient.logout();
            } catch (IOException e1) {
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

    private void ScanFtpFileList(FTPClient ftpClient, String remotepath, String localpath) {
        try {
            FTPFile[] files = ftpClient.listFiles();
            if (files != null && files.length > 0) {
                System.out.println("Find " + files.length + " files.");
                for (FTPFile file : files) {
                    if (file.isFile()) {
                        new DownloadHandle(file.getName(), file.getSize(), remotepath, localpath).run();
                    } else if (file.isDirectory()) {
                        File localfile = new File(localpath + File.separator + file.getName());
                        if (!localfile.exists())
                            localfile.mkdirs();
                        DownloadFileFromFtp(ftpClient, remotepath + File.separator + file.getName(),
                                localfile.getAbsolutePath());
                    }
                }
            } else
                System.out.println("Empty directory.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ScanLocalFileList(FTPClient ftpClient, String remotepath, String localpath) {
        File localfile = new File(localpath);
        if (!localfile.exists()) {
            System.out.println("File doesn't exist.");
            return;
        }
        if (localfile.isFile()) {
            new UploadHandle(localfile.getName(), localfile.length(), remotepath, localpath).run();
        } else if (localfile.isDirectory()) {
            File[] files = new File(localpath).listFiles();
            if (files != null && files.length > 0) {
                System.out.println("Find " + files.length + " files.");
                for (File file : files) {
                    if (file.isFile()) {
                        new UploadHandle(file.getName(), file.length(), remotepath, file.getAbsolutePath()).run();
                    } else if (file.isDirectory()) {
                        UploadFileFromFtp(ftpClient, remotepath + File.separator + file.getName(),
                                file.getAbsolutePath());
                    }
                }
            } else
                System.out.println("No file found");
        }
    }

    private class DownloadHandle {
        private String name;
        private long size;
        private String localpath;
        private String remotepath;

        public DownloadHandle(String name, long size, String remotepath, String localpath) {
            this.name = name;
            this.size = size;
            this.localpath = localpath;
            this.remotepath = remotepath;
        }

        public void run() {
            FTPClient ftpClient = CreateFTPClient();
            if (ftpClient == null)
                return;
            boolean success = false;
            try {
                ftpClient.changeWorkingDirectory(remotepath);
                ftpClient.setBufferSize(BUFF_SIZE);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                File localfile = new File(localpath + File.separator + name);
                if (!localfile.exists()) {
                    try {
                        localfile.createNewFile();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else if (localfile.length() == this.size) {
                    System.out.println(localfile.getName() + " exists! ");
                    return;
                }
                System.out.println("Start to download: " + name);
                try {
                    OutputStream outputStream = new FileOutputStream(localfile);
                    success = ftpClient.retrieveFile(
                            new String(name.getBytes(System.getProperty("file.encoding")), "ISO-8859-1"), outputStream);
                    outputStream.close();
                    if (ftpClient.isAvailable())
                        ftpClient.logout();
                    if (ftpClient.isConnected()) {
                        try {
                            ftpClient.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (localfile.length() == this.size && success)
                    System.out.println("Download file successfully: " + localfile.getName());
                else
                    localfile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class UploadHandle {
        private String name;
        private long size;
        private String localpath;
        private String remotepath;

        public UploadHandle(String name, long size, String remotepath, String localpath) {
            this.name = name;
            this.size = size;
            this.localpath = localpath;
            this.remotepath = remotepath;
        }

        public void run() {
            FTPClient ftpClient = CreateFTPClient();
            if (ftpClient == null)
                return;
            boolean success = false;
            try {
                ftpClient.setBufferSize(BUFF_SIZE);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                if (!ftpClient.changeWorkingDirectory(remotepath)) {
                    if (!ftpClient.makeDirectory(remotepath)) {
                        System.out.println("Make ftp directory fail.");
                        return;
                    }
                    ftpClient.changeWorkingDirectory(remotepath);
                }
                System.out.println("Start to upload: " + name);
                InputStream inputStream = new FileInputStream(new File(localpath));
                success = ftpClient.storeFile(
                        new String(name.getBytes(System.getProperty("file.encoding")), "ISO-8859-1"), inputStream);
                inputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ftpClient.isAvailable())
                        ftpClient.logout();
                } catch (IOException e1) {
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
                    e.printStackTrace();
                }
            } else
                System.out.println("Upload successfully: " + name);
        }
    }
}
