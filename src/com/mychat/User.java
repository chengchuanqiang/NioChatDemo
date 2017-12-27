package com.mychat;

import java.nio.channels.SocketChannel;

/**
 * 在线用户类
 * @author ccq
 *
 */
public class User {
	
	private String userName;
	private SocketChannel socketChannel;
	
	public User(String userName, SocketChannel socketChannel) {
		this.userName = userName;
		this.socketChannel = socketChannel;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

}
