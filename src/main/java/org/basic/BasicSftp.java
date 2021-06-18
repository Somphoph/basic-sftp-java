package org.basic;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;


public class BasicSftp {
    private static final Logger log = Logger.getLogger(BasicSftp.class);

    public void putFile(SFTPConfig conf, String localFilePath, String remoteFilePath)
            throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(session, sftpChannel, conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            File remoteFile = new File(remoteFilePath);
            File remoteDir = remoteFile.getParentFile();

            log.info("Working directory : " + sftpChannel.pwd());

            log.info("Change directory to : " + remoteDir.getPath());

            prepareDirectory(sftpChannel, remoteDir.getPath());

            File localFile = new File(localFilePath);
            File localDir = localFile.getParentFile();
            if (localDir.isDirectory()) {
                sftpChannel.lcd(localDir.getPath());
            }
            log.info("Upload file : " + localFilePath + " to : " + remoteFile.getName());
            sftpChannel.put(localFilePath, remoteFile.getName());
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            disconnect(session, sftpChannel);
        }
    }


    public void putFile(SFTPConfig conf, String localPath, String matchFile, String remotePath)
            throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(session, sftpChannel, conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            log.info("Working directory : " + sftpChannel.pwd());

            log.info("Change directory to : " + remotePath);
            prepareDirectory(sftpChannel, remotePath);

            sftpChannel.lcd(localPath);

            File[] files = getSourceFiles(localPath, matchFile);

            for (File file : files) {
                log.info("Upload file : " + file.getName() + " to : " + file.getName());
                sftpChannel.put(file.getName(), file.getName());
            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    private void get(SFTPConfig conf, String remoteFile, String localFile) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(session, sftpChannel, conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();

            sftpChannel.get(remoteFile, localFile);
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    public void getFile(SFTPConfig conf, String remoteFile, String localPath) throws Exception {
        File remote = new File(remoteFile);
        String fileName = remote.getName();

        File localDir = new File(localPath);
        if (!localDir.isDirectory()) {
            throw new RuntimeException("Your localPath is not directory.");
        }

        String localFile = localDir.getPath() + File.separator + fileName;
        get(conf, remoteFile, localFile);
    }

    public void getFromDir(SFTPConfig conf, String remoteDir, String localDir) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(session, sftpChannel, conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();
            String exp = remoteDir + "/*";
            Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(exp);
            for (ChannelSftp.LsEntry entry : list) {
                String remoteFile = remoteDir + "/" + entry.getFilename();
                String localFile = localDir + File.separator + entry.getFilename();

                sftpChannel.get(remoteFile, localFile);
            }
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    public void remove(SFTPConfig conf, String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            ConnectionObj connObj = connect(session, sftpChannel, conf);
            session = connObj.getSession();
            sftpChannel = connObj.getSftpChannel();
            String exp = remotePath;
            sftpChannel.rm(exp);
        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            disconnect(session, sftpChannel);
        }
    }

    private ConnectionObj connect(Session session, ChannelSftp sftpChannel, SFTPConfig conf) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(conf.getUsr(), conf.getHost(), conf.getPort());

        session.setConfig("StrictHostKeyChecking", "no");
        if ((conf.getPwd() != null) && (!"".equals(conf.getPwd()))) {
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

        sftpChannel = (ChannelSftp) channel;
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
                log.error(e);
            }
        }

        if (session != null) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private File[] getSourceFiles(String sourceDir, final String sourceFileName) {
        File dir = new File(sourceDir);
        if ((sourceFileName == null) || (sourceFileName.equals("*"))) {
            return dir.listFiles();
        }


        if (sourceFileName.indexOf("*") > -1) {
            dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
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
            });
        }

        return new File[0];
    }

    private void prepareDirectory(ChannelSftp sftp, String directory) throws Exception {
        File dir = new File(directory);
        List<String> dirs = new ArrayList();

        File tdir = dir;
        while (tdir.getParentFile() != null) {
            dirs.add(0, tdir.getParent().replaceAll("\\\\", "/"));
            tdir = tdir.getParentFile();
        }
        dirs.add(dir.getPath().replaceAll("\\\\", "/"));

        for (String d : dirs) {
            try {
                sftp.cd(d);
            } catch (Exception e) {
                log.error(e);
                log.info("Can't access directory : " + d + ".");
            }
            log.info("Working directory :" + sftp.pwd());
            if (!d.equals(sftp.pwd())) {
                log.info("Try to make destination directory...");
                File rdir = new File(d);
                log.info("Create directory : " + rdir.getName());
                sftp.mkdir(rdir.getName());
                log.info("Change working directory to :" + d);
                sftp.cd(d);
            }
        }
    }
}

