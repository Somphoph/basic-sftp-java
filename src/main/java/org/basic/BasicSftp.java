package org.basic;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Vector;


public class BasicSftp {
    private static final Logger log = LoggerFactory.getLogger(BasicSftp.class);

    public void putFile(SFTPConfig conf, String localFilePath, String remoteFilePath) throws SftpException, JSchException {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            File remoteFile = new File(remoteFilePath);
            File remoteDir = remoteFile.getParentFile();

            String workingDir = sftpChannel.pwd();
            log.info("Working directory : {}", workingDir);

            log.info("Change directory to : {}", remoteDir.getPath());

            prepareDirectory(sftpChannel, remoteDir.getPath());

            File localFile = new File(localFilePath);
            File localDir = localFile.getParentFile();
            if (localDir.isDirectory()) {
                sftpChannel.lcd(localDir.getPath());
            }
            log.info("Upload file : {} to : {} ", localFilePath, remoteFile.getName());
            sftpChannel.put(localFilePath, remoteFile.getName());
        } finally {
            disconnect(session, sftpChannel);
        }
    }


    public void putFile(SFTPConfig conf, String localPath, String matchFile, String remotePath) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            String workingDir = sftpChannel.pwd();
            log.info("Working directory : {} ", workingDir);

            log.info("Change directory to : {} ", remotePath);
            prepareDirectory(sftpChannel, remotePath);

            sftpChannel.lcd(localPath);

            File[] files = getSourceFiles(localPath, matchFile);

            for (File file : files) {
                log.info("Upload file : {} to : {} ", file.getName(), file.getName());
                sftpChannel.put(file.getName(), file.getName());
            }
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    private void get(SFTPConfig conf, String remoteFile, String localFile) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            sftpChannel.get(remoteFile, localFile);
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    public void getFile(SFTPConfig conf, String remoteFile, String localPath) throws JSchException, SftpException {
        File remote = new File(remoteFile);
        String fileName = remote.getName();

        File localDir = new File(localPath);
        if (!localDir.isDirectory()) {
            throw new RuntimeException("Your localPath is not directory.");
        }

        String localFile = localDir.getPath() + File.separator + fileName;
        get(conf, remoteFile, localFile);
    }

    public void getFromDir(SFTPConfig conf, String remoteDir, String localDir) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();
            String exp = remoteDir + "/*";
            Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(exp);
            for (ChannelSftp.LsEntry entry : list) {
                String remoteFile = remoteDir + "/" + entry.getFilename();
                String localFile = localDir + File.separator + entry.getFilename();

                sftpChannel.get(remoteFile, localFile);
            }
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    public void remove(SFTPConfig conf, String remotePath) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();
            String exp = remotePath;
            sftpChannel.rm(exp);
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    private ConnectionObj connect(SFTPConfig conf) throws JSchException {
        JSch jsch = new JSch();
        if (conf.getPrivateKeyFile() != null && !conf.getPrivateKeyFile().trim().isEmpty()) {
            jsch.addIdentity(conf.getPrivateKeyFile());
        }
        Session session = jsch.getSession(conf.getUsr(), conf.getHost(), conf.getPort());

        session.setConfig("StrictHostKeyChecking", "no");
        if (conf.getPwd() != null && !"".equals(conf.getPwd())) {
            session.setPassword(conf.getPwd());
        }

        session.setTimeout(300000);
        log.info("Session connect...");
        session.connect();
        log.info("Session connected.");

        Channel channel = session.openChannel("sftp");

        log.info("Channel connect...");
        channel.connect();
        log.info("Channel connected.");

        ChannelSftp sftpChannel = (ChannelSftp) channel;
        ConnectionObj obj = new ConnectionObj();
        obj.setSession(session);
        obj.setSftpChannel(sftpChannel);
        return obj;
    }

    private void disconnect(Session session, ChannelSftp sftpChannel) {
        if (sftpChannel != null) {
            try {
                if (!sftpChannel.isClosed()) {
                    sftpChannel.exit();
                }
            } catch (Exception e) {
                log.error("Disconnect error on close channel.", e);
            }
        }

        if (session != null) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception e) {
                log.error("Disconnect error on disconnect session.", e);
            }
        }
    }

    private File[] getSourceFiles(String sourceDir, final String sourceFileName) {
        File dir = new File(sourceDir);
        if ((sourceFileName == null) || (sourceFileName.equals("*"))) {
            return dir.listFiles();
        }


        if (sourceFileName.contains("*")) {
            dir.listFiles((dir1, name) -> fileName(sourceFileName, name));
        }

        return new File[0];
    }

    private boolean fileName(String sourceFileName, String name) {
        boolean match = false;
        String[] matchs = sourceFileName.split(".*");

        int i = 0;
        for (int idx = 0; i < matchs.length; i++) {
            int fidx = name.indexOf(matchs[i], idx);
            if (fidx > -1) {
                match = true;
                idx = fidx + matchs[i].length();
            } else {
                match = false;
                break;
            }
        }
        return match;
    }

    private void prepareDirectory(ChannelSftp sftp, String directory) throws SftpException {
        directory = directory.replace("\\\\", "/");
        directory = directory.replace("\\", "/");
        directory = directory.replaceFirst(sftp.getHome(), "");

        String[] dirs = directory.split("/");

        for (String d : dirs) {
            if (d.equals("/") || d.equals("\\") || d.equals("")) {
                continue;
            }
            try {
                sftp.cd(d);
            } catch (Exception e) {
                log.info("Can't access directory : " + d + "", e);
                sftp.mkdir(d);
                sftp.cd(d);
            }
        }
    }
}

