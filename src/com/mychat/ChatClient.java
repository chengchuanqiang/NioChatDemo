package com.mychat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;

/**
 * 客户端
 * 
 * 使用nio实现一个即时聊天系统
 * （1）用户登录
 * （2）单聊和群聊
 * （3）可查看所有在线用户
 * @author ccq
 *
 */
public class ChatClient extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private static final String CONNECTION = "连接";
	private static final String DIS_CONNECTION = "断开连接";
	private static final String SEND = "发送";
	private static final String CLEAR_CONTENT = "清除历史";
	
	private static final String LOGIN_COMMAND = "login";
	private static final String CHAT_COMMAND = "chat";
	private static final String ALL_USER_COMMAND = "allUser";
	private static final String OFFLINE_USE_COMMAND = "offlineUser";
	private static final String ONLINE_USER_COMMAND = "onlineUser";
	private static final String ONLINE_USERLIST_COMMAND = "onlineUserList";
	private static final String SERVER_STOP = "serverStop";
	
	private static final String SEPARATOR = "\b\b";
	
	// 
	private JLabel userNameLabel;
	private JLabel passwordLabel;
	private JLabel ipLabel;
	private JLabel portLabel;
	
	// text Field
	private JTextField userNameField;
	private JPasswordField passwordField;
	private JTextField ipField;
	private JTextField portField;
	private JTextField chatContentField;
	
	// area
	private JList<String> friendList;
	private DefaultListModel<String> listModel;
	private JTextArea historyRecordArea;
	
	//buttons
	private JButton connectBtn;
	private JButton disConnectBtn;
	private JButton sendBtn;
	private JButton clearContentBtn;
	
	//
	private JPanel settingPanel;
	private JPanel chatPanel;
	private JPanel contentPanel;

	
	private String hostAddress;
	private int port;

	private Selector selector;

	private SocketChannel socketChannel;

	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
	
	//　当前用户名称
	private String userName;
	
	public ChatClient() {
		initComponents();
		setupListener();
	}
	
	private void initComponents() {
		
		/******************用户信息和连接配置*********************/
		settingPanel = new JPanel();
		settingPanel.setBorder(new TitledBorder("用户信息  & 连接配置"));
		settingPanel.setLayout(new GridLayout(2, 5, 5, 10));
		
		
		/******************配置信息设置*********************/
		userNameField = new JTextField("chengchuanqiang");
		passwordField = new JPasswordField("123");
		ipField = new JTextField("192.168.184.1");
		portField = new JTextField("9090");
		
		userNameLabel = new JLabel("用户名：");
		passwordLabel = new JLabel("密码：");
		ipLabel = new JLabel("服务端ip：");
		portLabel = new JLabel("服务端端口：");
		
		connectBtn = new JButton(CONNECTION);
		disConnectBtn = new JButton(DIS_CONNECTION);
		
		
		/******************将组件添加到配置中*********************/
		settingPanel.add(userNameLabel);
		settingPanel.add(userNameField);
		settingPanel.add(passwordLabel);
		settingPanel.add(passwordField);
		settingPanel.add(connectBtn);
		settingPanel.add(ipLabel);
		settingPanel.add(ipField);
		settingPanel.add(portLabel);
		settingPanel.add(portField);
		settingPanel.add(disConnectBtn);
		
		
		/******************左边的在线用户*********************/
		listModel = new DefaultListModel<String>();
		friendList = new JList<String>(listModel);
		JScrollPane leftScroll = new JScrollPane(friendList);
		leftScroll.setBorder(new TitledBorder("在线用户"));
		
		
		/******************右边的历史消息显示和发送消息*********************/
		chatPanel = new JPanel(new BorderLayout());
		
		contentPanel = new JPanel(new BorderLayout());
		chatContentField = new JTextField();
		sendBtn = new JButton(SEND);
		clearContentBtn = new JButton(CLEAR_CONTENT);
		contentPanel.add(chatContentField, BorderLayout.CENTER);
		
		JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		btnPanel.add(sendBtn);
		btnPanel.add(clearContentBtn);
		
		
		contentPanel.add(btnPanel, BorderLayout.EAST);
		
		contentPanel.setBorder(new TitledBorder("发送消息"));

		historyRecordArea = new JTextArea();
		historyRecordArea.setForeground(Color.blue);
		historyRecordArea.setEditable(false);
		
		chatPanel.add(historyRecordArea,BorderLayout.CENTER);
		chatPanel.add(contentPanel, BorderLayout.SOUTH);
		
		JScrollPane rightScroll = new JScrollPane(chatPanel);
		rightScroll.setBorder(new TitledBorder("消息显示区"));
		
		
		/******************设置左右显示定位*********************/
		JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,rightScroll);
		centerSplit.setDividerLocation(100);
		
		
		/******************设置主体定位*********************/
		getContentPane().add(settingPanel,BorderLayout.NORTH);
		getContentPane().add(centerSplit,BorderLayout.CENTER);
		
		/******************初始化按钮和文本框状态*********************/
		initBtnAndTextConnect();
		
		/******************设置窗体大小和居中显示*********************/
		this.setTitle("客户机");
		this.setSize(800, 500);
		this.setLocationRelativeTo(this.getOwner());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		
	}
	/**
	 * 设置按钮的监听事件
	 */
	private void setupListener() {
		connectBtn.addActionListener(this);
		disConnectBtn.addActionListener(this);
		sendBtn.addActionListener(this);
		clearContentBtn.addActionListener(this);
		// 发送消息的文本框回车事件
		chatContentField.addActionListener(this);
		
	}
	// 用于监听按钮的点击事件
	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
				
		//System.out.println(e.getSource() == chatContentField);
		
	
		if(CONNECTION.equals(actionCommand)) {
			// 登录事件
			String userName = userNameField.getText();
			String password = new String(passwordField.getPassword());
			String serverIp = ipField.getText();
			String portStr = portField.getText();
			
			if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password) ||
					StringUtils.isEmpty(serverIp) || StringUtils.isEmpty(portStr)) {
				JOptionPane.showMessageDialog(this, "请输入用户名、密码、服务器ip和端口号!");
				return;
			}
			setTitle("用户 - " + userName);
			
			// 初始化连接信息
			initConnection(serverIp,Integer.parseInt(portStr));
			connect();
			
			Message message = new Message();
			message.setCommand(LOGIN_COMMAND);
			message.setFromUserName(userName);
			message.setContent(password);
			try {
				this.sendMessage(message);
			} catch (IOException e1) {
				System.out.println("发送消息失败！");
				e1.printStackTrace();
			}
			
		}else if(DIS_CONNECTION.equals(actionCommand)) {
			// 断开连接
			try {
				this.selector.close();
				this.socketChannel.close();
				
				System.out.println("断开连接");
				historyRecordArea.append(formatMessage("您已成功下线！"));
				
				initBtnAndTextConnect();
				
				this.setTitle("客户机");
				System.out.println("断开连接");
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			
		}else if(SEND.equals(actionCommand) || e.getSource() == chatContentField) {
			//发送按钮和文本框回车事件
			String message = chatContentField.getText();
			chatContentField.setText("");
			
			if (message == null || message.equals("")) {
				JOptionPane.showMessageDialog(this, "消息不能为空！", "错误",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			String toUserName = this.getSelectedUser();
			System.out.println(toUserName);
			
			if(toUserName.equals(ALL_USER_COMMAND)) {
				historyRecordArea.append(formatMessage("对所有人说：" + message));
			}else {
				historyRecordArea.append(formatMessage("对 " + toUserName + " 说：" + message));
			}
			
			try {
				this.sendMsgToUser(toUserName,message);
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, "服务端异常" + e1.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
				shutdownConnect();
				//e1.printStackTrace();
			}
			
		}else if(CLEAR_CONTENT.equals(actionCommand)) {
			// 清空历史记录
			historyRecordArea.setText("");
		}
	}
	
	//　初始化页面按钮设置
	public void initBtnAndTextConnect() {
		connectBtn.setEnabled(true);
		userNameField.setEnabled(true);
		passwordField.setEnabled(true);
		ipField.setEnabled(true);
		portField.setEnabled(true);
		
		disConnectBtn.setEnabled(false);
		sendBtn.setEnabled(false);
		//clearContentBtn.setEnabled(false);
		chatContentField.setEnabled(false);
	}
	
	// 连接成功按钮设置
	public void showBtnAndTextConnectSuccess() {
		
		disConnectBtn.setEnabled(true);
		sendBtn.setEnabled(true);
		//clearContentBtn.setEnabled(true);
		chatContentField.setEnabled(true);
		
		connectBtn.setEnabled(false);
		userNameField.setEnabled(false);
		passwordField.setEnabled(false);
		ipField.setEnabled(false);
		portField.setEnabled(false);
	}
	
	
	// 获取选中的人员
	public String getSelectedUser() {
		int index = friendList.getSelectedIndex();
		if(index >= 0) {
			String userName = friendList.getSelectedValue();
			return userName;
		}
		return ALL_USER_COMMAND;
	}
	
	public void initConnection(String hostAddress, int port) {
		this.hostAddress = hostAddress;
		this.port = port;
	}
	
	// 连接服务器
	public void connect() {
		try {
			this.selector = Selector.open();
			socketChannel = SocketChannel.open();
			
			boolean connect = socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));
			socketChannel.configureBlocking(false);
			
			System.out.println("connect　＝　"+connect);
			socketChannel.register(selector, SelectionKey.OP_READ);
			
			historyRecordArea.append(formatMessage("本地连接参数：" + socketChannel.getLocalAddress()));
			
			historyRecordArea.append(formatMessage("您已经成功连接服务器 ip:" + hostAddress + " 端口:"+port));
			
		} catch (ClosedChannelException e) {
			historyRecordArea.append(formatMessage("====服务器连接失败!===" + e.getMessage()));
			e.printStackTrace();
		} catch (IOException e) {
			historyRecordArea.append(formatMessage("服务器连接失败!" + e.getMessage()));
			e.printStackTrace();
		}
		
		ClientThread clientThread = new ClientThread();
		//　设置客户端线程为守护线程
		clientThread.setDaemon(true);
	    clientThread.start();
	    
	}
	
	// 客户端线程，用于监听事件
	class ClientThread extends Thread{
		
		@Override
		public void run() {
			try {
				while(selector.select()>0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();
					
					while(iterator.hasNext()) {
						SelectionKey key = iterator.next();
						if(key.isReadable()) {
							read(key);
						}
						iterator.remove();
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// 读事件
	private void read(SelectionKey key) throws IOException {
		
		SocketChannel socketChannel = (SocketChannel) key.channel();
		this.readBuffer.clear();
		
		int len;
		try {
			len = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();
			return;
		}
		System.out.println("收到字符串长度 len = " + len);
		if(len == -1) {
			key.channel().close();
			key.cancel();
			return;
		}
		
		String msg = new String(this.readBuffer.array(),0,len);
		
		Message message = (Message) JSONObject.toBean(JSONObject.fromObject(msg), Message.class);
		
		String command = message.getCommand();
		String fromUserName = message.getFromUserName();
		String content = message.getContent();
		String toUserName = message.getToUserName();
		String status = message.getStatus();
		
		// 逻辑处理
		switch(command) {
			case LOGIN_COMMAND:
				
				if("MSG_SUCCESS".equals(status)) {
					this.userName = fromUserName;
					showBtnAndTextConnectSuccess();
					historyRecordArea.append(formatMessage("您已成功上线！"));
					// 获取在线用户列表
					this.findOnlineList();
					
				}else if("MSG_PWD_ERROR".equals(status)){
					
					JOptionPane.showMessageDialog(this, content, "错误", JOptionPane.ERROR_MESSAGE);
					this.selector.close();
					this.socketChannel.close();
					historyRecordArea.append(formatMessage("登录失败，" + content));
					
				} else if("MSG_REPEAT".equals(status)){
					
					JOptionPane.showMessageDialog(this, content, "错误", JOptionPane.ERROR_MESSAGE);
					this.selector.close();
					this.socketChannel.close();
					historyRecordArea.append(formatMessage("登录失败，" + content));
				} 
				break;
			case CHAT_COMMAND:
				if("MSG_SUCCESS".equals(status)) {
					if(StringUtils.isNotEmpty(toUserName) && ALL_USER_COMMAND.equals(toUserName)) {
						
						historyRecordArea.append(formatMessage(fromUserName + "对所有人说：" + content));
					}else {
						
						historyRecordArea.append(formatMessage(fromUserName + "说：" + content));
					}
				}else {

					historyRecordArea.setDisabledTextColor(Color.BLACK);
					historyRecordArea.append(formatMessage("失败消息###发送给"+ toUserName+ " ：" + content));
				}
				break;
			case ONLINE_USER_COMMAND:

				historyRecordArea.append(formatMessage(fromUserName + "上线了！"));
				listModel.addElement(fromUserName);
				break;
			case OFFLINE_USE_COMMAND:

				historyRecordArea.append(formatMessage(fromUserName + "下线了！"));
				listModel.removeElement(fromUserName);
			case ONLINE_USERLIST_COMMAND:
				
				String[] userNames = content.split("#");
				System.out.println(userNames.length + "==============在线人数================");
				for(int i=0; i<userNames.length; i++) {
					if(!userNames[i].equals(this.userName)) {
						listModel.addElement(userNames[i]);
					}
				}
				break;
			case SERVER_STOP:
				shutdownConnect();
		}
		key.interestOps(SelectionKey.OP_READ);
	}
	
	// 发送数据
	private void sendMessage(Message message) throws IOException {
		JSONObject msg = JSONObject.fromObject(message);
		if(socketChannel != null && msg != null) {
			byte[] val = msg.toString().getBytes();
			socketChannel.write(ByteBuffer.wrap(val));
			//historyRecordArea.append(formatMessage("发送数据包：" + msg.toString()));
		}
	}
	
	// 聊天发送
	private void sendMsgToUser(String toUserName, String msg) throws IOException {
		Message message = new Message();
		message.setCommand(CHAT_COMMAND);
		message.setContent(msg);
		message.setFromUserName(this.userName);
		message.setToUserName(toUserName);
		sendMessage(message);
	}
	
	// 查询在线人数
	private void findOnlineList() throws IOException {
		Message message = new Message();
		message.setCommand(ONLINE_USERLIST_COMMAND);
		message.setFromUserName(this.userName);
		sendMessage(message);
	}
	
	//	主动断开客户端连接
	public void shutdownConnect() {
		try {
			historyRecordArea.append(formatMessage("服务器关闭！"));
			
			listModel.clear();
			this.selector.close();
			this.socketChannel.close();

			historyRecordArea.append(formatMessage("您已被强迫下线！"));
			
			initBtnAndTextConnect();
			
			this.setTitle("客户机");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String formatMessage(String connect) {
		return String.format(DateUtils.getCurrentDate(new Date())+ SEPARATOR + "%s\n", connect);
	}
	
	public static void main(String[] args) {
		new ChatClient();
	}
}
