package org.basic;

import com.jcraft.jsch.ChannelSftp;

public class ConnectionObj {
    private com.jcraft.jsch.Session session;
    private ChannelSftp sftpChannel;

    public com.jcraft.jsch.Session getSession() {
        return this.session;
    }

    public void setSession(com.jcraft.jsch.Session session) {
        this.session = session;
    }

    public ChannelSftp getSftpChannel() {
        return this.sftpChannel;
    }

    public void setSftpChannel(ChannelSftp sftpChannel) {
        this.sftpChannel = sftpChannel;
    }
}
