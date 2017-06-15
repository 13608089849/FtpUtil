# FtpUtil
An util for ftp to download and upload file

Copying this jar file to your lib directory of project, and adding dependency.

----

## Example

**Download**

	FtpUtil ftpUtil = new FtpUtil():
	ftpUtil.setParams(21, "ip", "user", "password", "remotePath", "localPath");
	ftpUtil.setConnectTimeOut(20000);
	ftpUtil.setDataTimeOut(20000);
	ftpUtil.download();

**Upload**

	FtpUtil ftpUtil = new FtpUtil():
	ftpUtil.setParams(21, "ip", "user", "password", "remotePath", "localPath");
	ftpUtil.setConnectTimeOut(20000);
	ftpUtil.setDataTimeOut(20000);
	ftpUtil.upload();