package com.mychat;

import net.sf.json.JSONObject;

/**
 * 消息类
 * 
 * @author ccq
 *
 */
public class Message {
	
	private String command;  // 命令
	private String status;	 // 状态
	private String content;	 // 内容
	private String fromUserName;
	private String toUserName;
	
	public Message() {}
	
	public Message(String command, String status, String content, String fromUserName, String toUserName) {
		super();
		this.command = command;
		this.status = status;
		this.content = content;
		this.fromUserName = fromUserName;
		this.toUserName = toUserName;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public String getFromUserName() {
		return fromUserName;
	}

	public void setFromUserName(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public String getToUserName() {
		return toUserName;
	}

	public void setToUserName(String toUserName) {
		this.toUserName = toUserName;
	}

	public static void main(String[] args) {
		Message msg = new Message("login","success","你好", "张三", "李四");
		JSONObject object = JSONObject.fromObject(msg);
		
		Message bean = (Message) JSONObject.toBean(object, Message.class);
		System.out.println(bean.getCommand());
	}

	@Override
	public String toString() {
		return "Message [command=" + command + ", status=" + status + ", content=" + content + ", fromUserName="
				+ fromUserName + ", toUserName=" + toUserName + "]";
	}
	
	

}
